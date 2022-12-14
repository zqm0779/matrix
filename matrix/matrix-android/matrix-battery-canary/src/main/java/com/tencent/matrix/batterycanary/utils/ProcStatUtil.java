package com.tencent.matrix.batterycanary.utils;

import android.os.Process;
import android.text.TextUtils;

import com.tencent.matrix.util.MatrixLog;
import com.tencent.matrix.util.MatrixUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * see {@linkplain com.android.internal.os.ProcessCpuTracker}
 *
 * @author Kaede
 * @since 2020/11/6
 */
@SuppressWarnings({"JavadocReference", "SpellCheckingInspection"})
public final class ProcStatUtil {
    private static final String TAG = "Matrix.battery.ProcStatUtil";
    private static final ThreadLocal<byte[]> sBufferRef = new ThreadLocal<>();
    @Nullable
    private static OnParseError sParseError;

    static byte[] getLocalBuffers() {
        if (sBufferRef.get() == null) {
            sBufferRef.set(new byte[128]);
        }
        return sBufferRef.get();
    }

    ProcStatUtil() {
    }

    @Nullable
    public static ProcStat currentPid() {
        return of(Process.myPid());
    }

    @Nullable
    public static ProcStat current() {
        return of(Process.myPid(), Process.myTid());
    }

    @Nullable
    public static ProcStat of(int pid) {
        return parse("/proc/" + pid + "/stat");
    }

    @Nullable
    public static ProcStat of(int pid, int tid) {
        return parse("/proc/" + pid + "/task/" + tid + "/stat");
    }

    @Nullable
    public static ProcStat parse(String path) {
        try {
            ProcStat procStatInfo = null;
            try {
                // For bettery perf: 30% millis dec
                procStatInfo = BetterProcStatParser.parse(path, getLocalBuffers());
            } catch (ParseException e) {
                if (sParseError != null) {
                    sParseError.onError(3, e.content);
                }
                try {
                    procStatInfo = parseWithBufferForPath(path, getLocalBuffers());
                } catch (ParseException e2) {
                    if (sParseError != null) {
                        sParseError.onError(1, e2.content);
                    }
                }
            }

            if (procStatInfo == null || procStatInfo.comm == null) {
                MatrixLog.w(TAG, "#parseJiffies read with buffer fail, fallback with spilts");
                try {
                    procStatInfo = parseWithSplits(BatteryCanaryUtil.cat(path));
                } catch (ParseException e) {
                    if (sParseError != null) {
                        sParseError.onError(2, e.content);
                    }
                }
                if (procStatInfo == null || procStatInfo.comm == null) {
                    MatrixLog.w(TAG, "#parseJiffies read with splits fail");
                    return null;
                }
            }
            return procStatInfo;
        } catch (Throwable e) {
            MatrixLog.w(TAG, "#parseJiffies fail: " + e.getMessage());
            if (sParseError != null) {
                sParseError.onError(0, BatteryCanaryUtil.cat(path) + "\n" + e.getMessage());
            }
            return null;
        }
    }

    public static ProcStat parseWithBufferForPath(String path, byte[] buffer) throws ParseException {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        int readBytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            readBytes = fis.read(buffer);
        } catch (IOException e) {
            MatrixLog.printErrStackTrace(TAG, e, "read buffer from file fail");
            readBytes = -1;
        }
        if (readBytes <= 0) {
            return null;
        }

        return parseWithBuffer(buffer);
    }

    /**
     * Do NOT modfiy this method untlil all the test cases within {@link ProcStatUtilsTest} is passed.
     */
    @VisibleForTesting
    static ProcStat parseWithBuffer(byte[] statBuffer) throws ParseException {
        /*
         * ??????:
         * 10966 (terycanary.test) S 699 699 0 0 -1 1077952832 6187 0 0 0 22 2 0 0 20 0 17 0 9087400 5414273024
         *  24109 18446744073709551615 421814448128 421814472944 549131058960 0 0 0 4612 1 1073775864
         *  1 0 0 17 7 0 0 0 0 0 421814476800 421814478232 422247952384 549131060923 549131061022 549131061022
         *  549131063262 0
         *
         * ??????:
         * - pid:  ??????ID.
         * - comm: task_struct?????????????????????
         * - state: ????????????, ?????????S
         * - ppid: ?????????ID ????????????????????????fork??????, ??????clone??????????????????
         * - pgrp: ?????????ID
         * - session: ???????????????ID
         * - tty_nr: ???????????????tty???????????????
         * - tpgid: ????????????????????????????????????
         * - flags: ???????????????, ?????????include/linux/sched.h??????PF_*, ????????????1077952832
         * - minflt:  ???????????????????????????, ?????????????????????????????????. ??????COW????????????
         * - cminflt: ??????????????????????????????minflt
         * - majflt: ???????????????????????????, ??????????????????????????????. ??????map??????
         * - majflt: ??????????????????????????????majflt
         * - utime: ?????????????????????????????????, ??????jiffies, ????????????166114
         * - stime: ?????????????????????????????????, ??????jiffies, ????????????129684
         * - cutime: ??????????????????????????????utime
         * - cstime: ??????????????????????????????utime
         * - priority: ???????????????, ????????????10.
         * - nice: nice???, ????????????[19, -20], ????????????-10
         * - num_threads: ????????????, ????????????221
         * - itrealvalue: ??????????????????, ?????????0
         * - starttime: ???????????????????????????????????????, ??????jiffies, ????????????2284
         * - vsize: ???????????????????????????, ?????????bytes
         * - rss: ??????????????????+?????????, ??????pages, ????????????93087
         * - rsslim: rss????????????
         *
         * ??????:
         * ???10~17??????????????????????????????????????????
         * ??????????????????, sysconf(_SC_CLK_TCK)??????????????????jiffies(???????????????10ms)
         * starttime: ???????????????jiffies, ??????/proc/stat???btime, ??????????????????????????????????????????
         * 1500827856 + 2284/100 = 1500827856, ????????????????????????2017/7/24 0:37:58
         * ???????????????????????????,?????????????????????7???9???????????????:
         * signal: ????????????????????????, ?????????, ????????????6660
         * blocked: ???????????????, ?????????
         * sigignore: ??????????????????, ?????????, ????????????36088
         */

        ProcStat stat = new ProcStat();
        int statBytes = statBuffer.length;
        for (int i = 0, spaceIdx = 0; i < statBytes;) {
            if (Character.isSpaceChar(statBuffer[i])) {
                spaceIdx++;
                i++;
                continue;
            }

            switch (spaceIdx) {
                case 1: { // read comm (thread name)
                    int readIdx = i, window = 0;
                    // seek end symobl of comm: ')'
                    while (i < statBytes && ')' != statBuffer[i]) {
                        i++;
                        window++;
                    }
                    if ('(' == statBuffer[readIdx]) {
                        readIdx++;
                        window--;
                    }
                    if (')' == statBuffer[readIdx + window - 1]) {
                        window--;
                    }
                    if (window > 0) {
                        stat.comm = safeBytesToString(statBuffer, readIdx, window);
                    }
                    spaceIdx = 2;
                    break;
                }

                case 3: { // thread state
                    int readIdx = i, window = 0;
                    // seek next space
                    while (i < statBytes && !Character.isSpaceChar(statBuffer[i])) {
                        i++;
                        window++;
                    }
                    stat.stat = safeBytesToString(statBuffer, readIdx, window);
                    break;
                }

                case 14: { // utime
                    int readIdx = i, window = 0;
                    // seek next space
                    while (i < statBytes && !Character.isSpaceChar(statBuffer[i])) {
                        i++;
                        window++;
                    }
                    String num = safeBytesToString(statBuffer, readIdx, window);
                    if (!isNumeric(num)) {
                        throw new ParseException(safeBytesToString(statBuffer, 0, statBuffer.length) + "\nutime: " + num);
                    }
                    stat.utime = MatrixUtil.parseLong(num, 0);
                    break;
                }
                case 15: { // stime
                    int readIdx = i, window = 0;
                    // seek next space
                    while (i < statBytes && !Character.isSpaceChar(statBuffer[i])) {
                        i++;
                        window++;
                    }
                    String num = safeBytesToString(statBuffer, readIdx, window);
                    if (!isNumeric(num)) {
                        throw new ParseException(safeBytesToString(statBuffer, 0, statBuffer.length) + "\nstime: " + num);
                    }
                    stat.stime = MatrixUtil.parseLong(num, 0);
                    break;
                }
                case 16: { // cutime
                    int readIdx = i, window = 0;
                    // seek next space
                    while (i < statBytes && !Character.isSpaceChar(statBuffer[i])) {
                        i++;
                        window++;
                    }
                    String num = safeBytesToString(statBuffer, readIdx, window);
                    if (!isNumeric(num)) {
                        throw new ParseException(safeBytesToString(statBuffer, 0, statBuffer.length) + "\ncutime: " + num);
                    }
                    stat.cutime = MatrixUtil.parseLong(num, 0);
                    break;
                }
                case 17: { // cstime
                    int readIdx = i, window = 0;
                    // seek next space
                    while (i < statBytes && !Character.isSpaceChar(statBuffer[i])) {
                        i++;
                        window++;
                    }
                    String num = safeBytesToString(statBuffer, readIdx, window);
                    if (!isNumeric(num)) {
                        throw new ParseException(safeBytesToString(statBuffer, 0, statBuffer.length) + "\ncstime: " + num);
                    }
                    stat.cstime = MatrixUtil.parseLong(num, 0);
                    break;
                }

                default:
                    i++;
            }
        }
        return stat;
    }

    @VisibleForTesting
    static ProcStat parseWithSplits(String cat) throws ParseException {
        ProcStat stat = new ProcStat();
        if (!TextUtils.isEmpty(cat)) {
            int index = cat.indexOf(")");
            if (index <= 0) throw new IllegalStateException(cat + " has not ')'");
            String prefix = cat.substring(0, index);
            int indexBgn = prefix.indexOf("(") + "(".length();
            stat.comm = prefix.substring(indexBgn, index);

            String suffix = cat.substring(index + ")".length());
            String[] splits = suffix.split(" ");

            if (!isNumeric(splits[12])) {
                throw new ParseException(cat + "\nutime: " + splits[12]);
            }
            if (!isNumeric(splits[13])) {
                throw new ParseException(cat + "\nstime: " + splits[13]);
            }
            if (!isNumeric(splits[14])) {
                throw new ParseException(cat + "\ncutime: " + splits[14]);
            }
            if (!isNumeric(splits[15])) {
                throw new ParseException(cat + "\ncstime: " + splits[15]);
            }
            stat.stat = splits[1];
            stat.utime = MatrixUtil.parseLong(splits[12], 0);
            stat.stime = MatrixUtil.parseLong(splits[13], 0);
            stat.cutime = MatrixUtil.parseLong(splits[14], 0);
            stat.cstime = MatrixUtil.parseLong(splits[15], 0);
        }
        return stat;
    }

    @VisibleForTesting
    static String safeBytesToString(byte[] buffer, int offset, int length) {
        try {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(buffer, offset, length));
            return String.valueOf(charBuffer.array(), 0, charBuffer.limit());
        } catch (IndexOutOfBoundsException e) {
            MatrixLog.w(TAG, "#safeBytesToString failed: " + e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isNumeric(String text) {
        if (TextUtils.isEmpty(text)) return false;
        if (text.startsWith("-")) {
            // negative number
            return TextUtils.isDigitsOnly(text.substring(1));
        }
        return TextUtils.isDigitsOnly(text);
    }

    public static void setParseErrorListener(OnParseError parseError) {
        sParseError = parseError;
    }

    static final class BetterProcStatParser {
        private static final int PROC_USER_TIME_FIELD = 13;
        private static final ThreadLocal<ProcStatReader> sLocalReaders = new InheritableThreadLocal<>();

        static ProcStat parse(String path, byte[] buffer) throws ParseException {
            ProcStatReader reader =  new ProcStatReader(path, buffer);
            try {
                reader.reset();
                reader.skipLeftBrace();
                CharBuffer comm = reader.readToSymbol(')', CharBuffer.allocate(16));
                reader.skipSpaces();
                CharBuffer state = reader.readWord(CharBuffer.allocate(1));

                int index = 0;
                while (index < PROC_USER_TIME_FIELD - 2) {
                    reader.skipSpaces();
                    index++;
                }

                ProcStat stat = new ProcStat();
                stat.comm = String.valueOf(comm);
                stat.stat = String.valueOf(state);
                stat.utime = readJiffy(reader);
                stat.stime = readJiffy(reader);
                stat.cutime = readJiffy(reader);
                stat.cstime = readJiffy(reader);
                return stat;
            } catch (Exception e) {
                if (e instanceof ParseException) {
                    throw e;
                } else {
                    throw new ParseException("ProcStatReader error: " + e.getClass().getName() + ", " + e.getMessage());
                }
            } finally {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }

        private static long readJiffy(ProcStatReader reader) {
            long jiffies = reader.readNumber();
            reader.skipSpaces();
            return jiffies;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static class ProcStat {
        public String comm = "";
        public String stat = "_";
        public long utime = -1;
        public long stime = -1;
        public long cutime = -1;
        public long cstime = -1;

        public long getJiffies() {
            return utime + stime + cutime + cstime;
        }
    }

    public interface OnParseError {
        void onError(int mode, String input);
    }

    public static class ParseException extends Exception {
        public final String content;

        public ParseException(String content) {
            this.content = content;
        }
    }
}
