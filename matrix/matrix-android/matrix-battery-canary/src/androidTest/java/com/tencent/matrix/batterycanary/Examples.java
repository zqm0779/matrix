/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.matrix.batterycanary;

import android.app.Application;
import android.content.Context;

import com.tencent.matrix.Matrix;
import com.tencent.matrix.batterycanary.monitor.AppStats;
import com.tencent.matrix.batterycanary.monitor.BatteryMonitorConfig;
import com.tencent.matrix.batterycanary.monitor.BatteryMonitorCore;
import com.tencent.matrix.batterycanary.monitor.feature.AlarmMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.AppStatMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.BlueToothMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.CompositeMonitors;
import com.tencent.matrix.batterycanary.monitor.feature.CpuStatFeature;
import com.tencent.matrix.batterycanary.monitor.feature.DeviceStatMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.JiffiesMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.LocationMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.LooperTaskMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.MonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.NotificationMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.TrafficMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.WakeLockMonitorFeature;
import com.tencent.matrix.batterycanary.monitor.feature.WifiMonitorFeature;
import com.tencent.matrix.batterycanary.utils.Consumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


@RunWith(AndroidJUnit4.class)
public class Examples {
    static final String TAG = "Matrix.test.Examples";

    Context mContext;

    /**
     * ?????? Cpu Load
     */
    @Test
    public void exampleForCpuLoad() {
        if (TestUtils.isAssembleTest()) {
            return;
        } else {
            mockSetup();
        }

        if (Matrix.isInstalled()) {
            BatteryMonitorPlugin monitor = Matrix.with().getPluginByClass(BatteryMonitorPlugin.class);
            if (monitor != null) {
                CompositeMonitors compositor = new CompositeMonitors(monitor.core());
                compositor.metric(JiffiesMonitorFeature.JiffiesSnapshot.class);
                compositor.metric(CpuStatFeature.CpuStateSnapshot.class);
                compositor.start();

                doSomething();

                compositor.finish();
                int cpuLoad = compositor.getCpuLoad();
                Assert.assertTrue(cpuLoad > 0);
            }
        }
    }

    /**
     * CpuFreq ??????
     */
    @Test
    public void exampleForCpuFreqSampling() {
        if (TestUtils.isAssembleTest()) {
            return;
        } else {
            mockSetup();
        }

        if (Matrix.isInstalled()) {
            BatteryMonitorPlugin monitor = Matrix.with().getPluginByClass(BatteryMonitorPlugin.class);
            if (monitor != null) {
                CompositeMonitors compositor = new CompositeMonitors(monitor.core());
                compositor.sample(DeviceStatMonitorFeature.CpuFreqSnapshot.class, 10L);
                compositor.start();

                doSomething();

                compositor.finish();
                MonitorFeature.Snapshot.Sampler.Result result = compositor.getSamplingResult(DeviceStatMonitorFeature.CpuFreqSnapshot.class);
                Assert.assertNotNull(result);
                Assert.assertTrue(result.sampleAvg > 0);
            }
        }
    }

    /**
     * ??????????????????
     */
    @Test
    public void exampleForTemperatureSampling() {
        if (TestUtils.isAssembleTest()) {
            return;
        } else {
            mockSetup();
        }

        if (Matrix.isInstalled()) {
            BatteryMonitorPlugin monitor = Matrix.with().getPluginByClass(BatteryMonitorPlugin.class);
            if (monitor != null) {
                CompositeMonitors compositor = new CompositeMonitors(monitor.core());
                compositor.sample(DeviceStatMonitorFeature.BatteryTmpSnapshot.class, 10L);
                compositor.start();

                doSomething();

                compositor.finish();
                MonitorFeature.Snapshot.Sampler.Result result = compositor.getSamplingResult(DeviceStatMonitorFeature.BatteryTmpSnapshot.class);
                Assert.assertNotNull(result);
                Assert.assertTrue(result.sampleAvg > 0);
            }
        }
    }

    /**
     * ??????????????????
     */
    @Test
    public void exampleForGeneralUseCase() {
        if (TestUtils.isAssembleTest()) {
            return;
        } else {
            mockSetup();
        }

        if (Matrix.isInstalled()) {
            BatteryMonitorPlugin monitor = Matrix.with().getPluginByClass(BatteryMonitorPlugin.class);
            if (monitor != null) {
                // ???????????????
                CompositeMonitors compositor = new CompositeMonitors(monitor.core())
                        .metric(JiffiesMonitorFeature.JiffiesSnapshot.class)
                        .metric(CpuStatFeature.CpuStateSnapshot.class)
                        .sample(DeviceStatMonitorFeature.BatteryTmpSnapshot.class)
                        .sample(DeviceStatMonitorFeature.CpuFreqSnapshot.class, 10L);

                // ????????????
                compositor.start();

                doSomething();

                // ????????????
                compositor.finish();

                // ??????????????????:
                // 1. ?????? App & Dev ??????
                compositor.getAppStats(new Consumer<AppStats>() {
                    @Override
                    public void accept(AppStats appStats) {
                        if (appStats.isValid) {
                            long minute = appStats.getMinute();  // ????????????(??????)

                            int appStat = appStats.getAppStat();     // App ??????
                            int fgRatio = appStats.appFgRatio;       // ??????????????????
                            int bgRatio = appStats.appBgRatio;       // ??????????????????
                            int fgSrvRatio = appStats.appFgSrvRatio; // ????????????????????????
                            int floatRatio = appStats.appFloatRatio; // ??????????????????

                            int devStat = appStats.getDevStat();               // Device ??????
                            int unChargingRatio = appStats.devUnChargingRatio; // ???????????????????????????
                            int screenOff = appStats.devSceneOffRatio;         // ????????????????????????
                            int lowEnergyRatio = appStats.devLowEnergyRatio;   // ???????????????????????????
                            int chargingRatio = appStats.devChargingRatio;     // ????????????????????????

                            String scene = appStats.sceneTop1;         // Top1 Activity
                            int sceneRatio = appStats.sceneTop1Ratio;  // Top1 Activity ??????
                            String scene2 = appStats.sceneTop2;        // Top2 Activity
                            int scene2Ratio = appStats.sceneTop2Ratio; // Top2 Activity ??????

                            if (appStats.isForeground()) {
                                // ???????????? App ????????????
                            }
                            if (appStats.isCharging()) {
                                // ???????????? Dev ????????????
                            }
                        }
                    }
                });

                // 2. ??????????????????
                // 2.1 ????????????
                compositor.getSamplingResult(DeviceStatMonitorFeature.BatteryTmpSnapshot.class, new Consumer<MonitorFeature.Snapshot.Sampler.Result>() {
                    @Override
                    public void accept(MonitorFeature.Snapshot.Sampler.Result sampling) {
                        long duringMillis = sampling.duringMillis; // ????????????
                        long interval = sampling.interval   ;      // ????????????
                        int count = sampling.count;                // ????????????
                        double sampleFst = sampling.sampleFst;     // ???????????????
                        double sampleLst = sampling.sampleLst;     // ??????????????????
                        double sampleMax = sampling.sampleMax;     // ???????????????
                        double sampleMin = sampling.sampleMin;     // ???????????????
                        double sampleAvg = sampling.sampleAvg;     // ???????????????
                    }
                });
                // 2.3 CpuFreq
                compositor.getSamplingResult(DeviceStatMonitorFeature.BatteryTmpSnapshot.class, new Consumer<MonitorFeature.Snapshot.Sampler.Result>() {
                    @Override
                    public void accept(MonitorFeature.Snapshot.Sampler.Result sampling) {
                        long duringMillis = sampling.duringMillis; // ????????????
                        long interval = sampling.interval   ;      // ????????????
                        int count = sampling.count;                // ????????????
                        double sampleFst = sampling.sampleFst;     // ???????????????
                        double sampleLst = sampling.sampleLst;     // ??????????????????
                        double sampleMax = sampling.sampleMax;     // ???????????????
                        double sampleMin = sampling.sampleMin;     // ???????????????
                        double sampleAvg = sampling.sampleAvg;     // ???????????????
                    }
                });

                // 3. ?????? & ?????? Cpu Load
                // 3.1 ?????? Cpu Load, ????????? [0, Cpu Core Num * 100]
                final int procCpuLoad = compositor.getCpuLoad();
                // 3.2 ??????????????????
                compositor.getDelta(JiffiesMonitorFeature.JiffiesSnapshot.class, new Consumer<MonitorFeature.Snapshot.Delta<JiffiesMonitorFeature.JiffiesSnapshot>>() {
                    @Override
                    public void accept(MonitorFeature.Snapshot.Delta<JiffiesMonitorFeature.JiffiesSnapshot> procDelta) {
                        long totalJiffies = procDelta.dlt.totalJiffies.get();
                        for (JiffiesMonitorFeature.JiffiesSnapshot.ThreadJiffiesEntry threadEntry : procDelta.dlt.threadEntries.getList()) {
                            String name = threadEntry.name;   // ?????????
                            int tid = threadEntry.tid;        // tid
                            String status = threadEntry.stat; // ????????????
                            long jiffies = threadEntry.get(); // ??????????????????????????? Jiffies
                            int threadCpuLoad = (int) (procCpuLoad * ((float) jiffies / totalJiffies));
                        }
                    }
                });
            }
        }
    }

    private void doSomething() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {
        }
    }

    @Before
    public void setUp() {
        System.setProperty("org.mockito.android.target", ApplicationProvider.getApplicationContext().getCacheDir().getPath());
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (!Matrix.isInstalled()) {
            Matrix.init(new Matrix.Builder(((Application) mContext.getApplicationContext())).build());
        }
        if (!BatteryEventDelegate.isInit()) {
            BatteryEventDelegate.init((Application) mContext.getApplicationContext());
        }
    }

    @After
    public void shutDown() {
    }

    private void mockSetup() {
        final BatteryMonitorCore monitor = mockMonitor();
        BatteryMonitorPlugin plugin = new BatteryMonitorPlugin(monitor.getConfig());
        Matrix.with().getPlugins().add(plugin);
        monitor.enableForegroundLoopCheck(true);
        monitor.start();
    }

    private BatteryMonitorCore mockMonitor() {
        BatteryMonitorConfig config = new BatteryMonitorConfig.Builder()
                .enable(JiffiesMonitorFeature.class)
                .enable(LooperTaskMonitorFeature.class)
                .enable(WakeLockMonitorFeature.class)
                .enable(DeviceStatMonitorFeature.class)
                .enable(AlarmMonitorFeature.class)
                .enable(AppStatMonitorFeature.class)
                .enable(BlueToothMonitorFeature.class)
                .enable(WifiMonitorFeature.class)
                .enable(LocationMonitorFeature.class)
                .enable(TrafficMonitorFeature.class)
                .enable(NotificationMonitorFeature.class)
                .enable(CpuStatFeature.class)
                .enableBuiltinForegroundNotify(false)
                .enableForegroundMode(true)
                .wakelockTimeout(1000)
                .greyJiffiesTime(100)
                .foregroundLoopCheckTime(1000)
                .build();
        return new BatteryMonitorCore(config);
    }
}
