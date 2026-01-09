package com.example.mentra.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.mentra.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA SMS NOTIFICATION MANAGER
 * Handles message notifications with inline reply support
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class SmsNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "mentra_sms_channel"
        const val CHANNEL_NAME = "Messages"
        const val CHANNEL_DESCRIPTION = "SMS and MMS notifications"

        const val KEY_TEXT_REPLY = "key_text_reply"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_SENDER_ADDRESS = "sender_address"

        const val ACTION_REPLY = "com.example.mentra.ACTION_REPLY"
        const val ACTION_MARK_READ = "com.example.mentra.ACTION_MARK_READ"
        const val ACTION_OPEN_CONVERSATION = "com.example.mentra.ACTION_OPEN_CONVERSATION"

        private const val NOTIFICATION_GROUP = "mentra_sms_group"
        private const val SUMMARY_NOTIFICATION_ID = 0
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val activeNotifications = mutableMapOf<String, Int>() // address -> notificationId
    private var nextNotificationId = 1000

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = 0xFF00F5D4.toInt() // Cyan
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification for incoming SMS
     */
    fun showMessageNotification(
        senderAddress: String,
        senderName: String?,
        messageBody: String,
        timestamp: Long,
        isReplyable: Boolean = true
    ) {
        val notificationId = activeNotifications[senderAddress] ?: run {
            val newId = nextNotificationId++
            activeNotifications[senderAddress] = newId
            newId
        }

        val displayName = senderName ?: senderAddress

        // Create person for messaging style
        val person = Person.Builder()
            .setName(displayName)
            .setIcon(createAvatarIcon(displayName))
            .setKey(senderAddress)
            .build()

        // Create messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )
            .setConversationTitle(displayName)
            .addMessage(messageBody, timestamp, person)

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use proper SMS icon
            .setStyle(messagingStyle)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setColor(0xFF00F5D4.toInt()) // Cyan accent

        // Add open conversation action
        val openIntent = createOpenConversationIntent(senderAddress, notificationId)
        builder.setContentIntent(openIntent)

        // Add mark as read action
        val markReadIntent = createMarkReadIntent(senderAddress, notificationId)
        builder.addAction(
            NotificationCompat.Action.Builder(
                IconCompat.createWithResource(context, android.R.drawable.ic_menu_view),
                "Mark Read",
                markReadIntent
            ).build()
        )

        // Add inline reply action (only for replyable messages)
        if (isReplyable) {
            val replyAction = createReplyAction(senderAddress, notificationId)
            builder.addAction(replyAction)
        }

        // Show notification
        try {
            notificationManager.notify(notificationId, builder.build())

            // Update summary notification
            updateSummaryNotification()
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        }
    }

    /**
     * Create inline reply action
     */
    private fun createReplyAction(senderAddress: String, notificationId: Int): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply...")
            .build()

        val replyIntent = Intent(context, SmsReplyReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(KEY_SENDER_ADDRESS, senderAddress)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, android.R.drawable.ic_menu_send),
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }

    /**
     * Create open conversation intent
     */
    private fun createOpenConversationIntent(senderAddress: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, SmsReplyReceiver::class.java).apply {
            action = ACTION_OPEN_CONVERSATION
            putExtra(KEY_SENDER_ADDRESS, senderAddress)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }

        return PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Create mark as read intent
     */
    private fun createMarkReadIntent(senderAddress: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, SmsReplyReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(KEY_SENDER_ADDRESS, senderAddress)
            putExtra(KEY_NOTIFICATION_ID, notificationId)
        }

        return PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Update summary notification for grouped messages
     */
    private fun updateSummaryNotification() {
        if (activeNotifications.size <= 1) return

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("${activeNotifications.size} conversations"))
            .setGroup(NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Cancel notification for a conversation
     */
    fun cancelNotification(senderAddress: String) {
        activeNotifications[senderAddress]?.let { notificationId ->
            notificationManager.cancel(notificationId)
            activeNotifications.remove(senderAddress)

            if (activeNotifications.isEmpty()) {
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
            }
        }
    }

    /**
     * Cancel notification by ID
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        activeNotifications.entries.removeIf { it.value == notificationId }

        if (activeNotifications.isEmpty()) {
            notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        }
    }

    /**
     * Cancel all message notifications
     */
    fun cancelAllNotifications() {
        activeNotifications.values.forEach { notificationManager.cancel(it) }
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        activeNotifications.clear()
    }

    /**
     * Update notification to show reply sent
     */
    fun updateNotificationReplySent(notificationId: Int, replyText: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reply sent")
            .setContentText(replyText)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto dismiss after 3 seconds
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Create avatar icon from name
     */
    private fun createAvatarIcon(name: String): IconCompat {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = generateColorFromName(name)
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // Text
        val textPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.4f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, textY, textPaint)

        return IconCompat.createWithBitmap(bitmap)
    }

    /**
     * Generate consistent color from name
     */
    private fun generateColorFromName(name: String): Int {
        val colors = listOf(
            0xFF00F5D4.toInt(), // Cyan
            0xFF7B61FF.toInt(), // Purple
            0xFFFF6B6B.toInt(), // Coral
            0xFF4CAF50.toInt(), // Green
            0xFFFF9800.toInt(), // Orange
            0xFF2196F3.toInt(), // Blue
            0xFFE91E63.toInt(), // Pink
            0xFF9C27B0.toInt()  // Deep Purple
        )
        return colors[name.hashCode().mod(colors.size).let { if (it < 0) it + colors.size else it }]
    }
}

