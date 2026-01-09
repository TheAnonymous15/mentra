package com.example.mentra.messaging

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * SMS Receiver - Receives incoming SMS messages
 * Required for default SMS app functionality
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS received")

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { message ->
                Log.d(TAG, "From: ${message.displayOriginatingAddress}, Body: ${message.displayMessageBody}")

                // Notify the messaging service
                val serviceIntent = Intent(context, SmsReceiverService::class.java).apply {
                    action = "com.example.mentra.SMS_RECEIVED"
                    putExtra("sender", message.displayOriginatingAddress)
                    putExtra("body", message.displayMessageBody)
                    putExtra("timestamp", message.timestampMillis)
                }
                context.startService(serviceIntent)
            }
        }
    }
}

/**
 * SMS Deliver Receiver - Receives SMS_DELIVER broadcasts
 * This is the primary receiver for default SMS apps
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsDeliverReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            Log.d(TAG, "SMS delivered to default app")

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { message ->
                Log.d(TAG, "From: ${message.displayOriginatingAddress}")

                // Process the message - save to database, show notification, etc.
                val serviceIntent = Intent(context, SmsReceiverService::class.java).apply {
                    action = "com.example.mentra.SMS_DELIVER"
                    putExtra("sender", message.displayOriginatingAddress)
                    putExtra("body", message.displayMessageBody)
                    putExtra("timestamp", message.timestampMillis)
                }
                context.startService(serviceIntent)
            }
        }
    }
}

/**
 * SMS Reply Receiver - Handles notification actions
 * - Inline reply from notification
 * - Mark as read
 * - Open conversation
 */
class SmsReplyReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReplyReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val senderAddress = intent.getStringExtra(SmsNotificationManager.KEY_SENDER_ADDRESS)
        val notificationId = intent.getIntExtra(SmsNotificationManager.KEY_NOTIFICATION_ID, -1)

        Log.d(TAG, "Action received: ${intent.action}, sender: $senderAddress, notificationId: $notificationId")

        when (intent.action) {
            SmsNotificationManager.ACTION_REPLY -> handleReply(context, intent, senderAddress, notificationId)
            SmsNotificationManager.ACTION_MARK_READ -> handleMarkRead(context, senderAddress, notificationId)
            SmsNotificationManager.ACTION_OPEN_CONVERSATION -> handleOpenConversation(context, senderAddress, notificationId)
        }
    }

    private fun handleReply(context: Context, intent: Intent, senderAddress: String?, notificationId: Int) {
        if (senderAddress == null) {
            Log.e(TAG, "Cannot reply: sender address is null")
            return
        }

        // Get reply text from RemoteInput
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(SmsNotificationManager.KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) {
            Log.e(TAG, "Cannot reply: reply text is empty")
            return
        }

        Log.d(TAG, "Sending reply to $senderAddress: $replyText")

        try {
            // Send SMS
            val smsManager = SmsManager.getDefault()

            // Create sent and delivered intents for tracking
            val sentIntent = Intent("com.example.mentra.SMS_SENT").apply {
                putExtra("address", senderAddress)
                putExtra("body", replyText)
            }
            val sentPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 30000,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = Intent("com.example.mentra.SMS_DELIVERED").apply {
                putExtra("address", senderAddress)
                putExtra("body", replyText)
            }
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 40000,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Send the message
            smsManager.sendTextMessage(
                senderAddress,
                null,
                replyText,
                sentPendingIntent,
                deliveredPendingIntent
            )

            // Update notification to show reply was sent
            val notificationManager = SmsNotificationManager(context)
            notificationManager.updateNotificationReplySent(notificationId, replyText)

            Log.d(TAG, "Reply sent successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply", e)
        }
    }

    private fun handleMarkRead(context: Context, senderAddress: String?, notificationId: Int) {
        Log.d(TAG, "Marking conversation as read: $senderAddress")

        // Cancel the notification
        val notificationManager = SmsNotificationManager(context)
        notificationManager.cancelNotification(notificationId)

        // TODO: Mark messages as read in the database/content provider
    }

    private fun handleOpenConversation(context: Context, senderAddress: String?, notificationId: Int) {
        Log.d(TAG, "Opening conversation: $senderAddress")

        // Cancel the notification
        val notificationManager = SmsNotificationManager(context)
        notificationManager.cancelNotification(notificationId)

        // Open the app to the conversation
        // TODO: Implement deep link to conversation
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_conversation", senderAddress)
        }
        launchIntent?.let { context.startActivity(it) }
    }
}

/**
 * SMS Sent/Delivered Receiver - Tracks message delivery status
 */
class SmsSentReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsSentReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra("address")
        val body = intent.getStringExtra("body")

        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                Log.d(TAG, "SMS sent successfully to $address")
                // Broadcast to update UI
                val updateIntent = Intent("com.example.mentra.MESSAGE_STATUS_UPDATE").apply {
                    putExtra("address", address)
                    putExtra("status", "sent")
                }
                context.sendBroadcast(updateIntent)
            }
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                Log.e(TAG, "SMS failed: Generic failure")
            }
            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                Log.e(TAG, "SMS failed: No service")
            }
            SmsManager.RESULT_ERROR_NULL_PDU -> {
                Log.e(TAG, "SMS failed: Null PDU")
            }
            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                Log.e(TAG, "SMS failed: Radio off")
            }
        }
    }
}

/**
 * SMS Delivered Receiver - Confirms message was delivered to recipient
 */
class SmsDeliveredReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsDeliveredReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra("address")

        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                Log.d(TAG, "SMS delivered to $address")
                // Broadcast to update UI
                val updateIntent = Intent("com.example.mentra.MESSAGE_STATUS_UPDATE").apply {
                    putExtra("address", address)
                    putExtra("status", "delivered")
                }
                context.sendBroadcast(updateIntent)
            }
            android.app.Activity.RESULT_CANCELED -> {
                Log.e(TAG, "SMS not delivered to $address")
            }
        }
    }
}

/**
 * MMS Receiver - Receives MMS messages
 * Required for default SMS app functionality
 */
class MmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "MMS received: ${intent.action}")
        // MMS handling would go here
        // For now, just acknowledge receipt
    }
}

/**
 * MMS Push Receiver - Receives WAP_PUSH_DELIVER broadcasts
 * Required for default SMS app functionality
 */
class MmsPushReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsPushReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "MMS push received: ${intent.action}")
        // MMS push handling would go here
    }
}

