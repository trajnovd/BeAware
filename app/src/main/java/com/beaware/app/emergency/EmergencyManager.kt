package com.beaware.app.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.beaware.app.audio.UrgencyLevel
import com.beaware.app.data.PreferencesManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Manages emergency responses: SMS sending with location.
 */
class EmergencyManager(private val context: Context) {

    companion object {
        private const val TAG = "EmergencyManager"
    }

    private val preferencesManager = PreferencesManager(context)
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Listener for emergency action results
     */
    interface EmergencyListener {
        fun onSmsSent(success: Boolean, message: String)
        fun onLocationRetrieved(location: Location?)
    }

    private var listener: EmergencyListener? = null

    fun setListener(listener: EmergencyListener) {
        this.listener = listener
    }

    /**
     * Send emergency SMS based on urgency level
     */
    fun sendEmergencySms(urgencyLevel: UrgencyLevel, soundType: String) {
        val phoneNumber = preferencesManager.emergencyContact
        if (phoneNumber.isNullOrBlank()) {
            Log.e(TAG, "No emergency contact configured")
            listener?.onSmsSent(false, "No emergency contact configured")
            return
        }

        if (!hasSmsPermission()) {
            Log.e(TAG, "SMS permission not granted")
            listener?.onSmsSent(false, "SMS permission not granted")
            return
        }

        // Get location and send SMS
        getCurrentLocation { location ->
            val message = buildSmsMessage(urgencyLevel, soundType, location)
            sendSms(phoneNumber, message)
        }
    }

    /**
     * Build the SMS message based on urgency level
     */
    private fun buildSmsMessage(urgencyLevel: UrgencyLevel, soundType: String, location: Location?): String {
        val locationLink = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"

        return when (urgencyLevel) {
            UrgencyLevel.CRITICAL -> {
                "BeAware Emergency Alert: I am wearing noise-canceling headphones and a Level 1 danger ($soundType) was detected near me. My location: $locationLink"
            }
            UrgencyLevel.DANGER -> {
                "BeAware Dead Man's Switch Activated: I did not respond to a potential danger detected near me. My location: $locationLink. Audio recording started."
            }
            UrgencyLevel.WARNING -> {
                // Level 3 shouldn't trigger SMS, but just in case
                "BeAware Warning: A potential hazard ($soundType) was detected near me. My location: $locationLink"
            }
        }
    }

    /**
     * Send SMS to the specified phone number
     */
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            // Split message if too long
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                null,
                null
            )
            
            Log.d(TAG, "Emergency SMS sent to $phoneNumber")
            listener?.onSmsSent(true, "Emergency SMS sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            listener?.onSmsSent(false, "Failed to send SMS: ${e.message}")
        }
    }

    /**
     * Get current location
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            listener?.onLocationRetrieved(null)
            callback(null)
            return
        }

        val cancellationToken = CancellationTokenSource()
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            listener?.onLocationRetrieved(location)
            callback(location)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get location", e)
            listener?.onLocationRetrieved(null)
            callback(null)
        }
    }

    /**
     * Check if SMS permission is granted
     */
    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

