@file:Suppress("unused")
package com.example.mentra.dialer.ui

/**
 * INCOMING CALL SCREEN - Re-exports
 *
 * This file provides backward compatibility by re-exporting components
 * from the refactored call package.
 *
 * The implementation has been split into multiple files:
 * - call/CallColors.kt - Color palette
 * - call/CallPopupState.kt - State enum
 * - call/CallUtils.kt - Utility functions
 * - call/AmbientGlowEffect.kt - Background glow effect
 * - call/AvatarWithGlow.kt - Animated avatar component
 * - call/SlideToAnswerReject.kt - Slide gesture control
 * - call/InCallControls.kt - In-call control buttons
 * - call/NexusIncomingCallCard.kt - Ringing state card
 * - call/CallCards.kt - Active, Connecting, Ended cards
 * - call/IncomingCallPopup.kt - Main popup composable
 */

import androidx.compose.runtime.Composable
import com.example.mentra.dialer.IncomingCallState

/**
 * Incoming Call Popup - Futuristic Mini Modal with Slide Gesture
 * Handles both RINGING and ACTIVE call states in one modal
 *
 * @see com.example.mentra.dialer.ui.call.IncomingCallPopup
 */
@Composable
fun IncomingCallPopup(
    incomingCallState: IncomingCallState.Ringing,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    onEndCall: () -> Unit = onReject,
    onMuteToggle: (Boolean) -> Unit = {},
    onSpeakerToggle: (Boolean) -> Unit = {},
    onHoldToggle: (Boolean) -> Unit = {},
    onKeypadPress: (Char) -> Unit = {},
    // Conference call support
    onAddCall: ((String) -> Unit)? = null, // Takes the number to call
    onMergeCalls: (() -> Unit)? = null,
    onSwapCalls: (() -> Unit)? = null,
    hasMultipleCalls: Boolean = false,
    isConference: Boolean = false,
    conferenceParticipants: List<String> = emptyList(),
    // External call state
    isCallEnded: Boolean = false
) {
    com.example.mentra.dialer.ui.call.IncomingCallPopup(
        incomingCallState = incomingCallState,
        onAnswer = onAnswer,
        onReject = onReject,
        onDismiss = onDismiss,
        onEndCall = onEndCall,
        onMuteToggle = onMuteToggle,
        onSpeakerToggle = onSpeakerToggle,
        onHoldToggle = onHoldToggle,
        onKeypadPress = onKeypadPress,
        onAddCall = onAddCall,
        onMergeCalls = onMergeCalls,
        onSwapCalls = onSwapCalls,
        hasMultipleCalls = hasMultipleCalls,
        isConference = isConference,
        conferenceParticipants = conferenceParticipants,
        isCallEnded = isCallEnded
    )
}

/**
 * Incoming Call Handler Composable
 * Place this at the root of your app to handle incoming calls globally
 *
 * @see com.example.mentra.dialer.ui.call.IncomingCallHandler
 */
@Composable
fun IncomingCallHandler(
    incomingCallState: IncomingCallState,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onEndCall: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    com.example.mentra.dialer.ui.call.IncomingCallHandler(
        incomingCallState = incomingCallState,
        onAnswer = onAnswer,
        onReject = onReject,
        onEndCall = onEndCall,
        onDismiss = onDismiss,
        content = content
    )
}
