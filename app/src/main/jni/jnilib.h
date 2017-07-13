
#include <jni.h>

#ifndef OPENCVNATIVETEST_JNILIB_H
#define OPENCVNATIVETEST_JNILIB_H
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
        Java_com_studi_timesyncwifi_Utility_OpenCVNative_function(JNIEnv *, jobject);

JNIEXPORT jboolean JNICALL
        Java_com_studi_timesyncwifi_Utility_OpenCVNative_cameraFrame(JNIEnv *, jobject
        , jlong addrRgbIn, jlong addrFgMask, jlong addrBgModel, jlong addrCalc);

#ifdef __cplusplus
}

#endif
#endif //HELLOANDROIDJNI_TESTLIB_JNI_H
