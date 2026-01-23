package com.example.mentra.shell.calling

import android.content.Context
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
