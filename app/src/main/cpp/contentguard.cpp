// Native protection primitives. The Android layer owns permissions and lifecycle;
// these small, allocation-conscious engines are shared by VPN/accessibility paths.
#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cctype>
#include <mutex>
#include <string>
#include <unordered_set>
#include <vector>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"ContentGuard",__VA_ARGS__)
namespace {
std::mutex lock; std::unordered_set<std::string> patterns={"porn","xxx","nsfw","sexual","adult","nudity","onlyfans","incognito","private browsing"};
std::string lower(const std::string&s){std::string x=s; for(char&c:x)c=(char)std::tolower((unsigned char)c); return x;}
}
extern "C" JNIEXPORT jboolean JNICALL Java_com_etnajid_appblocker_NativeGuard_matches(JNIEnv* env,jobject,jstring input){
 const char* raw=env->GetStringUTFChars(input,nullptr); std::string value=lower(raw?raw:""); env->ReleaseStringUTFChars(input,raw);
 std::lock_guard<std::mutex> g(lock); for(const auto&p:patterns) if(value.find(p)!=std::string::npos)return JNI_TRUE; return JNI_FALSE;
}
// DNS/packet engine: domain hashes permit opaque, fast membership checks.
extern "C" JNIEXPORT jlong JNICALL Java_com_etnajid_appblocker_NativeGuard_hashDomain(JNIEnv* e,jobject,jstring s){const char*x=e->GetStringUTFChars(s,0); std::hash<std::string>h; auto v=(jlong)h(x);e->ReleaseStringUTFChars(s,x);return v;}
// Keyword engine (Aho-Corasick-compatible API surface); patterns are replaceable at runtime.
extern "C" JNIEXPORT void JNICALL Java_com_etnajid_appblocker_NativeGuard_addPattern(JNIEnv*e,jobject,jstring s){const char*x=e->GetStringUTFChars(s,0);{std::lock_guard<std::mutex>g(lock);patterns.insert(lower(x));}e->ReleaseStringUTFChars(s,x);}
// Shared classifier boundary. A supplied TFLite model can replace this conservative hook.
// Returning false when no model is present is fail-safe and avoids accidental blackouts.
extern "C" JNIEXPORT jboolean JNICALL Java_com_etnajid_appblocker_NativeGuard_classifyFrame(JNIEnv*,jobject,jobject,jint,jint){ return JNI_FALSE; }
