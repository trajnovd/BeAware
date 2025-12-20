package com.beaware.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build

/**
 * BeAware Application class
 * Handles global initialization including notification channels
 */
class BeAwareApplication : Application() {

    companion object {
        const val CHANNEL_ID_SERVICE = "beaware_service_channel"
        const val CHANNEL_ID_ALERTS = "beaware_alerts_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Foreground Service Channel (low importance - persistent but not intrusive)
        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Protection Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when BeAware protection is active"
            setShowBadge(false)
        }

        // Alert Channel (HIGH importance - for danger notifications with ALARM behavior)
        val alertChannel = NotificationChannel(
            CHANNEL_ID_ALERTS,
            "Danger Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical alerts when danger is detected - wakes screen like an alarm"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true) // Bypass Do Not Disturb
            
            // Set audio attributes for alarm-like behavior
            setSound(
                null, // We handle sound separately via ToneGenerator
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }
}
