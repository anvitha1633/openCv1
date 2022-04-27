#include "../blurdemo.hpp"

#include <cstring>
#include <jni.h>

extern "C" JNIEXPORT int JNICALL JNI_OnLoad(JavaVM* vm, void*)
{
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_skype_slimcv_blurdemo_MainActivity_nativeMain(JNIEnv* env, jobject thiz, jstring jargs)
{
    char* args = (char*)env->GetStringUTFChars(jargs, NULL);

    char TestName[] = "blurdemo";
    char* argv[256] { TestName };
    int argc = 1;
    if (args && *args) {
        argv[argc] = ::strtok(args, " ");
        while(argv[argc] != NULL) {
            argv[++argc] = ::strtok(NULL, " ");
        }
    }

    int result = 0;
    try {
        BlurDemo::create(argc, argv);
    }
    catch (const char* message) {
        std::cerr << "SlimBlur setup failed: " << message << std::endl;
        result = -1;
    }

    env->ReleaseStringUTFChars(jargs, args);
    return result;
}

extern "C" JNIEXPORT int JNICALL
Java_com_skype_slimcv_blurdemo_MainActivity_processFrame(JNIEnv* env, jobject thiz, jlong mat, jint gravity)
{
    auto& bd = BlurDemo::get();
    return bd.process(*(cv::Mat*)mat, nullptr, (SlimCV::SlimSeg::Gravity)gravity) ? 0 : -1;
}
