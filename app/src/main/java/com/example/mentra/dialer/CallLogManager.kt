package com.example.mentra.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call Log Manager
 *
 * Handles reading and managing call history with complete integrity.
 * Monitors call log changes in REALTIME via ContentObserver.
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _callHistory = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callHistory: StateFlow<List<CallLogEntry>> = _callHistory.asStateFlow()

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls.asStateFlow()

    // ContentObserver for realtime call log updates
    private var callLogObserver: ContentObserver? = null
    private var isObserving = false

    init {
        // Start observing call log changes immediately
        startObservingCallLog()
    }

    /**
     * Start observing call log changes for realtime updates
     */
    fun startObservingCallLog() {
        if (isObserving) return
        if (!hasCallLogPermission()) return

        callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                // Reload call history when any change is detected
                scope.launch {
                    loadCallHistory()
                }
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Reload call history when any change is detected
                scope.launch {
                    loadCallHistory()
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true, // notifyForDescendants - observe all changes
                callLogObserver!!
            )
            isObserving = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop observing call log changes
     */
    fun stopObservingCallLog() {
        callLogObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        callLogObserver = null
        isObserving = false
    }

    /**
     * Load call history from system
     * Fetches ALL call logs without any limit - REALTIME
     */
    suspend fun loadCallHistory() = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) {
            _callHistory.value = emptyList()
            _recentCalls.value = emptyList()
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

            // Query ALL call logs - no selection, no limit
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,  // No selection - get ALL
                null,  // No selection args
                sortOrder  // Just sort by date, no LIMIT
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val photoIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
                val accountIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)

                // Load ALL entries
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val number = cursor.getString(numberIndex) ?: ""
                    val name = cursor.getString(nameIndex)
                    val type = cursor.getInt(typeIndex)
                    val date = cursor.getLong(dateIndex)
                    val duration = cursor.getLong(durationIndex)
                    val photoUri = cursor.getString(photoIndex)
                    val accountId = cursor.getString(accountIndex)

                    // Detect call source (WhatsApp, Telegram, etc.)
                    val callSource = CallSource.fromAccountId(accountId)

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
                            isNew = type == CallLog.Calls.MISSED_TYPE,
                            callSource = callSource
                        )
                    )
                }
            }

            // Update both flows with ALL entries
            _callHistory.value = entries
            _recentCalls.value = entries  // Same as callHistory - ALL logs

        } catch (e: Exception) {
            e.printStackTrace()
            _callHistory.value = emptyList()
            _recentCalls.value = emptyList()
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
    val isNew: Boolean = false,
    val callSource: CallSource = CallSource.PHONE // Identifies if call is from WhatsApp, Telegram, etc.
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

    /**
     * Check if this call was made via a social/VoIP app
     */
    fun isSocialCall(): Boolean = callSource != CallSource.PHONE && callSource != CallSource.UNKNOWN

    /**
     * Get display label for the call source
     */
    fun getSourceLabel(): String = callSource.displayName
}

/**
 * Call source - identifies the app that made the call
 */
enum class CallSource(val displayName: String, val packageHints: List<String>) {
    PHONE("Phone", listOf()),
    WHATSAPP("WhatsApp", listOf("whatsapp", "com.whatsapp")),
    TELEGRAM("Telegram", listOf("telegram", "org.telegram")),
    FACEBOOK("Messenger", listOf("facebook", "com.facebook.orca", "com.facebook.mlite")),
    VIBER("Viber", listOf("viber", "com.viber")),
    SIGNAL("Signal", listOf("signal", "org.thoughtcrime.securesms")),
    SKYPE("Skype", listOf("skype", "com.skype")),
    DISCORD("Discord", listOf("discord", "com.discord")),
    ZOOM("Zoom", listOf("zoom", "us.zoom")),
    GOOGLE_DUO("Google Duo", listOf("duo", "com.google.android.apps.tachyon")),
    GOOGLE_MEET("Google Meet", listOf("meet", "com.google.android.apps.meetings")),
    LINE("LINE", listOf("line", "jp.naver.line")),
    WECHAT("WeChat", listOf("wechat", "com.tencent.mm")),
    IMO("imo", listOf("imo", "com.imo")),
    BOTIM("BOTIM", listOf("botim", "com.algocian.botim")),
    TRUECALLER("Truecaller", listOf("truecaller", "com.truecaller")),
    UNKNOWN("Unknown", listOf());

    companion object {
        /**
         * Detect call source from phone account ID
         */
        fun fromAccountId(accountId: String?): CallSource {
            if (accountId.isNullOrBlank()) return PHONE

            val lowerAccountId = accountId.lowercase()

            // Check each source's package hints
            for (source in entries) {
                if (source.packageHints.any { hint -> lowerAccountId.contains(hint) }) {
                    return source
                }
            }

            // If account ID contains known VoIP indicators but not matched above
            if (lowerAccountId.contains("voip") ||
                lowerAccountId.contains("sip") ||
                lowerAccountId.contains("call")) {
                return UNKNOWN
            }

            return PHONE
        }
    }
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

