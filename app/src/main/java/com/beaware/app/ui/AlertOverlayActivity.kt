package com.beaware.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.beaware.app.R
import com.beaware.app.alert.AlertManager
import com.beaware.app.audio.UrgencyLevel
import com.beaware.app.data.PreferencesManager
import com.beaware.app.databinding.ActivityAlertOverlayBinding
import com.beaware.app.emergency.EmergencyManager
import com.beaware.app.service.AudioClassifierService

/**
 * Full-screen alert overlay activity - behaves like an ALARM.
 * 
 * 🔴 Level 1 (DANGER): 
 *    - Wakes up screen even when locked (via full-screen intent)
 *    - Full red screen, 30-second countdown to SOS
 *    - Music STOPS, continuous beeping
 *    - Dismissing restores music playback
 * 
 * 🟡 Level 2 (CAUTION): 
 *    - Wakes screen, shows over lock screen
 *    - Auto-dismisses after 5 seconds
 *    - Music at 20%, double ping
 */
class AlertOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_URGENCY_LEVEL = "extra_urgency_level"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        
        // Level 2 (CAUTION) auto-dismiss delay
        private const val LEVEL_2_AUTO_DISMISS_MS = 5000L
        
        // Alert notification ID (must match service)
        private const val ALERT_NOTIFICATION_ID = 2001
        
        // Wake lock tag
        private const val WAKE_LOCK_TAG = "BeAware:AlertWakeLock"
    }

    private lateinit var binding: ActivityAlertOverlayBinding
    private lateinit var alertManager: AlertManager
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var preferencesManager: PreferencesManager
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Wake lock to keep screen on during alert
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Service connection
    private var audioService: AudioClassifierService? = null
    private var serviceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioClassifierService.LocalBinder
            audioService = binder?.getService()
            serviceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }

    private var countdownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var soundType: String = ""
    private var urgencyLevel: UrgencyLevel = UrgencyLevel.CRITICAL
    
    // Runnable for auto-dismissing Level 2 alerts
    private val autoDismissRunnable = Runnable {
        if (urgencyLevel == UrgencyLevel.DANGER) {
            cancelAlert()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔔 ALARM BEHAVIOR: Show over lock screen
        setupAlarmBehavior()

        binding = ActivityAlertOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alertManager = AlertManager.getInstance(this)
        emergencyManager = EmergencyManager(this)
        preferencesManager = PreferencesManager(this)
        
        // Bind to service to access cancelAlertNotification
        bindToService()

        setupBackPressHandler()
        parseIntent()
        
        // Level 3 (AWARENESS) should not show overlay - close immediately
        if (urgencyLevel == UrgencyLevel.WARNING) {
            releaseWakeLock()
            finish()
            return
        }
        
        setupUI()
        triggerAlert()
    }
    
    private fun bindToService() {
        val intent = Intent(this, AudioClassifierService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * Setup alarm-like behavior:
     * - Turn screen ON
     * - Show OVER lock screen
     * - Keep screen awake
     * - Dismiss keyguard (on supported devices)
     */
    private fun setupAlarmBehavior() {
        // Acquire wake lock to keep screen on
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock?.acquire(60 * 1000L) // 60 second timeout
        
        // Show over lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            
            // Dismiss keyguard (allows showing without unlock)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        // Keep screen on while alert is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Make sure activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
    }
    
    /**
     * Release the wake lock
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Level 1 (DANGER): Cannot dismiss with back button
                // Level 2 (CAUTION): Can dismiss with back button
                if (urgencyLevel != UrgencyLevel.CRITICAL) {
                    cancelAlert()
                }
                // Level 1: Force user to tap "I'm Safe"
            }
        })
    }

    private fun parseIntent() {
        soundType = intent.getStringExtra(EXTRA_SOUND_TYPE) ?: "UNKNOWN"
        val levelInt = intent.getIntExtra(EXTRA_URGENCY_LEVEL, 1)
        urgencyLevel = UrgencyLevel.fromLevel(levelInt)
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)

        binding.tvSoundType.text = "$soundType DETECTED"
        binding.tvConfidence.text = "Confidence: ${(confidence * 100).toInt()}%"
    }

    private fun setupUI() {
        when (urgencyLevel) {
            // 🔴 LEVEL 1: DANGER - Safety Mode (like an ALARM)
            // Full red screen, 30-second countdown to SOS
            UrgencyLevel.CRITICAL -> {
                binding.tvAlertTitle.text = "🔴 ${urgencyLevel.displayName}"
                binding.tvAlertTitle.setTextColor(ContextCompat.getColor(this, R.color.alert_red))
                binding.tvCountdown.setTextColor(ContextCompat.getColor(this, R.color.alert_red))
                binding.tvCountdownLabel.text = "Sending SOS in:"
                binding.countdownContainer.visibility = View.VISIBLE
                binding.btnImSafe.text = "I'M SAFE"
                startPulseAnimation()
            }
            
            // 🟡 LEVEL 2: CAUTION - Potential Danger
            // Orange screen, no countdown, AUTO-DISMISS after 5 seconds
            UrgencyLevel.DANGER -> {
                binding.tvAlertTitle.text = "🟡 ${urgencyLevel.displayName}"
                binding.tvAlertTitle.setTextColor(ContextCompat.getColor(this, R.color.alert_orange))
                binding.countdownContainer.visibility = View.GONE
                binding.btnImSafe.text = "DISMISS"
                binding.viewPulsingBg.setBackgroundColor(ContextCompat.getColor(this, R.color.alert_orange))
                
                // Auto-dismiss after 5 seconds
                mainHandler.postDelayed(autoDismissRunnable, LEVEL_2_AUTO_DISMISS_MS)
            }
            
            // 🟢 LEVEL 3: AWARENESS - Should not reach here
            UrgencyLevel.WARNING -> {
                releaseWakeLock()
                finish()
                return
            }
        }

        // Show the user message from UrgencyLevel
        binding.tvConfidence.text = urgencyLevel.userMessage

        binding.btnImSafe.setOnClickListener {
            cancelAlert()
        }
    }

    private fun triggerAlert() {
        // NOTE: Alerts (audio focus, sound, vibration) are already triggered by the SERVICE
        // The Activity only handles UI (countdown, buttons)

        // Start countdown only for Level 1 (DANGER) - 30 seconds
        if (urgencyLevel == UrgencyLevel.CRITICAL && urgencyLevel.countdownSeconds > 0) {
            startCountdown(urgencyLevel.countdownSeconds)
        }
    }

    private fun startCountdown(seconds: Int) {
        binding.tvCountdown.text = seconds.toString()
        
        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.tvCountdown.text = secondsRemaining.toString()
            }

            override fun onFinish() {
                binding.tvCountdown.text = "0"
                onCountdownComplete()
            }
        }.start()
    }

    private fun onCountdownComplete() {
        // Send emergency SMS
        emergencyManager.sendEmergencySms(urgencyLevel, soundType)
        
        // Update UI to show SMS sent
        binding.countdownContainer.visibility = View.GONE
        binding.smsSentContainer.visibility = View.VISIBLE
        binding.tvSmsRecipient.text = "Sent to: ${preferencesManager.emergencyContact}"
        
        // Change button to dismiss
        binding.btnImSafe.text = "DISMISS"
        binding.btnImSafe.setOnClickListener {
            // Stop alerts and cleanup
            cancelAlert()
        }
    }

    /**
     * Cancel the alert - stops sounds, releases audio focus (music resumes), dismisses UI
     */
    private fun cancelAlert() {
        // Remove auto-dismiss callback
        mainHandler.removeCallbacks(autoDismissRunnable)
        
        // Stop countdown
        countdownTimer?.cancel()
        countdownTimer = null

        // Stop alerts (this releases audio focus -> MUSIC RESUMES)
        alertManager.stopAlerts()
        pulseAnimator?.cancel()
        
        // Cancel the notification
        cancelAlertNotification()
        
        // Tell the service to cancel its notification too
        audioService?.cancelAlertNotification()
        
        // Release wake lock
        releaseWakeLock()

        finish()
    }
    
    /**
     * Cancel the alert notification
     */
    private fun cancelAlertNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    private fun startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofFloat(binding.viewPulsingBg, "alpha", 0f, 0.3f, 0f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(autoDismissRunnable)
        countdownTimer?.cancel()
        pulseAnimator?.cancel()
        releaseWakeLock()
        
        // Unbind from service
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore
            }
            serviceBound = false
        }
    }
}
