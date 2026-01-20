package com.example.mentra.dialer.ui.call

import androidx.compose.ui.graphics.Color

/**
 * Color palette for call UI components
 */
object CallColors {
    // Background layers
    val voidBlack = Color(0xFF000308)
    val deepSpace = Color(0xFF030810)
    val nebula = Color(0xFF0A1628)

    // Glass effects
    val glassCore = Color(0xFF0D1B2A).copy(alpha = 0.85f)
    val glassSurface = Color(0xFF1B2838).copy(alpha = 0.6f)
    val glassHighlight = Color(0xFF00E5FF).copy(alpha = 0.1f)

    // Accent colors
    val cyberCyan = Color(0xFF00E5FF)
    val neonPurple = Color(0xFF7C4DFF)
    val plasmaBlue = Color(0xFF2979FF)
    val holoPink = Color(0xFFFF4081)

    // Action colors
    val answerGreen = Color(0xFF00E676)
    val answerGreenGlow = Color(0xFF00E676).copy(alpha = 0.4f)
    val rejectRed = Color(0xFFFF5252)
    val rejectRedGlow = Color(0xFFFF5252).copy(alpha = 0.4f)

    // Text
    val textPure = Color.White
    val textSilver = Color(0xFFE0E6ED)
    val textMuted = Color(0xFF8892A0)
    val textDim = Color(0xFF4A5568)

    // Borders
    val borderGlow = Color(0xFF00E5FF).copy(alpha = 0.5f)
    val borderSubtle = Color(0xFF1E3A5F).copy(alpha = 0.8f)
}

