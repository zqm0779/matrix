# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

CMAKE_MINIMUM_REQUIRED(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

SET(TARGET test-openglhook)

SET(SOURCE_DIR src/main/cpp)

OPTION(EnableLOG "Enable QUT Logs" ON)
IF(EnableLOG)
    ADD_DEFINITIONS(-DEnableLOG)
ENDIF()

SET(
        SOURCE_FILES
        ${SOURCE_DIR}/test/OpenglHookTest.cpp
)

ADD_LIBRARY( # Specifies the name of the library.
        ${TARGET}

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SOURCE_FILES}
        )

TARGET_INCLUDE_DIRECTORIES(
        ${TARGET}
        PUBLIC ${SOURCE_DIR}/test
        PRIVATE ${EXT_DEP}/include
        PRIVATE ${EXT_DEP}/include/backtrace
        PRIVATE ${EXT_DEP}/include/backtrace/common
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

FIND_LIBRARY( # Sets the name of the path variable.
        log-lib
        log
)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

TARGET_LINK_LIBRARIES( # Specifies the target library.
        ${TARGET}
        # Links the target library to the log library
        # included in the NDK.
        PRIVATE ${log-lib}
        GLESv2
        GLESv3
        EGL
        PRIVATE -Wl,--gc-sections
        PRIVATE ${EXT_DEP}/lib/${ANDROID_ABI}/libwechatbacktrace.so
        PRIVATE ${EXT_DEP}/lib/${ANDROID_ABI}/libmatrix-hookcommon.so
        PRIVATE ${EXT_DEP}/lib/${ANDROID_ABI}/libmatrix-opengl-leak.so
)

TARGET_COMPILE_OPTIONS(
        ${TARGET}
        PRIVATE $<$<COMPILE_LANGUAGE:C>:-std=c99 -O0>
        PRIVATE $<$<COMPILE_LANGUAGE:CXX>:-std=c++17 -fno-exceptions -frtti -O0>
        PRIVATE -fstack-protector
)

