package com.beaware.app.audio

/**
 * Defines the urgency levels for detected sounds.
 * Each level has different alert behaviors.
 * 
 * 🔴 Level 1 (DANGER) - Safety Mode Triggered
 *    - Music STOPS completely
 *    - Continuous alert tone + repeating vibration
 *    - 30-second countdown, then SOS message sent
 * 
 * 🟡 Level 2 (CAUTION) - Potential Danger
 *    - Music lowered to 20%
 *    - Double ping + two strong vibration pulses
 *    - Screen banner shown
 * 
 * 🟢 Level 3 (AWARENESS) - No Immediate Danger
 *    - No music change
 *    - Single soft ping + light vibration
 *    - No UI interruption (toast only)
 */
enum class UrgencyLevel(
    val level: Int,
    val displayName: String,
    val countdownSeconds: Int,
    val userMessage: String
) {
    /**
     * 🔴 Level 1: DANGER - Safety Mode
     * Shouting for help, screaming, physical struggle, glass breaking
     * Response: Stop music, continuous alert, 30-second countdown to SOS
     */
    CRITICAL(1, "DANGER", 30, "Safety mode activated."),

    /**
     * 🟡 Level 2: CAUTION - Potential Danger
     * Sirens, car horns, shouting (non-distress), engine revving
     * Response: Lower music to 20%, double ping, strong vibration
     */
    DANGER(2, "CAUTION", 0, "Pay attention now."),

    /**
     * 🟢 Level 3: AWARENESS - No Immediate Danger
     * Bicycle bells, bus sounds, footsteps, dog barking, crowd noise
     * Response: Soft ping, light vibration, no UI interruption
     */
    WARNING(3, "AWARENESS", 0, "Be aware.");

    companion object {
        fun fromLevel(level: Int): UrgencyLevel {
            return entries.find { it.level == level } ?: WARNING
        }
    }
}
