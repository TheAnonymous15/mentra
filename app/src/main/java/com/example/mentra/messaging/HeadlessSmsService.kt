package com.example.mentra.messaging

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

/**
 * Headless SMS Service
 * Handles "respond via message" functionality
 * Required for default SMS app functionality
 */
class HeadlessSmsService : Service() {

    companion object {
        private const val TAG = "HeadlessSmsService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val recipient = intent.dataString?.removePrefix("smsto:")?.removePrefix("sms:")
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)

            Log.d(TAG, "Respond via message to: $recipient")

            if (!recipient.isNullOrBlank() && !message.isNullOrBlank()) {
                try {
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(recipient, null, message, null, null)
                    Log.d(TAG, "Message sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message", e)
                }
            }
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }
}

