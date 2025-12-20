package com.beaware.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app preferences using SharedPreferences
 * Stores emergency contact and other user settings
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "beaware_prefs"
        private const val KEY_EMERGENCY_CONTACT = "emergency_contact"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_USE_SPEECH = "use_speech_announcements"
        
        // TTS category preferences
        private const val KEY_TTS_BELLS = "tts_bells_enabled"
        private const val KEY_TTS_SIRENS = "tts_sirens_enabled"
        private const val KEY_TTS_VEHICLES = "tts_vehicles_enabled"
        private const val KEY_TTS_GENERAL = "tts_general_enabled"
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the emergency contact phone number
     * @return Phone number string or null if not set
     */
    var emergencyContact: String?
        get() = prefs.getString(KEY_EMERGENCY_CONTACT, null)
        set(value) = prefs.edit().putString(KEY_EMERGENCY_CONTACT, value).apply()

    /**
     * Check if protection was enabled (to restore state on app restart)
     */
    var protectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    /**
     * Check if this is the first launch (for permission prompts)
     */
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    /**
     * Use speech announcements for Level 3 (AWARENESS) alerts
     * If true: "A bike is coming", "A bus is coming", etc.
     * If false: Just play a ping sound
     * Default: true (speech enabled)
     */
    var useSpeechAnnouncements: Boolean
        get() = prefs.getBoolean(KEY_USE_SPEECH, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_SPEECH, value).apply()

    // ==================== TTS CATEGORY PREFERENCES ====================
    
    /**
     * Enable TTS for bell/bike sounds (e.g., "A bike is coming")
     */
    var ttsBellsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_BELLS, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_BELLS, value).apply()
    
    /**
     * Enable TTS for siren sounds (e.g., "Ambulance nearby")
     */
    var ttsSirensEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_SIRENS, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_SIRENS, value).apply()
    
    /**
     * Enable TTS for vehicle sounds (e.g., "A bus is coming", "A train is approaching")
     */
    var ttsVehiclesEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_VEHICLES, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_VEHICLES, value).apply()
    
    /**
     * Enable TTS for general awareness sounds (e.g., "Dog nearby", "Footsteps nearby")
     */
    var ttsGeneralEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_GENERAL, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_GENERAL, value).apply()

    /**
     * Check if emergency contact has been configured
     */
    fun hasEmergencyContact(): Boolean {
        return !emergencyContact.isNullOrBlank()
    }

    /**
     * Clear all preferences (for testing/reset)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
