package com.example.mentra.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Incoming Call Handler
 *
 * Manages incoming call detection and state for showing the incoming call UI.
 * Works regardless of whether we're the default dialer by listening to phone state broadcasts.
 */
@Singleton
class IncomingCallHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dialerManager: DialerManager
) {
    companion object {
        private const val TAG = "IncomingCallHandler"
    }

    // Incoming call state
    private val _incomingCallState = MutableStateFlow<IncomingCallState>(IncomingCallState.NoCall)
    val incomingCallState: StateFlow<IncomingCallState> = _incomingCallState.asStateFlow()

    // Track if we're registered
    private var isReceiverRegistered = false
    private var phoneStateReceiver: BroadcastReceiver? = null


    /**
     * Start listening for incoming calls
     */
    fun startListening() {
        if (isReceiverRegistered) return

        phoneStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    handlePhoneStateChange(intent)
                }
            }
        }

        try {
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // System broadcasts like PHONE_STATE need RECEIVER_EXPORTED
                context.registerReceiver(phoneStateReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(phoneStateReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "Started listening for incoming calls")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
    }

    /**
     * Stop listening for incoming calls
     */
    fun stopListening() {
        if (!isReceiverRegistered) return

        try {
            phoneStateReceiver?.let { context.unregisterReceiver(it) }
            isReceiverRegistered = false
            Log.d(TAG, "Stopped listening for incoming calls")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister receiver", e)
        }
    }

    private fun handlePhoneStateChange(intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state changed: $state, number: $incomingNumber")

        // NOTE: We only track state here for UI updates.
        // DO NOT show notifications or play ringtones here!
        // MentraInCallService handles all incoming call UI via CallForegroundService
        // when it detects Call.STATE_RINGING from the Telecom framework.

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Incoming call - update state only
                val number = incomingNumber ?: "Unknown"
                val contactName = lookupContactName(number)

                _incomingCallState.value = IncomingCallState.Ringing(
                    phoneNumber = number,
                    contactName = contactName,
                    startTime = System.currentTimeMillis()
                )
                Log.d(TAG, "Incoming call from: $number ($contactName)")
                // NOTE: CallForegroundService is started by MentraInCallService
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered - transition to active call
                val currentState = _incomingCallState.value
                if (currentState is IncomingCallState.Ringing) {
                    _incomingCallState.value = IncomingCallState.Active(
                        phoneNumber = currentState.phoneNumber,
                        contactName = currentState.contactName,
                        connectedTime = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Call connected")
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                _incomingCallState.value = IncomingCallState.NoCall
                Log.d(TAG, "Call ended")
            }
        }
    }


    /**
     * Answer the incoming call
     */
    fun answerCall(): Boolean {
        return try {
            val answered = dialerManager.answerCall()
            if (answered) {
                val currentState = _incomingCallState.value
                if (currentState is IncomingCallState.Ringing) {
                    _incomingCallState.value = IncomingCallState.Active(
                        phoneNumber = currentState.phoneNumber,
                        contactName = currentState.contactName,
                        connectedTime = System.currentTimeMillis()
                    )
                }
            }
            answered
        } catch (e: Exception) {
            Log.e(TAG, "Failed to answer call", e)
            false
        }
    }

    /**
     * Reject the incoming call
     */
    fun rejectCall(): Boolean {
        return try {
            val rejected = dialerManager.rejectCall()
            if (rejected) {
                _incomingCallState.value = IncomingCallState.NoCall
            }
            rejected
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject call", e)
            false
        }
    }

    /**
     * End the active call
     */
    fun endCall(): Boolean {
        return try {
            val ended = dialerManager.endCall()
            if (ended) {
                _incomingCallState.value = IncomingCallState.NoCall
            }
            ended
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            false
        }
    }

    /**
     * Dismiss the incoming call UI without answering/rejecting
     */
    fun dismissIncomingCallUI() {
        _incomingCallState.value = IncomingCallState.NoCall
    }

    /**
     * Look up contact name from phone number
     */
    private fun lookupContactName(phoneNumber: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
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
}

/**
 * Incoming call state
 */
sealed class IncomingCallState {
    object NoCall : IncomingCallState()

    data class Ringing(
        val phoneNumber: String,
        val contactName: String?,
        val startTime: Long
    ) : IncomingCallState()

    data class Active(
        val phoneNumber: String,
        val contactName: String?,
        val connectedTime: Long
    ) : IncomingCallState()
}

