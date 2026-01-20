package com.example.mentra.dialer.ui

import androidx.compose.ui.graphics.Color

/**
 * NEXUS DIALER - Color Palette
 * Unified color scheme for the futuristic dialer interface
 */
object NexusDialerColors {
    val background = Color(0xFF030508)
    val surface = Color(0xFF080C14)
    val card = Color(0xFF0C1220)
    val cardGlass = Color(0xFF141C2D)

    val primary = Color(0xFF00E5FF)      // Electric Cyan
    val secondary = Color(0xFF9D4EDD)    // Vivid Purple
    val accent = Color(0xFFFF6090)       // Pink
    val success = Color(0xFF00E676)      // Neon Green

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0BEC5)
    val textMuted = Color(0xFF607D8B)

    val callGreen = Color(0xFF00E676)
    val callRed = Color(0xFFFF5252)
    val simBlue = Color(0xFF448AFF)
    val simPurple = Color(0xFFB388FF)

    val gradientPrimary = listOf(Color(0xFF00E5FF), Color(0xFF00B8D4))
    val gradientCall = listOf(Color(0xFF00E676), Color(0xFF00C853))
    val gradientGlass = listOf(
        Color(0xFF1A2332).copy(alpha = 0.85f),
        Color(0xFF0D1520).copy(alpha = 0.7f)
    )
}

