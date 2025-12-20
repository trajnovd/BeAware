package com.beaware.app.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beaware.app.audio.SoundClassification
import com.beaware.app.data.PreferencesManager
import com.beaware.app.databinding.ActivityMainBinding
import com.beaware.app.service.AudioClassifierService
import com.beaware.app.R

/**
 * Main activity - Home screen with protection toggle and waveform visualization.
 */
class MainActivity : AppCompatActivity(), AudioClassifierService.ServiceListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    
    private var audioService: AudioClassifierService? = null
    private var isServiceBound = false
    private var isProtectionActive = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioClassifierService.LocalBinder
            audioService = binder.getService()
            audioService?.setServiceListener(this@MainActivity)
            isServiceBound = true
            updateUI(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService?.setServiceListener(null)
            audioService = null
            isServiceBound = false
            updateUI(false)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startProtection()
        } else {
            Toast.makeText(this, "Permissions required for protection", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        
        setupUI()
        checkServiceRunning()
    }

    private fun setupUI() {
        binding.btnPower.setOnClickListener {
            if (isProtectionActive) {
                stopProtection()
            } else {
                requestPermissionsAndStart()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            startProtection()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun startProtection() {
        // Check if emergency contact is set
        if (!preferencesManager.hasEmergencyContact()) {
            Toast.makeText(
                this,
                "Please set an emergency contact in Settings first",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        // Start the foreground service
        val serviceIntent = Intent(this, AudioClassifierService::class.java).apply {
            action = AudioClassifierService.ACTION_START
        }
        
        ContextCompat.startForegroundService(this, serviceIntent)
        
        // Bind to the service
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        preferencesManager.protectionEnabled = true
        isProtectionActive = true
        updateUI(true)
    }

    private fun stopProtection() {
        // Unbind from service
        if (isServiceBound) {
            audioService?.setServiceListener(null)
            unbindService(serviceConnection)
            isServiceBound = false
        }

        // Stop the service
        val serviceIntent = Intent(this, AudioClassifierService::class.java).apply {
            action = AudioClassifierService.ACTION_STOP
        }
        startService(serviceIntent)

        preferencesManager.protectionEnabled = false
        isProtectionActive = false
        audioService = null
        updateUI(false)
    }

    private fun checkServiceRunning() {
        // Try to bind to existing service
        if (preferencesManager.protectionEnabled) {
            val serviceIntent = Intent(this, AudioClassifierService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateUI(active: Boolean) {
        isProtectionActive = active
        
        binding.waveformView.setActive(active)
        
        if (active) {
            binding.tvStatus.text = getString(R.string.protection_active)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.safe_green))
            binding.tvStatusHint.text = getString(R.string.tap_to_stop)
            binding.btnPower.text = "STOP"
            binding.btnPower.setBackgroundColor(ContextCompat.getColor(this, R.color.button_active))
        } else {
            binding.tvStatus.text = getString(R.string.protection_inactive)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.inactive_gray))
            binding.tvStatusHint.text = getString(R.string.tap_to_start)
            binding.btnPower.text = "START"
            binding.btnPower.setBackgroundColor(ContextCompat.getColor(this, R.color.button_inactive))
            binding.waveformView.setAmplitude(0f)
        }
    }

    // AudioClassifierService.ServiceListener implementation
    override fun onClassification(classification: SoundClassification) {
        runOnUiThread {
            binding.cardLastDetection.visibility = android.view.View.VISIBLE
            binding.tvLastDetection.text = "${classification.displayName} - just now"
        }
    }

    override fun onAudioAmplitude(amplitude: Float) {
        runOnUiThread {
            binding.waveformView.setAmplitude(amplitude)
        }
    }

    override fun onServiceStateChanged(isRunning: Boolean) {
        runOnUiThread {
            updateUI(isRunning)
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDebugInfo(info: String) {
        // Debug info is shown via toast in AudioClassifier
        // This callback is for additional UI updates if needed
        runOnUiThread {
            // Update status hint with last detection info
            binding.tvStatusHint.text = "Listening... (check toasts)"
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-bind to service if protection was enabled
        if (preferencesManager.protectionEnabled && !isServiceBound) {
            val serviceIntent = Intent(this, AudioClassifierService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't unbind - let service run in background
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            audioService?.setServiceListener(null)
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

