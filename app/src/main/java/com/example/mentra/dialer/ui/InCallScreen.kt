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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
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

/**
 * NEXUS IN-CALL MODAL - Compact Alien Glassmorphic Interface
 * Ultra-modern floating call control
 */

private object NexusCallTheme {
    val voidBlack = Color(0xFF000000)
    val deepSpace = Color(0xFF050A15)
    val nebula = Color(0xFF0A1628)
    val glass = Color(0xFF0D1B2A).copy(alpha = 0.95f)

    val neonCyan = Color(0xFF00F5FF)
    val electricPurple = Color(0xFFBF40FF)
    val plasmaGreen = Color(0xFF00FF87)
    val solarOrange = Color(0xFFFF6B35)
    val cosmicPink = Color(0xFFFF2E63)

    val textWhite = Color(0xFFFFFFFF)
    val textSilver = Color(0xFFB8C5D6)
    val textDim = Color(0xFF5C7A99)
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
    val totalCallCost by viewModel.totalCallCost.collectAsState()
    val isBillingTracking by viewModel.isBillingTracking.collectAsState()

    var showDtmfKeypad by remember { mutableStateOf(false) }
    var dtmfInput by remember { mutableStateOf("") }
    var isCallActive by remember { mutableStateOf(false) }
    var callDuration by remember { mutableLongStateOf(0L) }
    var callConnectedTime by remember { mutableLongStateOf(0L) }

    // Phone state listener
    DisposableEffect(Unit) {
        val callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    when (state) {
                        TelephonyManager.EXTRA_STATE_IDLE -> isCallActive = false
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            if (!isCallActive) {
                                isCallActive = true
                                if (callConnectedTime == 0L) callConnectedTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
        }
        context.registerReceiver(callStateReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        onDispose { try { context.unregisterReceiver(callStateReceiver) } catch (_: Exception) {} }
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

    // Auto-close
    LaunchedEffect(callState, isCallActive) {
        if ((callState == CallState.DISCONNECTED || callState == CallState.IDLE) && !isCallActive) {
            delay(1500)
            onCallEnded()
        }
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(NexusCallTheme.voidBlack.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient particles
            AlienAmbientEffect(callState, isCallActive)

            // Compact floating modal
            NexusCallModal(
                currentCall = currentCall,
                callState = callState,
                audioState = audioState,
                callDuration = callDuration,
                callCost = if (isBillingTracking) totalCallCost else null,
                isCallActive = isCallActive,
                showDtmfKeypad = showDtmfKeypad,
                dtmfInput = dtmfInput,
                onDtmfDigit = { digit -> dtmfInput += digit; viewModel.sendDtmf(digit.first()) },
                onToggleKeypad = { showDtmfKeypad = !showDtmfKeypad },
                onMuteToggle = { viewModel.toggleMute() },
                onSpeakerToggle = { viewModel.toggleSpeaker() },
                onHoldToggle = { viewModel.toggleHold() },
                onEndCall = { viewModel.endCall() }
            )
        }
    }
}

@Composable
private fun AlienAmbientEffect(callState: CallState, isCallActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotate"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(if (callState == CallState.DIALING) 600 else 2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    val stateColor = when {
        callState == CallState.DIALING -> NexusCallTheme.solarOrange
        callState == CallState.RINGING -> NexusCallTheme.plasmaGreen
        isCallActive -> NexusCallTheme.neonCyan
        else -> NexusCallTheme.cosmicPink
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        // Orbital rings
        for (i in 1..3) {
            rotate(rotation + i * 40f, Offset(cx, cy)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(stateColor.copy(alpha = 0.15f / i), Color.Transparent, stateColor.copy(alpha = 0.1f / i)),
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

@Composable
private fun NexusCallModal(
    currentCall: CallInfo?,
    callState: CallState,
    audioState: AudioRouteState,
    callDuration: Long,
    callCost: Double?,
    isCallActive: Boolean,
    showDtmfKeypad: Boolean,
    dtmfInput: String,
    onDtmfDigit: (String) -> Unit,
    onToggleKeypad: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "modal")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Surface(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .shadow(48.dp, RoundedCornerShape(32.dp), ambientColor = NexusCallTheme.neonCyan.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(listOf(NexusCallTheme.glass, NexusCallTheme.deepSpace)),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(
                        NexusCallTheme.neonCyan.copy(alpha = borderGlow),
                        NexusCallTheme.electricPurple.copy(alpha = borderGlow * 0.6f)
                    )),
                    RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status chip
                CallStatusChip(callState, isCallActive)

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar & Info
                CompactCallerSection(
                    number = currentCall?.number ?: "Unknown",
                    name = currentCall?.contactName,
                    duration = if (isCallActive && callDuration > 0) callDuration else null,
                    callCost = callCost,
                    callState = callState,
                    isCallActive = isCallActive
                )

                // DTMF display
                if (showDtmfKeypad && dtmfInput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DtmfDisplayCompact(dtmfInput)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls or Keypad
                AnimatedContent(
                    targetState = showDtmfKeypad,
                    transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
                    label = "controls"
                ) { showKeypad ->
                    if (showKeypad) {
                        CompactDtmfKeypad(onDtmfDigit, onToggleKeypad)
                    } else if (callState == CallState.ACTIVE || isCallActive) {
                        CompactCallControls(audioState, currentCall?.isOnHold == true, onMuteToggle, onSpeakerToggle, onHoldToggle, onToggleKeypad)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // End call
                CompactEndCallButton(onEndCall)
            }
        }
    }
}

@Composable
private fun CallStatusChip(callState: CallState, isCallActive: Boolean) {
    val (text, color) = when {
        callState == CallState.DIALING -> "CALLING" to NexusCallTheme.solarOrange
        callState == CallState.RINGING -> "INCOMING" to NexusCallTheme.plasmaGreen
        callState == CallState.ACTIVE || isCallActive -> "LIVE" to NexusCallTheme.neonCyan
        callState == CallState.DISCONNECTED -> "ENDED" to NexusCallTheme.cosmicPink
        else -> "..." to NexusCallTheme.textDim
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun CompactCallerSection(number: String, name: String?, duration: Long?, callCost: Double?, callState: CallState, isCallActive: Boolean) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )

    // Load contact photo
    var contactBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var resolvedName by remember { mutableStateOf(name) }

    LaunchedEffect(number) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contactUri = android.net.Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(number)
                )
                context.contentResolver.query(
                    contactUri,
                    arrayOf(android.provider.ContactsContract.PhoneLookup.PHOTO_URI, android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        if (resolvedName == null) {
                            val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                            if (nameIdx >= 0) resolvedName = cursor.getString(nameIdx)
                        }
                        val photoIdx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.PHOTO_URI)
                        if (photoIdx >= 0) {
                            cursor.getString(photoIdx)?.let { photoUri ->
                                context.contentResolver.openInputStream(android.net.Uri.parse(photoUri))?.use {
                                    contactBitmap = android.graphics.BitmapFactory.decodeStream(it)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val initial = remember(resolvedName, number) {
        resolvedName?.firstOrNull { it.isLetter() }?.uppercase()?.toString()
            ?: number.firstOrNull { it.isDigit() }?.toString() ?: "?"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Compact avatar with rotating ring
        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            // Rotating gradient ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .rotate(ringRotation)
                    .border(
                        2.dp,
                        Brush.sweepGradient(listOf(NexusCallTheme.neonCyan, NexusCallTheme.electricPurple, Color.Transparent, NexusCallTheme.neonCyan)),
                        CircleShape
                    )
            )

            // Inner avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NexusCallTheme.nebula, NexusCallTheme.deepSpace)))
                    .border(1.dp, NexusCallTheme.textDim.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = contactBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(initial, color = NexusCallTheme.textWhite, fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Name & Number
        val displayName = resolvedName ?: name
        if (displayName != null) {
            Text(displayName, color = NexusCallTheme.textWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(number, color = NexusCallTheme.textDim, fontSize = 12.sp)
        } else {
            Text(number, color = NexusCallTheme.textWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Duration
        duration?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NexusCallTheme.plasmaGreen.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, NexusCallTheme.plasmaGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(4.dp).background(NexusCallTheme.plasmaGreen, CircleShape))
                    Text(formatDuration(it), color = NexusCallTheme.plasmaGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }
            }
        }

        // Call Cost (only for outgoing calls when billing is active)
        callCost?.let { cost ->
            if (cost > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KSH %.2f".format(cost),
                    color = NexusCallTheme.solarOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CompactCallControls(
    audioState: AudioRouteState,
    isOnHold: Boolean,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onHold: () -> Unit,
    onKeypad: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MiniControlButton(if (audioState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute", audioState.isMuted, NexusCallTheme.cosmicPink, onMute)
        MiniControlButton(Icons.Filled.Dialpad, "Keypad", false, NexusCallTheme.neonCyan, onKeypad)
        MiniControlButton(if (audioState.currentRoute == AudioRoute.SPEAKER) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp, "Speaker", audioState.currentRoute == AudioRoute.SPEAKER, NexusCallTheme.neonCyan, onSpeaker)
        MiniControlButton(if (isOnHold) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (isOnHold) "Resume" else "Hold", isOnHold, NexusCallTheme.solarOrange, onHold)
    }
}

@Composable
private fun MiniControlButton(icon: ImageVector, label: String, isActive: Boolean, activeColor: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                .border(1.dp, if (isActive) activeColor.copy(alpha = 0.5f) else NexusCallTheme.textDim.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = if (isActive) activeColor else NexusCallTheme.textSilver, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = if (isActive) activeColor else NexusCallTheme.textDim, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DtmfDisplayCompact(input: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(10.dp),
        color = NexusCallTheme.nebula,
        border = BorderStroke(1.dp, NexusCallTheme.neonCyan.copy(alpha = 0.3f))
    ) {
        Text(input, color = NexusCallTheme.neonCyan, fontSize = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun CompactDtmfKeypad(onDigit: (String) -> Unit, onHide: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#"))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(NexusCallTheme.nebula.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, NexusCallTheme.textDim.copy(alpha = 0.2f), CircleShape)
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDigit(digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit, color = NexusCallTheme.textWhite, fontSize = 18.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
        TextButton(onClick = onHide, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Hide", color = NexusCallTheme.textDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CompactEndCallButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "end")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow
        Box(modifier = Modifier.size(64.dp).background(Brush.radialGradient(listOf(NexusCallTheme.cosmicPink.copy(alpha = glow), Color.Transparent)), CircleShape))

        Surface(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
            modifier = Modifier.size(52.dp).shadow(8.dp, CircleShape, ambientColor = NexusCallTheme.cosmicPink),
            shape = CircleShape,
            color = NexusCallTheme.cosmicPink
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.CallEnd, "End", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

