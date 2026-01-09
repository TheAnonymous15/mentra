package com.example.mentra.dialer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.dialer.*
import kotlinx.coroutines.delay

/**
 * In-Call Screen
 *
 * Minimal but correct call UI with:
 * - Large touch targets
 * - No heavy animations
 * - Essential controls only
 */
@Composable
fun InCallScreen(
    viewModel: DialerViewModel = hiltViewModel(),
    onCallEnded: () -> Unit = {}
) {
    val currentCall by viewModel.currentCall.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()

    // Track call duration
    var callDuration by remember { mutableLongStateOf(0L) }

    // Update duration every second when call is active
    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE) {
            while (true) {
                currentCall?.connectTime?.let { connectTime ->
                    if (connectTime > 0) {
                        callDuration = (System.currentTimeMillis() - connectTime) / 1000
                    }
                }
                delay(1000)
            }
        }
    }

    // Navigate back when call ends
    LaunchedEffect(callState) {
        if (callState == CallState.DISCONNECTED || callState == CallState.IDLE) {
            delay(1500) // Brief delay to show "Call Ended"
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF0D1229),
                        Color(0xFF1A1F3A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Call state indicator
            CallStateIndicator(state = callState)

            Spacer(modifier = Modifier.height(32.dp))

            // Caller info
            CallerInfo(
                number = currentCall?.number ?: "Unknown",
                name = currentCall?.contactName,
                duration = if (callState == CallState.ACTIVE) callDuration else null
            )

            Spacer(modifier = Modifier.weight(1f))

            // Call controls (only when active)
            if (callState == CallState.ACTIVE) {
                CallControls(
                    audioState = audioState,
                    isOnHold = currentCall?.isOnHold == true,
                    onMuteToggle = { viewModel.toggleMute() },
                    onSpeakerToggle = { viewModel.toggleSpeaker() },
                    onHoldToggle = { viewModel.toggleHold() },
                    onKeypadClick = { /* Show DTMF keypad */ },
                    onAudioRouteClick = { /* Show audio route picker */ }
                )

                Spacer(modifier = Modifier.height(48.dp))
            }

            // End call / Answer buttons
            when (callState) {
                CallState.RINGING -> {
                    // Incoming call - Answer and Decline
                    IncomingCallButtons(
                        onAnswer = { viewModel.answerCall() },
                        onDecline = { viewModel.rejectCall() }
                    )
                }
                CallState.DIALING, CallState.ACTIVE -> {
                    // Active call - End button only
                    EndCallButton(onClick = { viewModel.endCall() })
                }
                CallState.DISCONNECTED -> {
                    // Call ended state
                    Text(
                        text = "Call Ended",
                        color = Color(0xFFCE9178),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CallStateIndicator(state: CallState) {
    val stateText = when (state) {
        CallState.INIT -> "Initializing..."
        CallState.DIALING -> "Calling..."
        CallState.RINGING -> "Incoming Call"
        CallState.ACTIVE -> "Connected"
        CallState.DISCONNECTED -> "Call Ended"
        CallState.IDLE -> ""
    }

    val stateColor = when (state) {
        CallState.DIALING -> Color(0xFF569CD6)
        CallState.RINGING -> Color(0xFF4EC9B0)
        CallState.ACTIVE -> Color(0xFF4EC9B0)
        CallState.DISCONNECTED -> Color(0xFFCE9178)
        else -> Color.White.copy(alpha = 0.6f)
    }

    // Pulsing animation for calling/ringing states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val showPulse = state == CallState.DIALING || state == CallState.RINGING

    Text(
        text = stateText,
        color = stateColor.copy(alpha = if (showPulse) alpha else 1f),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp
    )
}

@Composable
fun CallerInfo(
    number: String,
    name: String?,
    duration: Long?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(16.dp, CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4EC9B0), Color(0xFF2A8A7A))
                    ),
                    shape = CircleShape
                )
                .border(3.dp, Color(0xFF6EDDCA).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (name?.firstOrNull() ?: number.firstOrNull())?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Name/Number
        if (name != null) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = number,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        } else {
            Text(
                text = number,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Duration
        duration?.let {
            Text(
                text = formatDuration(it),
                color = Color(0xFF4EC9B0),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun CallControls(
    audioState: AudioRouteState,
    isOnHold: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onKeypadClick: () -> Unit,
    onAudioRouteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // First row
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            CallControlButton(
                icon = if (audioState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = "Mute",
                isActive = audioState.isMuted,
                onClick = onMuteToggle
            )

            CallControlButton(
                icon = Icons.Default.Dialpad,
                label = "Keypad",
                onClick = onKeypadClick
            )

            CallControlButton(
                icon = if (audioState.currentRoute == AudioRoute.SPEAKER)
                    Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                label = "Speaker",
                isActive = audioState.currentRoute == AudioRoute.SPEAKER,
                onClick = onSpeakerToggle
            )
        }

        // Second row
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            CallControlButton(
                icon = Icons.Default.PersonAdd,
                label = "Add Call",
                onClick = { /* Add call */ }
            )

            CallControlButton(
                icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                label = if (isOnHold) "Resume" else "Hold",
                isActive = isOnHold,
                onClick = onHoldToggle
            )

            // Audio route selector (shows Bluetooth if available)
            CallControlButton(
                icon = when (audioState.currentRoute) {
                    AudioRoute.BLUETOOTH -> Icons.Default.Bluetooth
                    AudioRoute.WIRED_HEADSET -> Icons.Default.Headphones
                    else -> Icons.Default.PhoneInTalk
                },
                label = "Audio",
                onClick = onAudioRouteClick
            )
        }
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onClick,
            color = if (isActive) Color(0xFF4EC9B0) else Color(0xFF2A2F4A),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun IncomingCallButtons(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Decline button
        FloatingActionButton(
            onClick = onDecline,
            containerColor = Color.Transparent,
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE57373), Color(0xFFB71C1C))
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "Decline",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Answer button
        FloatingActionButton(
            onClick = onAnswer,
            containerColor = Color.Transparent,
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4EC9B0), Color(0xFF2A8A7A))
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = "Answer",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun EndCallButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "endCall")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "endCallGlow"
    )

    Box {
        // Glow effect
        Box(
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE57373).copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        FloatingActionButton(
            onClick = onClick,
            containerColor = Color.Transparent,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFE57373),
                    spotColor = Color(0xFFE57373)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE57373), Color(0xFFB71C1C))
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

