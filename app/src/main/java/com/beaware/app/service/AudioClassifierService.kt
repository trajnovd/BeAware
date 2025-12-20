package com.beaware.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.beaware.app.BeAwareApplication
import com.beaware.app.R
import com.beaware.app.alert.AlertManager
import com.beaware.app.audio.BeAwareAudioClassifier
import com.beaware.app.audio.SoundClassification
import com.beaware.app.audio.UrgencyLevel
import com.beaware.app.ui.AlertOverlayActivity
import com.beaware.app.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service for continuous audio classification.
 * Listens for danger sounds and triggers appropriate alerts.
 * 
 * Uses FULL-SCREEN INTENT for Level 1 alerts to wake screen like an alarm.
 */
class AudioClassifierService : Service(), BeAwareAudioClassifier.ClassificationListener {

    companion object {
        private const val TAG = "AudioClassifierService"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 2001
        
        // Audio recording parameters (YAMNet requirements)
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // ⚡ YAMNet SWEET SPOT: 960ms chunks (model was trained on 0.96s windows)
        private const val CHUNK_DURATION_MS = 960
        private const val CHUNK_SIZE_SAMPLES = 15360  // Perfect YAMNet alignment
        
        // Actions
        const val ACTION_START = "com.beaware.app.action.START"
        const val ACTION_STOP = "com.beaware.app.action.STOP"
        
        // Wake lock tag
        private const val WAKE_LOCK_TAG = "BeAware:ServiceWakeLock"
    }

    private val binder = LocalBinder()
    private var audioClassifier: BeAwareAudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private var alertManager: AlertManager? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Wake lock for screen wakeup
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Track classification count
    private var classificationCount = 0

    /**
     * Listener for service events
     */
    interface ServiceListener {
        fun onClassification(classification: SoundClassification)
        fun onAudioAmplitude(amplitude: Float)
        fun onServiceStateChanged(isRunning: Boolean)
        fun onError(message: String)
        fun onDebugInfo(info: String)
    }

    private var serviceListener: ServiceListener? = null

    fun setServiceListener(listener: ServiceListener?) {
        this.serviceListener = listener
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioClassifierService = this@AudioClassifierService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service created")
        alertManager = AlertManager.getInstance(this)
        initializeClassifier()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "🛑 Stop action received")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                Log.d(TAG, "▶️ Start action received")
                startForegroundService()
                startAudioClassification()
            }
        }
        return START_STICKY
    }

    private fun initializeClassifier() {
        Log.d(TAG, "🎯 Initializing classifier...")
        audioClassifier = BeAwareAudioClassifier(this).apply {
            setListener(this@AudioClassifierService)
            if (!initialize()) {
                Log.e(TAG, "❌ Failed to initialize audio classifier")
                serviceListener?.onError("Failed to initialize audio classifier")
            } else {
                Log.d(TAG, "✅ Classifier initialized successfully")
            }
        }
    }

    private fun startForegroundService() {
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Log.d(TAG, "✅ Foreground service started")
    }

    private fun createNotification(): Notification {
        // Main tap action - open app
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(this, AudioClassifierService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BeAwareApplication.CHANNEL_ID_SERVICE)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.stop_protection),
                stopPendingIntent
            )
            .build()
    }

    /**
     * Get the best audio source for raw, unprocessed audio.
     */
    private fun getBestAudioSource(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "🎙️ Using UNPROCESSED audio source (raw ambient audio)")
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            Log.d(TAG, "🎙️ Using VOICE_RECOGNITION audio source (less filtering than MIC)")
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
    }

    private fun startAudioClassification() {
        if (!hasRecordPermission()) {
            Log.e(TAG, "❌ Audio recording permission not granted")
            serviceListener?.onError("Microphone permission required")
            stopSelf()
            return
        }

        if (isRecording) {
            Log.d(TAG, "⚠️ Already recording")
            return
        }

        serviceScope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val recordBufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_SAMPLES * 2)
                
                Log.d(TAG, "📊 Min buffer: $minBufferSize, Chunk size: $CHUNK_SIZE_SAMPLES samples (${CHUNK_DURATION_MS}ms)")
                
                val audioSource = getBestAudioSource()
                
                audioRecord = AudioRecord(
                    audioSource,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    recordBufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "❌ AudioRecord not initialized - trying fallback to MIC")
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        recordBufferSize
                    )
                    
                    if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                        withContext(Dispatchers.Main) {
                            serviceListener?.onError("Failed to initialize audio recording")
                        }
                        return@launch
                    }
                    Log.d(TAG, "⚠️ Fell back to standard MIC source")
                }

                audioRecord?.startRecording()
                isRecording = true
                
                withContext(Dispatchers.Main) {
                    serviceListener?.onServiceStateChanged(true)
                }

                Log.d(TAG, "🎙️ Audio recording started - ${CHUNK_DURATION_MS}ms chunks (~1 classification/sec)")

                val accumulationBuffer = ShortArray(CHUNK_SIZE_SAMPLES)
                var accumulatedSamples = 0
                val readBuffer = ShortArray(minBufferSize)

                while (isRecording && audioRecord != null) {
                    val readResult = audioRecord?.read(readBuffer, 0, minBufferSize) ?: -1
                    
                    if (readResult > 0) {
                        val amplitude = calculateAmplitude(readBuffer, readResult)
                        withContext(Dispatchers.Main) {
                            serviceListener?.onAudioAmplitude(amplitude)
                        }

                        val samplesToAdd = minOf(readResult, CHUNK_SIZE_SAMPLES - accumulatedSamples)
                        System.arraycopy(readBuffer, 0, accumulationBuffer, accumulatedSamples, samplesToAdd)
                        accumulatedSamples += samplesToAdd
                        
                        if (accumulatedSamples >= CHUNK_SIZE_SAMPLES) {
                            val floatBuffer = FloatArray(CHUNK_SIZE_SAMPLES)
                            for (i in 0 until CHUNK_SIZE_SAMPLES) {
                                floatBuffer[i] = accumulationBuffer[i] / 32768.0f
                            }
                            
                            classificationCount++
                            Log.d(TAG, "📤 Chunk #$classificationCount sent (${CHUNK_DURATION_MS}ms of audio)")
                            audioClassifier?.classifyAsync(floatBuffer, System.currentTimeMillis())
                            
                            accumulatedSamples = 0
                            
                            if (samplesToAdd < readResult) {
                                val remaining = readResult - samplesToAdd
                                System.arraycopy(readBuffer, samplesToAdd, accumulationBuffer, 0, remaining)
                                accumulatedSamples = remaining
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security exception during audio recording", e)
                withContext(Dispatchers.Main) {
                    serviceListener?.onError("Permission denied for audio recording")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during audio classification", e)
                withContext(Dispatchers.Main) {
                    serviceListener?.onError("Error: ${e.message}")
                }
            }
        }
    }

    private fun calculateAmplitude(buffer: ShortArray, size: Int): Float {
        var sum = 0L
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = kotlin.math.sqrt(sum.toDouble() / size)
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    private fun stopAudioClassification() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "🛑 Audio recording stopped")
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // BeAwareAudioClassifier.ClassificationListener implementation
    override fun onClassification(classification: SoundClassification) {
        Log.d(TAG, "🔔 Alert: ${classification.displayName} (Level ${classification.urgencyLevel.level})")
        
        // Notify listener
        serviceListener?.onClassification(classification)
        
        when (classification.urgencyLevel) {
            // 🔴 Level 1 (DANGER): Use FULL-SCREEN INTENT to wake screen like alarm
            UrgencyLevel.CRITICAL -> {
                Log.d(TAG, "🔴 DANGER - Triggering ALARM-style alert")
                alertManager?.triggerAlert(UrgencyLevel.CRITICAL, classification.displayName)
                showFullScreenAlertNotification(classification)
            }
            
            // 🟡 Level 2 (CAUTION): NO notification (per requirement). Only launch overlay + duck audio.
            UrgencyLevel.DANGER -> {
                Log.d(TAG, "🟡 CAUTION - Triggering alert")
                alertManager?.triggerAlert(UrgencyLevel.DANGER, classification.displayName)
                launchAlertOverlay(classification)
            }
            
            // 🟢 Level 3 (AWARENESS): NO notification, NO overlay. Just soft ping/TTS + light vibration.
            UrgencyLevel.WARNING -> {
                Log.d(TAG, "🟢 AWARENESS - Soft ping/TTS + light vibration, no overlay")
                alertManager?.triggerAlert(UrgencyLevel.WARNING, classification.displayName)
            }
        }
    }

    private fun launchAlertOverlay(classification: SoundClassification) {
        val intent = Intent(this, AlertOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlertOverlayActivity.EXTRA_SOUND_TYPE, classification.displayName)
            putExtra(AlertOverlayActivity.EXTRA_URGENCY_LEVEL, classification.urgencyLevel.level)
            putExtra(AlertOverlayActivity.EXTRA_CONFIDENCE, classification.confidence)
        }
        startActivity(intent)
    }
    
    /**
     * Show a FULL-SCREEN INTENT notification - this is how alarms wake the screen.
     * 
     * On Android 10+, this is the ONLY way to reliably wake the screen from a service.
     * The system will either:
     * 1. Launch the activity directly if the phone is locked/off (like an alarm)
     * 2. Show a heads-up notification if the user is actively using the phone
     */
    private fun showFullScreenAlertNotification(classification: SoundClassification) {
        // Wake up the screen first
        wakeScreen()
        
        // Create intent for the alert activity
        val alertIntent = Intent(this, AlertOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlertOverlayActivity.EXTRA_SOUND_TYPE, classification.displayName)
            putExtra(AlertOverlayActivity.EXTRA_URGENCY_LEVEL, classification.urgencyLevel.level)
            putExtra(AlertOverlayActivity.EXTRA_CONFIDENCE, classification.confidence)
        }
        
        // Full-screen pending intent (used when phone is locked/screen off)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 
            ALERT_NOTIFICATION_ID,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Content pending intent (used when notification is tapped)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            ALERT_NOTIFICATION_ID + 1,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification with FULL-SCREEN INTENT
        val notification = NotificationCompat.Builder(this, BeAwareApplication.CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ ${classification.urgencyLevel.displayName}")
            .setContentText("${classification.displayName} detected!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            // 🔔 THIS IS THE KEY: Full-screen intent wakes the screen like an alarm
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
        
        // Show the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        
        Log.d(TAG, "📢 Full-screen alert notification shown")
    }
    
    /**
     * Wake up the screen using a wake lock
     */
    private fun wakeScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                WAKE_LOCK_TAG
            )
            
            wakeLock?.acquire(60 * 1000L) // 60 second timeout
            Log.d(TAG, "📱 Screen wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    /**
     * Cancel the alert notification (called when alert is dismissed)
     */
    fun cancelAlertNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        
        // Release wake lock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
        
        Log.d(TAG, "🔕 Alert notification cancelled")
    }

    override fun onError(error: String) {
        Log.e(TAG, "❌ Classifier error: $error")
        serviceListener?.onError(error)
    }
    
    override fun onDebugInfo(info: String) {
        Log.d(TAG, "🔍 Debug: $info")
        serviceListener?.onDebugInfo(info)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioClassification()
        audioClassifier?.close()
        alertManager?.release()
        cancelAlertNotification()
        serviceScope.cancel()
        serviceListener?.onServiceStateChanged(false)
        Log.d(TAG, "💀 Service destroyed")
    }
}
