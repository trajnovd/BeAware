package com.beaware.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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

        // Alert Channel (high importance - for danger notifications)
        val alertChannel = NotificationChannel(
            CHANNEL_ID_ALERTS,
            "Danger Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical alerts when danger is detected"
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }
}

