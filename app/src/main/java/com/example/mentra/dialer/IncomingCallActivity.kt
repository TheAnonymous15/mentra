package com.example.mentra.dialer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.mentra.dialer.ui.IncomingCallPopup
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen call activity
 *
 * This activity handles incoming calls (ringing state) AND active calls.
 * It bypasses the lock screen and shows the slide-to-answer UI,
 * then transitions to in-call controls after answering.
 */
@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window to show on lock screen
        setupLockScreenBypass()

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)

        setContent {
            MentraTheme {
                // Create ringing state from intent
                val ringingState = remember {
                    IncomingCallState.Ringing(
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        startTime = System.currentTimeMillis()
                    )
                }

                // Track multiple calls state
                var hasMultipleCalls by remember { mutableStateOf(false) }
                var isConference by remember { mutableStateOf(false) }
                var conferenceParticipants by remember { mutableStateOf<List<String>>(emptyList()) }
                var shouldDismiss by remember { mutableStateOf(false) }

                // Monitor call state changes
                LaunchedEffect(Unit) {
                    // Check for call state periodically
                    while (!shouldDismiss) {
                        val calls = MentraInCallService.getInstance()?.getCurrentCalls() ?: emptyList()

                        // Filter out disconnected calls - only consider active/ringing/holding calls
                        val activeCalls = calls.filter { call ->
                            call.state != android.telecom.Call.STATE_DISCONNECTED &&
                            call.state != android.telecom.Call.STATE_DISCONNECTING
                        }

                        // Only dismiss if there are NO active calls remaining
                        if (activeCalls.isEmpty()) {
                            // All calls have ended - dismiss the activity
                            shouldDismiss = true
                            break
                        }

                        // Update UI state based on remaining active calls
                        hasMultipleCalls = activeCalls.size > 1

                        // Check if any call is a conference
                        isConference = activeCalls.any { call ->
                            call.details?.hasProperty(android.telecom.Call.Details.PROPERTY_CONFERENCE) == true
                        }

                        // Get conference participants if in conference
                        if (isConference) {
                            // Get all participants including children of conference call
                            val participants = mutableListOf<String>()
                            activeCalls.forEach { call ->
                                // Add the call's number
                                call.details?.handle?.schemeSpecificPart?.let { participants.add(it) }

                                // If it's a conference, also get children
                                if (call.details?.hasProperty(android.telecom.Call.Details.PROPERTY_CONFERENCE) == true) {
                                    call.children.forEach { childCall ->
                                        childCall.details?.handle?.schemeSpecificPart?.let { participants.add(it) }
                                    }
                                }
                            }
                            conferenceParticipants = participants.distinct()
                        } else {
                            conferenceParticipants = emptyList()
                        }

                        kotlinx.coroutines.delay(500)
                    }
                }

                // Handle dismissal
                LaunchedEffect(shouldDismiss) {
                    if (shouldDismiss) {
                        kotlinx.coroutines.delay(1500) // Brief delay to show "Call Ended"
                        finish()
                    }
                }

                IncomingCallPopup(
                    incomingCallState = ringingState,
                    onAnswer = {
                        answerCall()
                    },
                    onReject = {
                        rejectCall()
                    },
                    onDismiss = {
                        finish()
                    },
                    onEndCall = {
                        endCall()
                    },
                    onMuteToggle = { muted ->
                        toggleMute(muted)
                    },
                    onSpeakerToggle = { speaker ->
                        toggleSpeaker(speaker)
                    },
                    onHoldToggle = { hold ->
                        toggleHold(hold)
                    },
                    onKeypadPress = { key ->
                        sendDtmf(key)
                    },
                    // Conference call support
                    onAddCall = { numberToCall ->
                        addCall(numberToCall)
                    },
                    onMergeCalls = {
                        mergeCalls()
                    },
                    onSwapCalls = {
                        swapCalls()
                    },
                    hasMultipleCalls = hasMultipleCalls,
                    isConference = isConference,
                    conferenceParticipants = conferenceParticipants,
                    // Pass call ended state to trigger UI transition
                    isCallEnded = shouldDismiss
                )
            }
        }
    }

    /**
     * Answer the call via CallForegroundService
     */
    private fun answerCall() {
        val intent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_ANSWER
        }
        startService(intent)
    }

    /**
     * Reject the call via CallForegroundService
     */
    private fun rejectCall() {
        val intent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_REJECT
        }
        startService(intent)
    }

    /**
     * End an active call
     */
    private fun endCall() {
        val intent = Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_END
        }
        startService(intent)
    }

    /**
     * Toggle mute
     */
    private fun toggleMute(muted: Boolean) {
        MentraInCallService.getInstance()?.setMutedState(muted)
    }

    /**
     * Toggle speaker
     */
    private fun toggleSpeaker(speaker: Boolean) {
        val route = if (speaker) {
            android.telecom.CallAudioState.ROUTE_SPEAKER
        } else {
            android.telecom.CallAudioState.ROUTE_EARPIECE
        }
        MentraInCallService.getInstance()?.setAudioRouteState(route)
    }

    /**
     * Toggle hold
     */
    private fun toggleHold(hold: Boolean) {
        val call = MentraInCallService.getInstance()?.getCurrentCalls()?.firstOrNull()
        if (hold) {
            call?.hold()
        } else {
            call?.unhold()
        }
    }

    /**
     * Send DTMF tone
     */
    private fun sendDtmf(key: Char) {
        val call = MentraInCallService.getInstance()?.getCurrentCalls()?.firstOrNull()
        call?.playDtmfTone(key)
        call?.stopDtmfTone()
    }

    /**
     * Add a new call (puts current call on hold and dials the number using the same SIM)
     */
    private fun addCall(numberToCall: String) {
        // First, hold the current call
        val currentCall = MentraInCallService.getInstance()?.getCurrentCalls()?.firstOrNull()
        currentCall?.hold()

        // Get the phone account (SIM) from the current call to use the same one
        val phoneAccountHandle = currentCall?.details?.accountHandle

        // Place the new call using the same SIM
        val uri = android.net.Uri.parse("tel:${numberToCall}")
        val extras = Bundle().apply {
            if (phoneAccountHandle != null) {
                putParcelable(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            }
        }

        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                telecomManager.placeCall(uri, extras)
            } else {
                // Fallback to ACTION_CALL intent
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    if (phoneAccountHandle != null) {
                        putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                    }
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to dialer
            val dialerIntent = Intent(Intent.ACTION_DIAL, uri)
            startActivity(dialerIntent)
        }
    }

    /**
     * Merge calls into a conference
     */
    private fun mergeCalls() {
        val calls = MentraInCallService.getInstance()?.getCurrentCalls() ?: return
        if (calls.size < 2) return

        // Find the active call and merge with conference or other calls
        val activeCall = calls.find {
            it.state == android.telecom.Call.STATE_ACTIVE
        } ?: calls.firstOrNull()

        // Try to conference/merge calls
        activeCall?.conference(calls.find { it != activeCall })
    }

    /**
     * Swap between active and held calls
     */
    private fun swapCalls() {
        val calls = MentraInCallService.getInstance()?.getCurrentCalls() ?: return
        if (calls.size < 2) return

        // Find the held call and unhold it (this automatically holds the active call)
        val heldCall = calls.find {
            it.state == android.telecom.Call.STATE_HOLDING
        }
        val activeCall = calls.find {
            it.state == android.telecom.Call.STATE_ACTIVE
        }

        if (heldCall != null && activeCall != null) {
            // Unhold the held call - the system will automatically hold the active one
            heldCall.unhold()
        }
    }

    /**
     * Setup window flags to bypass lock screen
     */
    private fun setupLockScreenBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on while this activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
