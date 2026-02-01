package com.margelo.nitro.audiovisualizer

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class AudioVisualizerNativeView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
  }

  private var visualizationType = "strings"
  private var visualizationColor = "#00D9FF"
  private var sensitivity = 1.0f
  private var smoothing = 0.8f

  private var amplitude = 0f
  private var targetAmplitude = 0f
  private var frequencyBands = FloatArray(7) { 0f }

  // Particles for 'particles' style
  private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float, var alpha: Int)
  private val particles = mutableListOf<Particle>()

  // History for 'waveform' style
  private val historySize = 100
  private val amplitudeHistory = FloatArray(historySize) { 0f }
  private var lastHistoryUpdate = 0L

  // Animation state
  private var animationPhase = 0f
  
  // Padding to prevent clipping
  private val paddingPercent = 0.15f

  init {
    // Enable software rendering Layer for Shadow/Glow effect if needed, 
    // though hardware usually supports setShadowLayer on modern Android
    setLayerType(LAYER_TYPE_HARDWARE, null)
  }

  fun setType(type: String) {
    visualizationType = type
    invalidate()
  }

  fun setColor(color: String) {
    visualizationColor = color
    invalidate()
  }

  fun setSensitivity(value: Float) {
    sensitivity = value.coerceIn(0f, 2f)
  }

  fun setSmoothing(value: Float) {
    smoothing = value.coerceIn(0f, 1f)
  }

  fun updateAmplitude(amp: Float) {
    targetAmplitude = (amp * sensitivity).coerceIn(0f, 1f)
  }

  fun updateFrequencyBands(bands: FloatArray) {
    if (bands.size >= frequencyBands.size) {
      for (i in frequencyBands.indices) {
        frequencyBands[i] = bands[i]
      }
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    // Smooth amplitude transition
    amplitude += (targetAmplitude - amplitude) * (1f - smoothing)
    // Add a tiny bit of "breathing" noise so it's never perfectly static
    val breathing = (sin(animationPhase * 0.5f) * 0.02f).coerceAtLeast(0f)
    // Update waveform history (~20 times per second for smooth scrolling)
    val now = System.currentTimeMillis()
    if (now - lastHistoryUpdate > 50) {
      System.arraycopy(amplitudeHistory, 1, amplitudeHistory, 0, historySize - 1)
      amplitudeHistory[historySize - 1] = amplitude + breathing
      lastHistoryUpdate = now
    }

    when (visualizationType) {
      "strings" -> drawStrings(canvas, breathing)
      "bars" -> drawBars(canvas, breathing)
      "waves" -> drawWaves(canvas, breathing)
      "thin-bars" -> drawThinBars(canvas, breathing)
      "particles" -> drawParticles(canvas, breathing)
      "orbit" -> drawOrbit(canvas, breathing)
      "waveform" -> drawWaveform(canvas)
      else -> drawStrings(canvas, breathing)
    }

    // Continue animation
    postInvalidateOnAnimation()
  }

  private fun drawStrings(canvas: Canvas, breathing: Float) {
    val centerY = height / 2f
    val color = Color.parseColor(visualizationColor)
    val numWaves = 5
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    
    // Internal vertical safe zone
    val usableHeight = height * (1f - paddingPercent * 2)
    val vOffset = height * paddingPercent

    paint.style = Paint.Style.STROKE
    // Add glow
    paint.setShadowLayer(15f, 0f, 0f, color)

    for (i in 0 until numWaves) {
      val path = Path()
      val alpha = (160 * (1f - i / numWaves.toFloat()) + 40 * sin(animationPhase + i)).toInt().coerceIn(0, 255)
      paint.color = Color.argb(alpha, r, g, b)
      paint.strokeWidth = (4f - i * 0.5f).coerceAtLeast(1.5f)
      
      val speed = animationPhase * (0.6f + i * 0.2f)
      val freq = 0.012f + i * 0.004f
      
      path.moveTo(0f, centerY)
      
      val steps = 60
      val stepWidth = width / steps.toFloat()
      
      for (step in 0..steps) {
        val x = step * stepWidth
        // Gaussian envelope for nice tapering
        val distFromCenter = abs(step - steps / 2f) / (steps / 2f)
        val envelope = exp(-distFromCenter * distFromCenter * 5f)
        
        val angle = x * freq + speed
        val waveAmplitude = (amplitude + breathing) * usableHeight * 0.5f * (1f - i * 0.15f)
        val y = centerY + sin(angle) * waveAmplitude * envelope
        path.lineTo(x, y)
      }
      canvas.drawPath(path, paint)
    }
    paint.clearShadowLayer()
  }

  private fun drawBars(canvas: Canvas, breathing: Float) {
    val numBars = 70
    val barWidth = width / numBars.toFloat()
    val gap = barWidth * 0.4f
    val actualBarWidth = (barWidth - gap).coerceAtLeast(3f)
    val color = Color.parseColor(visualizationColor)
    val centerY = height / 2f
    val usableHeight = height * (1f - paddingPercent * 2)
    
    paint.style = Paint.Style.FILL
    paint.shader = null // Ensure no shader is used
    // Keep a strong glow for the "DJ LED" feel
    paint.setShadowLayer(10f, 0f, 0f, color)
    paint.color = color
    
    for (i in 0 until numBars) {
      val relativeIdx = if (i < numBars / 2) i else (numBars - 1 - i)
      val progress = relativeIdx / (numBars / 2f)
      
      val bandIdx = (progress * (frequencyBands.size - 1)).toInt()
      val bandLow = frequencyBands[bandIdx]
      val bandHigh = frequencyBands[(bandIdx + 1).coerceAtMost(frequencyBands.size - 1)]
      val interpolation = (progress * (frequencyBands.size - 1)) - bandIdx
      
      val value = lerp(bandLow, bandHigh, interpolation)
      
      // Dynamic scaling with a bit of per-bar variation to prevent "flat top" at max volume
      val variation = 0.85f + 0.15f * sin(i.toFloat() * 0.4f + animationPhase)
      val h = (value * usableHeight * variantScaling(i) * variation + 8f * (amplitude + breathing)).coerceIn(12f, usableHeight)
      val x = i * barWidth + gap / 2f
      val top = centerY - h / 2f
      val bottom = centerY + h / 2f
      
      canvas.drawRoundRect(x, top, x + actualBarWidth, bottom, actualBarWidth / 2f, actualBarWidth / 2f, paint)
    }
    paint.clearShadowLayer()
  }

  private fun drawWaves(canvas: Canvas, breathing: Float) {
    val color = Color.parseColor(visualizationColor)
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    val usableHeight = height * (1f - paddingPercent)
    val baseLine = height * 0.7f

    paint.clearShadowLayer() // Fill styles usually don't need shadow layers
    
    for (i in 0 until 3) {
      val path = Path()
      val alpha = (90 + i * 60).coerceAtMost(255)
      paint.color = Color.argb(alpha, r, g, b)
      paint.style = Paint.Style.FILL
      
      val speed = animationPhase * (0.5f + i * 0.25f)
      val freq = 0.007f + i * 0.003f
      val layerOffset = i * (height * 0.05f)
      
      path.moveTo(0f, height.toFloat())
      path.lineTo(0f, baseLine + layerOffset)
      
      val steps = 40
      val stepWidth = width / steps.toFloat()
      
      for (step in 0..steps) {
        val x = step * stepWidth
        val angle = x * freq + speed
        // Waves react to amplitude but stay in safe zone
        val y = (baseLine + layerOffset) + sin(angle) * (amplitude + breathing) * (height * 0.2f)
        path.lineTo(x, y.coerceAtMost(height.toFloat() - 5f))
      }
      
      path.lineTo(width.toFloat(), height.toFloat())
      path.close()
      canvas.drawPath(path, paint)
    }
  }

  private fun drawThinBars(canvas: Canvas, breathing: Float) {
    val numBars = 100
    val barWidth = width / numBars.toFloat()
    val actualBarWidth = 2f
    val gap = barWidth - actualBarWidth
    val color = Color.parseColor(visualizationColor)
    val centerY = height / 2f
    val usableHeight = height * (1f - paddingPercent * 2)

    paint.style = Paint.Style.FILL
    paint.shader = null
    paint.setShadowLayer(5f, 0f, 0f, color)
    paint.color = color

    for (i in 0 until numBars) {
      val progress = i / numBars.toFloat()
      val bandIdx = (progress * (frequencyBands.size - 1)).toInt()
      val bandLow = frequencyBands[bandIdx]
      val bandHigh = frequencyBands[(bandIdx + 1).coerceAtMost(frequencyBands.size - 1)]
      val interpolation = (progress * (frequencyBands.size - 1)) - bandIdx
      val value = lerp(bandLow, bandHigh, interpolation)

      // Add variation to thin bars as well
      val variation = 0.8f + 0.2f * sin(i.toFloat() * 0.8f + animationPhase)
      val h = (value * usableHeight * variation + 6f * (amplitude + breathing)).coerceIn(4f, usableHeight)
      val x = i * barWidth + gap / 2f
      canvas.drawRect(x, centerY - h / 2f, x + actualBarWidth, centerY + h / 2f, paint)
    }
    paint.clearShadowLayer()
  }

  private fun drawParticles(canvas: Canvas, breathing: Float) {
    val color = Color.parseColor(visualizationColor)
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)

    // Initialize particles if empty
    if (particles.isEmpty()) {
      for (i in 0 until 40) {
        particles.add(Particle(
          Random.nextFloat() * width,
          Random.nextFloat() * height,
          (Random.nextFloat() - 0.5f) * 2f,
          (Random.nextFloat() - 0.5f) * 2f,
          Random.nextFloat() * 8f + 2f,
          Random.nextInt(50, 200)
        ))
      }
    }

    paint.style = Paint.Style.FILL
    paint.setShadowLayer(12f, 0f, 0f, color)

    for (p in particles) {
      // Move particles based on velocity and amplitude
      p.x += p.vx * (1f + amplitude * 5f)
      p.y += p.vy * (1f + amplitude * 5f)

      // Wrap around screen
      if (p.x < 0) p.x = width.toFloat()
      if (p.x > width) p.x = 0f
      if (p.y < 0) p.y = height.toFloat()
      if (p.y > height) p.y = 0f

      val currentSize = p.size * (1f + amplitude * 1.5f)
      paint.color = Color.argb(p.alpha, r, g, b)
      canvas.drawCircle(p.x, p.y, currentSize, paint)
    }
    paint.clearShadowLayer()
  }

  private fun drawOrbit(canvas: Canvas, breathing: Float) {
    val centerX = width / 2f
    val centerY = height / 2f
    val color = Color.parseColor(visualizationColor)
    val usableRadius = (min(width, height) / 2f) * 0.8f

    paint.style = Paint.Style.STROKE
    paint.setShadowLayer(15f, 0f, 0f, color)
    paint.color = color

    for (i in 0 until 4) {
      val phaseOffset = i * (PI.toFloat() / 2f)
      val radius = (usableRadius * 0.4f) + (i * 15f) + (amplitude * usableRadius * 0.5f)
      
      paint.strokeWidth = (5f - i).coerceAtLeast(1f)
      paint.alpha = (255 * (1f - i * 0.2f)).toInt()
      
      val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
      
      // Draw pulsating arcs
      val startAngle = (animationPhase * 50f + i * 90f) % 360f
      val sweepAngle = 60f + amplitude * 180f
      canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
      canvas.drawArc(rect, startAngle + 180f, sweepAngle, false, paint)
    }
    paint.clearShadowLayer()
  }

  private fun drawWaveform(canvas: Canvas) {
    val barWidth = width / historySize.toFloat()
    val gap = barWidth * 0.3f
    val actualBarWidth = (barWidth - gap).coerceAtLeast(2f)
    val color = Color.parseColor(visualizationColor)
    val centerY = height / 2f
    val usableHeight = height * (1f - paddingPercent * 2)

    paint.style = Paint.Style.FILL
    paint.shader = null
    paint.color = color
    paint.setShadowLayer(8f, 0f, 0f, color)

    for (i in 0 until historySize) {
      val amp = amplitudeHistory[i]
      val h = (amp * usableHeight + 4f).coerceIn(4f, usableHeight)
      val x = i * barWidth + gap / 2f
      
      // Draw symmetrical bars like iOS Voice Memos
      canvas.drawRoundRect(
        x, 
        centerY - h / 2f, 
        x + actualBarWidth, 
        centerY + h / 2f, 
        actualBarWidth / 2f, 
        actualBarWidth / 2f, 
        paint
      )
    }
    paint.clearShadowLayer()
  }

  private fun adjustAlpha(color: Int, factor: Float): Int {
    val alpha = round(Color.alpha(color) * factor).toInt()
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return Color.argb(alpha, red, green, blue)
  }

  private fun lerp(start: Float, stop: Float, amount: Float): Float {
    return start + (stop - start) * amount
  }

  // Helper to add some asymmetric character to the bar heights
  private fun variantScaling(index: Int): Float {
    return 0.9f + 0.1f * sin(index.toFloat() * 0.1f)
  }
}
