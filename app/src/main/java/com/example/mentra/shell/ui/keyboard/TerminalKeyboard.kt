package com.example.mentra.shell.ui.keyboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * MENTRA NEXUS KEYBOARD
 * Ultra-futuristic glassmorphic keyboard with alien aesthetics
 * Stunning neon accents, smooth animations, premium UI/UX
 */

@Composable
fun TerminalKeyboard(
    visible: Boolean,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    onArrowLeft: () -> Unit,
    onArrowRight: () -> Unit,
    onHome: () -> Unit,
    onEnd: () -> Unit,
    onTab: () -> Unit,
    onCtrlC: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isShiftActive by remember { mutableStateOf(false) }
    var keyboardMode by remember { mutableStateOf(KeyboardMode.LETTERS) }

    // CTRL key state for Ctrl+C functionality
    var isCtrlActive by remember { mutableStateOf(false) }
    var ctrlActivationTime by remember { mutableStateOf(0L) }

    // Auto-deactivate CTRL after 5 seconds
    LaunchedEffect(isCtrlActive) {
        if (isCtrlActive) {
            ctrlActivationTime = System.currentTimeMillis()
            delay(5000L)
            // Only deactivate if still the same activation
            if (System.currentTimeMillis() - ctrlActivationTime >= 5000L) {
                isCtrlActive = false
            }
        }
    }

    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "keyboard_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(150)
        ) + fadeOut(animationSpec = tween(100)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .drawBehind {
                    // Top neon glow line
                    drawLine(
                        brush = Brush.horizontalGradient(MentraColors.rainbowGradient),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx(),
                        alpha = glowAlpha
                    )
                    // Subtle glow beneath the line
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MentraColors.neonCyan.copy(alpha = 0.1f * glowAlpha),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 40.dp.toPx()
                        )
                    )
                }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MentraColors.deepSpace,
                            MentraColors.voidBlack
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MentraColors.glassBorder,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                // Function row (always visible)
                FunctionRow(
                    onTab = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onTab() },
                    onEsc = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // ESC cancels CTRL mode if active, otherwise triggers onCtrlC
                        if (isCtrlActive) {
                            isCtrlActive = false
                        } else {
                            onCtrlC()
                        }
                    },
                    onCharInsert = { char ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKeyPress(char)
                    },
                    isCtrlActive = isCtrlActive,
                    onCtrlToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isCtrlActive = !isCtrlActive
                        if (isCtrlActive) {
                            ctrlActivationTime = System.currentTimeMillis()
                        }
                    },
                    onClear = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClear() },
                    onHome = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onHome() },
                    onEnd = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onEnd() },
                    onArrowUp = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onArrowUp() },
                    onArrowDown = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onArrowDown() },
                    onArrowLeft = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onArrowLeft() },
                    onArrowRight = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onArrowRight() }
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (keyboardMode) {
                    KeyboardMode.LETTERS -> {
                        LettersKeyboard(
                            isShiftActive = isShiftActive,
                            isCtrlActive = isCtrlActive,
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // Check for Ctrl+C
                                if (isCtrlActive && key.equals("c", ignoreCase = true)) {
                                    isCtrlActive = false
                                    onCtrlC()
                                } else {
                                    onKeyPress(if (isShiftActive) key.uppercase() else key.lowercase())
                                    if (isShiftActive) isShiftActive = false
                                }
                            },
                            onShiftToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isShiftActive = !isShiftActive
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToNumbers = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMBERS
                            }
                        )
                    }
                    KeyboardMode.NUMBERS -> {
                        NumbersKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            },
                            onSwitchToSymbols = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.SYMBOLS
                            },
                            onSwitchToNumpad = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMPAD
                            }
                        )
                    }
                    KeyboardMode.SYMBOLS -> {
                        SymbolsKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            },
                            onSwitchToNumbers = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMBERS
                            }
                        )
                    }
                    KeyboardMode.NUMPAD -> {
                        NumpadKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            },
                            onSwitchToNumpadSymbols = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMPAD_SYMBOLS
                            },
                            onSwitchToMath = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.MATH
                            },
                            onSwitchToPhysics = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.PHYSICS
                            }
                        )
                    }
                    KeyboardMode.NUMPAD_SYMBOLS -> {
                        NumpadSymbolsKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToNumpad = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMPAD
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            }
                        )
                    }
                    KeyboardMode.MATH -> {
                        MathKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToNumpad = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMPAD
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            }
                        )
                    }
                    KeyboardMode.PHYSICS -> {
                        PhysicsKeyboard(
                            onKeyPress = { key ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeyPress(key)
                            },
                            onBackspace = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onBackspace()
                            },
                            onEnter = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEnter()
                            },
                            onSwitchToNumpad = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.NUMPAD
                            },
                            onSwitchToLetters = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                keyboardMode = KeyboardMode.LETTERS
                            }
                        )
                    }
                }
            }
        }
    }
}

