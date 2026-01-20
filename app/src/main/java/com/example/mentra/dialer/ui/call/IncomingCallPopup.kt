package com.example.mentra.dialer.ui.call

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mentra.dialer.IncomingCallState
import kotlinx.coroutines.delay

/**
 * NEXUS INCOMING CALL POPUP
 * Futuristic Glassmorphic Modal with slide-to-answer/reject gesture control
 *
 * Handles both RINGING and ACTIVE call states in one modal
 */
@Composable
fun IncomingCallPopup(
    incomingCallState: IncomingCallState.Ringing,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    // In-call control callbacks (optional - for active call state)
    onEndCall: () -> Unit = onReject,
    onMuteToggle: (Boolean) -> Unit = {},
    onSpeakerToggle: (Boolean) -> Unit = {},
    onHoldToggle: (Boolean) -> Unit = {},
    onKeypadPress: (Char) -> Unit = {},
    // Conference call callbacks
    onAddCall: ((String) -> Unit)? = null, // Takes the number to call
    onMergeCalls: (() -> Unit)? = null,
    onSwapCalls: (() -> Unit)? = null,
    hasMultipleCalls: Boolean = false,
    isConference: Boolean = false,
    conferenceParticipants: List<String> = emptyList(),
    // External call state - when true, transition to ENDED
    isCallEnded: Boolean = false
) {
    var ringTime by remember { mutableLongStateOf(0L) }
    var callState by remember { mutableStateOf(CallPopupState.RINGING) }
    var callDuration by remember { mutableLongStateOf(0L) }
    var callStartTime by remember { mutableLongStateOf(0L) }

    // In-call state
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }

    // Handle external call ended signal
    LaunchedEffect(isCallEnded) {
        if (isCallEnded && callState != CallPopupState.ENDED) {
            callState = CallPopupState.ENDED
        }
    }

    // Timer for ringing
    LaunchedEffect(incomingCallState, callState) {
        if (callState == CallPopupState.RINGING) {
            while (callState == CallPopupState.RINGING) {
                ringTime = (System.currentTimeMillis() - incomingCallState.startTime) / 1000
                delay(1000)
            }
        }
    }

    // Timer for call duration - only run when not ended
    LaunchedEffect(callState) {
        if (callState == CallPopupState.ACTIVE) {
            callStartTime = System.currentTimeMillis()
            while (callState == CallPopupState.ACTIVE) {
                callDuration = (System.currentTimeMillis() - callStartTime) / 1000
                delay(1000)
            }
        }
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CallColors.voidBlack.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow effects
            AmbientGlowEffect()

            // Main modal card - switches between ringing and active
            AnimatedContent(
                targetState = callState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "callState"
            ) { state ->
                when (state) {
                    CallPopupState.RINGING -> {
                        NexusIncomingCallCard(
                            phoneNumber = incomingCallState.phoneNumber,
                            contactName = incomingCallState.contactName,
                            ringTime = ringTime,
                            onAnswer = {
                                callState = CallPopupState.CONNECTING
                                onAnswer()
                                // Transition to active after short delay
                                callState = CallPopupState.ACTIVE
                            },
                            onReject = {
                                callState = CallPopupState.ENDED
                                onReject()
                            }
                        )
                    }
                    CallPopupState.CONNECTING -> {
                        ConnectingCallCard(
                            phoneNumber = incomingCallState.phoneNumber,
                            contactName = incomingCallState.contactName
                        )
                    }
                    CallPopupState.ACTIVE -> {
                        ActiveCallCard(
                            phoneNumber = incomingCallState.phoneNumber,
                            contactName = incomingCallState.contactName,
                            callDuration = callDuration,
                            isMuted = isMuted,
                            isSpeaker = isSpeaker,
                            isOnHold = isOnHold,
                            onMuteToggle = {
                                isMuted = it
                                onMuteToggle(it)
                            },
                            onSpeakerToggle = {
                                isSpeaker = it
                                onSpeakerToggle(it)
                            },
                            onHoldToggle = {
                                isOnHold = it
                                onHoldToggle(it)
                            },
                            onKeypadPress = onKeypadPress,
                            onEndCall = {
                                callState = CallPopupState.ENDED
                                onEndCall()
                            },
                            // Conference call support
                            onAddCall = onAddCall,
                            onMergeCalls = onMergeCalls,
                            onSwapCalls = onSwapCalls,
                            hasMultipleCalls = hasMultipleCalls,
                            isConference = isConference,
                            conferenceParticipants = conferenceParticipants
                        )
                    }
                    CallPopupState.ENDED -> {
                        // Auto dismiss after short delay
                        LaunchedEffect(Unit) {
                            delay(1000)
                            onDismiss()
                        }
                        EndedCallCard(
                            phoneNumber = incomingCallState.phoneNumber,
                            contactName = incomingCallState.contactName
                        )
                    }
                }
            }
        }
    }
}

/**
 * Incoming Call Handler Composable
 * Place this at the root of your app to handle incoming calls globally
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
    Box(modifier = Modifier.fillMaxSize()) {
        // Main app content
        content()

        // Incoming call popup overlay
        AnimatedVisibility(
            visible = incomingCallState is IncomingCallState.Ringing,
            enter = fadeIn(animationSpec = tween(300)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)) +
                   scaleOut(targetScale = 0.8f, animationSpec = tween(200))
        ) {
            if (incomingCallState is IncomingCallState.Ringing) {
                IncomingCallPopup(
                    incomingCallState = incomingCallState,
                    onAnswer = onAnswer,
                    onReject = onReject,
                    onDismiss = onDismiss,
                    onEndCall = onEndCall
                )
            }
        }
    }
}
