package com.example.mentra.messaging

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SCHEDULED MESSAGE MANAGER
 * Manages scheduled SMS messages
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class ScheduledMessageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ScheduledMessageManager"
        private const val PREFS_NAME = "mentra_scheduled_messages"
        private const val KEY_SCHEDULED_MESSAGES = "scheduled_messages"
        const val ACTION_SEND_SCHEDULED = "com.example.mentra.ACTION_SEND_SCHEDULED"
        const val EXTRA_MESSAGE_ID = "message_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val _scheduledMessages = MutableStateFlow<List<ScheduledMessage>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledMessage>> = _scheduledMessages.asStateFlow()

    init {
        loadScheduledMessages()
    }

    /**
     * Schedule a message to be sent at a specific time
     */
    fun scheduleMessage(
        recipient: String,
        message: String,
        scheduledTime: Long,
        simId: Int = -1
    ): ScheduledMessage {
        val scheduledMessage = ScheduledMessage(
            id = UUID.randomUUID().toString(),
            recipient = recipient,
            message = message,
            scheduledTime = scheduledTime,
            simId = simId,
            status = ScheduledStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        // Add to list
        val messages = _scheduledMessages.value.toMutableList()
        messages.add(scheduledMessage)
        _scheduledMessages.value = messages

        // Persist
        saveScheduledMessages()

        // Set alarm
        setAlarm(scheduledMessage)

        Log.d(TAG, "Scheduled message ${scheduledMessage.id} for ${Date(scheduledTime)}")

        return scheduledMessage
    }

    /**
     * Cancel a scheduled message
     */
    fun cancelScheduledMessage(messageId: String) {
        val messages = _scheduledMessages.value.toMutableList()
        val index = messages.indexOfFirst { it.id == messageId }

        if (index >= 0) {
            val message = messages[index]
            cancelAlarm(message)
            messages.removeAt(index)
            _scheduledMessages.value = messages
            saveScheduledMessages()

            Log.d(TAG, "Cancelled scheduled message $messageId")
        }
    }

    /**
     * Update scheduled message status
     */
    fun updateMessageStatus(messageId: String, status: ScheduledStatus) {
        val messages = _scheduledMessages.value.toMutableList()
        val index = messages.indexOfFirst { it.id == messageId }

        if (index >= 0) {
            messages[index] = messages[index].copy(status = status)
            _scheduledMessages.value = messages
            saveScheduledMessages()
        }
    }

    /**
     * Get pending scheduled messages
     */
    fun getPendingMessages(): List<ScheduledMessage> {
        return _scheduledMessages.value.filter { it.status == ScheduledStatus.PENDING }
    }

    /**
     * Set alarm for scheduled message
     */
    private fun setAlarm(message: ScheduledMessage) {
        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply {
            action = ACTION_SEND_SCHEDULED
            putExtra(EXTRA_MESSAGE_ID, message.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                message.scheduledTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancel alarm for scheduled message
     */
    private fun cancelAlarm(message: ScheduledMessage) {
        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply {
            action = ACTION_SEND_SCHEDULED
            putExtra(EXTRA_MESSAGE_ID, message.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    /**
     * Save scheduled messages to storage
     */
    private fun saveScheduledMessages() {
        try {
            val json = gson.toJson(_scheduledMessages.value)
            prefs.edit().putString(KEY_SCHEDULED_MESSAGES, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load scheduled messages from storage
     */
    private fun loadScheduledMessages() {
        try {
            val json = prefs.getString(KEY_SCHEDULED_MESSAGES, null)
            if (json != null) {
                val type = object : TypeToken<List<ScheduledMessage>>() {}.type
                val messages: List<ScheduledMessage> = gson.fromJson(json, type)
                _scheduledMessages.value = messages

                // Re-schedule pending messages on app start
                messages.filter { it.status == ScheduledStatus.PENDING && it.scheduledTime > System.currentTimeMillis() }
                    .forEach { setAlarm(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Send a scheduled message (called from broadcast receiver)
     */
    fun sendScheduledMessage(messageId: String) {
        val message = _scheduledMessages.value.find { it.id == messageId } ?: return

        if (message.status != ScheduledStatus.PENDING) {
            Log.w(TAG, "Message $messageId is not pending, skipping")
            return
        }

        Log.d(TAG, "Sending scheduled message $messageId to ${message.recipient}")

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split message if too long
            val parts = smsManager.divideMessage(message.message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    message.recipient,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    message.recipient,
                    null,
                    message.message,
                    null,
                    null
                )
            }

            updateMessageStatus(messageId, ScheduledStatus.SENT)
            Log.d(TAG, "Scheduled message $messageId sent successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send scheduled message $messageId", e)
            updateMessageStatus(messageId, ScheduledStatus.FAILED)
        }
    }
}

/**
 * Scheduled message data class
 */
data class ScheduledMessage(
    val id: String,
    val recipient: String,
    val message: String,
    val scheduledTime: Long,
    val simId: Int,
    val status: ScheduledStatus,
    val createdAt: Long
)

/**
 * Scheduled message status
 */
enum class ScheduledStatus {
    PENDING,
    SENT,
    FAILED,
    CANCELLED
}

/**
 * Broadcast receiver for scheduled messages
 */
class ScheduledMessageReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduledMessageRcvr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ScheduledMessageManager.ACTION_SEND_SCHEDULED) {
            val messageId = intent.getStringExtra(ScheduledMessageManager.EXTRA_MESSAGE_ID)

            if (messageId != null) {
                Log.d(TAG, "Received alarm for scheduled message: $messageId")

                // Send the message
                val manager = ScheduledMessageManager(context)
                manager.sendScheduledMessage(messageId)
            }
        }
    }
}

