// JNI shim between io.horizontalsystems.litecoinmwebkit.MwebNative and
// libmwebd.so (see external-libs/include/mwebd.h). Errors reported by the
// daemon library are rethrown as io.horizontalsystems.litecoinmwebkit.MwebNativeException.

#include <jni.h>
#include <string>

#include "mwebd.h"

namespace {

class Utf8 {
public:
    Utf8(JNIEnv *env, jstring value) : env_(env), value_(value), chars_(nullptr) {
        if (value_ != nullptr) {
            chars_ = env_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~Utf8() {
        if (chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }

    const char *get() const { return chars_ != nullptr ? chars_ : ""; }

private:
    JNIEnv *env_;
    jstring value_;
    const char *chars_;
};

void throwNativeException(JNIEnv *env, char *message) {
    std::string text = message != nullptr ? message : "unknown error";
    mweb_free(message);
    jclass cls = env->FindClass("io/horizontalsystems/litecoinmwebkit/MwebNativeException");
    if (cls != nullptr) {
        env->ThrowNew(cls, text.c_str());
    }
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL
Java_io_horizontalsystems_litecoinmwebkit_MwebNative_start(
        JNIEnv *env, jobject /* this */, jstring dataDir, jstring chain, jstring proxyUrl,
        jstring checkpointPath, jboolean blocksOnly) {
    Utf8 dataDirUtf8(env, dataDir);
    Utf8 chainUtf8(env, chain);
    Utf8 proxyUtf8(env, proxyUrl);
    Utf8 checkpointUtf8(env, checkpointPath);

    char *error = nullptr;
    int port = mweb_start(dataDirUtf8.get(), chainUtf8.get(), proxyUtf8.get(),
                          checkpointUtf8.get(), blocksOnly == JNI_TRUE ? 1 : 0, &error);
    if (port < 0) {
        throwNativeException(env, error);
        return -1;
    }
    return port;
}

JNIEXPORT void JNICALL
Java_io_horizontalsystems_litecoinmwebkit_MwebNative_stop(JNIEnv * /* env */, jobject /* this */) {
    mweb_stop();
}

JNIEXPORT jstring JNICALL
Java_io_horizontalsystems_litecoinmwebkit_MwebNative_addresses(
        JNIEnv *env, jobject /* this */, jstring scanSecretHex, jstring spendPubKeyHex,
        jstring chain, jint from, jint to) {
    Utf8 scanUtf8(env, scanSecretHex);
    Utf8 spendUtf8(env, spendPubKeyHex);
    Utf8 chainUtf8(env, chain);

    char *error = nullptr;
    char *result = mweb_addresses(scanUtf8.get(), spendUtf8.get(), chainUtf8.get(),
                                  static_cast<unsigned int>(from), static_cast<unsigned int>(to),
                                  &error);
    if (result == nullptr) {
        throwNativeException(env, error);
        return nullptr;
    }
    jstring value = env->NewStringUTF(result);
    mweb_free(result);
    return value;
}

} // extern "C"
