LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:=/home/studi/Android/OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include $(OPENCVROOT)/sdk/native/jni/OpenCV.mk

LOCAL_MODULE     := jnilib

LOCAL_SRC_FILES  := jnilib.cpp Blob.cpp AdaptiveBackgroundLearning.cpp

LOCAL_CPP_FEATURES := rtti exceptions

include $(BUILD_SHARED_LIBRARY)
