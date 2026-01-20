package com.example.mentra.dialer.ui.call

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nexus Incoming Call Card - The ringing state card
 */
@Composable
fun NexusIncomingCallCard(
    phoneNumber: String,
    contactName: String?,
    ringTime: Long,
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card")

    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    val phoneShake by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Surface(
        modifier = Modifier
            .width(340.dp)
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = CallColors.cyberCyan.copy(alpha = 0.3f),
                spotColor = CallColors.answerGreen.copy(alpha = 0.2f)
            ),
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
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.answerGreen.copy(alpha = borderPulse),
                            CallColors.cyberCyan.copy(alpha = borderPulse * 0.6f),
                            CallColors.neonPurple.copy(alpha = borderPulse * 0.4f),
                            CallColors.answerGreen.copy(alpha = borderPulse)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
        ) {
            // Glass highlight overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CallColors.glassHighlight,
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CallColors.answerGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, CallColors.answerGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Animated phone icon
                        Icon(
                            Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = CallColors.answerGreen,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(phoneShake)
                        )
                        Text(
                            "INCOMING CALL",
                            color = CallColors.answerGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Avatar with glow ring
                AvatarWithGlow(
                    initial = (contactName?.firstOrNull() ?: phoneNumber.firstOrNull())
                        ?.uppercase()?.toString() ?: "?"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Caller info
                if (contactName != null) {
                    Text(
                        text = contactName,
                        color = CallColors.textPure,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = phoneNumber,
                        color = CallColors.textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = phoneNumber,
                        color = CallColors.textPure,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ring duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                CallColors.answerGreen.copy(alpha = if (ringTime % 2 == 0L) 1f else 0.5f),
                                CircleShape
                            )
                    )
                    Text(
                        text = CallUtils.formatRingTime(ringTime),
                        color = CallColors.textDim,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Slide to Answer/Reject
                SlideToAnswerReject(
                    onAnswer = onAnswer,
                    onReject = onReject
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Quick reply hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.alpha(0.6f)
                ) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = null,
                        tint = CallColors.textDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Swipe up for quick reply",
                        color = CallColors.textDim,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
