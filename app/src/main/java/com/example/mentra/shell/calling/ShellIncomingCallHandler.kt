package com.example.mentra.shell.calling

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.example.mentra.dialer.DialerManager
import com.example.mentra.dialer.IncomingCallHandler
import com.example.mentra.dialer.IncomingCallState
import com.example.mentra.shell.models.ShellOutput
import com.example.mentra.shell.models.ShellOutputType
import com.example.mentra.shell.settings.ShellSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL INCOMING CALL HANDLER
 * Handles incoming calls directly in the shell terminal
 *
 * When "In-Shell Incoming Call" is enabled:
 * - Shows incoming call notification in shell
 * - A = Answer call
 * - R = Reject call
 * - Q = Quick reply (reject + send message)
 *
 * When call is active (answered):
 * - X = End call
 * - S = Toggle speaker
 * - M = Toggle mute
 * - 0-9, *, # = Send DTMF tones
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class ShellIncomingCallHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dialerManager: DialerManager,
    private val incomingCallHandler: IncomingCallHandler,
    private val shellSettings: ShellSettingsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State for shell incoming call handling
    private val _shellCallState = MutableStateFlow<ShellIncomingCallState>(ShellIncomingCallState.Idle)
    val shellCallState: StateFlow<ShellIncomingCallState> = _shellCallState.asStateFlow()

    // Track if we're waiting for quick reply message
    private val _awaitingQuickReply = MutableStateFlow(false)
    val awaitingQuickReply: StateFlow<Boolean> = _awaitingQuickReply.asStateFlow()

    // Store call info for quick reply
    private var lastIncomingNumber: String? = null
    private var lastIncomingName: String? = null
    private var lastSimSlot: Int = 0

    // Audio states
    private var isSpeaker = false
    private var isMuted = false

    // Call start time for duration tracking
    private var callStartTime: Long = 0L

    // Direct phone state receiver for faster detection
    private var phoneStateReceiver: android.content.BroadcastReceiver? = null
    private var isReceiverRegistered = false

    init {
        // Observe incoming call state when shell handling is enabled
        scope.launch {
            combine(
                shellSettings.inShellIncomingCall,
                incomingCallHandler.incomingCallState
            ) { shellEnabled, callState ->
                Pair(shellEnabled, callState)
            }.collect { (shellEnabled, callState) ->
                if (shellEnabled) {
                    handleIncomingCallStateChange(callState)
                } else {
                    // Not handling in shell - reset state
                    _shellCallState.value = ShellIncomingCallState.Idle
                }
            }
        }

        // Also observe DialerManager call state for better call end detection
        scope.launch {
            dialerManager.callState.collect { callState ->
                if (shellSettings.inShellIncomingCall.value) {
                    when (callState) {
                        com.example.mentra.dialer.CallState.IDLE,
                        com.example.mentra.dialer.CallState.DISCONNECTED -> {
                            // Call ended - reset shell state if not awaiting quick reply
                            val currentState = _shellCallState.value
                            if (currentState !is ShellIncomingCallState.AwaitingQuickReply &&
                                currentState !is ShellIncomingCallState.Idle) {
                                android.util.Log.d("ShellIncomingCallHandler",
                                    "Call ended via DialerManager - resetting state")
                                _shellCallState.value = ShellIncomingCallState.Idle
                                _awaitingQuickReply.value = false
                                isSpeaker = false
                                isMuted = false
                            }
                        }
                        else -> { /* Other states handled by IncomingCallHandler flow */ }
                    }
                }
            }
        }

        // Register direct phone state receiver for realtime detection
        registerPhoneStateReceiver()
    }

    /**
     * Register direct phone state receiver for faster incoming call detection
     */
    private fun registerPhoneStateReceiver() {
        if (isReceiverRegistered) return

        phoneStateReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                    val incomingNumber = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER)

                    android.util.Log.d("ShellIncomingCallHandler",
                        "Direct phone state: $state, number: $incomingNumber, shellEnabled: ${shellSettings.inShellIncomingCall.value}")

                    if (!shellSettings.inShellIncomingCall.value) return

                    when (state) {
                        android.telephony.TelephonyManager.EXTRA_STATE_RINGING -> {
                            // Incoming call - update immediately
                            val number = incomingNumber ?: lastIncomingNumber ?: "Unknown"
                            val contactName = lookupContactName(number)
                            lastIncomingNumber = number
                            lastIncomingName = contactName
                            lastSimSlot = detectSimSlot()

                            if (_shellCallState.value !is ShellIncomingCallState.Ringing) {
                                _shellCallState.value = ShellIncomingCallState.Ringing(
                                    phoneNumber = number,
                                    contactName = contactName,
                                    simSlot = lastSimSlot,
                                    startTime = System.currentTimeMillis()
                                )
                                android.util.Log.d("ShellIncomingCallHandler",
                                    "Shell incoming call detected: $number")
                            }
                        }
                        android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            // Call answered
                            val currentState = _shellCallState.value
                            if (currentState is ShellIncomingCallState.Ringing) {
                                callStartTime = System.currentTimeMillis()
                                isSpeaker = false
                                isMuted = false

                                _shellCallState.value = ShellIncomingCallState.Active(
                                    phoneNumber = currentState.phoneNumber,
                                    contactName = currentState.contactName,
                                    connectedTime = callStartTime
                                )
                            }
                        }
                        android.telephony.TelephonyManager.EXTRA_STATE_IDLE -> {
                            // Call ended - reset state
                            val currentState = _shellCallState.value
                            if (currentState !is ShellIncomingCallState.AwaitingQuickReply &&
                                currentState !is ShellIncomingCallState.Idle) {
                                android.util.Log.d("ShellIncomingCallHandler",
                                    "Call ended via phone state - resetting")
                                _shellCallState.value = ShellIncomingCallState.Idle
                                _awaitingQuickReply.value = false
                                isSpeaker = false
                                isMuted = false
                            }
                        }
                    }
                }
            }
        }

        try {
            val filter = android.content.IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // System broadcasts need RECEIVER_EXPORTED
                context.registerReceiver(phoneStateReceiver, filter, android.content.Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(phoneStateReceiver, filter)
            }
            isReceiverRegistered = true
            android.util.Log.d("ShellIncomingCallHandler", "Registered direct phone state receiver")
        } catch (e: Exception) {
            android.util.Log.e("ShellIncomingCallHandler", "Failed to register receiver", e)
        }
    }

    /**
     * Lookup contact name from phone number
     */
    private fun lookupContactName(phoneNumber: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if shell should handle incoming calls
     */
    fun isShellIncomingCallEnabled(): Boolean = shellSettings.inShellIncomingCall.value

    /**
     * Check if there's an active incoming call waiting for shell input
     */
    fun hasActiveIncomingCall(): Boolean {
        val state = _shellCallState.value
        return state is ShellIncomingCallState.Ringing ||
               state is ShellIncomingCallState.Active ||
               state is ShellIncomingCallState.AwaitingQuickReply
    }

    /**
     * Check if there's a pending incoming call that should be displayed
     * This is used when user navigates to shell while a call is ringing
     */
    fun hasPendingIncomingCall(): Boolean {
        return isShellIncomingCallEnabled() && _shellCallState.value is ShellIncomingCallState.Ringing
    }

    /**
     * Check if there's an active call in progress
     */
    fun hasActiveCall(): Boolean {
        return isShellIncomingCallEnabled() && _shellCallState.value is ShellIncomingCallState.Active
    }

    /**
     * Get the current call state for display when shell becomes visible
     * Returns outputs that should be shown immediately when shell is opened
     */
    fun getPendingCallDisplay(): List<ShellOutput> {
        if (!isShellIncomingCallEnabled()) return emptyList()
        return getCallStateDisplay()
    }

    /**
     * Force refresh call state - useful when shell becomes visible
     * This ensures the call state is re-emitted for the UI to pick up
     */
    fun refreshCallState() {
        val currentState = _shellCallState.value
        if (currentState !is ShellIncomingCallState.Idle) {
            // Re-emit the current state to trigger UI update
            _shellCallState.value = currentState
            android.util.Log.d("ShellIncomingCallHandler", "Refreshed call state: $currentState")
        }
    }

    /**
     * Handle state changes from IncomingCallHandler
     */
    private fun handleIncomingCallStateChange(callState: IncomingCallState) {
        when (callState) {
            is IncomingCallState.Ringing -> {
                lastIncomingNumber = callState.phoneNumber
                lastIncomingName = callState.contactName
                lastSimSlot = detectSimSlot()

                _shellCallState.value = ShellIncomingCallState.Ringing(
                    phoneNumber = callState.phoneNumber,
                    contactName = callState.contactName,
                    simSlot = lastSimSlot,
                    startTime = callState.startTime
                )
            }
            is IncomingCallState.Active -> {
                callStartTime = callState.connectedTime
                isSpeaker = false
                isMuted = false

                _shellCallState.value = ShellIncomingCallState.Active(
                    phoneNumber = callState.phoneNumber,
                    contactName = callState.contactName,
                    connectedTime = callState.connectedTime
                )
            }
            is IncomingCallState.NoCall -> {
                // Only transition to Idle if not awaiting quick reply
                if (_shellCallState.value !is ShellIncomingCallState.AwaitingQuickReply) {
                    _shellCallState.value = ShellIncomingCallState.Idle
                    _awaitingQuickReply.value = false
                    isSpeaker = false
                    isMuted = false
                }
            }
        }
    }

    /**
     * Get display output for current call state
     */
    fun getCallStateDisplay(): List<ShellOutput> {
        return when (val state = _shellCallState.value) {
            is ShellIncomingCallState.Idle -> emptyList()

            is ShellIncomingCallState.Ringing -> {
                val name = state.contactName ?: state.phoneNumber
                listOf(
                    ShellOutput(
                        "📞 INCOMING CALL from $name (${state.phoneNumber}) SIM ${state.simSlot + 1}",
                        ShellOutputType.WARNING
                    ),
                    ShellOutput(
                        "   ➤ A=Answer  R=Reject  Q=Quick Reply",
                        ShellOutputType.INFO
                    )
                )
            }

            is ShellIncomingCallState.Active -> {
                val name = state.contactName ?: state.phoneNumber
                val duration = formatDuration(System.currentTimeMillis() - state.connectedTime)
                val speakerIcon = if (isSpeaker) "🔊" else ""
                val muteIcon = if (isMuted) "🔇" else ""

                listOf(
                    ShellOutput(
                        "📞 ACTIVE: $name [$duration] $speakerIcon$muteIcon | X=End S=Spkr M=Mute 0-9=DTMF",
                        ShellOutputType.SUCCESS
                    )
                )
            }

            is ShellIncomingCallState.AwaitingQuickReply -> {
                listOf(
                    ShellOutput(
                        "📝 Quick reply to ${state.contactName ?: state.phoneNumber}:",
                        ShellOutputType.PROMPT
                    ),
                    ShellOutput(
                        "   Type your message and press Enter (or Ctrl+C to cancel)",
                        ShellOutputType.INFO
                    )
                )
            }
        }
    }

    /**
     * Handle user input for incoming call
     * Returns true if input was handled, false otherwise
     */
    suspend fun handleCallInput(input: String): Pair<Boolean, List<ShellOutput>> {
        val state = _shellCallState.value
        val trimmedInput = input.trim()

        return when (state) {
            is ShellIncomingCallState.Ringing -> {
                handleRingingInput(trimmedInput, state)
            }

            is ShellIncomingCallState.Active -> {
                handleActiveCallInput(trimmedInput, state)
            }

            is ShellIncomingCallState.AwaitingQuickReply -> {
                handleQuickReplyInput(trimmedInput, state)
            }

            is ShellIncomingCallState.Idle -> {
                Pair(false, emptyList())
            }
        }
    }

    /**
     * Handle input while phone is ringing
     */
    private fun handleRingingInput(
        input: String,
        state: ShellIncomingCallState.Ringing
    ): Pair<Boolean, List<ShellOutput>> {
        val inputLower = input.lowercase()

        return when {
            inputLower == "a" || inputLower == "answer" -> {
                val answered = incomingCallHandler.answerCall()
                if (answered) {
                    Pair(true, listOf(
                        ShellOutput("✅ Call answered", ShellOutputType.SUCCESS)
                    ))
                } else {
                    Pair(true, listOf(
                        ShellOutput("❌ Failed to answer call", ShellOutputType.ERROR)
                    ))
                }
            }

            inputLower == "r" || inputLower == "reject" -> {
                val rejected = incomingCallHandler.rejectCall()
                _shellCallState.value = ShellIncomingCallState.Idle
                if (rejected) {
                    Pair(true, listOf(
                        ShellOutput("📵 Call rejected", ShellOutputType.ERROR)
                    ))
                } else {
                    Pair(true, listOf(
                        ShellOutput("❌ Failed to reject call", ShellOutputType.ERROR)
                    ))
                }
            }

            inputLower == "q" || inputLower == "quick" || inputLower == "reply" -> {
                // Stop ringtone first
                try {
                    val stopRingtoneIntent = android.content.Intent("com.example.mentra.STOP_RINGTONE")
                    context.sendBroadcast(stopRingtoneIntent)
                    dialerManager.silenceRinger()
                } catch (e: Exception) {
                    // Ignore
                }

                // Reject call and prepare for quick reply
                incomingCallHandler.rejectCall()

                _shellCallState.value = ShellIncomingCallState.AwaitingQuickReply(
                    phoneNumber = state.phoneNumber,
                    contactName = state.contactName,
                    simSlot = state.simSlot
                )
                _awaitingQuickReply.value = true

                Pair(true, listOf(
                    ShellOutput("📵 Call rejected - sending quick reply", ShellOutputType.WARNING),
                    ShellOutput("📝 Enter message:", ShellOutputType.PROMPT)
                ))
            }

            else -> {
                // Unknown input - show help
                Pair(true, listOf(
                    ShellOutput("❓ Unknown. A=Answer R=Reject Q=Quick Reply", ShellOutputType.WARNING)
                ))
            }
        }
    }

    /**
     * Handle input during active call
     */
    private fun handleActiveCallInput(
        input: String,
        state: ShellIncomingCallState.Active
    ): Pair<Boolean, List<ShellOutput>> {
        val inputLower = input.lowercase()

        // Check for DTMF digits first
        if (input.length == 1 && input[0] in "0123456789*#") {
            val tone = input[0]
            val sent = dialerManager.sendDtmf(tone)
            return Pair(true, listOf(
                ShellOutput("📞 Sent: $tone${if (!sent) " (failed)" else ""}", ShellOutputType.INFO)
            ))
        }

        // Multi-digit DTMF
        if (input.all { it in "0123456789*#" } && input.isNotEmpty()) {
            input.forEach { dialerManager.sendDtmf(it) }
            return Pair(true, listOf(
                ShellOutput("📞 Sent: $input", ShellOutputType.INFO)
            ))
        }

        return when (inputLower) {
            "x", "end", "hangup", "cut" -> {
                val name = state.contactName ?: state.phoneNumber
                val duration = formatDuration(System.currentTimeMillis() - state.connectedTime)

                incomingCallHandler.endCall()
                _shellCallState.value = ShellIncomingCallState.Idle

                Pair(true, listOf(
                    ShellOutput("📵 Call ended: $name [$duration]", ShellOutputType.ERROR)
                ))
            }

            "s", "speaker" -> {
                isSpeaker = !isSpeaker
                toggleSpeaker(isSpeaker)

                Pair(true, listOf(
                    ShellOutput("🔊 Speaker ${if (isSpeaker) "ON" else "OFF"}", ShellOutputType.SUCCESS)
                ))
            }

            "m", "mute" -> {
                isMuted = !isMuted
                toggleMute(isMuted)

                Pair(true, listOf(
                    ShellOutput("🎤 Mic ${if (isMuted) "MUTED" else "ON"}", ShellOutputType.SUCCESS)
                ))
            }

            "h", "hold" -> {
                Pair(true, listOf(
                    ShellOutput("⏸️ Hold toggled (if supported)", ShellOutputType.INFO)
                ))
            }

            "?", "help" -> {
                Pair(true, listOf(
                    ShellOutput("📞 Call Controls: X=End S=Speaker M=Mute 0-9=DTMF", ShellOutputType.INFO)
                ))
            }

            else -> {
                Pair(true, listOf(
                    ShellOutput("❓ Unknown. X=End S=Spkr M=Mute 0-9=DTMF ?=Help", ShellOutputType.WARNING)
                ))
            }
        }
    }

    /**
     * Handle quick reply message input
     */
    private suspend fun handleQuickReplyInput(
        input: String,
        state: ShellIncomingCallState.AwaitingQuickReply
    ): Pair<Boolean, List<ShellOutput>> {
        if (input.isBlank()) {
            return Pair(true, listOf(
                ShellOutput("❌ Message cannot be empty", ShellOutputType.ERROR)
            ))
        }

        // Send the SMS
        val sent = sendQuickReply(state.phoneNumber, input, state.simSlot)

        _shellCallState.value = ShellIncomingCallState.Idle
        _awaitingQuickReply.value = false

        return if (sent) {
            Pair(true, listOf(
                ShellOutput("✅ Quick reply sent to ${state.contactName ?: state.phoneNumber}", ShellOutputType.SUCCESS)
            ))
        } else {
            Pair(true, listOf(
                ShellOutput("❌ Failed to send quick reply", ShellOutputType.ERROR)
            ))
        }
    }

    /**
     * Cancel quick reply mode
     */
    fun cancelQuickReply(): List<ShellOutput> {
        if (_shellCallState.value is ShellIncomingCallState.AwaitingQuickReply) {
            _shellCallState.value = ShellIncomingCallState.Idle
            _awaitingQuickReply.value = false
            return listOf(
                ShellOutput("📝 Quick reply cancelled", ShellOutputType.WARNING)
            )
        }
        return emptyList()
    }

    /**
     * Send SMS quick reply
     */
    private fun sendQuickReply(phoneNumber: String, message: String, simSlot: Int): Boolean {
        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val subscriptionId = getSubscriptionIdForSlot(simSlot)
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("ShellIncomingCall", "Failed to send quick reply", e)
            false
        }
    }

    /**
     * Get subscription ID for SIM slot
     */
    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            subscriptionManager.activeSubscriptionInfoList?.find { it.simSlotIndex == slotIndex }?.subscriptionId
                ?: SmsManager.getDefaultSmsSubscriptionId()
        } catch (e: Exception) {
            SmsManager.getDefaultSmsSubscriptionId()
        }
    }

    /**
     * Detect which SIM slot is receiving the call
     */
    private fun detectSimSlot(): Int {
        // Default to first SIM - actual detection would require more system access
        return 0
    }

    /**
     * Toggle speaker
     */
    private fun toggleSpeaker(enabled: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isSpeakerphoneOn = enabled
        } catch (e: Exception) {
            android.util.Log.e("ShellIncomingCall", "Failed to toggle speaker", e)
        }
    }

    /**
     * Toggle mute
     */
    private fun toggleMute(muted: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.isMicrophoneMute = muted
        } catch (e: Exception) {
            android.util.Log.e("ShellIncomingCall", "Failed to toggle mute", e)
        }
    }

    /**
     * Format duration as mm:ss or hh:mm:ss
     */
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    /**
     * Force end any active call (for cleanup)
     */
    fun forceEndCall() {
        incomingCallHandler.endCall()
        _shellCallState.value = ShellIncomingCallState.Idle
        _awaitingQuickReply.value = false
        isSpeaker = false
        isMuted = false
    }
}

/**
 * Shell incoming call state
 */
sealed class ShellIncomingCallState {
    object Idle : ShellIncomingCallState()

    data class Ringing(
        val phoneNumber: String,
        val contactName: String?,
        val simSlot: Int,
        val startTime: Long
    ) : ShellIncomingCallState()

    data class Active(
        val phoneNumber: String,
        val contactName: String?,
        val connectedTime: Long
    ) : ShellIncomingCallState()

    data class AwaitingQuickReply(
        val phoneNumber: String,
        val contactName: String?,
        val simSlot: Int
    ) : ShellIncomingCallState()
}
