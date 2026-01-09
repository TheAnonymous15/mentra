package com.example.mentra.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call Log Manager
 *
 * Handles reading and managing call history with complete integrity.
 *
 * Required metadata per entry:
 * - Call direction (incoming / outgoing / missed)
 * - Duration
 * - Timestamp
 * - SIM used
 * - Call end reason
 */
@Singleton
class CallLogManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _callHistory = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callHistory: StateFlow<List<CallLogEntry>> = _callHistory.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    /**
     * Load call history from system
     */
    suspend fun loadCallHistory(limit: Int = 100) = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) {
            _callHistory.value = emptyList()
            return@withContext
        }

        try {
            val entries = mutableListOf<CallLogEntry>()

            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.CACHED_PHOTO_URI,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )

            val sortOrder = "${CallLog.Calls.DATE} DESC"

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "$sortOrder LIMIT $limit"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val photoIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
                val accountIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val number = cursor.getString(numberIndex) ?: ""
                    val name = cursor.getString(nameIndex)
                    val type = cursor.getInt(typeIndex)
                    val date = cursor.getLong(dateIndex)
                    val duration = cursor.getLong(durationIndex)
                    val photoUri = cursor.getString(photoIndex)
                    val accountId = cursor.getString(accountIndex)

                    entries.add(
                        CallLogEntry(
                            id = id,
                            number = number,
                            contactName = name,
                            callType = mapCallType(type),
                            timestamp = date,
                            duration = duration,
                            photoUri = photoUri,
                            simId = accountId,
                            isNew = type == CallLog.Calls.MISSED_TYPE
                        )
                    )
                }
            }

            _callHistory.value = entries
            _recentCalls.value = entries.take(20)

        } catch (e: Exception) {
            e.printStackTrace()
            _callHistory.value = emptyList()
        }
    }

    /**
     * Get calls for specific contact
     */
    suspend fun getCallsForNumber(phoneNumber: String): List<CallLogEntry> = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext emptyList()

        val normalizedNumber = phoneNumber.filter { it.isDigit() || it == '+' }

        return@withContext _callHistory.value.filter { entry ->
            val entryNumber = entry.number.filter { it.isDigit() || it == '+' }
            entryNumber.endsWith(normalizedNumber.takeLast(10)) ||
            normalizedNumber.endsWith(entryNumber.takeLast(10))
        }
    }

    /**
     * Delete call log entry
     */
    suspend fun deleteCallLogEntry(id: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext false

        try {
            val deleted = context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls._ID} = ?",
                arrayOf(id.toString())
            ) > 0

            if (deleted) {
                loadCallHistory()
            }

            return@withContext deleted
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Clear all call history
     */
    suspend fun clearCallHistory(): Boolean = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext false

        try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                null,
                null
            )
            _callHistory.value = emptyList()
            _recentCalls.value = emptyList()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Mark missed calls as read
     */
    suspend fun markMissedCallsAsRead() = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext

        try {
            val values = android.content.ContentValues().apply {
                put(CallLog.Calls.NEW, 0)
            }

            context.contentResolver.update(
                CallLog.Calls.CONTENT_URI,
                values,
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = 1",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            )

            // Refresh
            loadCallHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get missed call count
     */
    fun getMissedCallCount(): Int {
        return _callHistory.value.count {
            it.callType == CallType.MISSED && it.isNew
        }
    }

    /**
     * Get call statistics
     */
    fun getCallStatistics(): CallStatistics {
        val history = _callHistory.value

        return CallStatistics(
            totalCalls = history.size,
            incomingCalls = history.count { it.callType == CallType.INCOMING },
            outgoingCalls = history.count { it.callType == CallType.OUTGOING },
            missedCalls = history.count { it.callType == CallType.MISSED },
            rejectedCalls = history.count { it.callType == CallType.REJECTED },
            totalDuration = history.sumOf { it.duration },
            averageDuration = if (history.isNotEmpty()) {
                history.sumOf { it.duration } / history.size
            } else 0L
        )
    }

    private fun mapCallType(type: Int): CallType {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
            CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
            else -> CallType.UNKNOWN
        }
    }

    private fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Call Log Entry
 */
data class CallLogEntry(
    val id: Long,
    val number: String,
    val contactName: String?,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long, // in seconds
    val photoUri: String?,
    val simId: String?,
    val isNew: Boolean = false
) {
    fun getFormattedDuration(): String {
        if (duration == 0L) return "0:00"

        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun getFormattedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            diff < 604800_000 -> SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun getDisplayName(): String = contactName ?: number
}

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED,
    VOICEMAIL,
    UNKNOWN
}

data class CallStatistics(
    val totalCalls: Int,
    val incomingCalls: Int,
    val outgoingCalls: Int,
    val missedCalls: Int,
    val rejectedCalls: Int,
    val totalDuration: Long,
    val averageDuration: Long
)

