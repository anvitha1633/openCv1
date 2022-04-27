if (NOT CFW_TARGET_OS STREQUAL "android")
    message(FATAL_ERROR "This is Android-specific cmake")
endif()

if(OpenCV_V4)
    set(OpenCV_Native_Lib ${OpenCV_DIR}/sdk/native/libs/${CFW_ANDROID_ABI}/libopencv_java4.so)
else()
    set(OpenCV_Native_Lib ${OpenCV_DIR}/sdk/native/libs/${CFW_ANDROID_ABI}/libopencv_java3.so)
endif()
if (NOT EXISTS ${OpenCV_Native_Lib})
    message(WARNING "OpenCV lib not found: ${OpenCV_Native_Lib}")
endif()

# Build a native shared library with slimcv
add_library(blurdemo SHARED ${GENERIC_SOURCES} blurdemo_jni.cpp)
target_include_directories(blurdemo PRIVATE ${OpenCV_DIR}/sdk/native/jni/include)
target_link_libraries(blurdemo PRIVATE SlimCV skypert ${OpenCV_Native_Lib} ${CFW_ANDROID_STL_SHARED})
set_target_properties(blurdemo PROPERTIES CFW_CXX_RTTI ON CFW_CXX_EXCEPTIONS ON)

# Build BlurDemo APK
set(OPENCV_JAR_ROOT ${CMAKE_CURRENT_BINARY_DIR}/opencv_jar_pkgroot)
set(APKROOT ${CMAKE_CURRENT_BINARY_DIR}/blurdemo_apk_pkgroot)
add_custom_command(OUTPUT blurdemo.apk
    # Create project
    COMMAND ${CFW_ANDROID_SDK_TOOL} create lib-project --target `${CFW_ANDROID_SDK_TOOL} list targets | grep android-${CFW_ANDROID_SDK_LEVEL} | cut -d " " -f 2` --path ${OPENCV_JAR_ROOT} --package org.opencv

    # set compiler arguments
    #COMMAND echo java.compilerargs="${JAVAC_ARGS}" >> ${OPENCV_JAR_ROOT}/ant.properties

    # Copy OpenCV java files
    COMMAND mkdir -p ${OPENCV_JAR_ROOT}/src/org/opencv
    COMMAND mkdir -p ${OPENCV_JAR_ROOT}/res
    COMMAND cd ${OpenCV_DIR}/sdk/java/src/org/opencv && cp * -rf ${OPENCV_JAR_ROOT}/src/org/opencv
    COMMAND cd ${OpenCV_DIR}/sdk/java/res && cp * -rf ${OPENCV_JAR_ROOT}/res

    COMMAND cd ${OpenCV_DIR}/sdk/java && if [ -f lint.xml ]\; then cp lint.xml -rf ${OPENCV_JAR_ROOT}\; fi\;

    # Create project
    COMMAND ${CFW_ANDROID_SDK_TOOL} create project --target `${CFW_ANDROID_SDK_TOOL} list targets | grep android-${CFW_ANDROID_SDK_LEVEL} | cut -d " " -f 2` --name blurdemo --path ${APKROOT} --activity MainActivity --package com.skype.slimcv.blurdemo
    #echo "android.library.reference.1=${OPENCV_JAR_ROOT}" >> ${APKROOT}/project.properties
    #COMMAND ${CFW_ANDROID_SDK_TOOL} update project --target `${CFW_ANDROID_SDK_TOOL} list targets | grep android-${CFW_ANDROID_SDK_LEVEL} | cut -d " " -f 2` --path ${APKROOT} --library ../opencv_jar_pkgroot

    # set compiler arguments
    #COMMAND echo java.compilerargs="${JAVAC_ARGS}" >> ${APKROOT}/ant.properties

    # Copy BlurDemo java files
    COMMAND mkdir -p ${APKROOT}/src
    COMMAND mkdir -p ${APKROOT}/res
    COMMAND cd ${CMAKE_CURRENT_SOURCE_DIR}/java && cp * -rf ${APKROOT}/src
    COMMAND cd ${CMAKE_CURRENT_SOURCE_DIR}/res && cp * -rf ${APKROOT}/res
    COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/AndroidManifest.xml -f ${APKROOT}

    # Build OpenCV, move outputs to BlurDemo
    COMMAND cd ${OPENCV_JAR_ROOT} && ${CFW_ANT_PATH} release
    COMMAND rm -r ${APKROOT}/bin/classes || true
    COMMAND mv ${OPENCV_JAR_ROOT}/bin/classes ${APKROOT}/bin

    # Place skypert.jar
    COMMAND ln -sf ${ROOTTOOLS_LIBRARY_JAR} ${APKROOT}/libs

    # Move native libraries into APK
    COMMAND mkdir -p ${APKROOT}/libs/${CFW_ANDROID_ABI}
    COMMAND ln -sf $<TARGET_FILE:blurdemo> ${APKROOT}/libs/${CFW_ANDROID_ABI}
    COMMAND ln -sf $<TARGET_FILE:skypert> ${APKROOT}/libs/${CFW_ANDROID_ABI}
    COMMAND ln -sf ${OpenCV_Native_Lib} ${APKROOT}/libs/${CFW_ANDROID_ABI}
    COMMAND ln -sf ${CFW_ANDROID_STL_SHARED} ${APKROOT}/libs/${CFW_ANDROID_ABI}

    # Build BlurDemo
    COMMAND cd ${APKROOT} && ${CFW_ANT_PATH} debug

    # Move APK out of the project
    COMMAND mv ${APKROOT}/bin/blurdemo-debug.apk ${CMAKE_CURRENT_BINARY_DIR}/blurdemo.apk

    DEPENDS blurdemo
)

# Ensure APK gets built
add_custom_target(blurdemo_apk ALL DEPENDS blurdemo.apk)
