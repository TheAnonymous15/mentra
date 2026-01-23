package com.example.mentra.shell.ui.keyboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FUNCTION ROW - Glassmorphic futuristic terminal navigation bar
 * Premium design with subtle glow and gradient effects
 */

@Composable
fun FunctionRow(
    onTab: () -> Unit,
    onEsc: () -> Unit,
    onCharInsert: (String) -> Unit,
    isCtrlActive: Boolean,
    onCtrlToggle: () -> Unit,
    onClear: () -> Unit,
    onHome: () -> Unit,
    onEnd: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit
) {
    // Subtle pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "func_row_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "func_glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                // Outer glow effect
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MentraColors.neonCyan.copy(alpha = 0.05f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 0.8f
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx())
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MentraColors.glassSurface.copy(alpha = 0.4f),
                        MentraColors.glassBase.copy(alpha = 0.6f),
                        MentraColors.deepSpace.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MentraColors.glassHighlight.copy(alpha = 0.5f),
                        MentraColors.glassBorder.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        // Inner highlight line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 20.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MentraColors.neonCyan.copy(alpha = 0.2f * glowAlpha),
                            MentraColors.neonPurple.copy(alpha = 0.15f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: ESC | / | - | HOME | ↑ | END | PGUP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuncKeyFlat(text = "ESC", onClick = onEsc, accentColor = MentraColors.neonRed)
                FuncKeyFlat(text = "/", onClick = { onCharInsert("/") })
                FuncKeyFlat(text = "-", onClick = { onCharInsert("-") })
                FuncKeyFlat(text = "HOME", onClick = onHome)
                FuncKeyFlat(text = "↑", onClick = onArrowUp, accentColor = MentraColors.neonCyan)
                FuncKeyFlat(text = "END", onClick = onEnd)
                FuncKeyFlat(text = "PGUP", onClick = { })
            }

            // Subtle separator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(horizontal = 16.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MentraColors.glassBorder.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Row 2: TAB | CTRL | ALT | ← | ↓ | → | PGDN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuncKeyFlat(text = "TAB", onClick = onTab, accentColor = MentraColors.neonPurple)
                FuncKeyFlat(
                    text = "CTRL",
                    onClick = onCtrlToggle,
                    accentColor = if (isCtrlActive) MentraColors.neonGreen else null,
                    isActive = isCtrlActive
                )
                FuncKeyFlat(text = "ALT", onClick = { })
                FuncKeyFlat(text = "←", onClick = onArrowLeft, accentColor = MentraColors.neonCyan)
                FuncKeyFlat(text = "↓", onClick = onArrowDown, accentColor = MentraColors.neonCyan)
                FuncKeyFlat(text = "→", onClick = onArrowRight, accentColor = MentraColors.neonCyan)
                FuncKeyFlat(text = "PGDN", onClick = { })
            }
        }
    }
}

// Flat function key - clean text with glow on press
@Composable
fun FuncKeyFlat(
    text: String,
    onClick: () -> Unit,
    accentColor: Color? = null,
    isActive: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "func_scale"
    )

    val displayColor = if (isActive) MentraColors.neonGreen else (accentColor ?: MentraColors.textSecondary)
    val pressedColor = accentColor ?: MentraColors.neonCyan

    // Pulsing animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "active_pulse")
    val activeGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "active_glow"
    )

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 38.dp)
            .scale(scale)
            .then(
                if (isActive) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MentraColors.neonGreen.copy(alpha = 0.15f * activeGlow))
                        .border(
                            width = 1.dp,
                            color = MentraColors.neonGreen.copy(alpha = 0.5f * activeGlow),
                            shape = RoundedCornerShape(6.dp)
                        )
                } else if (isPressed && accentColor != null) {
                    Modifier.drawBehind {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.2f),
                            radius = size.maxDimension * 0.6f
                        )
                    }
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPressed) pressedColor else displayColor,
            fontSize = when {
                text.length > 3 -> 10.sp
                text.length == 1 -> 14.sp
                else -> 11.sp
            },
            fontWeight = if (accentColor != null || isActive) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

