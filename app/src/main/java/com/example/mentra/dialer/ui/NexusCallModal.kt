package com.example.mentra.dialer.ui

import android.content.Context
import android.media.AudioManager
import android.telecom.TelecomManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS CALL MODAL
 * Futuristic glassmorphic in-call overlay
 * ═══════════════════════════════════════════════════════════════════
 */

// Color palette
private object NexusCallColors {
    val background = Color(0xFF050810)
    val glass = Color(0xFF0A0F1C)
    val glassLight = Color(0xFF151D35)

    val primary = Color(0xFF00F5D4)
    val secondary = Color(0xFF7B61FF)
    val accent = Color(0xFFFF6B6B)

    val textPrimary = Color.White
    val textSecondary = Color(0xFFB8C1D1)
    val textMuted = Color(0xFF6B7A99)

    val callGreen = Color(0xFF00D26A)
    val callRed = Color(0xFFFF4757)
    val simBlue = Color(0xFF3B82F6)
    val simPurple = Color(0xFF8B5CF6)

    val gradientGlass = listOf(
        Color(0xFF1A1F3A).copy(alpha = 0.85f),
        Color(0xFF0D1229).copy(alpha = 0.75f)
    )
}

enum class CallModalState {
    SIM_SELECTION,
    CALLING,
    CONNECTED,
    ENDED
}

data class CallModalData(
    val phoneNumber: String,
    val contactName: String? = null,
    val photoUri: String? = null
)

@Composable
fun NexusCallModal(
    data: CallModalData,
    availableSims: List<SimInfo>,
    onDismiss: () -> Unit,
    onCall: (simSlot: Int) -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    val dialerManager = remember { com.example.mentra.dialer.DialerManagerProvider.getDialerManager() }

    var modalState by remember { mutableStateOf(CallModalState.SIM_SELECTION) }
    var selectedSim by remember { mutableStateOf(-1) }
    var callDuration by remember { mutableStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }

    // Observe call cost from DialerManager for outgoing calls
    val callCost by dialerManager?.totalCallCost?.collectAsState() ?: remember { mutableStateOf(0.0) }

    // Function to send DTMF to the active call
    fun sendDtmf(digit: Char) {
        // Send DTMF through DialerManager which sends to the active call
        dialerManager?.sendDtmf(digit)
    }

    // Function to toggle mute
    fun toggleMute() {
        isMuted = !isMuted
        dialerManager?.toggleMute()
    }

    // Function to toggle speaker
    fun toggleSpeaker() {
        isSpeaker = !isSpeaker
        dialerManager?.toggleSpeaker()
    }

    // Cleanup when modal is dismissed
    DisposableEffect(Unit) {
        onDispose {
            // Reset audio state through DialerManager
            if (isSpeaker) {
                dialerManager?.toggleSpeaker()
            }
            if (isMuted) {
                dialerManager?.toggleMute()
            }
        }
    }

    // Call timer
    LaunchedEffect(modalState) {
        if (modalState == CallModalState.CONNECTED) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    // Simulated call connection (in real app, this would be tied to actual call state)
    LaunchedEffect(modalState) {
        if (modalState == CallModalState.CALLING) {
            delay(2000) // Simulate connection time
            modalState = CallModalState.CONNECTED
        }
    }

    Dialog(
        onDismissRequest = {
            if (modalState == CallModalState.SIM_SELECTION || modalState == CallModalState.ENDED) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = modalState == CallModalState.SIM_SELECTION,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .blur(if (modalState != CallModalState.SIM_SELECTION) 0.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow animation
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            // Main modal content
            AnimatedContent(
                targetState = modalState,
                transitionSpec = {
                    fadeIn(tween(300)) + scaleIn(
                        initialScale = 0.9f,
                        animationSpec = tween(300)
                    ) togetherWith fadeOut(tween(200))
                },
                label = "modalContent"
            ) { state ->
                when (state) {
                    CallModalState.SIM_SELECTION -> {
                        SimSelectionModal(
                            data = data,
                            sims = availableSims,
                            onSimSelected = { slot ->
                                selectedSim = slot
                                onCall(slot)
                                modalState = CallModalState.CALLING
                            },
                            onCancel = onDismiss
                        )
                    }
                    CallModalState.CALLING -> {
                        CallingModal(
                            data = data,
                            glowAlpha = glowAlpha,
                            onEndCall = {
                                onEndCall()
                                modalState = CallModalState.ENDED
                            }
                        )
                    }
                    CallModalState.CONNECTED -> {
                        ConnectedCallModal(
                            data = data,
                            duration = callDuration,
                            callCost = callCost,
                            isMuted = isMuted,
                            isSpeaker = isSpeaker,
                            showDialpad = showDialpad,
                            onMuteToggle = { toggleMute() },
                            onSpeakerToggle = { toggleSpeaker() },
                            onDialpadToggle = { showDialpad = !showDialpad },
                            onEndCall = {
                                // Reset audio state before ending using dialerManager
                                if (isSpeaker) {
                                    dialerManager?.toggleSpeaker()
                                }
                                if (isMuted) {
                                    dialerManager?.toggleMute()
                                }
                                onEndCall()
                                modalState = CallModalState.ENDED
                            },
                            onDtmf = { digit -> sendDtmf(digit) }
                        )
                    }
                    CallModalState.ENDED -> {
                        CallEndedModal(
                            data = data,
                            duration = callDuration,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimSelectionModal(
    data: CallModalData,
    sims: List<SimInfo>,
    onSimSelected: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    GlassmorphicCard(
        modifier = Modifier
            .width(320.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "📞 Call",
                fontSize = 14.sp,
                color = NexusCallColors.primary,
                fontWeight = FontWeight.Medium
            )

            // Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(NexusCallColors.primary, NexusCallColors.secondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (data.contactName?.firstOrNull() ?: data.phoneNumber.firstOrNull() ?: '#').uppercase().toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCallColors.background
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexusCallColors.textPrimary
                )

                if (data.contactName != null) {
                    Text(
                        text = data.phoneNumber,
                        fontSize = 14.sp,
                        color = NexusCallColors.textMuted
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                NexusCallColors.primary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // SIM selection
            Text(
                text = "Select SIM",
                fontSize = 13.sp,
                color = NexusCallColors.textSecondary
            )

            if (sims.isEmpty() || sims.size == 1) {
                // Single SIM or no SIM info - just call
                NexusGlowButton(
                    text = "Call Now",
                    icon = Icons.Default.Call,
                    colors = listOf(NexusCallColors.callGreen, Color(0xFF00B85C)),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSimSelected(sims.firstOrNull()?.slotIndex ?: 0)
                    }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sims.forEachIndexed { index, sim ->
                        val color = if (index == 0) NexusCallColors.simBlue else NexusCallColors.simPurple

                        SimButton(
                            label = "SIM ${index + 1}",
                            carrier = sim.carrierName,
                            color = color,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSimSelected(sim.slotIndex)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cancel button
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    color = NexusCallColors.textMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CallingModal(
    data: CallModalData,
    glowAlpha: Float,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    GlassmorphicCard(
        modifier = Modifier
            .width(320.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pulsing avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .drawBehind {
                        drawCircle(
                            color = NexusCallColors.callGreen.copy(alpha = glowAlpha * 0.5f),
                            radius = size.minDimension * 0.7f
                        )
                    }
                    .background(
                        brush = Brush.linearGradient(
                            listOf(NexusCallColors.callGreen, Color(0xFF00B85C))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexusCallColors.textPrimary
                )

                Text(
                    text = "Calling...",
                    fontSize = 14.sp,
                    color = NexusCallColors.callGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // End call button
            NexusEndCallButton(onClick = onEndCall)
        }
    }
}

@Composable
private fun ConnectedCallModal(
    data: CallModalData,
    duration: Long,
    callCost: Double = 0.0,
    isMuted: Boolean,
    isSpeaker: Boolean,
    showDialpad: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onDialpadToggle: () -> Unit,
    onEndCall: () -> Unit,
    onDtmf: (Char) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .width(340.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Connected indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(NexusCallColors.callGreen, CircleShape)
                )
                Text(
                    text = "Connected",
                    fontSize = 12.sp,
                    color = NexusCallColors.callGreen
                )
            }

            // Contact info and duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexusCallColors.textPrimary
                )

                Text(
                    text = formatDuration(duration),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = NexusCallColors.primary,
                    letterSpacing = 2.sp
                )

                // Call cost (for outgoing calls)
                if (callCost > 0) {
                    Text(
                        text = "KSH %.2f".format(callCost),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NexusCallColors.textSecondary
                    )
                }
            }

            // Dialpad or controls
            AnimatedContent(
                targetState = showDialpad,
                transitionSpec = {
                    fadeIn(tween(200)) + slideInVertically { it / 2 } togetherWith
                    fadeOut(tween(150)) + slideOutVertically { -it / 2 }
                },
                label = "dialpadToggle"
            ) { showPad ->
                if (showPad) {
                    InCallDialpad(onDtmf = onDtmf)
                } else {
                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControlButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            isActive = isMuted,
                            activeColor = NexusCallColors.accent,
                            onClick = onMuteToggle
                        )

                        CallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            onClick = onDialpadToggle
                        )

                        CallControlButton(
                            icon = if (isSpeaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown,
                            label = if (isSpeaker) "Speaker" else "Earpiece",
                            isActive = isSpeaker,
                            activeColor = NexusCallColors.primary,
                            onClick = onSpeakerToggle
                        )
                    }
                }
            }

            if (showDialpad) {
                TextButton(onClick = onDialpadToggle) {
                    Text("Hide Keypad", color = NexusCallColors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End call button
            NexusEndCallButton(onClick = onEndCall)
        }
    }
}

@Composable
private fun CallEndedModal(
    data: CallModalData,
    duration: Long,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }

    GlassmorphicCard(
        modifier = Modifier
            .width(300.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = null,
                tint = NexusCallColors.accent,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Call Ended",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = NexusCallColors.textPrimary
            )

            if (duration > 0) {
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    fontSize = 14.sp,
                    color = NexusCallColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun InCallDialpad(
    onDtmf: (Char) -> Unit
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    DtmfButton(
                        digit = digit,
                        onClick = { onDtmf(digit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DtmfButton(
    digit: Char,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(NexusCallColors.glassLight.copy(alpha = 0.5f))
            .border(
                1.dp,
                NexusCallColors.textMuted.copy(alpha = 0.2f),
                CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = NexusCallColors.textPrimary
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = NexusCallColors.primary,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.2f)
                    else NexusCallColors.glassLight.copy(alpha = 0.5f)
                )
                .border(
                    1.dp,
                    if (isActive) activeColor.copy(alpha = 0.5f)
                    else NexusCallColors.textMuted.copy(alpha = 0.2f),
                    CircleShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else NexusCallColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            color = NexusCallColors.textMuted
        )
    }
}

@Composable
private fun SimButton(
    label: String,
    carrier: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.SimCard,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = carrier,
                fontSize = 11.sp,
                color = NexusCallColors.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NexusEndCallButton(
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = NexusCallColors.callRed
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(NexusCallColors.callRed, Color(0xFFE83F4F))
                )
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun NexusGlowButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = colors.first()
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = NexusCallColors.primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(NexusCallColors.gradientGlass)
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            NexusCallColors.primary.copy(alpha = 0.3f),
                            NexusCallColors.secondary.copy(alpha = 0.2f),
                            NexusCallColors.primary.copy(alpha = 0.1f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Column(content = content)
        }
    }
}

// Helper data class for SIM info
data class SimInfo(
    val slotIndex: Int,
    val carrierName: String,
    val phoneNumber: String = ""
)

// Helper function to format duration
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

