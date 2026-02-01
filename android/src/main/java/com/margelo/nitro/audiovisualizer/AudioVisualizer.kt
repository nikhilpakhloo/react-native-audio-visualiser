package com.margelo.nitro.audiovisualizer
  
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
class AudioVisualizer : HybridAudioVisualizerSpec() {
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }
}
