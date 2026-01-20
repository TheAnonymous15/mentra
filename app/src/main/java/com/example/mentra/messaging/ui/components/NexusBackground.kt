package com.example.mentra.messaging.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.mentra.messaging.ui.theme.NexusColors

/**
 * ═══════════════════════════════════════════════════════════════════
 * BACKGROUND EFFECTS - Futuristic/Cyberpunk backgrounds
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Futuristic gradient background with glassmorphism effect
 */
@Composable
fun FuturisticBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF232526)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        content()
    }
}

/**
 * Animated glowing orbs for cyberpunk aesthetic
 */
@Composable
fun NexusBackgroundOrbs(glowAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top-right cyan orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NexusColors.primary.copy(alpha = glowAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.1f),
            radius = size.width * 0.5f
        )

        // Bottom-left purple orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NexusColors.secondary.copy(alpha = glowAlpha * 0.2f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.1f, size.height * 0.9f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.1f, size.height * 0.9f),
            radius = size.width * 0.4f
        )
    }
}

