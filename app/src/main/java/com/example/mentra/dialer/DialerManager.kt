package com.example.mentra.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core Dialer Manager
 *
 * System-grade telephony foundation built on Android's Telecom framework.
 *
 * Design Philosophy:
 * - Correct
 * - Deterministic
 * - Reliable
 * - System-respectful
 * - Built as infrastructure, not UI
 */
@Singleton
class DialerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telecomManager: TelecomManager? by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    }

    private val telephonyManager: TelephonyManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    private val subscriptionManager: SubscriptionManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    }

    // Call state management
    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    val currentCall: StateFlow<CallInfo?> = _currentCall.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _audioState = MutableStateFlow<AudioRouteState>(AudioRouteState())
    val audioState: StateFlow<AudioRouteState> = _audioState.asStateFlow()

    private val _availableSims = MutableStateFlow<List<SimAccount>>(emptyList())
    val availableSims: StateFlow<List<SimAccount>> = _availableSims.asStateFlow()

    // Active call reference (managed by InCallService)
    private var activeCall: Call? = null

    init {
        loadAvailableSims()
    }

    // ============================================
    // OUTGOING CALL HANDLING
    // ============================================

    /**
     * Place an outgoing call
     *
     * @param phoneNumber The number to dial (will be normalized)
     * @param simSlot Optional SIM slot index (-1 for default)
     * @return Result indicating success or failure reason
     */
    fun placeCall(phoneNumber: String, simSlot: Int = -1): CallResult {
        // Permission check
        if (!hasCallPermission()) {
            return CallResult.Error(CallError.PERMISSION_DENIED)
        }

        // Validate phone number
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            return CallResult.Error(CallError.INVALID_NUMBER)
        }

        // Check telecom availability
        val telecom = telecomManager ?: return CallResult.Error(CallError.TELECOM_UNAVAILABLE)

        try {
            val uri = Uri.fromParts("tel", normalizedNumber, null)
            val extras = android.os.Bundle()

            // Set SIM selection if specified
            if (simSlot >= 0) {
                val simAccount = _availableSims.value.getOrNull(simSlot)
                simAccount?.phoneAccountHandle?.let { handle ->
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
            }

            // Place call through TelecomManager
            telecom.placeCall(uri, extras)

            // Update state
            _callState.value = CallState.DIALING
            _currentCall.value = CallInfo(
                number = normalizedNumber,
                direction = CallDirection.OUTGOING,
                state = CallState.DIALING,
                startTime = System.currentTimeMillis(),
                simSlot = simSlot
            )

            return CallResult.Success

        } catch (e: SecurityException) {
            return CallResult.Error(CallError.PERMISSION_DENIED)
        } catch (e: Exception) {
            return CallResult.Error(CallError.UNKNOWN, e.message)
        }
    }

    /**
     * End the current call
     */
    fun endCall(): Boolean {
        return try {
            activeCall?.let { call ->
                when (call.state) {
                    Call.STATE_RINGING -> call.reject(false, null)
                    else -> call.disconnect()
                }
                true
            } ?: run {
                // Fallback: use TelecomManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecomManager?.endCall() ?: false
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Answer incoming call
     */
    fun answerCall(): Boolean {
        return try {
            activeCall?.let { call ->
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reject incoming call
     */
    fun rejectCall(rejectWithMessage: Boolean = false, textMessage: String? = null): Boolean {
        return try {
            activeCall?.let { call ->
                call.reject(rejectWithMessage, textMessage)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // ============================================
    // CALL CONTROLS
    // ============================================

    /**
     * Toggle mute state
     */
    fun toggleMute(): Boolean {
        return try {
            val newMuteState = !_audioState.value.isMuted
            activeCall?.let {
                // Mute is handled through InCallService
                MentraInCallService.getInstance()?.setMutedState(newMuteState)
                _audioState.value = _audioState.value.copy(isMuted = newMuteState)
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggle speaker
     */
    fun toggleSpeaker(): Boolean {
        return try {
            val currentRoute = _audioState.value.currentRoute
            val newRoute = if (currentRoute == AudioRoute.SPEAKER) {
                AudioRoute.EARPIECE
            } else {
                AudioRoute.SPEAKER
            }
            setAudioRoute(newRoute)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Set audio route
     */
    fun setAudioRoute(route: AudioRoute): Boolean {
        return try {
            val telecomRoute = when (route) {
                AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
                AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
                AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                AudioRoute.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
            }

            MentraInCallService.getInstance()?.setAudioRouteState(telecomRoute)
            _audioState.value = _audioState.value.copy(currentRoute = route)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggle hold state
     */
    fun toggleHold(): Boolean {
        return try {
            activeCall?.let { call ->
                if (call.state == Call.STATE_HOLDING) {
                    call.unhold()
                    _currentCall.value = _currentCall.value?.copy(isOnHold = false)
                } else {
                    call.hold()
                    _currentCall.value = _currentCall.value?.copy(isOnHold = true)
                }
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send DTMF tone
     */
    fun sendDtmf(digit: Char): Boolean {
        return try {
            activeCall?.playDtmfTone(digit)
            activeCall?.stopDtmfTone()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ============================================
    // CALL STATE MANAGEMENT (Called by InCallService)
    // ============================================

    internal fun onCallAdded(call: Call) {
        activeCall = call

        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val isIncoming = call.state == Call.STATE_RINGING

        _currentCall.value = CallInfo(
            number = number,
            direction = if (isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING,
            state = mapCallState(call.state),
            startTime = System.currentTimeMillis(),
            simSlot = -1
        )

        _callState.value = mapCallState(call.state)

        // Register callback for state changes
        call.registerCallback(callCallback)
    }

    internal fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)

        // Calculate duration if call was connected
        _currentCall.value?.let { info ->
            if (info.connectTime > 0) {
                val duration = System.currentTimeMillis() - info.connectTime
                // Log call to history
                logCallToHistory(info.copy(
                    state = CallState.DISCONNECTED,
                    duration = duration
                ))
            }
        }

        activeCall = null
        _currentCall.value = null
        _callState.value = CallState.IDLE

        // Reset audio state
        _audioState.value = AudioRouteState()
    }

    internal fun onAudioStateChanged(audioState: CallAudioState) {
        _audioState.value = AudioRouteState(
            currentRoute = mapAudioRoute(audioState.route),
            availableRoutes = getAvailableRoutes(audioState.supportedRouteMask),
            isMuted = audioState.isMuted
        )
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            val newState = mapCallState(state)
            _callState.value = newState

            _currentCall.value = _currentCall.value?.copy(
                state = newState,
                connectTime = if (state == Call.STATE_ACTIVE && _currentCall.value?.connectTime == 0L) {
                    System.currentTimeMillis()
                } else {
                    _currentCall.value?.connectTime ?: 0L
                }
            )
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            // Handle details changes if needed
        }
    }

    // ============================================
    // SIM MANAGEMENT
    // ============================================

    /**
     * Load available SIM cards
     */
    fun loadAvailableSims() {
        if (!hasPhonePermission()) {
            _availableSims.value = listOf(
                SimAccount(
                    slotIndex = 0,
                    subscriptionId = -1,
                    carrierName = "Default SIM",
                    phoneNumber = "",
                    phoneAccountHandle = null
                )
            )
            return
        }

        try {
            val accounts = mutableListOf<SimAccount>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                @Suppress("MissingPermission")
                val subscriptions = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()

                // Get phone accounts from TelecomManager
                val phoneAccounts = telecomManager?.callCapablePhoneAccounts ?: emptyList()

                subscriptions.forEachIndexed { index, info ->
                    val phoneAccountHandle = phoneAccounts.getOrNull(index)

                    accounts.add(
                        SimAccount(
                            slotIndex = info.simSlotIndex,
                            subscriptionId = info.subscriptionId,
                            carrierName = info.carrierName?.toString() ?: "SIM ${index + 1}",
                            phoneNumber = info.number ?: "",
                            phoneAccountHandle = phoneAccountHandle
                        )
                    )
                }
            }

            if (accounts.isEmpty()) {
                accounts.add(
                    SimAccount(
                        slotIndex = 0,
                        subscriptionId = -1,
                        carrierName = "Default SIM",
                        phoneNumber = "",
                        phoneAccountHandle = telecomManager?.callCapablePhoneAccounts?.firstOrNull()
                    )
                )
            }

            _availableSims.value = accounts

        } catch (e: Exception) {
            _availableSims.value = listOf(
                SimAccount(
                    slotIndex = 0,
                    subscriptionId = -1,
                    carrierName = "Default SIM",
                    phoneNumber = "",
                    phoneAccountHandle = null
                )
            )
        }
    }

    // ============================================
    // PHONE NUMBER HANDLING
    // ============================================

    /**
     * Normalize phone number to E.164 format
     */
    fun normalizePhoneNumber(number: String): String {
        if (number.isBlank()) return ""

        // Remove all non-digit characters except leading +
        val cleaned = number.trim().let { n ->
            if (n.startsWith("+")) {
                "+" + n.drop(1).filter { it.isDigit() }
            } else {
                n.filter { it.isDigit() }
            }
        }

        if (cleaned.isEmpty()) return ""

        // Try to format to E.164
        return try {
            val countryCode = telephonyManager?.networkCountryIso?.uppercase() ?: Locale.getDefault().country
            PhoneNumberUtils.formatNumberToE164(cleaned, countryCode) ?: cleaned
        } catch (e: Exception) {
            cleaned
        }
    }

    /**
     * Format number for display
     */
    fun formatNumberForDisplay(number: String): String {
        return try {
            val countryCode = telephonyManager?.networkCountryIso?.uppercase() ?: Locale.getDefault().country
            PhoneNumberUtils.formatNumber(number, countryCode) ?: number
        } catch (e: Exception) {
            number
        }
    }

    // ============================================
    // CALL LOG
    // ============================================

    private fun logCallToHistory(callInfo: CallInfo) {
        // Call logging is handled by the system
        // This is for internal tracking if needed
    }

    // ============================================
    // PERMISSION CHECKS
    // ============================================

    fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ============================================
    // UTILITY FUNCTIONS
    // ============================================

    private fun mapCallState(state: Int): CallState {
        return when (state) {
            Call.STATE_NEW -> CallState.INIT
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_HOLDING -> CallState.ACTIVE
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            Call.STATE_DISCONNECTING -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }
    }

    private fun mapAudioRoute(route: Int): AudioRoute {
        return when (route) {
            CallAudioState.ROUTE_EARPIECE -> AudioRoute.EARPIECE
            CallAudioState.ROUTE_SPEAKER -> AudioRoute.SPEAKER
            CallAudioState.ROUTE_BLUETOOTH -> AudioRoute.BLUETOOTH
            CallAudioState.ROUTE_WIRED_HEADSET -> AudioRoute.WIRED_HEADSET
            else -> AudioRoute.EARPIECE
        }
    }

    private fun getAvailableRoutes(mask: Int): List<AudioRoute> {
        val routes = mutableListOf<AudioRoute>()
        if (mask and CallAudioState.ROUTE_EARPIECE != 0) routes.add(AudioRoute.EARPIECE)
        if (mask and CallAudioState.ROUTE_SPEAKER != 0) routes.add(AudioRoute.SPEAKER)
        if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) routes.add(AudioRoute.BLUETOOTH)
        if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) routes.add(AudioRoute.WIRED_HEADSET)
        return routes
    }
}

// ============================================
// DATA CLASSES
// ============================================

/**
 * Call state following deterministic lifecycle:
 * IDLE → INIT → DIALING → RINGING → ACTIVE → DISCONNECTED
 */
enum class CallState {
    IDLE,           // No active call
    INIT,           // Call being initialized
    DIALING,        // Outgoing call dialing
    RINGING,        // Incoming call ringing
    ACTIVE,         // Call connected
    DISCONNECTED    // Call ended
}

enum class CallDirection {
    INCOMING,
    OUTGOING
}

enum class AudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET
}

data class CallInfo(
    val number: String,
    val direction: CallDirection,
    val state: CallState,
    val startTime: Long,
    val connectTime: Long = 0L,
    val duration: Long = 0L,
    val simSlot: Int = -1,
    val isOnHold: Boolean = false,
    val contactName: String? = null
)

data class AudioRouteState(
    val currentRoute: AudioRoute = AudioRoute.EARPIECE,
    val availableRoutes: List<AudioRoute> = listOf(AudioRoute.EARPIECE, AudioRoute.SPEAKER),
    val isMuted: Boolean = false
)

data class SimAccount(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val phoneNumber: String,
    val phoneAccountHandle: PhoneAccountHandle?
) {
    fun getLabel(): String = if (phoneNumber.isNotBlank()) {
        "$carrierName (${phoneNumber.takeLast(4)})"
    } else {
        "$carrierName - SIM ${slotIndex + 1}"
    }

    fun getShortLabel(): String = "SIM ${slotIndex + 1}"
}

// ============================================
// RESULT TYPES
// ============================================

sealed class CallResult {
    object Success : CallResult()
    data class Error(val error: CallError, val message: String? = null) : CallResult()
}

enum class CallError {
    PERMISSION_DENIED,
    INVALID_NUMBER,
    TELECOM_UNAVAILABLE,
    SIM_NOT_AVAILABLE,
    NETWORK_ERROR,
    UNKNOWN
}

