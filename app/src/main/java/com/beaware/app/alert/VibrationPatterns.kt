package com.beaware.app.alert

import android.os.VibrationEffect

/**
 * Predefined vibration patterns for different alert levels.
 * 
 * 🔴 Level 1 (DANGER): Repeating strong vibration - continuous alert
 * 🟡 Level 2 (CAUTION): Two strong vibration pulses
 * 🟢 Level 3 (AWARENESS): Single light vibration pulse
 */
object VibrationPatterns {

    /**
     * 🔴 Level 1: DANGER - Repeating strong vibration
     * Continuous alert pattern for safety mode
     * Pattern: vibrate, pause, vibrate, pause... (repeating)
     */
    val DANGER_PATTERN = longArrayOf(0, 400, 200, 400, 200, 400, 200, 400)
    val DANGER_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)

    /**
     * 🟡 Level 2: CAUTION - Two strong pulses
     * Double pulse for attention
     */
    val CAUTION_PATTERN = longArrayOf(0, 300, 150, 300)
    val CAUTION_AMPLITUDES = intArrayOf(0, 220, 0, 220)

    /**
     * 🟢 Level 3: AWARENESS - Single light pulse
     * Soft, non-intrusive vibration
     */
    val AWARENESS_PATTERN = longArrayOf(0, 150)
    val AWARENESS_AMPLITUDES = intArrayOf(0, 100)

    /**
     * Create a VibrationEffect for Level 1 DANGER (repeating strong vibration)
     */
    fun createDangerEffect(): VibrationEffect {
        return VibrationEffect.createWaveform(DANGER_PATTERN, DANGER_AMPLITUDES, 0) // Repeat at index 0
    }

    /**
     * Create a VibrationEffect for Level 2 CAUTION (two strong pulses)
     */
    fun createCautionEffect(): VibrationEffect {
        return VibrationEffect.createWaveform(CAUTION_PATTERN, CAUTION_AMPLITUDES, -1) // No repeat
    }

    /**
     * Create a VibrationEffect for Level 3 AWARENESS (single light pulse)
     */
    fun createAwarenessEffect(): VibrationEffect {
        return VibrationEffect.createOneShot(150, 100) // 150ms, light intensity
    }
    
    // ========== Legacy patterns (kept for compatibility) ==========
    
    /**
     * Legacy: SOS Pattern (3 long pulses) - now replaced by DANGER pattern
     */
    val SOS_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    val SOS_AMPLITUDES = intArrayOf(0, 255, 0, 255, 0, 255)

    /**
     * Legacy: Pulse Pattern - now replaced by CAUTION pattern
     */
    val PULSE_PATTERN = longArrayOf(0, 100, 100, 100, 100, 100, 100, 100, 100, 100)
    val PULSE_AMPLITUDES = intArrayOf(0, 200, 0, 200, 0, 200, 0, 200, 0, 200)

    fun createSosEffect(): VibrationEffect {
        return createDangerEffect()
    }

    fun createPulseEffect(): VibrationEffect {
        return createCautionEffect()
    }

    fun createSinglePulseEffect(): VibrationEffect {
        return createAwarenessEffect()
    }
}
