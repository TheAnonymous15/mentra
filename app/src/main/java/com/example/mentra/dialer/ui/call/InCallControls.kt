package com.example.mentra.dialer.ui.call

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-call control button
 */
@Composable
fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) activeColor.copy(alpha = 0.25f)
                    else CallColors.glassSurface
                )
                .border(
                    width = 1.dp,
                    color = if (isActive) activeColor.copy(alpha = 0.6f) else CallColors.borderSubtle,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = activeColor),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) activeColor else CallColors.textSilver,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = if (isActive) activeColor else CallColors.textDim,
            fontSize = 10.sp
        )
    }
}

/**
 * In-call keypad for DTMF tones
 */
@Composable
fun InCallKeypad(
    onKeyPress: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(CallColors.glassSurface.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = CallColors.cyberCyan),
                                onClick = { onKeyPress(key) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key.toString(),
                            color = CallColors.textPure,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * End call button
 */
@Composable
fun EndCallButton(
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(12.dp, CircleShape, ambientColor = CallColors.rejectRed.copy(alpha = 0.5f))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CallColors.rejectRed,
                            CallColors.rejectRed.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onEndCall
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "End Call",
            color = CallColors.rejectRed,
            fontSize = 12.sp
        )
    }
}

/**
 * Add Call button - Opens dialer to add another party
 */
@Composable
fun AddCallButton(
    onAddCall: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled) CallColors.answerGreen.copy(alpha = 0.15f)
                    else CallColors.glassSurface.copy(alpha = 0.3f)
                )
                .border(
                    width = 1.dp,
                    color = if (isEnabled) CallColors.answerGreen.copy(alpha = 0.5f)
                           else CallColors.borderSubtle.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .then(
                    if (isEnabled) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = CallColors.answerGreen),
                            onClick = onAddCall
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = "Add Call",
                tint = if (isEnabled) CallColors.answerGreen else CallColors.textDim,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add Call",
            color = if (isEnabled) CallColors.answerGreen else CallColors.textDim,
            fontSize = 10.sp
        )
    }
}

/**
 * Merge Calls button - Merges multiple calls into conference
 */
@Composable
fun MergeCallsButton(
    onMergeCalls: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled) CallColors.cyberCyan.copy(alpha = 0.2f)
                    else CallColors.glassSurface.copy(alpha = 0.3f)
                )
                .border(
                    width = 1.dp,
                    color = if (isEnabled) CallColors.cyberCyan.copy(alpha = 0.6f)
                           else CallColors.borderSubtle.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .then(
                    if (isEnabled) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = CallColors.cyberCyan),
                            onClick = onMergeCalls
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CallMerge,
                contentDescription = "Merge Calls",
                tint = if (isEnabled) CallColors.cyberCyan else CallColors.textDim,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Merge",
            color = if (isEnabled) CallColors.cyberCyan else CallColors.textDim,
            fontSize = 10.sp
        )
    }
}

/**
 * Swap Calls button - Swaps between active and held call
 */
@Composable
fun SwapCallsButton(
    onSwapCalls: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled) CallColors.neonPurple.copy(alpha = 0.2f)
                    else CallColors.glassSurface.copy(alpha = 0.3f)
                )
                .border(
                    width = 1.dp,
                    color = if (isEnabled) CallColors.neonPurple.copy(alpha = 0.6f)
                           else CallColors.borderSubtle.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .then(
                    if (isEnabled) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = CallColors.neonPurple),
                            onClick = onSwapCalls
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SwapCalls,
                contentDescription = "Swap Calls",
                tint = if (isEnabled) CallColors.neonPurple else CallColors.textDim,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Swap",
            color = if (isEnabled) CallColors.neonPurple else CallColors.textDim,
            fontSize = 10.sp
        )
    }
}

/**
 * Conference call participants display
 */
@Composable
fun ConferenceParticipants(
    participants: List<String>,
    modifier: Modifier = Modifier
) {
    if (participants.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                CallColors.glassSurface.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                CallColors.cyberCyan.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                tint = CallColors.cyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Conference Call",
                color = CallColors.cyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        participants.forEachIndexed { index, participant ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(CallColors.neonPurple.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.firstOrNull()?.uppercase() ?: "?",
                        color = CallColors.textPure,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = participant,
                    color = CallColors.textSilver,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < participants.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

