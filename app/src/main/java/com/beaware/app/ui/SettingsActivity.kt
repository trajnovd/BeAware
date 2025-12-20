package com.beaware.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beaware.app.R
import com.beaware.app.data.PreferencesManager
import com.beaware.app.databinding.ActivitySettingsBinding

/**
 * Settings activity - Emergency contact configuration, alert preferences, and permissions status.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupToolbar()
        setupUI()
        loadSettings()
        updatePermissionStatus()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupUI() {
        // Save emergency contact button
        binding.btnSaveContact.setOnClickListener {
            saveEmergencyContact()
        }
        
        // Speech toggle listener
        binding.switchSpeech.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.useSpeechAnnouncements = isChecked
            updateTtsCategoriesVisibility(isChecked)
            val message = if (isChecked) {
                "Speech announcements enabled"
            } else {
                "Using ping sounds instead"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // TTS Category toggles
        binding.switchTtsBells.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.ttsBellsEnabled = isChecked
        }
        
        binding.switchTtsSirens.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.ttsSirensEnabled = isChecked
        }
        
        binding.switchTtsVehicles.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.ttsVehiclesEnabled = isChecked
        }
        
        binding.switchTtsGeneral.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.ttsGeneralEnabled = isChecked
        }
    }
    
    private fun updateTtsCategoriesVisibility(speechEnabled: Boolean) {
        val visibility = if (speechEnabled) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvTtsCategoriesHeader.visibility = visibility
        binding.cardTtsCategories.visibility = visibility
    }

    private fun loadSettings() {
        // Load emergency contact
        preferencesManager.emergencyContact?.let { contact ->
            binding.etEmergencyContact.setText(contact)
        }
        
        // Load speech preference
        binding.switchSpeech.isChecked = preferencesManager.useSpeechAnnouncements
        
        // Load TTS category preferences
        binding.switchTtsBells.isChecked = preferencesManager.ttsBellsEnabled
        binding.switchTtsSirens.isChecked = preferencesManager.ttsSirensEnabled
        binding.switchTtsVehicles.isChecked = preferencesManager.ttsVehiclesEnabled
        binding.switchTtsGeneral.isChecked = preferencesManager.ttsGeneralEnabled
        
        // Show/hide TTS categories based on speech announcements toggle
        updateTtsCategoriesVisibility(preferencesManager.useSpeechAnnouncements)
    }

    private fun saveEmergencyContact() {
        val phoneNumber = binding.etEmergencyContact.text.toString().trim()
        
        if (phoneNumber.isEmpty()) {
            binding.tilEmergencyContact.error = getString(R.string.invalid_phone)
            return
        }

        // Basic phone number validation
        if (!isValidPhoneNumber(phoneNumber)) {
            binding.tilEmergencyContact.error = getString(R.string.invalid_phone)
            return
        }

        binding.tilEmergencyContact.error = null
        preferencesManager.emergencyContact = phoneNumber
        
        Toast.makeText(this, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show()
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Accept numbers with at least 7 digits (after removing non-digit chars)
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return digitsOnly.length >= 7
    }

    private fun updatePermissionStatus() {
        // Microphone permission
        val micGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        updatePermissionText(binding.tvMicPermission, micGranted)

        // Location permission
        val locationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        updatePermissionText(binding.tvLocationPermission, locationGranted)

        // SMS permission
        val smsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        updatePermissionText(binding.tvSmsPermission, smsGranted)
    }

    private fun updatePermissionText(textView: android.widget.TextView, granted: Boolean) {
        if (granted) {
            textView.text = "Granted"
            textView.setTextColor(ContextCompat.getColor(this, R.color.safe_green))
        } else {
            textView.text = "Not Granted"
            textView.setTextColor(ContextCompat.getColor(this, R.color.alert_red))
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }
}
