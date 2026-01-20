package com.example.mentra.dialer

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MENTRA INCALL SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * System-grade InCallService implementation for handling active calls.
 * This service is the bridge between the Telecom framework and our app.
 *
 * Key responsibilities:
 * - Receive call events from Telecom
 * - Detect Call.STATE_RINGING and start foreground service
 * - Manage call audio state
 * - Forward events to DialerManager
 *
 * IMPORTANT: Do NOT start UI or ringtone before STATE_RINGING is detected here
 */
class MentraInCallService : InCallService() {

    companion object {
        private const val TAG = "MentraInCallService"

        @Volatile
        private var instance: MentraInCallService? = null

        fun getInstance(): MentraInCallService? = instance
    }

    // Track current call and its callback
    private var currentCall: Call? = null
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallStateChanged(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "InCallService created")
    }

    override fun onDestroy() {
        instance = null
        Log.d(TAG, "InCallService destroyed")
        super.onDestroy()
    }

    /**
     * Called when a new call is added to the system
     * This is where we detect incoming/outgoing calls
     */
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val phoneNumber = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val callDirection = call.details?.callDirection
        Log.d(TAG, "Call added: $phoneNumber, state: ${call.state}, direction: $callDirection")

        // Register callback to track state changes
        currentCall = call
        call.registerCallback(callCallback)

        // Check initial state
        handleCallStateChanged(call, call.state)

        // Forward to DialerManager
        try {
            val dialerManager = DialerManagerProvider.getDialerManager()
            dialerManager?.onCallAdded(call)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding call added event", e)
        }
    }

    /**
     * Called when a call is removed from the system
     */
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details?.handle}")

        // Unregister callback
        call.unregisterCallback(callCallback)
        currentCall = null

        // Stop foreground service
        CallForegroundService.stop(this)

        // Forward to DialerManager
        try {
            val dialerManager = DialerManagerProvider.getDialerManager()
            dialerManager?.onCallRemoved(call)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding call removed event", e)
        }
    }

    /**
     * Handle call state changes
     * CRITICAL: This is where we detect STATE_RINGING and start the foreground service
     */
    private fun handleCallStateChanged(call: Call, state: Int) {
        val phoneNumber = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val contactName = lookupContactName(phoneNumber)

        Log.d(TAG, "Call state changed: $state for $phoneNumber")

        when (state) {
            Call.STATE_RINGING -> {
                // ⚠️ INCOMING CALL DETECTED - Start foreground service IMMEDIATELY
                Log.d(TAG, "STATE_RINGING detected - Starting foreground service")
                CallForegroundService.startIncomingCall(this, phoneNumber, contactName)
            }

            Call.STATE_DIALING -> {
                // Outgoing call started
                Log.d(TAG, "STATE_DIALING - Outgoing call")
                CallForegroundService.startOutgoingCall(this, phoneNumber, contactName)
            }

            Call.STATE_ACTIVE -> {
                // Call connected - stop ringtone (handled by foreground service)
                Log.d(TAG, "STATE_ACTIVE - Call connected")
                // Foreground service handles this via answer action
            }

            Call.STATE_DISCONNECTED -> {
                // Call ended - stop everything
                Log.d(TAG, "STATE_DISCONNECTED - Call ended")
                CallForegroundService.stop(this)
            }

            Call.STATE_HOLDING -> {
                Log.d(TAG, "STATE_HOLDING - Call on hold")
            }
        }

        // Forward to DialerManager for UI updates
        try {
            DialerManagerProvider.getDialerManager()?.onCallStateChanged(call, state)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding call state change", e)
        }
    }

    /**
     * Lookup contact name from phone number
     */
    private fun lookupContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lookup contact", e)
            null
        }
    }

    /**
     * Called when audio state changes
     */
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        Log.d(TAG, "Audio state changed: route=${audioState.route}, muted=${audioState.isMuted}")

        // Forward to DialerManager
        try {
            val dialerManager = DialerManagerProvider.getDialerManager()
            dialerManager?.onAudioStateChanged(audioState)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding audio state event", e)
        }
    }

    /**
     * Set mute state
     */
    fun setMutedState(muted: Boolean) {
        try {
            setMuted(muted)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mute", e)
        }
    }

    /**
     * Set audio route
     */
    fun setAudioRouteState(route: Int) {
        try {
            setAudioRoute(route)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting audio route", e)
        }
    }

    /**
     * Get current calls
     */
    fun getCurrentCalls(): List<Call> = calls

    /**
     * Check if there are active calls
     */
    fun hasActiveCalls(): Boolean = calls.isNotEmpty()

    /**
     * Answer the current call
     */
    fun answerCurrentCall() {
        currentCall?.answer(0) // 0 = video state none
    }

    /**
     * Reject the current call
     */
    fun rejectCurrentCall() {
        currentCall?.reject(false, null)
    }

    /**
     * Disconnect the current call
     */
    fun disconnectCurrentCall() {
        currentCall?.disconnect()
    }
}

/**
 * Provider for DialerManager instance
 * This allows the InCallService to access the DialerManager without direct dependency
 */
object DialerManagerProvider {
    private var dialerManager: DialerManager? = null

    fun setDialerManager(manager: DialerManager) {
        dialerManager = manager
    }

    fun getDialerManager(): DialerManager? = dialerManager
}

