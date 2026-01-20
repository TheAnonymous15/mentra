package com.example.mentra.messaging.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.messaging.ui.theme.NexusColors
import kotlinx.coroutines.delay

/**
 * ═══════════════════════════════════════════════════════════════════
 * COMPOSE BUTTON COMPONENTS - 3D Futuristic Design
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * 3D Compose Button with pulsing glow and floating animation
 */
@Composable
fun Nexus3DComposeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "fab3d")

    // Pulsing glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Floating animation
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Rotating halo
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        ),
        label = "halo"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Box(
        modifier = modifier
            .offset(y = (-floatOffset).dp)
            .scale(scale)
    ) {
        // Outer glow ring
        Canvas(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = haloRotation }
        ) {
            // Draw rotating dashed circle
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NexusColors.primary.copy(alpha = pulseAlpha),
                        Color.Transparent,
                        NexusColors.secondary.copy(alpha = pulseAlpha * 0.7f),
                        Color.Transparent,
                        NexusColors.accent.copy(alpha = pulseAlpha * 0.5f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Shadow layer (3D effect)
        Box(
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.Center)
                .offset(y = 4.dp)
                .blur(8.dp)
                .background(
                    color = NexusColors.primary.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Main 3D button
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isPressed = true
                onClick()
            },
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.Center)
                .shadow(elevation, CircleShape, spotColor = NexusColors.primary),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NexusColors.primary,
                                NexusColors.primary.copy(alpha = 0.8f),
                                Color(0xFF00A896) // Darker at bottom for 3D effect
                            )
                        ),
                        shape = CircleShape
                    )
                    // Inner highlight for 3D effect
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.5f
                            ),
                            radius = size.minDimension / 2 - 4.dp.toPx()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Pro icon: Pen with plus
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = "New Message",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    // Small plus badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(
                                color = NexusColors.accent,
                                shape = CircleShape
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = NexusColors.background,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

/**
 * Legacy compose button - delegates to 3D version
 */
@Composable
fun NexusComposeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Nexus3DComposeButton(onClick = onClick, modifier = modifier)
}

/**
 * Futuristic FAB with glassmorphism
 */
@Composable
fun FuturisticFAB(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .padding(24.dp)
            .size(64.dp)
            .scale(pulse)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = Color.Cyan
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta
                    )
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Message",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

