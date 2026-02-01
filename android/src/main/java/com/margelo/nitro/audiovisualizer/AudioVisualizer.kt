package com.margelo.nitro.audiovisualizer
  
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

@DoNotStrip
class AudioVisualizer : HybridAudioVisualizerSpec() {
  private var audioRecord: AudioRecord? = null
  private var isRecordingFlag = false
  private var recordingThread: Thread? = null
  
  private val sampleRate = 44100
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
  
  // Audio analysis data
  @Volatile
  private var currentAmplitude = 0.0
  @Volatile
  private var frequencyBands = DoubleArray(7) { 0.0 } // 7 bands for higher resolution
  
  
  
  override fun startRecording() {
    if (isRecordingFlag) return
    
    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        bufferSize * 2
      )
      
      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        throw RuntimeException("AudioRecord initialization failed")
      }
      
      audioRecord?.startRecording()
      isRecordingFlag = true
      
      recordingThread = Thread {
        processAudio()
      }.apply { start() }
      
    } catch (e: Exception) {
      e.printStackTrace()
      isRecordingFlag = false
    }
  }
  
  override fun stopRecording() {
    isRecordingFlag = false
    recordingThread?.join(1000)
    
    audioRecord?.apply {
      stop()
      release()
    }
    audioRecord = null
    currentAmplitude = 0.0
    frequencyBands = DoubleArray(7) { 0.0 }
  }
  
  override fun isRecording(): Boolean {
    return isRecordingFlag
  }
  
  override fun getAmplitude(): Double {
    return currentAmplitude
  }
  
  override fun getFrequencyBands(): DoubleArray {
    return frequencyBands.clone()
  }
  
  override fun requestPermission(): Promise<Boolean> {
    val context = NitroModules.applicationContext
    if (context == null) {
      return Promise.resolved(false)
    }
    
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    
    return Promise.resolved(hasPermission)
  }
  
  private fun processAudio() {
    val buffer = ShortArray(bufferSize)
    
    while (isRecordingFlag) {
      val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
      
      if (readSize > 0) {
        // Calculate RMS amplitude
        var sum = 0.0
        for (i in 0 until readSize) {
          sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = sqrt(sum / readSize)
        
        // Normalize and amplify for better visibility (speech is usually much quieter than max Short)
        val normalized = (rms / Short.MAX_VALUE)
        // Use square root for better dynamic range (boosts quiet, tames loud)
        currentAmplitude = (sqrt(normalized) * 2.0).coerceIn(0.0, 1.0)
        
        // Simple frequency band analysis
        // This is a simplified approach - for better results, use FFT
        analyzeFrequencyBands(buffer, readSize)

        // Update shared view if available
        AudioVisualizerViewManager.updateAmplitude(currentAmplitude.toFloat())
        AudioVisualizerViewManager.updateFrequencyBands(frequencyBands.map { it.toFloat() }.toFloatArray())
      }
      
      Thread.sleep(16) // ~60 FPS update rate
    }
  }
  
  private fun analyzeFrequencyBands(buffer: ShortArray, size: Int) {
    val bandsCount = frequencyBands.size
    val chunkSize = size / bandsCount
    
    for (i in 0 until bandsCount) {
      var sum = 0.0
      val start = i * chunkSize
      val end = if (i == bandsCount - 1) size else (i + 1) * chunkSize
      val count = (end - start).coerceAtLeast(1)
      
      for (j in start until end) {
        sum += abs(buffer[j].toDouble())
      }
      
      val rawValue = sum / count / Short.MAX_VALUE
      frequencyBands[i] = (sqrt(rawValue) * 2.5).coerceIn(0.0, 1.0)
    }
  }
}

