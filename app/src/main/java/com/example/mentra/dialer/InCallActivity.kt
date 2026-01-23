package com.example.mentra.dialer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.mentra.dialer.ui.CallDirection
import com.example.mentra.dialer.ui.UnifiedCallData
import com.example.mentra.dialer.ui.UnifiedCallModal
import com.example.mentra.dialer.ui.UnifiedCallState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * In-Call Activity
 *
 * Activity that displays during an active call.
 * Uses UnifiedCallModal for all call scenarios.
 */
@AndroidEntryPoint
class InCallActivity : ComponentActivity() {

    @Inject
    lateinit var dialerManager: DialerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mark that call UI is showing
        dialerManager.setCallUiShowing(true)

        // Keep screen on during call
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get call info from intent
        val phoneNumber = intent.getStringExtra("phone_number") ?: ""
        val contactName = intent.getStringExtra("contact_name")
        val photoUri = intent.getStringExtra("photo_uri")

        setContent {
            val callState by dialerManager.callState.collectAsState()
            val currentCall by dialerManager.currentCall.collectAsState()
            val availableSims by dialerManager.availableSims.collectAsState()
            val audioState by dialerManager.audioState.collectAsState()

            // Determine the display number/name
            val displayNumber = currentCall?.number ?: phoneNumber
            val displayName = currentCall?.contactName ?: contactName

            // Close activity when call ends
            LaunchedEffect(callState) {
                if (callState == CallState.IDLE) {
                    finish()
                }
            }

            // Use UnifiedCallModal for the call UI
            UnifiedCallModal(
                data = UnifiedCallData(
                    phoneNumber = displayNumber,
                    contactName = displayName,
                    photoUri = photoUri,
                    direction = CallDirection.OUTGOING, // Outgoing since this is launched for active calls
                    simSlot = currentCall?.simSlot ?: -1
                ),
                availableSims = availableSims,
                initialState = when (callState) {
                    CallState.DIALING, CallState.INIT -> UnifiedCallState.DIALING
                    CallState.RINGING -> UnifiedCallState.CONNECTING
                    CallState.ACTIVE -> UnifiedCallState.ACTIVE
                    CallState.DISCONNECTED, CallState.IDLE -> UnifiedCallState.ENDED
                },
                onDismiss = { finish() },
                onSimSelected = null, // SIM already selected
                onAnswer = null, // Not incoming
                onReject = null, // Not incoming
                onEndCall = {
                    dialerManager.endCall()
                    finish()
                },
                onMuteToggle = { _ -> dialerManager.toggleMute() },
                onSpeakerToggle = { _ -> dialerManager.toggleSpeaker() },
                onHoldToggle = { _ -> dialerManager.toggleHold() },
                onDtmfDigit = { digit -> dialerManager.sendDtmf(digit) }
            )
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onBackPressedDispatcher.onBackPressed()"))
    override fun onBackPressed() {
        // Don't allow back button during call
        // User must use end call button
        // Intentionally not calling super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the flag when activity is destroyed
        dialerManager.setCallUiShowing(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

