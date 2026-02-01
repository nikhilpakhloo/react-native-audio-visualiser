#include <jni.h>
#include "audiovisualizerOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return margelo::nitro::audiovisualizer::initialize(vm);
}
