package com.margelo.nitro.audiovisualizer

import android.graphics.Color
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class AudioVisualizerViewManager(
  private val reactContext: ReactApplicationContext
) : SimpleViewManager<AudioVisualizerNativeView>() {

  override fun getName() = "AudioVisualizerView"

  override fun createViewInstance(reactContext: ThemedReactContext): AudioVisualizerNativeView {
    return AudioVisualizerNativeView(reactContext)
  }

  @ReactProp(name = "type")
  fun setType(view: AudioVisualizerNativeView, type: String) {
    view.setType(type)
  }

  @ReactProp(name = "color")
  fun setColor(view: AudioVisualizerNativeView, color: String) {
    view.setColor(color)
  }

  @ReactProp(name = "sensitivity")
  fun setSensitivity(view: AudioVisualizerNativeView, sensitivity: Float) {
    view.setSensitivity(sensitivity)
  }

  @ReactProp(name = "smoothing")
  fun setSmoothing(view: AudioVisualizerNativeView, smoothing: Float) {
    view.setSmoothing(smoothing)
  }

  companion object {
    private var sharedView: AudioVisualizerNativeView? = null

    fun updateAmplitude(amplitude: Float) {
      sharedView?.updateAmplitude(amplitude)
    }

    fun updateFrequencyBands(bands: FloatArray) {
      sharedView?.updateFrequencyBands(bands)
    }

    fun setSharedView(view: AudioVisualizerNativeView?) {
      sharedView = view
    }
  }

  override fun onDropViewInstance(view: AudioVisualizerNativeView) {
    if (sharedView == view) {
      sharedView = null
    }
    super.onDropViewInstance(view)
  }

  override fun onAfterUpdateTransaction(view: AudioVisualizerNativeView) {
    super.onAfterUpdateTransaction(view)
    setSharedView(view)
  }
}
