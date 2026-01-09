package com.example.mentra.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Call Broadcast Receiver
 *
 * Receives system broadcasts for phone state changes.
 * Used as a fallback mechanism for call detection.
 */
class CallBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneStateChange(context, intent)
            }
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                handleOutgoingCall(context, intent)
            }
        }
    }

    private fun handlePhoneStateChange(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state changed: $state, number: $number")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call
                number?.let {
                    IncomingCallNotificationManager.showIncomingCallNotification(context, it)
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered or outgoing call started
                IncomingCallNotificationManager.cancelIncomingCallNotification(context)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                IncomingCallNotificationManager.cancelIncomingCallNotification(context)
            }
        }
    }

    private fun handleOutgoingCall(context: Context, intent: Intent) {
        val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Outgoing call: $number")
    }
}

/**
 * Incoming Call Notification Manager
 *
 * Handles showing fullscreen incoming call notifications.
 * Works when screen is off, on lock screen, and during Doze mode.
 */
object IncomingCallNotificationManager {
    private const val NOTIFICATION_CHANNEL_ID = "mentra_incoming_call"
    private const val NOTIFICATION_ID = 9999

    fun showIncomingCallNotification(context: Context, phoneNumber: String) {
        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Incoming Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming calls"
                setSound(null, null) // Ringtone handled separately
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create fullscreen intent for incoming call UI
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("phone_number", phoneNumber)
        }

        val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action
        val answerIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ANSWER
            putExtra("phone_number", phoneNumber)
        }
        val answerPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 1, answerIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE
        }
        val declinePendingIntent = android.app.PendingIntent.getBroadcast(
            context, 2, declineIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }.apply {
            setSmallIcon(android.R.drawable.ic_menu_call)
            setContentTitle("Incoming Call")
            setContentText(phoneNumber)
            setCategory(android.app.Notification.CATEGORY_CALL)
            setFullScreenIntent(fullScreenPendingIntent, true)
            setOngoing(true)
            setAutoCancel(false)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_call),
                        "Answer",
                        answerPendingIntent
                    ).build()
                )
                addAction(
                    android.app.Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
                        "Decline",
                        declinePendingIntent
                    ).build()
                )
            }
        }.build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelIncomingCallNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

/**
 * Call Action Receiver
 *
 * Handles notification actions for answering/declining calls.
 */
class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER = "com.example.mentra.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.example.mentra.ACTION_DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val dialerManager = DialerManagerProvider.getDialerManager()

        when (intent.action) {
            ACTION_ANSWER -> {
                dialerManager?.answerCall()
                // Launch in-call UI
                context.startActivity(Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            ACTION_DECLINE -> {
                dialerManager?.rejectCall()
            }
        }

        // Cancel notification
        IncomingCallNotificationManager.cancelIncomingCallNotification(context)
    }
}

