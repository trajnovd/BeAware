package com.beaware.app.ui



import android.content.Context

import android.graphics.Canvas

import android.graphics.Path

import android.graphics.Paint

import android.graphics.RadialGradient

import android.graphics.Shader

import android.os.Handler

import android.os.Looper

import android.util.AttributeSet

import android.view.View

import kotlin.math.*

import kotlin.random.Random



/**

 * 3D Particle Audio Visualizer

 * Creates a beautiful undulating 3D particle effect that responds to audio input.

 * Particles glow with gradient colors (blue → teal → purple → magenta).

 */

class ParticleVisualizerView @JvmOverloads constructor(

    context: Context,

    attrs: AttributeSet? = null,

    defStyleAttr: Int = 0

) : View(context, attrs, defStyleAttr) {



    companion object {

        private const val PARTICLE_COUNT = 300

        private const val ANIMATION_SPEED = 0.02f

        private const val MAX_AMPLITUDE_MULTIPLIER = 3.0f

        private const val BASE_PARTICLE_SIZE = 3.5f

        private const val MAX_PARTICLE_SIZE = 11f

        private const val TARGET_FPS = 60

        private const val FRAME_DELAY_MS = 1000 / TARGET_FPS

    }



    private val particles = mutableListOf<Particle>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val clipPath = Path()

    private val handler = Handler(Looper.getMainLooper())

    private val animationRunnable = object : Runnable {

        override fun run() {

            animationTime += ANIMATION_SPEED

            updateParticles()

            invalidate()

            handler.postDelayed(this, FRAME_DELAY_MS.toLong())

        }

    }



    private var currentAmplitude = 0f

    private var targetAmplitude = 0f

    private var smoothedEnergy = 0f

    private var animationTime = 0f

    private var isActive = false



    // Color gradients (blue → teal → purple → magenta)

    private val colors = intArrayOf(

        0xFF00D4FF.toInt(), // Bright blue

        0xFF00FFE5.toInt(), // Teal

        0xFF9D4EDD.toInt(), // Purple

        0xFFFF006E.toInt()  // Magenta

    )



    init {

        setLayerType(LAYER_TYPE_HARDWARE, null) // Enable hardware acceleration

        initializeParticles()

    }



    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        startAnimation()

    }



    override fun onDetachedFromWindow() {

        super.onDetachedFromWindow()

        stopAnimation()

    }



    /**

     * Initialize particles in 3D space

     */

    private fun initializeParticles() {

        particles.clear()

        repeat(PARTICLE_COUNT) {

            particles.add(

                Particle(

                    x = Random.nextFloat() * 2f - 1f, // -1 to 1

                    y = Random.nextFloat() * 2f - 1f,

                    z = Random.nextFloat() * 2f - 1f,

                    baseX = Random.nextFloat() * 2f - 1f,

                    baseY = Random.nextFloat() * 2f - 1f,

                    baseZ = Random.nextFloat() * 2f - 1f,

                    phase = Random.nextFloat() * PI.toFloat() * 2f,

                    speed = 0.5f + Random.nextFloat() * 0.5f

                )

            )

        }

    }



    /**

     * Set audio amplitude (0.0 to 1.0)

     */

    fun setAmplitude(amplitude: Float) {

        targetAmplitude = amplitude.coerceIn(0f, 1f)

    }



    /**

     * Set active state

     */

    fun setActive(active: Boolean) {

        isActive = active

        if (!active) {

            targetAmplitude = 0f

        }

    }



    /**

     * Start continuous animation

     */

    private fun startAnimation() {

        handler.post(animationRunnable)

    }



    /**

     * Stop animation

     */

    private fun stopAnimation() {

        handler.removeCallbacks(animationRunnable)

    }



    /**

     * Update particle positions based on audio amplitude and time

     */

    private fun updateParticles() {

        // Smooth amplitude interpolation

        currentAmplitude += (targetAmplitude - currentAmplitude) * 0.1f

        // Smoothed energy (more punchy than currentAmplitude)

        smoothedEnergy += ((currentAmplitude * currentAmplitude) - smoothedEnergy) * 0.08f



        val amplitudeMultiplier = 1f + (currentAmplitude * MAX_AMPLITUDE_MULTIPLIER)



        particles.forEach { particle ->

            // Create undulating wave motion

            val waveX = sin(animationTime * particle.speed + particle.phase) * 0.3f

            val waveY = cos(animationTime * particle.speed * 1.3f + particle.phase) * 0.3f

            val waveZ = sin(animationTime * particle.speed * 0.7f + particle.phase) * 0.3f



            // Apply amplitude-based movement

            val amplitudeWave = sin(animationTime * 2f + particle.phase) * currentAmplitude * 0.5f



            particle.x = particle.baseX + waveX * amplitudeMultiplier + amplitudeWave

            particle.y = particle.baseY + waveY * amplitudeMultiplier + amplitudeWave

            particle.z = particle.baseZ + waveZ * amplitudeMultiplier + amplitudeWave



            // Calculate distance from center for size/color

            val distance = sqrt(particle.x * particle.x + particle.y * particle.y + particle.z * particle.z)

            particle.distance = distance

        }



        // Sort by Z depth for proper rendering

        particles.sortByDescending { it.z }

    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val minSize = 200.dpToPx()

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)

        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)



        var width = widthSize

        var height = heightSize



        when (widthMode) {

            View.MeasureSpec.UNSPECIFIED -> width = minSize

            View.MeasureSpec.AT_MOST -> width = maxOf(width, minSize)

            View.MeasureSpec.EXACTLY -> width = maxOf(width, minSize)

        }



        when (heightMode) {

            View.MeasureSpec.UNSPECIFIED -> height = minSize

            View.MeasureSpec.AT_MOST -> height = maxOf(height, minSize)

            View.MeasureSpec.EXACTLY -> height = maxOf(height, minSize)

        }



        setMeasuredDimension(width, height)

    }



    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)



        if (width == 0 || height == 0) {

            // Draw a placeholder if not measured yet

            return

        }



        val centerX = width / 2f

        val centerY = height / 2f

        val minDim = min(width, height).toFloat()

        val radius = minDim * 0.48f



        // Clip to a circle (so the visual is circular, not a square)

        clipPath.reset()

        clipPath.addCircle(centerX, centerY, radius, Path.Direction.CW)

        val saveCount = canvas.save()

        canvas.clipPath(clipPath)



        // Stronger reaction to voice input: expand + rotate more with energy

        val breathing = 0.92f + (smoothedEnergy * 0.35f) + (sin(animationTime * 1.2f) * 0.02f)

        val scale = radius * 0.78f * breathing

        val rotation = animationTime * (0.25f + smoothedEnergy * 1.4f)



        val sinR = sin(rotation)

        val cosR = cos(rotation)



        // Draw particles

        particles.forEach { particle ->

            // Rotate in X/Y plane for a more dynamic “3D” feel

            val rx = particle.x * cosR - particle.y * sinR

            val ry = particle.x * sinR + particle.y * cosR



            // Project 3D to 2D

            val projectedX = centerX + rx * scale

            val projectedY = centerY + ry * scale



            // Skip particles outside view bounds

            if (projectedX < -50f || projectedX > width + 50f ||

                projectedY < -50f || projectedY > height + 50f) {

                return@forEach

            }



            // Calculate size based on Z depth and amplitude

            val depthFactor = (particle.z + 1f) / 2f // Normalize to 0-1

            val sizeMultiplier = 0.45f + depthFactor * 0.55f + smoothedEnergy * 0.75f

            val particleSize = (BASE_PARTICLE_SIZE + (MAX_PARTICLE_SIZE - BASE_PARTICLE_SIZE) * sizeMultiplier)



            // Calculate color based on position and distance

            val colorIndex = ((particle.distance * 0.5f + animationTime * 0.1f) % colors.size).toInt()

            val nextColorIndex = (colorIndex + 1) % colors.size

            val colorProgress = (particle.distance * 0.5f + animationTime * 0.1f) % 1f



            val color = interpolateColor(colors[colorIndex], colors[nextColorIndex], colorProgress)



            // Create radial gradient for glow effect

            val gradientRadius = particleSize * 2.5f

            val gradient = RadialGradient(

                projectedX, projectedY, gradientRadius,

                intArrayOf(color, color and 0x00FFFFFF), // Fade to transparent

                floatArrayOf(0f, 1f),

                Shader.TileMode.CLAMP

            )



            paint.shader = gradient

            val alpha = (255 * (0.30f + depthFactor * 0.60f + smoothedEnergy * 0.20f)).toInt().coerceIn(60, 255)

            paint.alpha = alpha



            canvas.drawCircle(projectedX, projectedY, particleSize, paint)

        }



        paint.shader = null

        canvas.restoreToCount(saveCount)

    }



    /**

     * Interpolate between two colors

     */

    private fun interpolateColor(color1: Int, color2: Int, progress: Float): Int {

        val r1 = (color1 shr 16) and 0xFF

        val g1 = (color1 shr 8) and 0xFF

        val b1 = color1 and 0xFF



        val r2 = (color2 shr 16) and 0xFF

        val g2 = (color2 shr 8) and 0xFF

        val b2 = color2 and 0xFF



        val r = (r1 + (r2 - r1) * progress).toInt()

        val g = (g1 + (g2 - g1) * progress).toInt()

        val b = (b1 + (b2 - b1) * progress).toInt()



        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    }



    /**

     * Convert dp to pixels

     */

    private fun Int.dpToPx(): Int {

        return (this * resources.displayMetrics.density).toInt()

    }



    /**

     * Particle data class

     */

    private data class Particle(

        var x: Float,

        var y: Float,

        var z: Float,

        val baseX: Float,

        val baseY: Float,

        val baseZ: Float,

        val phase: Float,

        val speed: Float,

        var distance: Float = 0f

    )

}