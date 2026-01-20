package com.example.mentra.dialer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.dialer.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * NEXUS IN-CALL MODAL - Alien Glassmorphic Calling Interface
 * Works even when not the default dialer - listens to call state
 */

private object NexusInCallColors {
    val background = Color(0xFF030810)
    val backgroundSecondary = Color(0xFF0A1628)
    val surface = Color(0xFF0F1A2E)
    val glass = Color(0xFF0A1628).copy(alpha = 0.95f)

    val primary = Color(0xFF00E5FF)
    val secondary = Color(0xFF7C4DFF)
    val accent = Color(0xFFFF4081)

    val success = Color(0xFF00E676)
    val warning = Color(0xFFFFAB00)
    val error = Color(0xFFFF5252)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0BEC5)
    val textMuted = Color(0xFF607D8B)

    val glassBorder = Color(0xFF00E5FF).copy(alpha = 0.4f)
    val glassBackground = Color(0xFF0A1628).copy(alpha = 0.92f)
}

@Composable
fun InCallScreen(
    viewModel: DialerViewModel = hiltViewModel(),
    onCallEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentCall by viewModel.currentCall.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()

    var showDtmfKeypad by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }
    var isCallActive by remember { mutableStateOf(false) } // Start as false, set true when OFFHOOK
    var callDuration by remember { mutableLongStateOf(0L) }
    var callConnectedTime by remember { mutableLongStateOf(0L) }

    // Phone state listener for tracking call state (works when not default dialer)
    DisposableEffect(Unit) {
        val callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    android.util.Log.d("InCallScreen", "Phone state changed: $state")
                    when (state) {
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            isCallActive = false
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            // Call just connected
                            if (!isCallActive) {
                                isCallActive = true
                                // Set the connected time NOW when the call actually connects
                                if (callConnectedTime == 0L) {
                                    callConnectedTime = System.currentTimeMillis()
                                    android.util.Log.d("InCallScreen", "Call connected, start time: $callConnectedTime")
                                }
                            }
                        }
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            // Incoming call ringing - don't set isCallActive yet
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        context.registerReceiver(callStateReceiver, filter)

        onDispose {
            try { context.unregisterReceiver(callStateReceiver) } catch (_: Exception) {}
        }
    }

    // Duration timer - only starts when we have a valid connected time
    LaunchedEffect(isCallActive, callConnectedTime) {
        if (isCallActive && callConnectedTime > 0) {
            while (isCallActive) {
                val now = System.currentTimeMillis()
                callDuration = (now - callConnectedTime) / 1000
                // Safety check - if duration is negative or unreasonably large, reset
                if (callDuration < 0 || callDuration > 86400) { // More than 24 hours is suspicious
                    callConnectedTime = now
                    callDuration = 0
                }
                delay(1000)
            }
        }
    }

    // Auto-close when call ends
    LaunchedEffect(callState, isCallActive) {
        if ((callState == CallState.DISCONNECTED || callState == CallState.IDLE) && !isCallActive) {
            delay(2000)
            onCallEnded()
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
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            // Animated background
            CallBackgroundEffect(callState, isCallActive)

            // Main modal
            CallModalContent(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                currentCall = currentCall,
                callState = callState,
                audioState = audioState,
                callDuration = callDuration,
                isCallActive = isCallActive,
                showDtmfKeypad = showDtmfKeypad,
                dtmfInput = dtmfInput,
                onDtmfDigit = { digit ->
                    dtmfInput += digit
                    viewModel.sendDtmf(digit.first())
                },
                onToggleKeypad = { showDtmfKeypad = !showDtmfKeypad },
                onMuteToggle = { viewModel.toggleMute() },
                onSpeakerToggle = { viewModel.toggleSpeaker() },
                onHoldToggle = { viewModel.toggleHold() },
                onAnswerCall = { viewModel.answerCall() },
                onDeclineCall = { viewModel.rejectCall() },
                onEndCall = { viewModel.endCall() }
            )
        }
    }
}

@Composable
private fun CallBackgroundEffect(callState: CallState, isCallActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (callState == CallState.DIALING) 800 else 2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val stateColor = when {
        callState == CallState.DIALING -> NexusInCallColors.warning
        callState == CallState.RINGING -> NexusInCallColors.success
        callState == CallState.ACTIVE || isCallActive -> NexusInCallColors.primary
        else -> NexusInCallColors.error
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height * 0.35f

        // Pulsing glow
        for (i in 1..5) {
            val radius = 80.dp.toPx() * i * pulseScale * 0.3f
            val alpha = (0.2f - (i * 0.035f)).coerceAtLeast(0.01f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(stateColor.copy(alpha = alpha), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = radius
                ),
                center = Offset(centerX, centerY),
                radius = radius
            )
        }

        // Rotating rings
        for (i in 1..3) {
            val ringRadius = 150.dp.toPx() + (i * 60.dp.toPx())
            rotate(ringRotation + (i * 30f), Offset(centerX, centerY)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.3f / i),
                            NexusInCallColors.secondary.copy(alpha = 0.2f / i),
                            Color.Transparent,
                            stateColor.copy(alpha = 0.3f / i)
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun CallModalContent(
    modifier: Modifier,
    currentCall: CallInfo?,
    callState: CallState,
    audioState: AudioRouteState,
    callDuration: Long,
    isCallActive: Boolean,
    showDtmfKeypad: Boolean,
    dtmfInput: String,
    onDtmfDigit: (String) -> Unit,
    onToggleKeypad: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onAnswerCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "modal")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Surface(
        modifier = modifier.shadow(32.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(NexusInCallColors.glass, NexusInCallColors.background.copy(alpha = 0.98f))
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NexusInCallColors.primary.copy(alpha = borderGlow),
                            NexusInCallColors.secondary.copy(alpha = borderGlow * 0.7f),
                            NexusInCallColors.accent.copy(alpha = borderGlow * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status bar
                StatusBar(callState, isCallActive, currentCall?.simSlot)

                Spacer(modifier = Modifier.height(20.dp))

                // State chip
                CallStateChip(callState, isCallActive)

                Spacer(modifier = Modifier.height(24.dp))

                // Caller info
                CallerInfoSection(
                    number = currentCall?.number ?: "Unknown",
                    name = currentCall?.contactName,
                    duration = if ((callState == CallState.ACTIVE || isCallActive) && callDuration > 0) callDuration else null
                )

                Spacer(modifier = Modifier.weight(1f))

                // DTMF Display
                if (showDtmfKeypad && dtmfInput.isNotEmpty()) {
                    DtmfDisplay(dtmfInput)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Controls or Keypad
                if (showDtmfKeypad) {
                    DtmfKeypad(onDtmfDigit)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onToggleKeypad) {
                        Text("Hide Keypad", color = NexusInCallColors.textMuted)
                    }
                } else if (callState == CallState.ACTIVE || isCallActive) {
                    CallControls(audioState, currentCall?.isOnHold == true, onMuteToggle, onSpeakerToggle, onHoldToggle, onToggleKeypad)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                CallActions(callState, isCallActive, onAnswerCall, onDeclineCall, onEndCall)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatusBar(callState: CallState, isCallActive: Boolean, simSlot: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val statusColor = when {
                callState == CallState.ACTIVE || isCallActive -> NexusInCallColors.success
                callState == CallState.DIALING || callState == CallState.RINGING -> NexusInCallColors.warning
                else -> NexusInCallColors.textMuted
            }
            Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
            Text(simSlot?.let { "SIM ${it + 1}" } ?: "SIM", color = NexusInCallColors.textSecondary, fontSize = 11.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Lock, null, tint = NexusInCallColors.primary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Text("HD", color = NexusInCallColors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CallStateChip(callState: CallState, isCallActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip")

    val stateText = when {
        callState == CallState.DIALING -> "CALLING"
        callState == CallState.RINGING -> "INCOMING"
        callState == CallState.ACTIVE || isCallActive -> "CONNECTED"
        callState == CallState.DISCONNECTED -> "ENDED"
        else -> "INITIALIZING"
    }

    val stateColor = when {
        callState == CallState.DIALING -> NexusInCallColors.warning
        callState == CallState.RINGING -> NexusInCallColors.success
        callState == CallState.ACTIVE || isCallActive -> NexusInCallColors.primary
        else -> NexusInCallColors.error
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = stateColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(stateColor, CircleShape))
            Text(stateText, color = stateColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun CallerInfoSection(number: String, name: String?, duration: Long?) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "ring"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Avatar
        Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(130.dp).rotate(ringRotation).border(
                    2.dp,
                    Brush.sweepGradient(listOf(NexusInCallColors.primary, NexusInCallColors.secondary, Color.Transparent, NexusInCallColors.primary)),
                    CircleShape
                )
            )

            Box(
                modifier = Modifier.size(100.dp).shadow(16.dp, CircleShape).background(
                    Brush.linearGradient(listOf(NexusInCallColors.surface, NexusInCallColors.backgroundSecondary)),
                    CircleShape
                ).border(1.dp, NexusInCallColors.glassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (name?.firstOrNull() ?: number.firstOrNull())?.uppercase()?.toString() ?: "?",
                    color = NexusInCallColors.textPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // Name/Number
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (name != null) {
                Text(name, color = NexusInCallColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(number, color = NexusInCallColors.textSecondary, fontSize = 14.sp)
            } else {
                Text(number, color = NexusInCallColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Duration
        duration?.let {
            Surface(shape = RoundedCornerShape(16.dp), color = NexusInCallColors.glassBackground, border = BorderStroke(1.dp, NexusInCallColors.glassBorder)) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(NexusInCallColors.success, CircleShape))
                    Text(formatDuration(it), color = NexusInCallColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun CallControls(
    audioState: AudioRouteState,
    isOnHold: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onKeypadClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NexusInCallColors.glassBackground,
        border = BorderStroke(1.dp, NexusInCallColors.glassBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlChip(if (audioState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute", audioState.isMuted, NexusInCallColors.error, onMuteToggle)
            ControlChip(Icons.Filled.Dialpad, "Keypad", false, NexusInCallColors.primary, onKeypadClick)
            ControlChip(if (audioState.currentRoute == AudioRoute.SPEAKER) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp, "Speaker", audioState.currentRoute == AudioRoute.SPEAKER, NexusInCallColors.primary, onSpeakerToggle)
            ControlChip(if (isOnHold) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (isOnHold) "Resume" else "Hold", isOnHold, NexusInCallColors.warning, onHoldToggle)
        }
    }
}

@Composable
private fun ControlChip(icon: ImageVector, label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(if (isActive) activeColor.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                .border(1.5.dp, if (isActive) activeColor else NexusInCallColors.textMuted.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = if (isActive) activeColor else NexusInCallColors.textSecondary, modifier = Modifier.size(22.dp))
        }
        Text(label, color = if (isActive) activeColor else NexusInCallColors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DtmfDisplay(input: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        shape = RoundedCornerShape(14.dp),
        color = NexusInCallColors.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, NexusInCallColors.glassBorder)
    ) {
        Text(input, color = NexusInCallColors.primary, fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
    }
}

@Composable
private fun DtmfKeypad(onDigitPressed: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier.size(64.dp).background(NexusInCallColors.surface.copy(alpha = 0.4f), CircleShape)
                            .border(1.dp, NexusInCallColors.glassBorder, CircleShape)
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDigitPressed(digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit, color = NexusInCallColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallActions(callState: CallState, isCallActive: Boolean, onAnswer: () -> Unit, onDecline: () -> Unit, onEndCall: () -> Unit) {
    when {
        callState == CallState.RINGING -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton(Icons.Default.CallEnd, "Decline", NexusInCallColors.error, false, onDecline)
                ActionButton(Icons.Default.Call, "Answer", NexusInCallColors.success, true, onAnswer)
            }
        }
        callState == CallState.DISCONNECTED && !isCallActive -> {
            Text("CALL ENDED", color = NexusInCallColors.error, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        }
        else -> EndCallButton(onEndCall)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, color: Color, isPrimary: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
            modifier = Modifier.size(64.dp).shadow(if (isPrimary) 12.dp else 6.dp, CircleShape, ambientColor = color),
            shape = CircleShape,
            color = color
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
        Text(label, color = NexusInCallColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EndCallButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "endCall")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = "glow"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(88.dp).background(Brush.radialGradient(listOf(NexusInCallColors.error.copy(alpha = glowAlpha), Color.Transparent)), CircleShape))

            Surface(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
                modifier = Modifier.size(64.dp).shadow(12.dp, CircleShape, ambientColor = NexusInCallColors.error),
                shape = CircleShape,
                color = NexusInCallColors.error
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.CallEnd, "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
        Text("End Call", color = NexusInCallColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, secs) else String.format("%02d:%02d", minutes, secs)
}

