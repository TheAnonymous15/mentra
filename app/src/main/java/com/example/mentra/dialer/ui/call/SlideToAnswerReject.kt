package com.example.mentra.dialer.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Slide to Answer/Reject gesture component
 */
@Composable
fun SlideToAnswerReject(
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val sliderWidth = 280.dp
    val thumbSize = 60.dp
    val maxOffset = with(density) { (sliderWidth - thumbSize).toPx() }
    val threshold = maxOffset * 0.7f

    var offsetX by remember { mutableFloatStateOf(maxOffset / 2) }
    var isDragging by remember { mutableStateOf(false) }
    var hasTriggered by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = if (!isDragging) maxOffset / 2 else offsetX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "thumbOffset"
    )

    // Calculate progress for visual feedback (-1 = reject, 0 = center, 1 = answer)
    val progress = ((animatedOffset - maxOffset / 2) / (maxOffset / 2)).coerceIn(-1f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "slider")
    val arrowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowPulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(sliderWidth)
                .height(70.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track background with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CallColors.rejectRed.copy(alpha = 0.15f + 0.15f * (-progress).coerceAtLeast(0f)),
                                CallColors.glassSurface,
                                CallColors.answerGreen.copy(alpha = 0.15f + 0.15f * progress.coerceAtLeast(0f))
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CallColors.rejectRed.copy(alpha = 0.5f + 0.3f * (-progress).coerceAtLeast(0f)),
                                CallColors.borderSubtle,
                                CallColors.answerGreen.copy(alpha = 0.5f + 0.3f * progress.coerceAtLeast(0f))
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reject indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.alpha(0.7f + 0.3f * (-progress).coerceAtLeast(0f))
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = CallColors.rejectRed,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(0.5f + 0.5f * arrowPulse)
                                .graphicsLayer { translationX = -4f * arrowPulse }
                        )
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = null,
                            tint = CallColors.rejectRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Answer indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.alpha(0.7f + 0.3f * progress.coerceAtLeast(0f))
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = CallColors.answerGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CallColors.answerGreen,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(0.5f + 0.5f * arrowPulse)
                                .graphicsLayer { translationX = 4f * arrowPulse }
                        )
                    }
                }
            }

            // Draggable thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffset.roundToInt() - (maxOffset / 2).roundToInt(), 0) }
                    .size(thumbSize)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = when {
                            progress > 0.3f -> CallColors.answerGreen.copy(alpha = 0.5f)
                            progress < -0.3f -> CallColors.rejectRed.copy(alpha = 0.5f)
                            else -> CallColors.cyberCyan.copy(alpha = 0.3f)
                        }
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                when {
                                    progress > 0.3f -> CallColors.answerGreen
                                    progress < -0.3f -> CallColors.rejectRed
                                    else -> CallColors.cyberCyan
                                },
                                when {
                                    progress > 0.3f -> CallColors.answerGreen.copy(alpha = 0.8f)
                                    progress < -0.3f -> CallColors.rejectRed.copy(alpha = 0.8f)
                                    else -> CallColors.neonPurple.copy(alpha = 0.6f)
                                }
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                hasTriggered = false
                            },
                            onDragEnd = {
                                isDragging = false
                                if (!hasTriggered) {
                                    offsetX = maxOffset / 2
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                if (!hasTriggered) {
                                    offsetX = maxOffset / 2
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (!hasTriggered) {
                                    offsetX = (offsetX + dragAmount).coerceIn(0f, maxOffset)

                                    // Check for answer (right side)
                                    if (offsetX > maxOffset - threshold / 3) {
                                        hasTriggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { onAnswer() }
                                    }
                                    // Check for reject (left side)
                                    else if (offsetX < threshold / 3) {
                                        hasTriggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { onReject() }
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        progress > 0.3f -> Icons.Default.Call
                        progress < -0.3f -> Icons.Default.CallEnd
                        else -> Icons.Default.SwipeRight
                    },
                    contentDescription = "Slide to answer or reject",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Instruction text
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                progress > 0.5f -> "Release to Answer"
                progress < -0.5f -> "Release to Decline"
                else -> "← Decline    |    Answer →"
            },
            color = when {
                progress > 0.5f -> CallColors.answerGreen
                progress < -0.5f -> CallColors.rejectRed
                else -> CallColors.textDim
            },
            fontSize = 12.sp,
            fontWeight = if (progress.absoluteValue > 0.5f) FontWeight.Medium else FontWeight.Normal
        )
    }
}

