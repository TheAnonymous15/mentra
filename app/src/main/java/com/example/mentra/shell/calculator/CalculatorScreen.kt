package com.example.mentra.shell.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA NEXUS CALCULATOR
 * Super futuristic calculator with simple/scientific toggle
 * ═══════════════════════════════════════════════════════════════════
 */

// Color palette - Futuristic neon theme
private object CalcColors {
    val background = Color(0xFF0A0A0F)
    val surface = Color(0xFF12141C)
    val glassSurface = Color(0xFF1A1D28)

    val primary = Color(0xFF00F5D4)      // Cyan
    val secondary = Color(0xFF7B61FF)    // Purple
    val accent = Color(0xFFFF2E63)       // Pink
    val warning = Color(0xFFFFD93D)      // Yellow

    val numberButton = Color(0xFF1E2235)
    val operatorButton = Color(0xFF2A1F4E)
    val functionButton = Color(0xFF1A2F3D)
    val equalButton = Color(0xFF00F5D4)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B8C4)
    val textMuted = Color(0xFF6B7280)

    val borderGlow = Color(0xFF00F5D4).copy(alpha = 0.3f)
    val purpleGlow = Color(0xFF7B61FF).copy(alpha = 0.3f)
}

@Composable
fun CalculatorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScientific by remember { mutableStateOf(false) }
    var isRadians by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val calculator = remember { CalculatorEngine() }

    // Animated background pulse
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcColors.background)
    ) {
        // Animated background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CalcColors.primary.copy(alpha = pulseAlpha),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with close button and mode toggle
            CalculatorHeader(
                isScientific = isScientific,
                onToggleMode = { isScientific = !isScientific },
                onShowHistory = { showHistory = !showHistory },
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display area
            CalculatorDisplay(
                expression = expression,
                result = result,
                isScientific = isScientific,
                isRadians = isRadians,
                onToggleRadians = { isRadians = !isRadians }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Keypad
            AnimatedContent(
                targetState = isScientific,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "keypad"
            ) { scientific ->
                if (scientific) {
                    ScientificKeypad(
                        isRadians = isRadians,
                        onKey = { key ->
                            handleKey(key, expression, calculator, isRadians) { newExpr, newResult ->
                                if (key == "=" && newResult != "Error") {
                                    history = (listOf("$expression = $newResult") + history).take(20)
                                }
                                expression = newExpr
                                result = newResult
                            }
                        }
                    )
                } else {
                    SimpleKeypad(
                        onKey = { key ->
                            handleKey(key, expression, calculator, isRadians) { newExpr, newResult ->
                                if (key == "=" && newResult != "Error") {
                                    history = (listOf("$expression = $newResult") + history).take(20)
                                }
                                expression = newExpr
                                result = newResult
                            }
                        }
                    )
                }
            }
        }

        // History overlay
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            HistoryPanel(
                history = history,
                onSelect = { item ->
                    val parts = item.split(" = ")
                    if (parts.size == 2) {
                        expression = parts[0]
                        result = parts[1]
                    }
                    showHistory = false
                },
                onClear = {
                    history = emptyList()
                    showHistory = false
                },
                onClose = { showHistory = false }
            )
        }
    }
}

@Composable
private fun CalculatorHeader(
    isScientific: Boolean,
    onToggleMode: () -> Unit,
    onShowHistory: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CalcColors.surface)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = CalcColors.textSecondary
            )
        }

        // Title with mode indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEXUS",
                color = CalcColors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isScientific) " SCI" else " CALC",
                color = CalcColors.textSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // History button
            IconButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CalcColors.surface)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = CalcColors.textSecondary
                )
            }

            // Mode toggle
            IconButton(
                onClick = onToggleMode,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isScientific) CalcColors.secondary.copy(alpha = 0.3f)
                        else CalcColors.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isScientific) CalcColors.secondary else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = "Toggle Scientific",
                    tint = if (isScientific) CalcColors.secondary else CalcColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun CalculatorDisplay(
    expression: String,
    result: String,
    isScientific: Boolean,
    isRadians: Boolean,
    onToggleRadians: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CalcColors.glassSurface,
                        CalcColors.surface
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CalcColors.borderGlow,
                        CalcColors.purpleGlow
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Mode indicator for scientific
            if (isScientific) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Angle mode toggle
                    Surface(
                        onClick = onToggleRadians,
                        shape = RoundedCornerShape(8.dp),
                        color = CalcColors.surface.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (isRadians) "RAD" else "DEG",
                            color = CalcColors.warning,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "SCIENTIFIC",
                        color = CalcColors.secondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Expression
            Text(
                text = expression.ifEmpty { "0" },
                color = CalcColors.textSecondary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Result
            Text(
                text = result,
                color = CalcColors.textPrimary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SimpleKeypad(
    onKey: (String) -> Unit
) {
    val keys = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("±", "0", ".", "=")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    CalculatorButton(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(key) },
                        type = when (key) {
                            "C", "⌫" -> ButtonType.FUNCTION
                            "÷", "×", "-", "+", "%" -> ButtonType.OPERATOR
                            "=" -> ButtonType.EQUALS
                            else -> ButtonType.NUMBER
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScientificKeypad(
    isRadians: Boolean,
    onKey: (String) -> Unit
) {
    val keys = listOf(
        listOf("sin", "cos", "tan", "C", "⌫"),
        listOf("ln", "log", "√", "(", ")"),
        listOf("π", "e", "^", "%", "÷"),
        listOf("7", "8", "9", "×", "!"),
        listOf("4", "5", "6", "-", "1/x"),
        listOf("1", "2", "3", "+", "x²"),
        listOf("±", "0", ".", "=", "xʸ")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    CalculatorButton(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(key) },
                        type = when (key) {
                            "C", "⌫" -> ButtonType.FUNCTION
                            "sin", "cos", "tan", "ln", "log", "√", "!", "1/x", "x²", "xʸ" -> ButtonType.SCIENTIFIC
                            "÷", "×", "-", "+", "%", "^", "(", ")" -> ButtonType.OPERATOR
                            "=" -> ButtonType.EQUALS
                            "π", "e" -> ButtonType.CONSTANT
                            else -> ButtonType.NUMBER
                        },
                        isSmall = true
                    )
                }
            }
        }
    }
}

enum class ButtonType {
    NUMBER, OPERATOR, FUNCTION, SCIENTIFIC, EQUALS, CONSTANT
}

@Composable
private fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    type: ButtonType,
    isSmall: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val backgroundColor = when (type) {
        ButtonType.NUMBER -> CalcColors.numberButton
        ButtonType.OPERATOR -> CalcColors.operatorButton
        ButtonType.FUNCTION -> CalcColors.accent.copy(alpha = 0.2f)
        ButtonType.SCIENTIFIC -> CalcColors.functionButton
        ButtonType.EQUALS -> CalcColors.equalButton
        ButtonType.CONSTANT -> CalcColors.secondary.copy(alpha = 0.2f)
    }

    val textColor = when (type) {
        ButtonType.NUMBER -> CalcColors.textPrimary
        ButtonType.OPERATOR -> CalcColors.secondary
        ButtonType.FUNCTION -> CalcColors.accent
        ButtonType.SCIENTIFIC -> CalcColors.primary
        ButtonType.EQUALS -> CalcColors.background
        ButtonType.CONSTANT -> CalcColors.secondary
    }

    val borderColor = when (type) {
        ButtonType.EQUALS -> CalcColors.primary.copy(alpha = 0.5f)
        ButtonType.OPERATOR -> CalcColors.secondary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(if (isSmall) 52.dp else 64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPressed) backgroundColor.copy(alpha = 0.8f) else backgroundColor
            )
            .border(
                width = 1.dp,
                color = if (isPressed) textColor.copy(alpha = 0.3f) else borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for equals button
        if (type == ButtonType.EQUALS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .background(CalcColors.primary.copy(alpha = 0.3f))
            )
        }

        Text(
            text = text,
            color = textColor,
            fontSize = if (isSmall) 16.sp else 22.sp,
            fontWeight = if (type == ButtonType.EQUALS) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun HistoryPanel(
    history: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalcColors.background.copy(alpha = 0.95f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORY",
                    color = CalcColors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = CalcColors.accent)
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CalcColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history yet",
                        color = CalcColors.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                history.forEach { item ->
                    Surface(
                        onClick = { onSelect(item) },
                        shape = RoundedCornerShape(12.dp),
                        color = CalcColors.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item,
                            color = CalcColors.textSecondary,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Calculator engine
private class CalculatorEngine {
    fun evaluate(expression: String, useRadians: Boolean = false): String {
        if (expression.isBlank()) return "0"

        return try {
            val processed = preprocessExpression(expression, useRadians)
            val result = evalExpression(processed)
            formatResult(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun preprocessExpression(expr: String, useRadians: Boolean): String {
        var processed = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "${Math.PI}")
            .replace("e", "${Math.E}")
            .replace("√", "sqrt")
            .replace("x²", "^2")
            .replace("xʸ", "^")

        // Handle functions
        val functions = listOf("sin", "cos", "tan", "ln", "log", "sqrt")
        functions.forEach { func ->
            val regex = Regex("$func\\(([^)]+)\\)")
            processed = regex.replace(processed) { match ->
                val arg = evalExpression(match.groupValues[1])
                val argRad = if (!useRadians && func in listOf("sin", "cos", "tan")) {
                    Math.toRadians(arg)
                } else arg

                val result = when (func) {
                    "sin" -> sin(argRad)
                    "cos" -> cos(argRad)
                    "tan" -> tan(argRad)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    "sqrt" -> sqrt(arg)
                    else -> arg
                }
                result.toString()
            }
        }

        // Handle factorial
        processed = processed.replace(Regex("(\\d+)!")) { match ->
            factorial(match.groupValues[1].toInt()).toString()
        }

        // Handle 1/x
        processed = processed.replace("1/x", "^-1")

        return processed
    }

    private fun evalExpression(expr: String): Double {
        val tokens = tokenize(expr)
        return parseExpression(tokens.iterator())
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.' || expr[i] == 'E' ||
                           (expr[i] == '-' && i > 0 && expr[i-1] == 'E'))) {
                        sb.append(expr[i++])
                    }
                    tokens.add(sb.toString())
                }
                c in "+-*/^%()" -> {
                    tokens.add(c.toString())
                    i++
                }
                c == ' ' -> i++
                else -> i++
            }
        }
        return tokens
    }

    private fun parseExpression(tokens: Iterator<String>): Double {
        val tokenList = mutableListOf<String>()
        tokens.forEach { tokenList.add(it) }
        return parseAddSub(tokenList)
    }

    private fun parseAddSub(tokens: MutableList<String>): Double {
        var result = parseMulDiv(tokens)
        while (tokens.isNotEmpty() && tokens.first() in listOf("+", "-")) {
            val op = tokens.removeFirst()
            val right = parseMulDiv(tokens)
            result = if (op == "+") result + right else result - right
        }
        return result
    }

    private fun parseMulDiv(tokens: MutableList<String>): Double {
        var result = parsePower(tokens)
        while (tokens.isNotEmpty() && tokens.first() in listOf("*", "/", "%")) {
            val op = tokens.removeFirst()
            val right = parsePower(tokens)
            result = when (op) {
                "*" -> result * right
                "/" -> result / right
                "%" -> result % right
                else -> result
            }
        }
        return result
    }

    private fun parsePower(tokens: MutableList<String>): Double {
        var result = parseUnary(tokens)
        if (tokens.isNotEmpty() && tokens.first() == "^") {
            tokens.removeFirst()
            result = result.pow(parsePower(tokens))
        }
        return result
    }

    private fun parseUnary(tokens: MutableList<String>): Double {
        if (tokens.isNotEmpty() && tokens.first() == "-") {
            tokens.removeFirst()
            return -parsePrimary(tokens)
        }
        if (tokens.isNotEmpty() && tokens.first() == "+") {
            tokens.removeFirst()
        }
        return parsePrimary(tokens)
    }

    private fun parsePrimary(tokens: MutableList<String>): Double {
        if (tokens.isEmpty()) return 0.0

        val token = tokens.first()
        return when {
            token == "(" -> {
                tokens.removeFirst()
                val result = parseAddSub(tokens)
                if (tokens.isNotEmpty() && tokens.first() == ")") {
                    tokens.removeFirst()
                }
                result
            }
            token.toDoubleOrNull() != null -> {
                tokens.removeFirst()
                token.toDouble()
            }
            else -> {
                tokens.removeFirst()
                0.0
            }
        }
    }

    private fun factorial(n: Int): Long {
        if (n < 0) throw IllegalArgumentException()
        if (n <= 1) return 1
        var result = 1L
        for (i in 2..n) result *= i
        return result
    }

    private fun formatResult(value: Double): String {
        return when {
            value.isNaN() -> "Error"
            value.isInfinite() -> "∞"
            value == value.toLong().toDouble() && abs(value) < 1e15 -> {
                value.toLong().toString()
            }
            abs(value) < 0.0001 || abs(value) >= 1e10 -> {
                String.format("%.6e", value)
            }
            else -> {
                String.format("%.10f", value).trimEnd('0').trimEnd('.')
            }
        }
    }
}

private fun handleKey(
    key: String,
    currentExpression: String,
    calculator: CalculatorEngine,
    isRadians: Boolean,
    onUpdate: (String, String) -> Unit
) {
    when (key) {
        "C" -> onUpdate("", "0")
        "⌫" -> {
            val newExpr = currentExpression.dropLast(1)
            val newResult = if (newExpr.isNotEmpty()) {
                calculator.evaluate(newExpr, isRadians)
            } else "0"
            onUpdate(newExpr, newResult)
        }
        "=" -> {
            val result = calculator.evaluate(currentExpression, isRadians)
            onUpdate(currentExpression, result)
        }
        "±" -> {
            val newExpr = if (currentExpression.startsWith("-")) {
                currentExpression.drop(1)
            } else {
                "-$currentExpression"
            }
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        "sin", "cos", "tan", "ln", "log", "√" -> {
            val newExpr = "$currentExpression$key("
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        "x²" -> {
            val newExpr = "$currentExpression^2"
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        "xʸ" -> {
            val newExpr = "$currentExpression^"
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        "1/x" -> {
            val newExpr = "1/($currentExpression)"
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        "!" -> {
            val newExpr = "$currentExpression!"
            onUpdate(newExpr, calculator.evaluate(newExpr, isRadians))
        }
        else -> {
            val newExpr = currentExpression + key
            val newResult = calculator.evaluate(newExpr, isRadians)
            onUpdate(newExpr, newResult)
        }
    }
}

