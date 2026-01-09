package com.example.mentra.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.mentra.MainActivity

/**
 * SMS Receiver Service
 * Background service for receiving SMS and showing notifications
 *
 * Features:
 * - Real-time SMS reception
 * - Smart notifications with inline reply
 * - Auto-refresh conversation list
 * - Notification grouping
 */
@AndroidEntryPoint
class SmsReceiverService : Service() {

    @Inject
    lateinit var smsManager: SmsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var smsReceiver: BroadcastReceiver? = null

    companion object {
        const val CHANNEL_ID = "mentra_sms_channel"
        const val CHANNEL_NAME = "SMS Notifications"
        const val NOTIFICATION_GROUP = "com.example.mentra.SMS_GROUP"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val ACTION_REPLY = "com.example.mentra.ACTION_REPLY"
        const val ACTION_MARK_READ = "com.example.mentra.ACTION_MARK_READ"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private val _incomingMessages = MutableSharedFlow<IncomingMessage>()
        val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages.asSharedFlow()

        fun start(context: Context) {
            val intent = Intent(context, SmsReceiverService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SmsReceiverService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerSmsReceiver()
        startForeground(1, createForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle reply action from notification
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterSmsReceiver()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SMS message notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mentra SMS")
            .setContentText("Listening for messages...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun registerSmsReceiver() {
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    handleIncomingSms(intent)
                }
            }
        }

        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        registerReceiver(smsReceiver, filter)
    }

    private fun unregisterSmsReceiver() {
        smsReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleIncomingSms(intent: Intent) {
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        val messages = mutableListOf<SmsMessage>()

        for (pdu in pdus) {
            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu as ByteArray)
            }
            messages.add(smsMessage)
        }

        if (messages.isNotEmpty()) {
            val sender = messages[0].displayOriginatingAddress
            val messageBody = messages.joinToString("") { it.messageBody }
            val timestamp = messages[0].timestampMillis

            val incomingMessage = IncomingMessage(
                sender = sender,
                body = messageBody,
                timestamp = timestamp
            )

            // Emit to flow for UI update
            serviceScope.launch {
                _incomingMessages.emit(incomingMessage)
                // Refresh conversations
                smsManager.loadConversations()
            }

            // Show notification
            showMessageNotification(incomingMessage)
        }
    }

    private fun showMessageNotification(message: IncomingMessage) {
        val notificationId = message.sender.hashCode()

        // Create reply action with RemoteInput
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to message...")
            .build()

        val replyIntent = Intent(this, SmsReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_PHONE_NUMBER, message.sender)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Mark as read action
        val markReadIntent = Intent(this, SmsReplyReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_PHONE_NUMBER, message.sender)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val markReadPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId + 1000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Mark as Read",
            markReadPendingIntent
        ).build()

        // Open conversation intent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_conversation", message.sender)
        }

        val openPendingIntent = PendingIntent.getActivity(
            this,
            notificationId + 2000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get contact name
        val contactName = getContactName(message.sender) ?: message.sender

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(contactName)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setWhen(message.timestamp)
            .setShowWhen(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )

            val cursor = contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_REPLY -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)

                if (!replyText.isNullOrBlank() && !phoneNumber.isNullOrBlank()) {
                    serviceScope.launch {
                        smsManager.sendSms(phoneNumber, replyText)
                        smsManager.loadConversations()
                    }
                }
            }
        }
    }
}

/**
 * Incoming message data class
 */
data class IncomingMessage(
    val sender: String,
    val body: String,
    val timestamp: Long
)
