package com.example.mentra.dialer.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Active Call Card - Shows in-call controls
 */
@Composable
fun ActiveCallCard(
    phoneNumber: String,
    contactName: String?,
    callDuration: Long,
    callCost: Double? = null,
    isMuted: Boolean,
    isSpeaker: Boolean,
    isOnHold: Boolean,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onHoldToggle: (Boolean) -> Unit,
    onKeypadPress: (Char) -> Unit,
    onEndCall: () -> Unit,
    // Conference call support
    onAddCall: ((String) -> Unit)? = null, // Now takes the number to call
    onMergeCalls: (() -> Unit)? = null,
    onSwapCalls: (() -> Unit)? = null,
    hasMultipleCalls: Boolean = false,
    isConference: Boolean = false,
    conferenceParticipants: List<String> = emptyList()
) {
    var showKeypad by remember { mutableStateOf(false) }
    var showAddCallModal by remember { mutableStateOf(false) }

    // Add Call Modal
    if (showAddCallModal && onAddCall != null) {
        AddCallModal(
            onDismiss = { showAddCallModal = false },
            onCallNumber = { number ->
                showAddCallModal = false
                onAddCall(number)
            }
        )
    }

    Surface(
        modifier = Modifier
            .width(340.dp)
            .shadow(32.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CallColors.glassCore,
                            CallColors.deepSpace.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.answerGreen.copy(alpha = 0.6f),
                            CallColors.cyberCyan.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status badge - changes for conference
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isConference) CallColors.cyberCyan.copy(alpha = 0.15f)
                           else CallColors.answerGreen.copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (isConference) CallColors.cyberCyan.copy(alpha = 0.5f)
                        else CallColors.answerGreen.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (isConference) Icons.Default.Groups else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (isConference) CallColors.cyberCyan else CallColors.answerGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isConference) "● CONFERENCE" else "● CONNECTED",
                            color = if (isConference) CallColors.cyberCyan else CallColors.answerGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Conference participants or single caller info
                if (isConference && conferenceParticipants.isNotEmpty()) {
                    ConferenceParticipants(
                        participants = conferenceParticipants,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Caller info
                    Text(
                        text = contactName ?: phoneNumber,
                        color = CallColors.textPure,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (contactName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phoneNumber,
                            color = CallColors.textMuted,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Call duration with LIVE indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Pulsing live dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CallColors.answerGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE",
                        color = CallColors.answerGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = CallUtils.formatCallDuration(callDuration),
                        color = CallColors.textPure,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Call cost (only for outgoing calls)
                callCost?.let { cost ->
                    if (cost > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CallColors.neonPurple.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, CallColors.neonPurple.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = CallColors.neonPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Call Cost:",
                                    color = CallColors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "KSH %.2f".format(cost),
                                    color = CallColors.neonPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // In-call controls - Row 1 (Main controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute button
                    InCallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        activeColor = CallColors.rejectRed,
                        onClick = { onMuteToggle(!isMuted) }
                    )

                    // Keypad button
                    InCallControlButton(
                        icon = Icons.Default.Dialpad,
                        label = "Keypad",
                        isActive = showKeypad,
                        activeColor = CallColors.cyberCyan,
                        onClick = { showKeypad = !showKeypad }
                    )

                    // Speaker button
                    InCallControlButton(
                        icon = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = "Speaker",
                        isActive = isSpeaker,
                        activeColor = CallColors.cyberCyan,
                        onClick = { onSpeakerToggle(!isSpeaker) }
                    )

                    // Hold button
                    InCallControlButton(
                        icon = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (isOnHold) "Resume" else "Hold",
                        isActive = isOnHold,
                        activeColor = CallColors.neonPurple,
                        onClick = { onHoldToggle(!isOnHold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // In-call controls - Row 2 (Conference controls)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Add Call button
                    if (onAddCall != null) {
                        AddCallButton(
                            onAddCall = { showAddCallModal = true },
                            isEnabled = !isOnHold
                        )
                    }

                    // Swap Calls button (visible when multiple calls)
                    if (hasMultipleCalls && onSwapCalls != null) {
                        SwapCallsButton(
                            onSwapCalls = onSwapCalls,
                            isEnabled = true
                        )
                    }

                    // Merge Calls button (visible when multiple calls)
                    if (hasMultipleCalls && onMergeCalls != null) {
                        MergeCallsButton(
                            onMergeCalls = onMergeCalls,
                            isEnabled = true
                        )
                    }
                }

                // Keypad (shown when active)
                AnimatedVisibility(
                    visible = showKeypad,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    InCallKeypad(
                        onKeyPress = onKeypadPress,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // End call button
                EndCallButton(onEndCall = onEndCall)
            }
        }
    }
}

/**
 * Connecting Call Card - Shows while call is being connected
 */
@Composable
fun ConnectingCallCard(
    phoneNumber: String,
    contactName: String?
) {
    Surface(
        modifier = Modifier
            .width(340.dp)
            .shadow(32.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CallColors.glassCore,
                            CallColors.deepSpace.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = CallColors.cyberCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Connecting...",
                    color = CallColors.cyberCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contactName ?: phoneNumber,
                    color = CallColors.textPure,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Ended Call Card
 */
@Composable
fun EndedCallCard(
    phoneNumber: String,
    contactName: String?
) {
    Surface(
        modifier = Modifier
            .width(340.dp)
            .shadow(32.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CallColors.glassCore,
                            CallColors.deepSpace.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = null,
                    tint = CallColors.textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Call Ended",
                    color = CallColors.textMuted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contactName ?: phoneNumber,
                    color = CallColors.textDim,
                    fontSize = 14.sp
                )
            }
        }
    }
}
