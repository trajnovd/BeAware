package com.beaware.app.audio

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions

/**
 * Wrapper for the MediaPipe Audio Classifier (YAMNet model).
 * Handles audio classification for danger sound detection.
 * 
 * KEY FEATURES:
 * - Priority filtering: Danger sounds override Speech/Silence even at lower confidence
 * - Different thresholds for different sound types
 * - Cooldown to prevent alert spam
 */
class BeAwareAudioClassifier(private val context: Context) {

    companion object {
        private const val TAG = "BeAwareAudioClassifier"
        private const val MODEL_PATH = "yamnet.tflite"
        private const val MAX_RESULTS = 15  // Get more results for priority filtering
        const val SAMPLE_RATE = 16000
        
        // Cooldown period to prevent repeated alerts for the same sound
        private const val COOLDOWN_MS = 5000L
        
        // 🎯 PRIORITY THRESHOLDS: Lower for danger sounds, higher for common sounds
        // This gives danger sounds priority over speech/silence
        private const val THRESHOLD_CRITICAL = 0.20f   // Sirens, gunshots - trigger at 20%
        private const val THRESHOLD_DANGER = 0.25f     // Screaming, shouting - trigger at 25%
        private const val THRESHOLD_WARNING = 0.30f    // Car horns, bells - trigger at 30%
        
        // Labels to ALWAYS IGNORE (these override everything)
        private val IGNORED_LABELS = setOf(
            "silence",
            "speech",
            "conversation",
            "narration, monologue",
            "music",
            "singing",
            "humming",
            "whistling",
            "breathing",
            "cough",
            "sneeze",
            "laughter",
            "chatter",
            "crowd",
            "background noise"
        )
    }

    private var audioClassifier: AudioClassifier? = null
    private val cooldownMap = mutableMapOf<String, Long>()

    /**
     * Listener for classification results
     */
    interface ClassificationListener {
        fun onClassification(classification: SoundClassification)
        fun onError(error: String)
        fun onDebugInfo(info: String)
    }

    private var listener: ClassificationListener? = null

    fun setListener(listener: ClassificationListener) {
        this.listener = listener
    }

    /**
     * Initialize the audio classifier with streaming mode
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "🎯 Initializing audio classifier...")
            
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(MAX_RESULTS)
                .setRunningMode(RunningMode.AUDIO_STREAM)
                .setResultListener { result: AudioClassifierResult ->
                    processResult(result)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "❌ Classifier error: ${error.message}")
                    listener?.onError(error.message ?: "Unknown error")
                }
                .build()

            audioClassifier = AudioClassifier.createFromOptions(context, options)
            Log.d(TAG, "✅ Audio classifier initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize audio classifier", e)
            listener?.onError("Failed to initialize: ${e.message}")
            false
        }
    }

    /**
     * Classify audio data asynchronously
     */
    fun classifyAsync(audioBuffer: FloatArray, timestampMs: Long) {
        try {
            val audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(SAMPLE_RATE.toFloat())
                    .build(),
                audioBuffer.size
            )
            audioData.load(audioBuffer)
            audioClassifier?.classifyAsync(audioData, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error classifying audio: ${e.message}", e)
        }
    }

    /**
     * Process classification results from YAMNet with PRIORITY FILTERING.
     * 
     * Priority logic:
     * 1. First, scan ALL results for danger sounds (even at low confidence)
     * 2. If a danger sound is found above its threshold, TRIGGER immediately
     * 3. Only log non-danger sounds if no danger was found
     */
    private fun processResult(result: AudioClassifierResult) {
        val currentTime = System.currentTimeMillis()
        
        val classifications = result.classificationResults().firstOrNull()?.classifications()?.firstOrNull()
        
        if (classifications == null) {
            Log.w(TAG, "⚠️ No classifications in result")
            return
        }
        
        val categories = classifications.categories()
        if (categories.isEmpty()) {
            Log.w(TAG, "⚠️ Empty categories list")
            return
        }
        
        // 🎯 PRIORITY FILTERING: Scan ALL results for danger sounds first
        var priorityMatch: SoundClassification? = null
        
        for (category in categories) {
            val label = category.categoryName()
            val score = category.score()
            
            // Skip ignored labels (speech, silence, music, etc.)
            if (IGNORED_LABELS.any { label.lowercase().contains(it) }) {
                continue
            }
            
            // Check if this is a danger sound
            val (urgency, displayName) = SoundClassification.getUrgencyForLabel(label) ?: continue
            
            // Apply priority thresholds based on urgency level
            val threshold = when (urgency) {
                UrgencyLevel.CRITICAL -> THRESHOLD_CRITICAL  // 20% for sirens
                UrgencyLevel.DANGER -> THRESHOLD_DANGER      // 25% for screaming
                UrgencyLevel.WARNING -> THRESHOLD_WARNING    // 30% for horns
            }
            
            if (score >= threshold) {
                priorityMatch = SoundClassification(
                    label = label,
                    displayName = displayName,
                    confidence = score,
                    urgencyLevel = urgency
                )
                Log.d(TAG, "🎯 Priority match found: $displayName ($label) = ${(score * 100).toInt()}% (threshold: ${(threshold * 100).toInt()}%)")
                break  // Take the first (highest confidence) danger match
            }
        }
        
        // If we found a danger sound, trigger alert
        if (priorityMatch != null) {
            val lastTrigger = cooldownMap[priorityMatch.displayName] ?: 0L
            if (currentTime - lastTrigger >= COOLDOWN_MS) {
                cooldownMap[priorityMatch.displayName] = currentTime
                Log.d(TAG, "🚨 ALERT TRIGGERED: ${priorityMatch.displayName} " +
                        "(${(priorityMatch.confidence * 100).toInt()}%, Level ${priorityMatch.urgencyLevel.level})")
                listener?.onClassification(priorityMatch)
            } else {
                Log.d(TAG, "⏳ Cooldown active for ${priorityMatch.displayName}")
            }
            return
        }
        
        // No danger found - just log top results (no toast)
        val top3 = categories.take(3)
        val debugInfo = StringBuilder("Ambient: ")
        top3.forEach { category ->
            val pct = (category.score() * 100).toInt()
            debugInfo.append("${category.categoryName()}($pct%) ")
        }
        Log.v(TAG, debugInfo.toString())
    }

    /**
     * Clear cooldown for testing purposes
     */
    fun clearCooldown() {
        cooldownMap.clear()
    }

    /**
     * Clean up resources
     */
    fun close() {
        audioClassifier?.close()
        audioClassifier = null
        cooldownMap.clear()
    }
}
