package com.example.mentra.dialer.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Avatar with animated glow ring
 */
@Composable
fun AvatarWithGlow(initial: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .rotate(ringRotation)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CallColors.answerGreen,
                            CallColors.cyberCyan,
                            Color.Transparent,
                            Color.Transparent,
                            CallColors.answerGreen
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glow layer
        Box(
            modifier = Modifier
                .size(90.dp)
                .alpha(glowPulse * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CallColors.answerGreen.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(20.dp)
        )

        // Main avatar
        Box(
            modifier = Modifier
                .size(85.dp)
                .shadow(16.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.nebula,
                            CallColors.deepSpace
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.cyberCyan.copy(alpha = 0.6f),
                            CallColors.neonPurple.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = CallColors.cyberCyan,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
