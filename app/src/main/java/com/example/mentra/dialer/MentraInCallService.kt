package com.example.mentra.dialer

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

/**
 * Mentra InCallService
 *
 * System-grade InCallService implementation for handling active calls.
 * This service is the bridge between the Telecom framework and our app.
 *
 * Key responsibilities:
 * - Receive call events from Telecom
 * - Manage call audio state
 * - Forward events to DialerManager
 */
class MentraInCallService : InCallService() {

    companion object {
        private const val TAG = "MentraInCallService"

        @Volatile
        private var instance: MentraInCallService? = null

        fun getInstance(): MentraInCallService? = instance
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
     */
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details?.handle}")

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

        // Forward to DialerManager
        try {
            val dialerManager = DialerManagerProvider.getDialerManager()
            dialerManager?.onCallRemoved(call)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding call removed event", e)
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

