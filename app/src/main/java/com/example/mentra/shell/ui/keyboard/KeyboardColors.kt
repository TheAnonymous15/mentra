package com.example.mentra.shell.ui.keyboard

import androidx.compose.ui.graphics.Color

/**
 * MENTRA NEXUS KEYBOARD - Color Theme
 * Ultra-futuristic glassmorphic color palette
 */
object MentraColors {
    // Deep space backgrounds
    val voidBlack = Color(0xFF050508)
    val deepSpace = Color(0xFF0A0D14)
    val nebulaDark = Color(0xFF0F1318)
    val cosmicGray = Color(0xFF151A22)

    // Glassmorphic surfaces
    val glassBase = Color(0xFF1A1F2A).copy(alpha = 0.85f)
    val glassSurface = Color(0xFF222836).copy(alpha = 0.75f)
    val glassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val glassBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)

    // Key backgrounds
    val keyDefault = Color(0xFF1E232E)
    val keyPressed = Color(0xFF2A3142)
    val keySpecial = Color(0xFF252B38)

    // Neon accent colors
    val neonCyan = Color(0xFF00F0FF)
    val neonMagenta = Color(0xFFFF00E5)
    val neonPurple = Color(0xFF8B5CF6)
    val neonBlue = Color(0xFF3B82F6)
    val neonGreen = Color(0xFF00FF94)
    val neonOrange = Color(0xFFFF6B2C)
    val neonRed = Color(0xFFFF3366)
    val neonGold = Color(0xFFFFD700)

    // Text colors
    val textPrimary = Color(0xFFF0F4FF)
    val textSecondary = Color(0xFF8892A8)
    val textMuted = Color(0xFF5A6478)

    // Gradients
    val cyanPurpleGradient = listOf(neonCyan, neonPurple)
    val magentaBlueGradient = listOf(neonMagenta, neonBlue)
    val rainbowGradient = listOf(neonCyan, neonBlue, neonPurple, neonMagenta)
}

