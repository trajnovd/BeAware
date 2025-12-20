package com.beaware.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.beaware.app.R
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom view that displays an animated waveform visualization.
 * Shows audio activity when protection is active.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.waveform_active)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.waveform_inactive)
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private var isActive = false
    private var amplitude = 0f
    private var phaseOffset = 0f
    
    private val waveAnimator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phaseOffset = it.animatedValue as Float
            invalidate()
        }
    }

    // Number of wave bars
    private val barCount = 40
    private val barAmplitudes = FloatArray(barCount) { Random.nextFloat() * 0.5f }

    /**
     * Set whether the waveform is active (animating)
     */
    fun setActive(active: Boolean) {
        isActive = active
        if (active) {
            waveAnimator.start()
        } else {
            waveAnimator.cancel()
            amplitude = 0f
        }
        invalidate()
    }

    /**
     * Update the amplitude based on audio input
     */
    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(0f, 1f)
        
        // Update random bar heights based on amplitude
        for (i in barAmplitudes.indices) {
            barAmplitudes[i] = (barAmplitudes[i] * 0.7f + amplitude * Random.nextFloat() * 0.3f)
                .coerceIn(0.1f, 1f)
        }
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        val paint = if (isActive) activePaint else inactivePaint

        if (isActive) {
            drawAnimatedBars(canvas, width, height, centerY, paint)
        } else {
            drawFlatLine(canvas, width, centerY, paint)
        }
    }

    private fun drawAnimatedBars(canvas: Canvas, width: Float, height: Float, centerY: Float, paint: Paint) {
        val barWidth = width / (barCount * 2)
        val maxBarHeight = height * 0.4f

        for (i in 0 until barCount) {
            val x = (i * 2 + 1) * barWidth
            
            // Calculate height with wave animation
            val wave = sin((i * 0.3f + phaseOffset).toDouble()).toFloat()
            val dynamicAmplitude = barAmplitudes[i] * (0.3f + amplitude * 0.7f)
            val barHeight = maxBarHeight * dynamicAmplitude * (0.5f + 0.5f * wave)
            
            canvas.drawLine(
                x, centerY - barHeight,
                x, centerY + barHeight,
                paint
            )
        }
    }

    private fun drawFlatLine(canvas: Canvas, width: Float, centerY: Float, paint: Paint) {
        canvas.drawLine(0f, centerY, width, centerY, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator.cancel()
    }
}

