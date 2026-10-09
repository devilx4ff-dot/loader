#include <jni.h>
#include <android/log.h>

#define LOG_TAG "ThrowHook"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_top_niunaijun_blackbox_core_NativeCore_installThrowHook(JNIEnv *env, jclass clazz) {
    LOGD("installThrowHook disabled");
}