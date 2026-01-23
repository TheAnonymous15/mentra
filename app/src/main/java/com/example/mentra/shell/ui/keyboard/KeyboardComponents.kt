package com.example.mentra.shell.ui.keyboard

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * MENTRA KEYBOARD - Common Key Components
 * Flat keys without borders/backgrounds on 3D glass card
 */

// 3D Glassmorphic card wrapper for keyboard sections
@Composable
fun GlassmorphicKeyboardCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_glow_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // 3D shadow effect
            .drawBehind {
                // Bottom shadow for 3D depth
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                // Outer glow
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MentraColors.neonPurple.copy(alpha = 0.03f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = size.maxDimension
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MentraColors.glassSurface.copy(alpha = 0.5f),
                        MentraColors.glassBase.copy(alpha = 0.7f),
                        MentraColors.deepSpace.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MentraColors.glassHighlight.copy(alpha = 0.4f),
                        MentraColors.glassBorder.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
    ) {
        // Top highlight line for 3D effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 24.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MentraColors.glassHighlight.copy(alpha = 0.3f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun CharKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isLarge: Boolean = false,
    isHighlighted: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "key_scale"
    )

    // Pulsing animation for highlighted state (Ctrl+C indicator)
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_pulse")
    val highlightGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlight_glow"
    )

    Box(
        modifier = modifier
            .height(if (isLarge) 52.dp else 46.dp)
            .scale(scale)
            .then(
                if (isHighlighted) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MentraColors.neonRed.copy(alpha = 0.2f * highlightGlow))
                        .border(
                            width = 1.dp,
                            color = MentraColors.neonRed.copy(alpha = 0.6f * highlightGlow),
                            shape = RoundedCornerShape(8.dp)
                        )
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
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPressed) MentraColors.neonCyan else MentraColors.textPrimary,
            fontSize = if (isLarge) 24.sp else 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Default
        )
    }
}

@Composable
fun FuncKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "func_scale"
    )

    Box(
        modifier = modifier
            .height(30.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                isPressed -> if (isDestructive) MentraColors.neonRed else MentraColors.neonCyan
                isDestructive -> MentraColors.neonRed
                else -> MentraColors.textSecondary
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}


@Composable
fun ShiftKey(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "shift_scale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.KeyboardCapslock else Icons.Default.KeyboardArrowUp,
            contentDescription = "Shift",
            tint = when {
                isActive -> MentraColors.neonCyan
                isPressed -> MentraColors.neonPurple
                else -> MentraColors.textSecondary
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun BackspaceKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "backspace_scale"
    )

    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            delay(400)
            while (isLongPressing) {
                onClick()
                delay(50)
            }
        }
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        isLongPressing = true
                        tryAwaitRelease()
                        isPressed = false
                        isLongPressing = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = if (isPressed) MentraColors.neonRed.copy(alpha = 1f) else MentraColors.neonRed,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun ModeKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "mode_scale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                isPressed -> MentraColors.neonCyan
                isHighlighted -> MentraColors.neonBlue
                else -> MentraColors.textSecondary
            },
            fontSize = if (text.contains("\n")) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun SpaceBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "space_scale"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isPressed) listOf(
                        MentraColors.neonPurple.copy(alpha = 0.1f),
                        MentraColors.neonCyan.copy(alpha = 0.08f),
                        MentraColors.neonPurple.copy(alpha = 0.1f)
                    ) else listOf(
                        Color.Transparent,
                        MentraColors.glassHighlight.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(12.dp)
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
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle line indicator
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isPressed) listOf(
                            MentraColors.neonCyan.copy(alpha = 0.3f),
                            MentraColors.neonCyan.copy(alpha = 0.5f),
                            MentraColors.neonCyan.copy(alpha = 0.3f)
                        ) else listOf(
                            Color.Transparent,
                            MentraColors.textMuted.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun EnterKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "enter_scale"
    )

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "enter_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "enter_glow"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .drawBehind {
                // Subtle glow behind enter key
                drawCircle(
                    color = MentraColors.neonGreen.copy(alpha = 0.15f * glowAlpha),
                    radius = size.minDimension * 0.8f
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
            contentDescription = "Enter",
            tint = if (isPressed) MentraColors.neonGreen else MentraColors.neonGreen.copy(alpha = glowAlpha),
            modifier = Modifier.size(24.dp)
        )
    }
}

// Clipboard paste key with full functionality
@Composable
fun ClipboardKey(
    modifier: Modifier = Modifier,
    onPaste: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isPressed by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "clipboard_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0.7f,
        animationSpec = tween(150),
        label = "clipboard_glow"
    )

    // Show toast when clipboard is empty
    LaunchedEffect(showToast) {
        if (showToast) {
            delay(100)
            showToast = false
        }
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        // Get clipboard content and paste
                        val clipboardText = clipboardManager.getText()?.text
                        if (!clipboardText.isNullOrEmpty()) {
                            onPaste(clipboardText)
                        } else {
                            showToast = true
                            Toast.makeText(
                                context,
                                "Clipboard empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = "Paste from clipboard",
            tint = if (isPressed) Color(0xFF00D9FF) else Color(0xFF00D9FF).copy(alpha = glowAlpha),
            modifier = Modifier.size(22.dp)
        )
    }
}

// Flat numpad key without background or outline
@Composable
fun FlatNumpadKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isNumber: Boolean = false,
    isOperator: Boolean = false,
    isMode: Boolean = false,
    isHighlighted: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                isHighlighted -> Color(0xFF00D9FF) // neonCyan
                isOperator -> Color(0xFF00D9FF) // neonCyan for operators
                isNumber -> Color.White
                isMode -> Color(0xFF7B8794) // dimmed for mode keys
                else -> Color(0xFFB0B8C4) // textSecondary
            },
            fontSize = when {
                isNumber -> 28.sp
                isOperator -> 24.sp
                isMode -> 14.sp
                else -> 18.sp
            },
            fontWeight = when {
                isNumber -> FontWeight.Medium
                isOperator -> FontWeight.Bold
                else -> FontWeight.Normal
            },
            fontFamily = FontFamily.SansSerif
        )
    }
}

// Numpad-specific number key with larger, bolder styling
@Composable
fun NumpadNumberKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1F2E).copy(alpha = 0.6f),
                        Color(0xFF0D1117).copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3A4155).copy(alpha = 0.5f),
                        Color(0xFF1A1F2E).copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// Numpad operator key (+ - × ÷)
@Composable
fun NumpadOperatorKey(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2433).copy(alpha = 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF00D9FF), // neonCyan
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Numpad action key (%, PHY, etc.)
@Composable
fun NumpadActionKey(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2433).copy(alpha = 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFB0B8C4), // textSecondary
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

