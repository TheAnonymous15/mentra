package com.example.mentra.shell.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS CALCULATOR MODAL - ALIEN GLASSMORPHIC DESIGN
 * Super futuristic, stunning UI/UX
 * ═══════════════════════════════════════════════════════════════════
 */

private object NexusCalcColors {
    val voidBlack = Color(0xFF010104)
    val deepSpace = Color(0xFF050508)
    val glassCore = Color(0xFF0A0C14).copy(alpha = 0.94f)
    val glassSurface = Color(0xFF12151F).copy(alpha = 0.88f)
    val hologramBlue = Color(0xFF00D4FF)
    val neonCyan = Color(0xFF00F5D4)
    val electricPurple = Color(0xFF7B61FF)
    val plasmaRed = Color(0xFFFF2E63)
    val solarYellow = Color(0xFFFFD93D)
    val matrixGreen = Color(0xFF00E676)

    val numberBtn = Color(0xFF0D1020)
    val operatorBtn = Color(0xFF1A0D2E)
    val functionBtn = Color(0xFF0D1A1A)
    val equalBtn = Color(0xFF00F5D4)

    val textPure = Color(0xFFFFFFFF)
    val textDim = Color(0xFF8892A4)
    val textMuted = Color(0xFF4A5568)

    val glowCyan = Color(0xFF00F5D4).copy(alpha = 0.6f)
    val glowPurple = Color(0xFF7B61FF).copy(alpha = 0.5f)
    val glowPink = Color(0xFFFF2E63).copy(alpha = 0.4f)
}

@Composable
fun NexusCalculatorModal(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var isScientific by remember { mutableStateOf(false) }
    var isRadians by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "calc_fx")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbitalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // Full screen backdrop with blur
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        // Animated orbital rings background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = orbitalRotation }
                .drawBehind {
                    // Outer ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NexusCalcColors.neonCyan.copy(alpha = 0.08f * pulseAlpha),
                                Color.Transparent,
                                NexusCalcColors.electricPurple.copy(alpha = 0.08f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.6f * glowScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                    // Inner ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                NexusCalcColors.plasmaRed.copy(alpha = 0.05f * pulseAlpha),
                                Color.Transparent,
                                NexusCalcColors.solarYellow.copy(alpha = 0.05f * pulseAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.45f * glowScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
        )

        // Main Modal Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NexusCalcColors.glassCore,
                            NexusCalcColors.deepSpace.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NexusCalcColors.neonCyan.copy(alpha = 0.6f * pulseAlpha),
                            NexusCalcColors.electricPurple.copy(alpha = 0.4f),
                            NexusCalcColors.plasmaRed.copy(alpha = 0.3f * pulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .drawBehind {
                    // Inner glow effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexusCalcColors.neonCyan.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.3f, size.height * 0.2f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                NexusCalcHeader(
                    isScientific = isScientific,
                    onToggleMode = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isScientific = !isScientific
                    },
                    onClose = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClose()
                    },
                    pulseAlpha = pulseAlpha
                )

                // Display
                NexusCalcDisplay(
                    expression = expression,
                    result = result,
                    isScientific = isScientific,
                    isRadians = isRadians,
                    onToggleRadians = { isRadians = !isRadians },
                    pulseAlpha = pulseAlpha
                )

                // Keypad
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = isScientific,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith
                            (fadeOut(tween(200)) + scaleOut(targetScale = 1.05f))
                        },
                        label = "keypad_switch"
                    ) { scientific ->
                        if (scientific) {
                            NexusScientificKeypad(
                                isRadians = isRadians,
                                pulseAlpha = pulseAlpha,
                                onKey = { key ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    processKey(key, expression, isRadians) { newExpr, newResult ->
                                        expression = newExpr
                                        result = newResult
                                    }
                                }
                            )
                        } else {
                            NexusSimpleKeypad(
                                pulseAlpha = pulseAlpha,
                                onKey = { key ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    processKey(key, expression, isRadians) { newExpr, newResult ->
                                        expression = newExpr
                                        result = newResult
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusCalcHeader(
    isScientific: Boolean,
    onToggleMode: () -> Unit,
    onClose: () -> Unit,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(NexusCalcColors.plasmaRed.copy(alpha = 0.15f))
                .border(1.dp, NexusCalcColors.plasmaRed.copy(alpha = 0.5f * pulseAlpha), CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = NexusCalcColors.plasmaRed,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NexusCalcColors.neonCyan)
            )
            Text(
                text = "◈ NEXUS",
                color = NexusCalcColors.neonCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )
            Text(
                text = if (isScientific) "SCI" else "CALC",
                color = NexusCalcColors.electricPurple,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Mode toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isScientific) NexusCalcColors.electricPurple.copy(alpha = 0.2f)
                    else NexusCalcColors.numberBtn
                )
                .border(
                    1.dp,
                    if (isScientific) NexusCalcColors.electricPurple.copy(alpha = 0.6f)
                    else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onToggleMode() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Functions,
                    contentDescription = null,
                    tint = if (isScientific) NexusCalcColors.electricPurple else NexusCalcColors.textDim,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isScientific) "SCIENTIFIC" else "SIMPLE",
                    color = if (isScientific) NexusCalcColors.electricPurple else NexusCalcColors.textDim,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NexusCalcDisplay(
    expression: String,
    result: String,
    isScientific: Boolean,
    isRadians: Boolean,
    onToggleRadians: () -> Unit,
    pulseAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NexusCalcColors.glassSurface,
                        NexusCalcColors.deepSpace.copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NexusCalcColors.neonCyan.copy(alpha = 0.4f * pulseAlpha),
                        NexusCalcColors.electricPurple.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Mode indicators
            if (isScientific) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Angle mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NexusCalcColors.solarYellow.copy(alpha = 0.15f))
                            .border(1.dp, NexusCalcColors.solarYellow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { onToggleRadians() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRadians) "RAD" else "DEG",
                            color = NexusCalcColors.solarYellow,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "◈ SCIENTIFIC MODE",
                        color = NexusCalcColors.electricPurple.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Expression
            Text(
                text = expression.ifEmpty { "0" },
                color = NexusCalcColors.textDim,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Result with glow effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NexusCalcColors.neonCyan.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.width * 0.4f,
                            center = Offset(size.width * 0.8f, size.height * 0.5f)
                        )
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = result,
                    color = NexusCalcColors.textPure,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NexusSimpleKeypad(
    pulseAlpha: Float,
    onKey: (String) -> Unit
) {
    val keys = listOf(
        listOf("C" to NexusCalcColors.plasmaRed, "⌫" to NexusCalcColors.solarYellow, "%" to NexusCalcColors.electricPurple, "÷" to NexusCalcColors.electricPurple),
        listOf("7" to NexusCalcColors.textPure, "8" to NexusCalcColors.textPure, "9" to NexusCalcColors.textPure, "×" to NexusCalcColors.electricPurple),
        listOf("4" to NexusCalcColors.textPure, "5" to NexusCalcColors.textPure, "6" to NexusCalcColors.textPure, "-" to NexusCalcColors.electricPurple),
        listOf("1" to NexusCalcColors.textPure, "2" to NexusCalcColors.textPure, "3" to NexusCalcColors.textPure, "+" to NexusCalcColors.electricPurple),
        listOf("±" to NexusCalcColors.textDim, "0" to NexusCalcColors.textPure, "." to NexusCalcColors.textPure, "=" to NexusCalcColors.neonCyan)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (key, color) ->
                    NexusCalcButton(
                        key = key,
                        color = color,
                        isEqual = key == "=",
                        pulseAlpha = pulseAlpha,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NexusScientificKeypad(
    isRadians: Boolean,
    pulseAlpha: Float,
    onKey: (String) -> Unit
) {
    val keys = listOf(
        listOf("sin" to NexusCalcColors.matrixGreen, "cos" to NexusCalcColors.matrixGreen, "tan" to NexusCalcColors.matrixGreen, "π" to NexusCalcColors.solarYellow, "e" to NexusCalcColors.solarYellow),
        listOf("ln" to NexusCalcColors.hologramBlue, "log" to NexusCalcColors.hologramBlue, "√" to NexusCalcColors.hologramBlue, "^" to NexusCalcColors.electricPurple, "(" to NexusCalcColors.textDim),
        listOf("C" to NexusCalcColors.plasmaRed, "⌫" to NexusCalcColors.solarYellow, "%" to NexusCalcColors.electricPurple, "÷" to NexusCalcColors.electricPurple, ")" to NexusCalcColors.textDim),
        listOf("7" to NexusCalcColors.textPure, "8" to NexusCalcColors.textPure, "9" to NexusCalcColors.textPure, "×" to NexusCalcColors.electricPurple, "!" to NexusCalcColors.hologramBlue),
        listOf("4" to NexusCalcColors.textPure, "5" to NexusCalcColors.textPure, "6" to NexusCalcColors.textPure, "-" to NexusCalcColors.electricPurple, "x²" to NexusCalcColors.hologramBlue),
        listOf("1" to NexusCalcColors.textPure, "2" to NexusCalcColors.textPure, "3" to NexusCalcColors.textPure, "+" to NexusCalcColors.electricPurple, "1/x" to NexusCalcColors.hologramBlue),
        listOf("±" to NexusCalcColors.textDim, "0" to NexusCalcColors.textPure, "." to NexusCalcColors.textPure, "=" to NexusCalcColors.neonCyan, "EXP" to NexusCalcColors.hologramBlue)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (key, color) ->
                    NexusCalcButton(
                        key = key,
                        color = color,
                        isEqual = key == "=",
                        pulseAlpha = pulseAlpha,
                        onClick = { onKey(key) },
                        modifier = Modifier.weight(1f),
                        fontSize = if (key.length > 2) 12 else 16
                    )
                }
            }
        }
    }
}

@Composable
private fun NexusCalcButton(
    key: String,
    color: Color,
    isEqual: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 20
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val bgColor = when {
        isEqual -> NexusCalcColors.neonCyan.copy(alpha = 0.9f)
        key in listOf("C", "⌫") -> NexusCalcColors.numberBtn.copy(alpha = 0.8f)
        key in listOf("+", "-", "×", "÷", "%", "^") -> NexusCalcColors.operatorBtn
        key in listOf("sin", "cos", "tan", "ln", "log", "√", "!", "x²", "1/x", "EXP") -> NexusCalcColors.functionBtn
        else -> NexusCalcColors.numberBtn
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (isEqual) Modifier.border(
                    2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            NexusCalcColors.neonCyan,
                            NexusCalcColors.matrixGreen.copy(alpha = 0.7f)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                )
                else if (key in listOf("+", "-", "×", "÷")) Modifier.border(
                    1.dp,
                    NexusCalcColors.electricPurple.copy(alpha = 0.4f * pulseAlpha),
                    RoundedCornerShape(16.dp)
                )
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            color = if (isEqual) NexusCalcColors.voidBlack else color,
            fontSize = fontSize.sp,
            fontWeight = if (isEqual) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Calculator processing
private fun processKey(
    key: String,
    currentExpr: String,
    isRadians: Boolean,
    onResult: (String, String) -> Unit
) {
    when (key) {
        "C" -> onResult("", "0")
        "⌫" -> {
            val newExpr = if (currentExpr.isNotEmpty()) currentExpr.dropLast(1) else ""
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "=" -> {
            val result = evaluateExpression(currentExpr, isRadians)
            onResult(currentExpr, result)
        }
        "±" -> {
            if (currentExpr.isNotEmpty()) {
                val newExpr = if (currentExpr.startsWith("-")) currentExpr.drop(1) else "-$currentExpr"
                onResult(newExpr, evaluateExpression(newExpr, isRadians))
            }
        }
        "π" -> {
            val newExpr = currentExpr + "π"
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "e" -> {
            val newExpr = currentExpr + "e"
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "x²" -> {
            val newExpr = "($currentExpr)²"
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "1/x" -> {
            val newExpr = "1/($currentExpr)"
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "√" -> {
            val newExpr = currentExpr + "√("
            onResult(newExpr, "")
        }
        "sin", "cos", "tan", "ln", "log" -> {
            val newExpr = currentExpr + "$key("
            onResult(newExpr, "")
        }
        "!" -> {
            val newExpr = "($currentExpr)!"
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
        "EXP" -> {
            val newExpr = currentExpr + "E"
            onResult(newExpr, "")
        }
        else -> {
            val displayKey = when (key) {
                "×" -> "*"
                "÷" -> "/"
                else -> key
            }
            val newExpr = currentExpr + displayKey
            onResult(newExpr, evaluateExpression(newExpr, isRadians))
        }
    }
}

private fun evaluateExpression(expr: String, isRadians: Boolean): String {
    if (expr.isEmpty()) return "0"

    return try {
        var processedExpr = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
            .replace("²", "^2")

        // Handle functions
        val functionPattern = "(sin|cos|tan|ln|log|√)\\(([^)]+)\\)".toRegex()
        while (functionPattern.containsMatchIn(processedExpr)) {
            processedExpr = functionPattern.replace(processedExpr) { match ->
                val func = match.groupValues[1]
                val arg = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val radArg = if (!isRadians && func in listOf("sin", "cos", "tan")) Math.toRadians(arg) else arg

                val result = when (func) {
                    "sin" -> sin(radArg)
                    "cos" -> cos(radArg)
                    "tan" -> tan(radArg)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    "√" -> sqrt(arg)
                    else -> arg
                }
                result.toString()
            }
        }

        // Handle factorial
        val factPattern = "\\(([^)]+)\\)!".toRegex()
        processedExpr = factPattern.replace(processedExpr) { match ->
            val n = match.groupValues[1].toDoubleOrNull()?.toInt() ?: 0
            factorial(n).toString()
        }

        // Handle power
        val powerPattern = "([0-9.]+)\\^([0-9.]+)".toRegex()
        while (powerPattern.containsMatchIn(processedExpr)) {
            processedExpr = powerPattern.replace(processedExpr) { match ->
                val base = match.groupValues[1].toDoubleOrNull() ?: 0.0
                val exp = match.groupValues[2].toDoubleOrNull() ?: 0.0
                base.pow(exp).toString()
            }
        }

        // Simple evaluation for basic operations
        val result = evaluateBasic(processedExpr)
        if (result.isNaN() || result.isInfinite()) "Error"
        else if (result == result.toLong().toDouble()) result.toLong().toString()
        else String.format("%.8f", result).trimEnd('0').trimEnd('.')
    } catch (e: Exception) {
        ""
    }
}

private fun evaluateBasic(expr: String): Double {
    return try {
        // Very basic evaluation - in production use a proper expression parser
        val cleanExpr = expr.replace(" ", "")
        if (cleanExpr.isEmpty()) return 0.0

        // Handle simple cases
        cleanExpr.toDoubleOrNull() ?: run {
            // Try to evaluate with basic operations
            var result = 0.0
            var currentNum = ""
            var currentOp = '+'

            for (char in cleanExpr + '+') {
                if (char.isDigit() || char == '.' || (char == '-' && currentNum.isEmpty())) {
                    currentNum += char
                } else if (char in listOf('+', '-', '*', '/')) {
                    if (currentNum.isNotEmpty()) {
                        val num = currentNum.toDoubleOrNull() ?: 0.0
                        result = when (currentOp) {
                            '+' -> result + num
                            '-' -> result - num
                            '*' -> result * num
                            '/' -> if (num != 0.0) result / num else Double.NaN
                            else -> result
                        }
                        currentNum = ""
                    }
                    currentOp = char
                }
            }
            result
        }
    } catch (e: Exception) {
        Double.NaN
    }
}

private fun factorial(n: Int): Long {
    if (n < 0) return 0
    if (n <= 1) return 1
    var result = 1L
    for (i in 2..n) result *= i
    return result
}

