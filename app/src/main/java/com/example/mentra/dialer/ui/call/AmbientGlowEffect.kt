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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ambient glow effect for the call modal background
 */
@Composable
fun AmbientGlowEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Rotating gradient orb
        Box(
            modifier = Modifier
                .size(350.dp)
                .rotate(rotation)
                .alpha(0.3f * pulse)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CallColors.answerGreen.copy(alpha = 0.3f),
                            Color.Transparent,
                            CallColors.cyberCyan.copy(alpha = 0.2f),
                            Color.Transparent,
                            CallColors.answerGreen.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                )
                .blur(60.dp)
        )

        // Pulsing ring
        Box(
            modifier = Modifier
                .size((180 + 40 * pulse).dp)
                .border(
                    width = (1 + pulse).dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CallColors.answerGreen.copy(alpha = 0.5f * pulse),
                            CallColors.cyberCyan.copy(alpha = 0.3f * pulse),
                            Color.Transparent,
                            CallColors.answerGreen.copy(alpha = 0.5f * pulse)
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

