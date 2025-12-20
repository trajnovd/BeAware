package com.beaware.app.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.beaware.app.audio.UrgencyLevel
import com.beaware.app.data.PreferencesManager
import java.util.Locale

/**
 * Manages alert responses: vibration, audio focus, and sound effects.
 * 
 * 🔴 Level 1 (DANGER): Music STOPS, CONTINUOUS alert tone (30 sec), repeating vibration
 * 🟡 Level 2 (CAUTION): Music at 20%, double ping, two strong vibrations
 * 🟢 Level 3 (AWARENESS): Music lowered briefly, TTS announcement OR soft ping
 * 
 * Uses singleton pattern so Service and Activity can share state.
 */
class AlertManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AlertManager"
        
        // How often to repeat the danger beep (in milliseconds)
        private const val DANGER_BEEP_INTERVAL_MS = 800L
        
        // Maximum duration for continuous beeping (30 seconds)
        private const val MAX_DANGER_DURATION_MS = 30000L
        
        // TTS utterance ID
        private const val TTS_UTTERANCE_ID = "beaware_announcement"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: AlertManager? = null
        
        /**
         * Get the singleton instance of AlertManager
         */
        fun getInstance(context: Context): AlertManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlertManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val preferencesManager = PreferencesManager(context)

    private var audioFocusRequest: AudioFocusRequest? = null
    private var toneGenerator: ToneGenerator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Text-to-Speech for Level 3 announcements
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    
    // For continuous danger beeping
    private var isDangerAlertActive = false
    private var dangerBeepCount = 0
    
    // Current sound type for TTS announcement
    private var currentSoundType: String? = null
    
    // Track if music was playing before alert (to resume after)
    private var wasMusicPlayingBeforeAlert = false

    // For TTS-only volume ducking (set STREAM_MUSIC to ~10% temporarily)
    private var previousMusicVolume: Int? = null
    private var previousMusicMaxVolume: Int? = null
    
    // Runnable for continuous danger beeping
    private val dangerBeepRunnable = object : Runnable {
        override fun run() {
            if (isDangerAlertActive) {
                playDangerBeep()
                dangerBeepCount++
                
                // Continue beeping until stopped or max duration reached
                val elapsed = dangerBeepCount * DANGER_BEEP_INTERVAL_MS
                if (elapsed < MAX_DANGER_DURATION_MS) {
                    mainHandler.postDelayed(this, DANGER_BEEP_INTERVAL_MS)
                } else {
                    Log.d(TAG, "🔴 Danger beeping reached max duration (30s)")
                }
            }
        }
    }
    
    init {
        initTextToSpeech()
    }
    
    /**
     * Initialize Text-to-Speech engine
     */
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && 
                             result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.d(TAG, "🗣️ TTS initialized: $isTtsReady")
                
                // Set up listener to release audio focus after speaking
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "🗣️ TTS started speaking")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "🗣️ TTS finished speaking")
                        // Release audio focus after announcement
                        mainHandler.postDelayed({
                            restoreMusicVolumeAfterTts()
                            releaseAudioFocus()
                        }, 500)
                    }
                    
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "🗣️ TTS error")
                        mainHandler.post {
                            restoreMusicVolumeAfterTts()
                            releaseAudioFocus()
                        }
                    }
                })
            } else {
                Log.e(TAG, "🗣️ TTS initialization failed")
                isTtsReady = false
            }
        }
    }

    /**
     * Trigger alert based on urgency level
     * @param soundType The detected sound type (e.g., "BICYCLE BELL", "BUS")
     */
    fun triggerAlert(urgencyLevel: UrgencyLevel, soundType: String? = null) {
        Log.d(TAG, "🔔 Triggering ${urgencyLevel.displayName} alert for: $soundType")
        currentSoundType = soundType
        
        when (urgencyLevel) {
            // 🔴 LEVEL 1: DANGER - Safety Mode
            // Music STOPS, CONTINUOUS alert (30 seconds), repeating vibration
            UrgencyLevel.CRITICAL -> {
                requestAudioFocusForAlarm()
                vibrateDanger()
                startContinuousDangerTone()
            }
            
            // 🟡 LEVEL 2: CAUTION - Potential Danger
            // Music at 20%, double ping, two strong vibrations
            UrgencyLevel.DANGER -> {
                requestAudioFocusDuck()
                vibrateCaution()
                playDoublePing()
            }
            
            // 🟢 LEVEL 3: AWARENESS - No Immediate Danger
            // Brief music duck, TTS announcement or soft ping, light vibration
            UrgencyLevel.WARNING -> {
                vibrateAwareness()
                if (preferencesManager.useSpeechAnnouncements && isTtsReady) {
                    speakAnnouncement(soundType)
                } else {
                    playSoftPing()
                }
            }
        }
    }

    /**
     * 🔴 Request audio focus to STOP other media completely (Level 1)
     * Uses AUDIOFOCUS_GAIN_TRANSIENT so music apps know to RESUME when we release
     * Uses USAGE_ALARM which works even when screen is off
     */
    private fun requestAudioFocusForAlarm() {
        // Remember if music was playing so we can resume it
        wasMusicPlayingBeforeAlert = audioManager.isMusicActive
        Log.d(TAG, "🎵 Music was playing before alert: $wasMusicPlayingBeforeAlert")
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        // Use AUDIOFOCUS_GAIN_TRANSIENT instead of AUDIOFOCUS_GAIN
        // This tells music apps: "pause temporarily, you'll get focus back"
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d(TAG, "Audio focus changed: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        Log.d(TAG, "🔴 Audio focus TRANSIENT request result: $result (music should pause temporarily)")
    }

    /**
     * 🟡 Request audio focus to DUCK other media to ~20% (Level 2)
     */
    private fun requestAudioFocusDuck() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d(TAG, "Audio focus changed: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        Log.d(TAG, "🟡 Audio focus DUCK request result: $result (music at ~20%)")
    }
    
    /**
     * 🟢 Request brief audio focus for TTS announcement (Level 3)
     */
    private fun requestAudioFocusForSpeech() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                Log.d(TAG, "Audio focus changed: $focusChange")
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        Log.d(TAG, "🟢 Audio focus SPEECH request result: $result (music lowered for TTS)")
    }
    
    /**
     * Release audio focus and resume music if it was playing before
     */
    private fun releaseAudioFocus() {
        audioFocusRequest?.let {
            val result = audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
            Log.d(TAG, "🎵 Audio focus released (result: $result)")
            
            // If music was playing before the alert, try to resume it
            if (wasMusicPlayingBeforeAlert) {
                resumeMusic()
                wasMusicPlayingBeforeAlert = false
            }
        }
    }
    
    /**
     * Try to resume music playback by sending media button events
     */
    private fun resumeMusic() {
        try {
            Log.d(TAG, "🎵 Attempting to resume music playback...")
            
            // Method 1: Use dispatchMediaKeyEvent to send PLAY
            val keyEvent = android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY
            )
            audioManager.dispatchMediaKeyEvent(keyEvent)
            
            val keyEventUp = android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_MEDIA_PLAY
            )
            audioManager.dispatchMediaKeyEvent(keyEventUp)
            
            Log.d(TAG, "🎵 Sent MEDIA_PLAY key event")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume music", e)
        }
    }

    // ==================== VIBRATION ====================

    /**
     * 🔴 Danger vibration - Repeating strong pattern (continues until stopped)
     */
    private fun vibrateDanger() {
        try {
            if (!vibrator.hasVibrator()) {
                Log.w(TAG, "Device has no vibrator")
                return
            }
            
            // Pattern: vibrate 400ms, pause 200ms, repeat
            val pattern = longArrayOf(0, 400, 200, 400, 200, 400, 200, 400)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(pattern, amplitudes, 0) // Repeat at index 0
                } else {
                    VibrationEffect.createWaveform(pattern, 0)
                }
            } else {
                null
            }
            
            if (effect != null) {
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
            Log.d(TAG, "🔴 Danger vibration started (CONTINUOUS until stopped)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate (danger)", e)
        }
    }

    /**
     * 🟡 Caution vibration - Two strong pulses
     */
    private fun vibrateCaution() {
        try {
            if (!vibrator.hasVibrator()) {
                Log.w(TAG, "Device has no vibrator")
                return
            }
            
            val pattern = longArrayOf(0, 300, 150, 300)
            val amplitudes = intArrayOf(0, 220, 0, 220)
            
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(pattern, amplitudes, -1) // No repeat
                } else {
                    VibrationEffect.createWaveform(pattern, -1)
                }
            } else {
                null
            }
            
            if (effect != null) {
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            Log.d(TAG, "🟡 Caution vibration (two pulses)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate (caution)", e)
        }
    }

    /**
     * 🟢 Awareness vibration - Single light pulse
     */
    private fun vibrateAwareness() {
        try {
            if (!vibrator.hasVibrator()) {
                Log.w(TAG, "Device has no vibrator")
                return
            }
            
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(150, 80) // Light intensity
            } else {
                null
            }
            
            if (effect != null) {
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
            Log.d(TAG, "🟢 Awareness vibration (single light pulse)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate (awareness)", e)
        }
    }

    // ==================== SOUND EFFECTS ====================

    /**
     * 🔴 Start CONTINUOUS danger alert tone - Beeps every 800ms until stopped
     * This continues for up to 30 seconds or until user taps "I'm Safe"
     */
    private fun startContinuousDangerTone() {
        try {
            // Stop any previous alert
            stopDangerTone()
            
            // Initialize tone generator
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100) // Max volume
            
            // Start continuous beeping
            isDangerAlertActive = true
            dangerBeepCount = 0
            
            Log.d(TAG, "🔴 Starting CONTINUOUS danger beeping (every ${DANGER_BEEP_INTERVAL_MS}ms)")
            
            // Start the first beep immediately
            mainHandler.post(dangerBeepRunnable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start continuous danger tone", e)
        }
    }
    
    /**
     * Play a single danger beep
     */
    private fun playDangerBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
            Log.d(TAG, "🔴 Beep #$dangerBeepCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play danger beep", e)
        }
    }
    
    /**
     * Stop the continuous danger tone
     */
    private fun stopDangerTone() {
        isDangerAlertActive = false
        dangerBeepCount = 0
        mainHandler.removeCallbacks(dangerBeepRunnable)
    }

    /**
     * 🟡 Play double ping - Two higher-pitched beeps for attention
     */
    private fun playDoublePing() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            
            // First ping
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            
            // Second ping after 250ms
            mainHandler.postDelayed({
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            }, 250)
            
            Log.d(TAG, "🟡 Double ping playing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play double ping", e)
        }
    }

    /**
     * 🟢 Play soft ping - Single gentle notification sound
     */
    private fun playSoftPing() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50) // Lower volume
            
            // Request brief audio focus
            requestAudioFocusForSpeech()
            
            // Soft, short beep
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            
            // Release audio focus after ping
            mainHandler.postDelayed({
                releaseAudioFocus()
            }, 500)
            
            Log.d(TAG, "🟢 Soft ping playing")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play soft ping", e)
        }
    }
    
    /**
     * 🟢 Speak an announcement for Level 3 (AWARENESS) alerts
     * Examples: "A bike is coming", "A bus is coming"
     */
    private fun speakAnnouncement(soundType: String?) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w(TAG, "TTS not ready, falling back to ping")
            playSoftPing()
            return
        }
        
        // Build the announcement message based on sound type
        val message = buildAnnouncementMessage(soundType)
        
        // Request audio focus to lower music
        requestAudioFocusForSpeech()

        // Additionally reduce STREAM_MUSIC volume to ~10% while TTS is speaking, then restore.
        // This gives us a predictable duck level (audio focus duck level varies by device/player).
        temporarilyDuckMusicVolumeForTts(targetPercent = 0.10f)
        
        // Speak the announcement
        val params = android.os.Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
        
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID)
        Log.d(TAG, "🗣️ Speaking: \"$message\"")
    }

    private fun temporarilyDuckMusicVolumeForTts(targetPercent: Float) {
        try {
            // Only duck if there is active music; otherwise we'd be changing system volume unnecessarily.
            if (!audioManager.isMusicActive) {
                Log.d(TAG, "🎵 No active music; skipping temporary volume ducking for TTS")
                return
            }

            if (previousMusicVolume != null) {
                // Already ducked (e.g., multiple announcements in quick succession)
                return
            }

            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
            previousMusicMaxVolume = max
            previousMusicVolume = current

            val clampedPercent = targetPercent.coerceIn(0.0f, 1.0f)
            val target = kotlin.math.max(1, kotlin.math.round(max * clampedPercent).toInt())

            if (current > target) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                Log.d(TAG, "🎵 Ducking STREAM_MUSIC from $current/$max to $target/$max for TTS")
            } else {
                Log.d(TAG, "🎵 STREAM_MUSIC already <= target ($current/$max), not reducing")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duck music volume for TTS", e)
            previousMusicVolume = null
            previousMusicMaxVolume = null
        }
    }

    private fun restoreMusicVolumeAfterTts() {
        try {
            val prev = previousMusicVolume ?: return
            val max = previousMusicMaxVolume ?: audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val restoreTo = prev.coerceIn(0, max.coerceAtLeast(1))
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreTo, 0)
            Log.d(TAG, "🎵 Restored STREAM_MUSIC volume to $restoreTo/$max after TTS")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore music volume after TTS", e)
        } finally {
            previousMusicVolume = null
            previousMusicMaxVolume = null
        }
    }
    
    /**
     * Build the TTS announcement message based on detected sound type
     */
    private fun buildAnnouncementMessage(soundType: String?): String {
        val s = soundType?.trim().orEmpty()
        return when {
            s.isBlank() -> "Be aware of your surroundings"

            // Sirens: always announce as ambulance (non-generic)
            s.contains("SIREN", ignoreCase = true) ||
                s.contains("AMBULANCE", ignoreCase = true) ||
                s.contains("POLICE", ignoreCase = true) ||
                s.contains("FIRE TRUCK", ignoreCase = true) ||
                s.contains("FIRE ENGINE", ignoreCase = true) ||
                s.contains("EMERGENCY", ignoreCase = true) ->
                "Ambulance nearby"

            // Any bell/chime/ding: announce as bike coming (non-generic)
            s.contains("BELL", ignoreCase = true) ||
                s.contains("CHIME", ignoreCase = true) ||
                s.contains("DING", ignoreCase = true) ||
                s.contains("BICYCLE", ignoreCase = true) ||
                s.contains("BIKE", ignoreCase = true) ->
                "A bike is coming"

            s.contains("BUS", ignoreCase = true) -> "A bus is coming"
            s.contains("TRAIN", ignoreCase = true) -> "A train is approaching"

            // Keep remaining messages specific but short
            s.contains("DOG", ignoreCase = true) -> "Dog nearby"
            s.contains("FOOTSTEPS", ignoreCase = true) -> "Footsteps nearby"
            s.contains("DOOR", ignoreCase = true) -> "A door closed nearby"
            s.contains("CONSTRUCTION", ignoreCase = true) -> "Construction nearby"
            s.contains("TRAFFIC", ignoreCase = true) -> "Traffic nearby"

            else -> "Be aware"
        }
    }

    /**
     * Stop all active alerts - Called when user taps "I'm Safe"
     */
    fun stopAlerts() {
        Log.d(TAG, "🛑 Stopping all alerts...")
        
        // Stop continuous danger beeping
        stopDangerTone()
        
        // Stop TTS
        textToSpeech?.stop()

        // Restore any temporary TTS volume ducking
        restoreMusicVolumeAfterTts()
        
        // Stop vibration
        vibrator.cancel()
        Log.d(TAG, "Vibration cancelled")

        // Release audio focus (music will resume)
        releaseAudioFocus()

        // Stop and release tone generator
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
        
        // Clear any pending callbacks
        mainHandler.removeCallbacksAndMessages(null)

        Log.d(TAG, "✅ All alerts stopped")
    }

    /**
     * Release resources
     */
    fun release() {
        stopAlerts()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
