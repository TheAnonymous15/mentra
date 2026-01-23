package com.example.mentra.dialer

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.example.mentra.dialer.billing.BillingInfo
import com.example.mentra.dialer.billing.NexusBillCalculator
import com.example.mentra.dialer.proximity.NexusProximitySensorHandler
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
    @ApplicationContext private val context: Context,
    private val billCalculator: NexusBillCalculator,
    private val proximitySensorHandler: NexusProximitySensorHandler
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

    // Default dialer status
    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    // Flag to indicate shell-initiated call (no UI should be shown)
    private val _isShellCall = MutableStateFlow(false)
    val isShellCall: StateFlow<Boolean> = _isShellCall.asStateFlow()

    // Flag to indicate an in-call UI is already showing (prevents duplicate modals)
    private val _isCallUiShowing = MutableStateFlow(false)
    val isCallUiShowing: StateFlow<Boolean> = _isCallUiShowing.asStateFlow()

    // Billing info - exposed from NexusBillCalculator
    val currentBillingInfo: StateFlow<BillingInfo?> = billCalculator.currentBillingInfo
    val totalCallCost: StateFlow<Double> = billCalculator.totalCost
    val isBillingTracking: StateFlow<Boolean> = billCalculator.isTracking

    // Proximity sensor state
    val isProximityNear: StateFlow<Boolean> = proximitySensorHandler.isNear

    // Active call reference (managed by InCallService)
    private var activeCall: Call? = null

    // Track current call SIM slot for billing
    private var currentCallSimSlot: Int = -1

    init {
        loadAvailableSims()
        checkDefaultDialerStatus()
        // Register with provider so CallForegroundService can access shell call flag
        DialerManagerProvider.setDialerManager(this)
    }

    /**
     * Check if this app is the default dialer
     */
    fun checkDefaultDialerStatus(): Boolean {
        val isDefault = telecomManager?.defaultDialerPackage == context.packageName
        _isDefaultDialer.value = isDefault
        return isDefault
    }

    /**
     * Create an intent to request becoming the default dialer
     * The activity should start this intent for result
     */
    fun createDefaultDialerIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    /**
     * Check if the app can be set as default dialer
     */
    fun canRequestDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
        } else {
            true
        }
    }

    // ============================================
    // OUTGOING CALL HANDLING
    // ============================================

    /**
     * Place an outgoing call in background mode (no UI)
     * Used by shell terminal for call control via text commands
     * Uses only TelecomManager to avoid showing any system dialer UI
     *
     * @param phoneNumber The number to dial
     * @param simSlot SIM slot index
     * @return Result indicating success or failure
     */
    fun placeCallBackground(phoneNumber: String, simSlot: Int = -1): CallResult {
        // Permission check
        if (!hasCallPermission()) {
            android.util.Log.e("DialerManager", "CALL_PHONE permission not granted")
            return CallResult.Error(CallError.PERMISSION_DENIED)
        }

        // Validate phone number
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            android.util.Log.e("DialerManager", "Invalid phone number: $phoneNumber")
            return CallResult.Error(CallError.INVALID_NUMBER)
        }

        // Mark this as a shell call - no UI should be shown
        _isShellCall.value = true

        // Track SIM slot for billing
        currentCallSimSlot = simSlot

        // Update internal state
        val callStartTime = System.currentTimeMillis()
        _callState.value = CallState.DIALING
        _currentCall.value = CallInfo(
            number = normalizedNumber,
            direction = CallDirection.OUTGOING,
            state = CallState.DIALING,
            startTime = callStartTime,
            simSlot = simSlot
        )

        return try {
            android.util.Log.d("DialerManager", "Placing background call to: $normalizedNumber")

            // Get phone account handle for SIM selection
            val phoneAccountHandle: PhoneAccountHandle? = if (simSlot >= 0) {
                _availableSims.value.getOrNull(simSlot)?.phoneAccountHandle
            } else {
                null
            }

            // Use TelecomManager.placeCall() exclusively - no system dialer UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && telecomManager != null) {
                val extras = android.os.Bundle().apply {
                    if (phoneAccountHandle != null) {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                    }
                    putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
                }

                telecomManager?.placeCall(Uri.parse("tel:$normalizedNumber"), extras)
                android.util.Log.d("DialerManager", "Background call placed via TelecomManager (no UI)")
                CallResult.Success
            } else {
                android.util.Log.e("DialerManager", "TelecomManager not available for background call")
                _callState.value = CallState.IDLE
                _currentCall.value = null
                CallResult.Error(CallError.UNKNOWN, "TelecomManager not available")
            }

        } catch (e: SecurityException) {
            android.util.Log.e("DialerManager", "SecurityException placing background call - may need default dialer", e)
            _callState.value = CallState.IDLE
            _currentCall.value = null
            CallResult.Error(CallError.PERMISSION_DENIED, "Need to be default dialer for background calls")
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Exception placing background call", e)
            _callState.value = CallState.IDLE
            _currentCall.value = null
            CallResult.Error(CallError.UNKNOWN, e.message)
        }
    }

    /**
     * Place an outgoing call
     *
     * Strategy:
     * 1. Try TelecomManager.placeCall() - this is the most direct method
     * 2. Fallback to ACTION_CALL intent if needed
     *
     * Note: If we're not the default dialer, the system dialer UI will briefly appear.
     * Our InCallScreen should be shown IMMEDIATELY to take over visual control.
     *
     * @param phoneNumber The number to dial (will be normalized)
     * @param simSlot Optional SIM slot index (-1 for default)
     * @return Result indicating success or failure reason
     */
    fun placeCall(phoneNumber: String, simSlot: Int = -1): CallResult {
        // Permission check
        if (!hasCallPermission()) {
            android.util.Log.e("DialerManager", "CALL_PHONE permission not granted")
            return CallResult.Error(CallError.PERMISSION_DENIED)
        }

        // Validate phone number
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        if (normalizedNumber.isBlank()) {
            android.util.Log.e("DialerManager", "Invalid phone number: $phoneNumber")
            return CallResult.Error(CallError.INVALID_NUMBER)
        }

        // Check if it's a USSD code - delegate directly to system dialer
        // USSD codes should NOT show our call UI
        val isUssd = normalizedNumber.startsWith("*") ||
                     normalizedNumber.startsWith("#") ||
                     (normalizedNumber.contains("*") && normalizedNumber.contains("#"))

        if (isUssd) {
            android.util.Log.d("DialerManager", "Detected USSD code: $normalizedNumber - delegating to system dialer")
            return try {
                // Use Uri.fromParts to properly handle # and * characters
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.fromParts("tel", normalizedNumber, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    // Add SIM selection if specified
                    if (simSlot >= 0) {
                        val phoneAccountHandle = _availableSims.value.getOrNull(simSlot)?.phoneAccountHandle
                        if (phoneAccountHandle != null) {
                            putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                        }
                        putExtra("com.android.phone.extra.slot", simSlot)
                        putExtra("simSlot", simSlot)
                    }
                }
                context.startActivity(intent)
                CallResult.Success // Return success but don't update call state (no UI needed)
            } catch (e: Exception) {
                android.util.Log.e("DialerManager", "Failed to dial USSD", e)
                CallResult.Error(CallError.UNKNOWN, e.message)
            }
        }

        // Track SIM slot for billing
        currentCallSimSlot = simSlot

        // Update internal state FIRST - our UI should show immediately
        val callStartTime = System.currentTimeMillis()
        _callState.value = CallState.DIALING
        _currentCall.value = CallInfo(
            number = normalizedNumber,
            direction = CallDirection.OUTGOING,
            state = CallState.DIALING,
            startTime = callStartTime,
            simSlot = simSlot
        )

        return try {
            android.util.Log.d("DialerManager", "Placing call to: $normalizedNumber")

            // Get phone account handle for SIM selection
            val phoneAccountHandle: PhoneAccountHandle? = if (simSlot >= 0) {
                _availableSims.value.getOrNull(simSlot)?.phoneAccountHandle
            } else {
                null
            }

            // Method 1: Use TelecomManager.placeCall() (Android M+)
            // This is more direct and may minimize dialer UI appearance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val extras = android.os.Bundle().apply {
                        if (phoneAccountHandle != null) {
                            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                        }
                        putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, VideoProfile.STATE_AUDIO_ONLY)
                    }

                    telecomManager?.placeCall(Uri.parse("tel:$normalizedNumber"), extras)
                    android.util.Log.d("DialerManager", "Call placed via TelecomManager.placeCall()")
                    return CallResult.Success
                } catch (e: SecurityException) {
                    android.util.Log.d("DialerManager", "TelecomManager.placeCall() failed: ${e.message}, trying intent")
                }
            }

            // Method 2: Fallback to ACTION_CALL intent
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$normalizedNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION

                if (phoneAccountHandle != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                }
            }

            context.startActivity(callIntent)
            android.util.Log.d("DialerManager", "Call placed via ACTION_CALL intent")

            CallResult.Success

        } catch (e: SecurityException) {
            android.util.Log.e("DialerManager", "SecurityException placing call", e)
            _callState.value = CallState.IDLE
            _currentCall.value = null
            CallResult.Error(CallError.PERMISSION_DENIED, e.message)
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.e("DialerManager", "No activity found to handle call", e)
            _callState.value = CallState.IDLE
            _currentCall.value = null
            CallResult.Error(CallError.UNKNOWN, "No phone app found")
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Exception placing call", e)
            _callState.value = CallState.IDLE
            _currentCall.value = null
            CallResult.Error(CallError.UNKNOWN, e.message)
        }
    }

    /**
     * End the current call
     * Works in both default dialer and non-default dialer modes
     */
    fun endCall(): Boolean {
        android.util.Log.d("DialerManager", "endCall() called")

        return try {
            var success = false

            // Method 1: If we have an active Call object (default dialer mode)
            activeCall?.let { call ->
                android.util.Log.d("DialerManager", "Ending call via activeCall object")
                when (call.state) {
                    Call.STATE_RINGING -> call.reject(false, null)
                    else -> call.disconnect()
                }
                success = true
            }

            // Method 2: Use TelecomManager.endCall() (Android 9+) - works without being default dialer
            if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.util.Log.d("DialerManager", "Ending call via TelecomManager.endCall()")
                success = telecomManager?.endCall() ?: false
            }

            if (success) {
                _callState.value = CallState.DISCONNECTED
                _currentCall.value = null
                _isShellCall.value = false // Clear shell call flag
            }

            success
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Exception ending call", e)
            _isShellCall.value = false // Clear shell call flag on error too
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
    // CALL UI STATE MANAGEMENT
    // ============================================

    /**
     * Mark that an in-call UI is showing
     * Call this when showing a call modal from ConversationScreen, etc.
     * Prevents CallForegroundService from launching duplicate InCallActivity
     */
    fun setCallUiShowing(showing: Boolean) {
        _isCallUiShowing.value = showing
        android.util.Log.d("DialerManager", "Call UI showing: $showing")
    }

    /**
     * Check if call should show UI
     * Returns false if it's a shell call OR if UI is already showing
     */
    fun shouldShowCallUi(): Boolean {
        val result = !_isShellCall.value && !_isCallUiShowing.value
        android.util.Log.d("DialerManager", "Should show call UI: $result (shell=${_isShellCall.value}, uiShowing=${_isCallUiShowing.value})")
        return result
    }

    // ============================================
    // CALL CONTROLS
    // ============================================

    private val audioManager: android.media.AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
    }

    /**
     * Toggle mute state
     * Uses AudioManager directly which works regardless of default dialer status
     */
    fun toggleMute(): Boolean {
        return try {
            val newMuteState = !_audioState.value.isMuted

            // Method 1: Try InCallService if we're default dialer
            if (_isDefaultDialer.value && activeCall != null) {
                MentraInCallService.getInstance()?.setMutedState(newMuteState)
            }

            // Method 2: Use AudioManager directly (works always)
            audioManager?.isMicrophoneMute = newMuteState

            _audioState.value = _audioState.value.copy(isMuted = newMuteState)
            android.util.Log.d("DialerManager", "Mute toggled to: $newMuteState")
            true
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Failed to toggle mute", e)
            false
        }
    }

    /**
     * Toggle speaker
     * Uses AudioManager directly which works regardless of default dialer status
     */
    fun toggleSpeaker(): Boolean {
        return try {
            val currentRoute = _audioState.value.currentRoute
            val newRoute = if (currentRoute == AudioRoute.SPEAKER) {
                AudioRoute.EARPIECE
            } else {
                AudioRoute.SPEAKER
            }

            // Method 1: Try InCallService if we're default dialer
            if (_isDefaultDialer.value && activeCall != null) {
                val telecomRoute = if (newRoute == AudioRoute.SPEAKER) {
                    CallAudioState.ROUTE_SPEAKER
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                MentraInCallService.getInstance()?.setAudioRouteState(telecomRoute)
            }

            // Method 2: Use AudioManager directly (works always)
            audioManager?.isSpeakerphoneOn = (newRoute == AudioRoute.SPEAKER)

            _audioState.value = _audioState.value.copy(currentRoute = newRoute)
            android.util.Log.d("DialerManager", "Speaker toggled to: ${newRoute == AudioRoute.SPEAKER}")
            true
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Failed to toggle speaker", e)
            false
        }
    }

    /**
     * Set audio route
     * Uses both InCallService (if available) and AudioManager
     */
    fun setAudioRoute(route: AudioRoute): Boolean {
        return try {
            val telecomRoute = when (route) {
                AudioRoute.EARPIECE -> CallAudioState.ROUTE_EARPIECE
                AudioRoute.SPEAKER -> CallAudioState.ROUTE_SPEAKER
                AudioRoute.BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                AudioRoute.WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
            }

            // Method 1: Try InCallService if available
            if (_isDefaultDialer.value && activeCall != null) {
                MentraInCallService.getInstance()?.setAudioRouteState(telecomRoute)
            }

            // Method 2: Use AudioManager directly
            when (route) {
                AudioRoute.SPEAKER -> audioManager?.isSpeakerphoneOn = true
                AudioRoute.EARPIECE -> audioManager?.isSpeakerphoneOn = false
                else -> { /* Bluetooth/wired handled by system */ }
            }

            _audioState.value = _audioState.value.copy(currentRoute = route)
            true
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Failed to set audio route", e)
            false
        }
    }

    /**
     * Toggle hold state
     * Only works when we're the default dialer (requires Call object)
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
            } ?: run {
                android.util.Log.w("DialerManager", "Cannot toggle hold - no active call or not default dialer")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Failed to toggle hold", e)
            false
        }
    }

    /**
     * Send DTMF tone to the active call
     * Works whether we're the default dialer or not by trying multiple methods
     */
    fun sendDtmf(digit: Char): Boolean {
        return try {
            // Method 1: Use our tracked activeCall if available
            if (activeCall != null) {
                activeCall?.playDtmfTone(digit)
                activeCall?.stopDtmfTone()
                return true
            }

            // Method 2: Try to get calls from MentraInCallService
            val inCallService = MentraInCallService.getInstance()
            val calls = inCallService?.getCurrentCalls()
            if (!calls.isNullOrEmpty()) {
                val call = calls.first()
                call.playDtmfTone(digit)
                call.stopDtmfTone()
                return true
            }

            // Method 3: Use ToneGenerator to play DTMF audio that gets picked up by the call
            // This is a fallback that plays the tone through the audio system
            val toneType = when (digit) {
                '0' -> android.media.ToneGenerator.TONE_DTMF_0
                '1' -> android.media.ToneGenerator.TONE_DTMF_1
                '2' -> android.media.ToneGenerator.TONE_DTMF_2
                '3' -> android.media.ToneGenerator.TONE_DTMF_3
                '4' -> android.media.ToneGenerator.TONE_DTMF_4
                '5' -> android.media.ToneGenerator.TONE_DTMF_5
                '6' -> android.media.ToneGenerator.TONE_DTMF_6
                '7' -> android.media.ToneGenerator.TONE_DTMF_7
                '8' -> android.media.ToneGenerator.TONE_DTMF_8
                '9' -> android.media.ToneGenerator.TONE_DTMF_9
                '*' -> android.media.ToneGenerator.TONE_DTMF_S
                '#' -> android.media.ToneGenerator.TONE_DTMF_P
                else -> return false
            }

            // Play through voice call stream so it goes into the active call
            val toneGen = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_VOICE_CALL,
                100
            )
            toneGen.startTone(toneType, 200)
            // Small delay then release
            Thread.sleep(200)
            toneGen.release()
            true
        } catch (e: Exception) {
            android.util.Log.e("DialerManager", "Failed to send DTMF: $digit", e)
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

        // Stop billing if still tracking
        val finalBilling = billCalculator.stopTracking()

        // Disable proximity sensor
        proximitySensorHandler.disable()

        // Calculate duration if call was connected
        _currentCall.value?.let { info ->
            if (info.connectTime > 0) {
                val duration = System.currentTimeMillis() - info.connectTime
                // Log call to history with final cost
                logCallToHistory(info.copy(
                    state = CallState.DISCONNECTED,
                    duration = duration,
                    finalCost = finalBilling?.totalCost ?: 0.0
                ))
            }
        }

        activeCall = null
        _currentCall.value = null
        _callState.value = CallState.IDLE
        currentCallSimSlot = -1

        // Clear call UI and shell call flags
        _isShellCall.value = false
        _isCallUiShowing.value = false

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

    /**
     * Called when call state changes (from InCallService)
     */
    internal fun onCallStateChanged(call: Call, state: Int) {
        val newState = mapCallState(state)
        _callState.value = newState

        _currentCall.value = _currentCall.value?.copy(state = newState)

        when (state) {
            Call.STATE_DIALING, Call.STATE_RINGING -> {
                // Enable proximity sensor when call starts (dialing or ringing)
                proximitySensorHandler.enable()
            }
            Call.STATE_ACTIVE -> {
                // Call connected - start billing for OUTGOING calls only
                _currentCall.value = _currentCall.value?.copy(
                    connectTime = System.currentTimeMillis()
                )

                // Start billing only for outgoing calls (not USSD)
                val currentCallInfo = _currentCall.value
                if (currentCallInfo?.direction == CallDirection.OUTGOING &&
                    !isUssdCode(currentCallInfo.number)) {
                    billCalculator.startTracking(currentCallSimSlot)
                    android.util.Log.d("DialerManager", "Billing started for outgoing call")
                }
            }
            Call.STATE_DISCONNECTED -> {
                // Call ended - stop billing and proximity
                val finalBilling = billCalculator.stopTracking()
                proximitySensorHandler.disable()

                // Store final billing info for display
                if (finalBilling != null) {
                    _currentCall.value = _currentCall.value?.copy(
                        finalCost = finalBilling.totalCost
                    )
                    android.util.Log.d("DialerManager", "Call ended, final cost: ${finalBilling.getFormattedCost()}")
                }
            }
        }
    }

    /**
     * Check if there's an active call
     */
    fun hasActiveCall(): Boolean = activeCall != null && _callState.value == CallState.ACTIVE

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
     * Preserves USSD codes (* and #)
     */
    fun normalizePhoneNumber(number: String): String {
        if (number.isBlank()) return ""

        val trimmed = number.trim()

        // Check if it's a USSD code (starts with * or contains # or *)
        val isUssd = trimmed.startsWith("*") || trimmed.contains("*") || trimmed.contains("#")

        if (isUssd) {
            // For USSD codes, only remove spaces and keep *, #, and digits
            return trimmed.filter { it.isDigit() || it == '*' || it == '#' }
        }

        // Remove all non-digit characters except leading +
        val cleaned = trimmed.let { n ->
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

    /**
     * Check if a number is a USSD code
     * USSD codes typically start with * and end with #
     */
    fun isUssdCode(number: String): Boolean {
        val trimmed = number.trim()
        return trimmed.startsWith("*") ||
               trimmed.startsWith("#") ||
               (trimmed.contains("*") && trimmed.contains("#"))
    }

    /**
     * Get formatted current call cost
     */
    fun getFormattedCallCost(): String = billCalculator.getFormattedCost()

    /**
     * Get call cost display string with MNO info
     */
    fun getCallCostDisplay(): String = billCalculator.getEstimatedCostDisplay()

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
    val contactName: String? = null,
    val finalCost: Double = 0.0 // Final call cost in KSH
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

