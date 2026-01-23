package com.example.mentra.dialer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mentra.dialer.DialerManagerProvider
import com.example.mentra.dialer.SimAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════
 * UNIFIED NEXUS CALL MODAL
 * One modal to rule them all - SIM selection, outgoing, incoming calls
 * ═══════════════════════════════════════════════════════════════════
 */

// Color palette
private object UnifiedCallColors {
    val voidBlack = Color(0xFF000000)
    val deepSpace = Color(0xFF050A15)
    val nebula = Color(0xFF0A1628)
    val glass = Color(0xFF0D1B2A).copy(alpha = 0.95f)
    val glassLight = Color(0xFF151D35)

    val neonCyan = Color(0xFF00F5FF)
    val electricPurple = Color(0xFFBF40FF)
    val plasmaGreen = Color(0xFF00FF87)
    val solarOrange = Color(0xFFFF6B35)
    val cosmicPink = Color(0xFFFF2E63)

    val callGreen = Color(0xFF00D26A)
    val callRed = Color(0xFFFF4757)
    val simBlue = Color(0xFF3B82F6)
    val simPurple = Color(0xFF8B5CF6)

    val textWhite = Color(0xFFFFFFFF)
    val textSilver = Color(0xFFB8C5D6)
    val textDim = Color(0xFF5C7A99)

    val gradientGlass = listOf(glass, deepSpace)
}

/**
 * Call modal state - unified for all scenarios
 */
enum class UnifiedCallState {
    SIM_SELECTION,   // User needs to select SIM
    RINGING_INCOMING, // Incoming call ringing
    DIALING,         // Outgoing call dialing
    CONNECTING,      // Call connecting
    ACTIVE,          // Call is active
    ENDED            // Call has ended
}

/**
 * Call direction
 */
enum class CallDirection {
    INCOMING,
    OUTGOING
}

/**
 * Data for unified call modal
 */
data class UnifiedCallData(
    val phoneNumber: String,
    val contactName: String? = null,
    val photoUri: String? = null,
    val direction: CallDirection = CallDirection.OUTGOING,
    val simSlot: Int = -1
)

/**
 * Main Unified Call Modal
 * Handles all call scenarios in one modal
 */
@Composable
fun UnifiedCallModal(
    data: UnifiedCallData,
    availableSims: List<SimAccount>,
    initialState: UnifiedCallState = if (data.direction == CallDirection.INCOMING)
        UnifiedCallState.RINGING_INCOMING
    else if (availableSims.size > 1 && data.simSlot < 0)
        UnifiedCallState.SIM_SELECTION
    else
        UnifiedCallState.DIALING,
    onDismiss: () -> Unit,
    onSimSelected: ((Int) -> Unit)? = null,
    onAnswer: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onEndCall: () -> Unit,
    onMuteToggle: ((Boolean) -> Unit)? = null,
    onSpeakerToggle: ((Boolean) -> Unit)? = null,
    onHoldToggle: ((Boolean) -> Unit)? = null,
    onDtmfDigit: ((Char) -> Unit)? = null
) {
    val context = LocalContext.current
    val dialerManager = remember { DialerManagerProvider.getDialerManager() }
    val haptic = LocalHapticFeedback.current

    // State management
    var callState by remember { mutableStateOf(initialState) }
    var callDuration by remember { mutableLongStateOf(0L) }
    var callConnectedTime by remember { mutableLongStateOf(0L) }
    var ringTime by remember { mutableLongStateOf(0L) }
    var isCallActive by remember { mutableStateOf(false) }

    // Call controls state
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var showDtmfKeypad by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }

    // Call cost (for outgoing calls only)
    val callCost by dialerManager?.totalCallCost?.collectAsState() ?: remember { mutableStateOf(0.0) }
    val isBillingTracking by dialerManager?.isBillingTracking?.collectAsState() ?: remember { mutableStateOf(false) }

    // Load contact photo
    var contactBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(data.photoUri, data.phoneNumber) {
        contactBitmap = withContext(Dispatchers.IO) {
            loadContactPhoto(context, data.photoUri, data.phoneNumber)
        }
    }

    // Track when call was initiated to prevent premature IDLE detection
    // Initialize immediately for outgoing calls that start in DIALING state
    var callInitiatedTime by remember {
        mutableLongStateOf(
            if (initialState == UnifiedCallState.DIALING ||
                initialState == UnifiedCallState.CONNECTING)
                System.currentTimeMillis()
            else 0L
        )
    }

    // Track if we've ever seen OFFHOOK state (call actually connected to network)
    var hasSeenOffhook by remember { mutableStateOf(false) }

    // Phone state listener for call state detection
    DisposableEffect(Unit) {
        val callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    val timeSinceInit = if (callInitiatedTime > 0)
                        System.currentTimeMillis() - callInitiatedTime
                    else 0L

                    android.util.Log.d("UnifiedCallModal",
                        "Phone state changed: $state, callState=$callState, " +
                        "isCallActive=$isCallActive, hasSeenOffhook=$hasSeenOffhook, " +
                        "timeSinceInit=$timeSinceInit")

                    when (state) {
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            // Only transition to ENDED if:
                            // 1. We've actually seen OFFHOOK state (call connected), OR
                            // 2. Significant time has passed (5+ seconds) since call was initiated
                            // This prevents premature IDLE detection
                            val shouldEnd = hasSeenOffhook ||
                                (callInitiatedTime > 0 && timeSinceInit > 5000)

                            android.util.Log.d("UnifiedCallModal",
                                "IDLE received, shouldEnd=$shouldEnd")

                            if (shouldEnd) {
                                isCallActive = false
                                if (callState == UnifiedCallState.ACTIVE ||
                                    callState == UnifiedCallState.DIALING ||
                                    callState == UnifiedCallState.CONNECTING) {
                                    callState = UnifiedCallState.ENDED
                                }
                            }
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            hasSeenOffhook = true
                            if (!isCallActive) {
                                isCallActive = true
                                if (callConnectedTime == 0L) {
                                    callConnectedTime = System.currentTimeMillis()
                                }
                                if (callState == UnifiedCallState.DIALING ||
                                    callState == UnifiedCallState.CONNECTING ||
                                    callState == UnifiedCallState.RINGING_INCOMING) {
                                    callState = UnifiedCallState.ACTIVE
                                }
                            }
                        }
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            if (data.direction == CallDirection.INCOMING &&
                                callState != UnifiedCallState.ACTIVE) {
                                callState = UnifiedCallState.RINGING_INCOMING
                            }
                        }
                    }
                }
            }
        }
        context.registerReceiver(
            callStateReceiver,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        )
        onDispose {
            try { context.unregisterReceiver(callStateReceiver) } catch (_: Exception) {}
        }
    }

    // Update call initiated time when transitioning to DIALING
    LaunchedEffect(callState) {
        if (callState == UnifiedCallState.DIALING && callInitiatedTime == 0L) {
            callInitiatedTime = System.currentTimeMillis()
        }
    }

    // Duration timer
    LaunchedEffect(isCallActive, callConnectedTime) {
        if (isCallActive && callConnectedTime > 0) {
            while (isCallActive) {
                callDuration = (System.currentTimeMillis() - callConnectedTime) / 1000
                if (callDuration < 0 || callDuration > 86400) {
                    callConnectedTime = System.currentTimeMillis()
                    callDuration = 0
                }
                delay(1000)
            }
        }
    }

    // Ring timer for incoming calls
    LaunchedEffect(callState) {
        if (callState == UnifiedCallState.RINGING_INCOMING) {
            val startTime = System.currentTimeMillis()
            while (callState == UnifiedCallState.RINGING_INCOMING) {
                ringTime = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        }
    }

    // Auto-dismiss after call ends
    LaunchedEffect(callState) {
        if (callState == UnifiedCallState.ENDED) {
            delay(1500)
            onDismiss()
        }
    }

    // Transition from DIALING to ACTIVE (simulated for UI)
    LaunchedEffect(callState) {
        if (callState == UnifiedCallState.DIALING) {
            delay(500)
            callState = UnifiedCallState.CONNECTING
        }
    }

    Dialog(
        onDismissRequest = {
            if (callState == UnifiedCallState.SIM_SELECTION || callState == UnifiedCallState.ENDED) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = callState == UnifiedCallState.SIM_SELECTION,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UnifiedCallColors.voidBlack.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient background effect
            UnifiedAmbientEffect(callState, data.direction, isCallActive)

            // Main modal content - animated transitions
            AnimatedContent(
                targetState = callState,
                transitionSpec = {
                    fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) togetherWith
                    fadeOut(tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                },
                label = "callStateTransition"
            ) { state ->
                when (state) {
                    UnifiedCallState.SIM_SELECTION -> {
                        UnifiedSimSelectionCard(
                            data = data,
                            sims = availableSims,
                            contactBitmap = contactBitmap,
                            onSimSelected = { slot ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSimSelected?.invoke(slot)
                                callState = UnifiedCallState.DIALING
                            },
                            onCancel = onDismiss
                        )
                    }

                    UnifiedCallState.RINGING_INCOMING -> {
                        UnifiedIncomingCard(
                            data = data,
                            ringTime = ringTime,
                            contactBitmap = contactBitmap,
                            onAnswer = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAnswer?.invoke()
                                callState = UnifiedCallState.ACTIVE
                            },
                            onReject = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReject?.invoke()
                                callState = UnifiedCallState.ENDED
                            }
                        )
                    }

                    UnifiedCallState.DIALING, UnifiedCallState.CONNECTING -> {
                        UnifiedDialingCard(
                            data = data,
                            isConnecting = state == UnifiedCallState.CONNECTING,
                            contactBitmap = contactBitmap,
                            onEndCall = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEndCall()
                                callState = UnifiedCallState.ENDED
                            }
                        )
                    }

                    UnifiedCallState.ACTIVE -> {
                        UnifiedActiveCallCard(
                            data = data,
                            duration = callDuration,
                            callCost = if (data.direction == CallDirection.OUTGOING && isBillingTracking) callCost else null,
                            contactBitmap = contactBitmap,
                            isMuted = isMuted,
                            isSpeaker = isSpeaker,
                            isOnHold = isOnHold,
                            showDtmfKeypad = showDtmfKeypad,
                            dtmfInput = dtmfInput,
                            onMuteToggle = {
                                isMuted = !isMuted
                                onMuteToggle?.invoke(isMuted)
                            },
                            onSpeakerToggle = {
                                isSpeaker = !isSpeaker
                                onSpeakerToggle?.invoke(isSpeaker)
                            },
                            onHoldToggle = {
                                isOnHold = !isOnHold
                                onHoldToggle?.invoke(isOnHold)
                            },
                            onKeypadToggle = { showDtmfKeypad = !showDtmfKeypad },
                            onDtmfDigit = { digit ->
                                dtmfInput += digit
                                onDtmfDigit?.invoke(digit)
                            },
                            onEndCall = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEndCall()
                                callState = UnifiedCallState.ENDED
                            }
                        )
                    }

                    UnifiedCallState.ENDED -> {
                        UnifiedCallEndedCard(
                            data = data,
                            duration = callDuration,
                            callCost = if (data.direction == CallDirection.OUTGOING && callCost > 0) callCost else null,
                            contactBitmap = contactBitmap
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// AMBIENT BACKGROUND EFFECT
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedAmbientEffect(
    callState: UnifiedCallState,
    direction: CallDirection,
    isCallActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotate"
    )

    val pulseSpeed = when (callState) {
        UnifiedCallState.RINGING_INCOMING -> 400
        UnifiedCallState.DIALING, UnifiedCallState.CONNECTING -> 600
        else -> 2000
    }

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(pulseSpeed, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    val stateColor = when (callState) {
        UnifiedCallState.SIM_SELECTION -> UnifiedCallColors.neonCyan
        UnifiedCallState.RINGING_INCOMING -> UnifiedCallColors.plasmaGreen
        UnifiedCallState.DIALING, UnifiedCallState.CONNECTING -> UnifiedCallColors.solarOrange
        UnifiedCallState.ACTIVE -> if (isCallActive) UnifiedCallColors.neonCyan else UnifiedCallColors.plasmaGreen
        UnifiedCallState.ENDED -> UnifiedCallColors.cosmicPink
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        // Orbital rings
        for (i in 1..3) {
            rotate(rotation + i * 40f, Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            stateColor.copy(alpha = 0.15f / i),
                            Color.Transparent,
                            stateColor.copy(alpha = 0.1f / i)
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = 180.dp.toPx() + i * 50.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        // Central glow
        drawCircle(
            brush = Brush.radialGradient(
                listOf(stateColor.copy(alpha = pulse * 0.3f), Color.Transparent),
                center = Offset(cx, cy),
                radius = 150.dp.toPx()
            ),
            center = Offset(cx, cy)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// SIM SELECTION CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedSimSelectionCard(
    data: UnifiedCallData,
    sims: List<SimAccount>,
    contactBitmap: android.graphics.Bitmap?,
    onSimSelected: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simCard")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    GlassmorphicCard(borderGlow = borderGlow) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Contact avatar
            ContactAvatar(
                contactBitmap = contactBitmap,
                name = data.contactName,
                phoneNumber = data.phoneNumber,
                size = 72,
                showRing = true
            )

            // Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UnifiedCallColors.textWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (data.contactName != null) {
                    Text(
                        text = data.phoneNumber,
                        fontSize = 14.sp,
                        color = UnifiedCallColors.textDim
                    )
                }
            }

            // Divider
            GradientDivider()

            // Select SIM label
            Text(
                text = "Select SIM",
                fontSize = 13.sp,
                color = UnifiedCallColors.textSilver
            )

            // SIM buttons
            if (sims.isEmpty() || sims.size == 1) {
                NexusGlowButton(
                    text = "Call Now",
                    icon = Icons.Default.Call,
                    colors = listOf(UnifiedCallColors.callGreen, Color(0xFF00B85C)),
                    onClick = { onSimSelected(sims.firstOrNull()?.slotIndex ?: 0) }
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sims.forEachIndexed { index, sim ->
                        val color = if (index == 0) UnifiedCallColors.simBlue else UnifiedCallColors.simPurple
                        SimButton(
                            label = "SIM ${index + 1}",
                            carrier = sim.carrierName,
                            color = color,
                            onClick = { onSimSelected(sim.slotIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cancel button
            TextButton(onClick = onCancel) {
                Text("Cancel", color = UnifiedCallColors.textDim, fontSize = 14.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// INCOMING CALL CARD - With Slide to Answer/Reject
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedIncomingCard(
    data: UnifiedCallData,
    ringTime: Long,
    contactBitmap: android.graphics.Bitmap?,
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "incoming")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    GlassmorphicCard(borderGlow = borderGlow, borderColor = UnifiedCallColors.plasmaGreen) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Incoming call indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = UnifiedCallColors.plasmaGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Incoming Call",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = UnifiedCallColors.plasmaGreen
                )
            }

            // Pulsing avatar
            Box(modifier = Modifier.scale(pulseScale)) {
                ContactAvatar(
                    contactBitmap = contactBitmap,
                    name = data.contactName,
                    phoneNumber = data.phoneNumber,
                    size = 88,
                    showRing = true,
                    ringColor = UnifiedCallColors.plasmaGreen
                )
            }

            // Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UnifiedCallColors.textWhite
                )
                if (data.contactName != null) {
                    Text(
                        text = data.phoneNumber,
                        fontSize = 14.sp,
                        color = UnifiedCallColors.textDim
                    )
                }
                Text(
                    text = "Ringing ${formatDuration(ringTime)}",
                    fontSize = 13.sp,
                    color = UnifiedCallColors.textSilver
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slide to Answer/Reject
            SlideToAnswerReject(
                onAnswer = onAnswer,
                onReject = onReject
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DIALING CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedDialingCard(
    data: UnifiedCallData,
    isConnecting: Boolean,
    contactBitmap: android.graphics.Bitmap?,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dialing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    GlassmorphicCard(borderGlow = borderGlow, borderColor = UnifiedCallColors.solarOrange) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pulsing avatar
            Box(modifier = Modifier.scale(pulseScale)) {
                ContactAvatar(
                    contactBitmap = contactBitmap,
                    name = data.contactName,
                    phoneNumber = data.phoneNumber,
                    size = 100,
                    showRing = true,
                    ringColor = UnifiedCallColors.callGreen
                )
            }

            // Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UnifiedCallColors.textWhite
                )
                Text(
                    text = if (isConnecting) "Connecting..." else "Calling...",
                    fontSize = 14.sp,
                    color = UnifiedCallColors.callGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // End call button
            NexusEndCallButton(onClick = onEndCall)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ACTIVE CALL CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedActiveCallCard(
    data: UnifiedCallData,
    duration: Long,
    callCost: Double?,
    contactBitmap: android.graphics.Bitmap?,
    isMuted: Boolean,
    isSpeaker: Boolean,
    isOnHold: Boolean,
    showDtmfKeypad: Boolean,
    dtmfInput: String,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onKeypadToggle: () -> Unit,
    onDtmfDigit: (Char) -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "active")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    GlassmorphicCard(borderGlow = borderGlow, borderColor = UnifiedCallColors.neonCyan) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connected indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(UnifiedCallColors.callGreen, CircleShape)
                )
                Text(
                    text = if (isOnHold) "On Hold" else "Connected",
                    fontSize = 12.sp,
                    color = if (isOnHold) UnifiedCallColors.solarOrange else UnifiedCallColors.callGreen
                )
            }

            // Contact avatar (smaller when active)
            ContactAvatar(
                contactBitmap = contactBitmap,
                name = data.contactName,
                phoneNumber = data.phoneNumber,
                size = 64,
                showRing = false
            )

            // Contact info and duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.contactName ?: data.phoneNumber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UnifiedCallColors.textWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formatDuration(duration),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = UnifiedCallColors.neonCyan,
                    letterSpacing = 2.sp
                )

                // Call cost (for outgoing calls)
                if (callCost != null && callCost > 0) {
                    Text(
                        text = "Cost: KSH %.2f".format(callCost),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = UnifiedCallColors.textSilver
                    )
                }
            }

            // DTMF input display
            if (showDtmfKeypad && dtmfInput.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = UnifiedCallColors.glassLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dtmfInput,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = UnifiedCallColors.neonCyan,
                        modifier = Modifier.padding(12.dp),
                        letterSpacing = 4.sp
                    )
                }
            }

            // Controls or DTMF Keypad
            AnimatedContent(
                targetState = showDtmfKeypad,
                transitionSpec = {
                    fadeIn(tween(200)) + slideInVertically { it / 2 } togetherWith
                    fadeOut(tween(150)) + slideOutVertically { -it / 2 }
                },
                label = "keypadToggle"
            ) { showPad ->
                if (showPad) {
                    DtmfKeypadGrid(onDtmf = onDtmfDigit)
                } else {
                    CallControlsRow(
                        isMuted = isMuted,
                        isSpeaker = isSpeaker,
                        isOnHold = isOnHold,
                        onMuteToggle = onMuteToggle,
                        onSpeakerToggle = onSpeakerToggle,
                        onHoldToggle = onHoldToggle,
                        onKeypadToggle = onKeypadToggle
                    )
                }
            }

            if (showDtmfKeypad) {
                TextButton(onClick = onKeypadToggle) {
                    Text("Hide Keypad", color = UnifiedCallColors.textDim)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End call button
            NexusEndCallButton(onClick = onEndCall)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// CALL ENDED CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun UnifiedCallEndedCard(
    data: UnifiedCallData,
    duration: Long,
    callCost: Double?,
    contactBitmap: android.graphics.Bitmap?
) {
    GlassmorphicCard(borderGlow = 0.3f, borderColor = UnifiedCallColors.cosmicPink) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ended indicator
            Icon(
                Icons.Default.CallEnd,
                contentDescription = null,
                tint = UnifiedCallColors.cosmicPink,
                modifier = Modifier.size(40.dp)
            )

            // Contact avatar
            ContactAvatar(
                contactBitmap = contactBitmap,
                name = data.contactName,
                phoneNumber = data.phoneNumber,
                size = 64,
                showRing = false
            )

            // Contact info
            Text(
                text = data.contactName ?: data.phoneNumber,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = UnifiedCallColors.textWhite
            )

            Text(
                text = "Call Ended",
                fontSize = 14.sp,
                color = UnifiedCallColors.cosmicPink
            )

            if (duration > 0) {
                Text(
                    text = "Duration: ${formatDuration(duration)}",
                    fontSize = 13.sp,
                    color = UnifiedCallColors.textSilver
                )
            }

            // Call cost summary (for outgoing calls)
            if (callCost != null && callCost > 0) {
                Text(
                    text = "Estimated Cost: KSH %.2f".format(callCost),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = UnifiedCallColors.solarOrange
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderGlow: Float = 0.5f,
    borderColor: Color = UnifiedCallColors.neonCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .widthIn(max = 340.dp)
            .shadow(32.dp, RoundedCornerShape(28.dp), ambientColor = borderColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(UnifiedCallColors.gradientGlass),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            borderColor.copy(alpha = borderGlow),
                            UnifiedCallColors.electricPurple.copy(alpha = borderGlow * 0.5f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ContactAvatar(
    contactBitmap: android.graphics.Bitmap?,
    name: String?,
    phoneNumber: String,
    size: Int,
    showRing: Boolean,
    ringColor: Color = UnifiedCallColors.neonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarRing")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRotate"
    )

    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated ring
        if (showRing) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(ringColor, UnifiedCallColors.electricPurple, Color.Transparent, ringColor)
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Avatar content
        Box(
            modifier = Modifier
                .size((size - 12).dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(ringColor.copy(alpha = 0.3f), UnifiedCallColors.electricPurple.copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (contactBitmap != null) {
                Image(
                    bitmap = contactBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = (name?.firstOrNull() ?: phoneNumber.firstOrNull() ?: '#').uppercase().toString(),
                    fontSize = (size / 3).sp,
                    fontWeight = FontWeight.Bold,
                    color = UnifiedCallColors.textWhite
                )
            }
        }
    }
}

@Composable
private fun GradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, UnifiedCallColors.neonCyan.copy(alpha = 0.3f), Color.Transparent)
                )
            )
    )
}

@Composable
private fun SimButton(
    label: String,
    carrier: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale).height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                carrier.take(10),
                fontSize = 11.sp,
                color = UnifiedCallColors.textDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NexusGlowButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp).scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NexusEndCallButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "scale"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .background(
                brush = Brush.linearGradient(listOf(UnifiedCallColors.callRed, Color(0xFFD63031))),
                shape = CircleShape
            )
            .border(2.dp, UnifiedCallColors.callRed.copy(alpha = 0.5f), CircleShape),
        interactionSource = interactionSource
    ) {
        Icon(
            Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun CallControlsRow(
    isMuted: Boolean,
    isSpeaker: Boolean,
    isOnHold: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onKeypadToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CallControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = isMuted,
            activeColor = UnifiedCallColors.callRed,
            onClick = onMuteToggle
        )
        CallControlButton(
            icon = Icons.Default.Dialpad,
            label = "Keypad",
            onClick = onKeypadToggle
        )
        CallControlButton(
            icon = if (isSpeaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown,
            label = if (isSpeaker) "Speaker" else "Earpiece",
            isActive = isSpeaker,
            activeColor = UnifiedCallColors.neonCyan,
            onClick = onSpeakerToggle
        )
        CallControlButton(
            icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
            label = if (isOnHold) "Resume" else "Hold",
            isActive = isOnHold,
            activeColor = UnifiedCallColors.solarOrange,
            onClick = onHoldToggle
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = UnifiedCallColors.neonCyan,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (isActive) activeColor.copy(alpha = 0.2f) else UnifiedCallColors.glassLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor, CircleShape)
                .border(
                    1.dp,
                    if (isActive) activeColor.copy(alpha = 0.5f) else Color.Transparent,
                    CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else UnifiedCallColors.textSilver,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) activeColor else UnifiedCallColors.textDim
        )
    }
}

@Composable
private fun DtmfKeypadGrid(onDtmf: (Char) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDtmf(key.first())
                        },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = UnifiedCallColors.glassLight.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, UnifiedCallColors.neonCyan.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = UnifiedCallColors.textWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideToAnswerReject(
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val maxDrag = with(density) { 100.dp.toPx() }
    val threshold = maxDrag * 0.7f

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "drag"
    )

    val answerProgress = (animatedOffset / maxDrag).coerceIn(0f, 1f)
    val rejectProgress = (-animatedOffset / maxDrag).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        UnifiedCallColors.callRed.copy(alpha = 0.2f * rejectProgress),
                        UnifiedCallColors.glassLight.copy(alpha = 0.3f),
                        UnifiedCallColors.callGreen.copy(alpha = 0.2f * answerProgress)
                    )
                ),
                shape = RoundedCornerShape(36.dp)
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        UnifiedCallColors.callRed.copy(alpha = 0.3f + 0.4f * rejectProgress),
                        UnifiedCallColors.textDim.copy(alpha = 0.2f),
                        UnifiedCallColors.callGreen.copy(alpha = 0.3f + 0.4f * answerProgress)
                    )
                ),
                RoundedCornerShape(36.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Left arrow hint (reject)
        Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = UnifiedCallColors.callRed.copy(alpha = 0.5f + 0.5f * rejectProgress),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(24.dp)
        )

        // Right arrow hint (answer)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = UnifiedCallColors.callGreen.copy(alpha = 0.5f + 0.5f * answerProgress),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(24.dp)
        )

        // Draggable button
        Box(
            modifier = Modifier
                .offset(x = with(density) { animatedOffset.toDp() })
                .size(60.dp)
                .background(
                    brush = Brush.linearGradient(
                        when {
                            answerProgress > 0.3f -> listOf(UnifiedCallColors.callGreen, Color(0xFF00B85C))
                            rejectProgress > 0.3f -> listOf(UnifiedCallColors.callRed, Color(0xFFD63031))
                            else -> listOf(UnifiedCallColors.neonCyan, UnifiedCallColors.electricPurple)
                        }
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragOffset > threshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAnswer()
                                }
                                dragOffset < -threshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onReject()
                                }
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount).coerceIn(-maxDrag, maxDrag)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    answerProgress > 0.3f -> Icons.Default.Call
                    rejectProgress > 0.3f -> Icons.Default.CallEnd
                    else -> Icons.Default.Phone
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════

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

private fun loadContactPhoto(
    context: Context,
    photoUri: String?,
    phoneNumber: String
): android.graphics.Bitmap? {
    // Try provided URI first
    if (photoUri != null) {
        try {
            val uri = Uri.parse(photoUri)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                return android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {}
    }

    // Try to look up by phone number
    try {
        val contactUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            contactUri,
            arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val photoUriStr = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                )
                if (photoUriStr != null) {
                    val uri = Uri.parse(photoUriStr)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        return android.graphics.BitmapFactory.decodeStream(inputStream)
                    }
                }
            }
        }
    } catch (_: Exception) {}

    return null
}

