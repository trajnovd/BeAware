package com.beaware.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Subtle animated \"liquid\" overlay that sits above the blurred background.
 * This isn't a true distortion blur; it's a soft moving glow layer that makes the background
 * blend more naturally (liquid-glass vibe) without blurring UI elements.
 */
class LiquidOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TARGET_FPS = 30
        private const val FRAME_DELAY_MS = 1000 / TARGET_FPS
        private const val BLOB_COUNT = 5
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var t = 0f

    private val blobs = List(BLOB_COUNT) { idx ->
        Blob(
            phase = Random.nextFloat() * 6.283f,
            speed = 0.35f + Random.nextFloat() * 0.35f,
            radiusPct = 0.28f + Random.nextFloat() * 0.18f,
            alpha = 0.10f + idx * 0.02f,
            color = when (idx % 3) {
                0 -> 0xFF6B5CFF.toInt() // violet
                1 -> 0xFF2EC4FF.toInt() // blue
                else -> 0xFFFF4ECD.toInt() // pink
            }
        )
    }

    private val runnable = object : Runnable {
        override fun run() {
            t += 0.02f
            invalidate()
            handler.postDelayed(this, FRAME_DELAY_MS.toLong())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(runnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val w = width.toFloat()
        val h = height.toFloat()
        val minDim = min(w, h)

        // very subtle vignette tint to blend the wallpaper
        paint.shader = null
        paint.color = 0x22000000
        canvas.drawRect(0f, 0f, w, h, paint)

        blobs.forEachIndexed { i, blob ->
            val cx = w * (0.5f + 0.22f * sin(t * blob.speed + blob.phase + i))
            val cy = h * (0.45f + 0.20f * cos(t * (blob.speed * 1.1f) + blob.phase))
            val r = max(1f, minDim * blob.radiusPct * (0.95f + 0.08f * sin(t * 1.3f + blob.phase)))

            val baseAlpha = (255f * blob.alpha).toInt().coerceIn(0, 255)
            val inner = (blob.color and 0x00FFFFFF) or (baseAlpha shl 24)
            val outer = (blob.color and 0x00FFFFFF) or (0 shl 24)

            paint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(inner, outer),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
        }

        paint.shader = null
    }

    private data class Blob(
        val phase: Float,
        val speed: Float,
        val radiusPct: Float,
        val alpha: Float,
        val color: Int
    )
}


