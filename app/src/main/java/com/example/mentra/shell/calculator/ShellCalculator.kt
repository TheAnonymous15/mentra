package com.example.mentra.shell.calculator

import com.example.mentra.shell.models.ShellOutput
import com.example.mentra.shell.models.ShellOutputType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA SUPER CALCULATOR
 * Advanced mathematical expression evaluator for the shell
 * ═══════════════════════════════════════════════════════════════════
 *
 * Supports:
 * - Basic operations: +, -, *, /, %, ^
 * - Functions: sin, cos, tan, sqrt, log, ln, abs, floor, ceil, round
 * - Constants: pi, e
 * - Parentheses for grouping
 * - Modulo: mod, %
 * - Power: ^, **
 * - Scientific notation: 1e5, 2.5e-3
 *
 * Commands:
 * - calc 2+2
 * - calculator sin(45)
 * - 1+1 (auto-detect)
 * - 5 mod 3
 * - sqrt(144)
 * - 2^10
 */
@Singleton
class ShellCalculator @Inject constructor() {

    // Calculator command keywords
    private val calcKeywords = listOf("calc", "calculator", "calculate", "math", "eval")

    // Math function names
    private val mathFunctions = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "sqrt", "cbrt", "log", "log10", "log2", "ln",
        "abs", "floor", "ceil", "round",
        "exp", "pow", "min", "max",
        "rad", "deg"
    )

    // Constants
    private val constants = mapOf(
        "pi" to Math.PI,
        "π" to Math.PI,
        "e" to Math.E,
        "phi" to 1.6180339887498949,
        "φ" to 1.6180339887498949
    )

    /**
     * Check if input should trigger the calculator UI
     */
    fun shouldShowUI(input: String): Boolean {
        val trimmed = input.trim().lowercase()
        return trimmed == "calc" || trimmed == "calculator" || trimmed == "calculate"
    }

    /**
     * Check if input is a calculator command
     */
    fun isCalculatorCommand(input: String): Boolean {
        val lower = input.lowercase().trim()
        return calcKeywords.any { lower.startsWith("$it ") || lower == it }
    }

    /**
     * Check if input looks like a math expression (auto-detect)
     */
    fun isMathExpression(input: String): Boolean {
        val trimmed = input.trim()

        // Skip if it's clearly a command or text
        if (trimmed.contains(" ") && !trimmed.contains("mod") &&
            !trimmed.contains("^") && !mathFunctions.any { trimmed.lowercase().contains(it) }) {
            // Check if it has operators between numbers
            val hasOperator = Regex("[+\\-*/^%]").containsMatchIn(trimmed)
            val startsWithNumber = trimmed.firstOrNull()?.let { it.isDigit() || it == '(' || it == '-' } == true
            if (!hasOperator || !startsWithNumber) return false
        }

        // Must contain at least one operator or function
        val hasOperator = Regex("[+\\-*/^%()]").containsMatchIn(trimmed) ||
                         trimmed.lowercase().contains("mod") ||
                         mathFunctions.any { trimmed.lowercase().startsWith(it) }

        // Must start with a number, minus, parenthesis, or function
        val validStart = trimmed.firstOrNull()?.let {
            it.isDigit() || it == '-' || it == '(' || it == '.'
        } == true || mathFunctions.any { trimmed.lowercase().startsWith(it) }

        // Should not be a phone number (7-15 consecutive digits)
        val looksLikePhone = Regex("^\\+?[0-9]{7,15}$").matches(trimmed.replace(" ", ""))

        // Should have numbers
        val hasNumbers = Regex("[0-9]").containsMatchIn(trimmed)

        return hasOperator && validStart && hasNumbers && !looksLikePhone
    }

    /**
     * Handle calculator command and return shell output
     * Returns special output with data="SHOW_UI" when UI should be displayed
     */
    fun handleCommand(input: String): List<ShellOutput> {
        val trimmed = input.trim().lowercase()

        // If just "calc" or "calculator" with no expression, signal to show UI
        if (trimmed == "calc" || trimmed == "calculator" || trimmed == "calculate") {
            return listOf(
                ShellOutput("Opening Calculator UI...", ShellOutputType.SUCCESS)
            ).also {
                // Signal that UI should be shown - check via shouldShowUI
            }
        }

        val expression = extractExpression(input)

        if (expression.isBlank()) {
            return listOf(
                ShellOutput("🧮 Calculator", ShellOutputType.HEADER),
                ShellOutput("Usage: calc <expression>", ShellOutputType.INFO),
                ShellOutput("", ShellOutputType.INFO),
                ShellOutput("Examples:", ShellOutputType.INFO),
                ShellOutput("  calc 2+2          → 4", ShellOutputType.SUCCESS),
                ShellOutput("  calc sqrt(144)    → 12", ShellOutputType.SUCCESS),
                ShellOutput("  calc sin(45)      → 0.707... (degrees)", ShellOutputType.SUCCESS),
                ShellOutput("  calc 2^10         → 1024", ShellOutputType.SUCCESS),
                ShellOutput("  calc 17 mod 5     → 2", ShellOutputType.SUCCESS),
                ShellOutput("  calc log(100)     → 2", ShellOutputType.SUCCESS),
                ShellOutput("  calc pi * 2       → 6.283...", ShellOutputType.SUCCESS),
                ShellOutput("", ShellOutputType.INFO),
                ShellOutput("Tip: Type 'calc' or 'calculator' to open UI", ShellOutputType.PROMPT)
            )
        }

        return evaluate(expression)
    }

    /**
     * Evaluate a math expression directly
     */
    fun evaluate(expression: String): List<ShellOutput> {
        return try {
            val result = evaluateExpression(expression)
            val formattedResult = formatResult(result)
            val formattedExpr = expression.replace(" ", "")

            listOf(
                ShellOutput("$formattedExpr = $formattedResult", ShellOutputType.SUCCESS)
            )
        } catch (e: Exception) {
            listOf(
                ShellOutput("❌ ${e.message ?: "Invalid expression"}", ShellOutputType.ERROR)
            )
        }
    }

    /**
     * Extract expression from command
     */
    private fun extractExpression(input: String): String {
        val lower = input.lowercase().trim()
        for (keyword in calcKeywords) {
            if (lower.startsWith("$keyword ")) {
                return input.substring(keyword.length).trim()
            }
        }
        return input.trim()
    }

    /**
     * Format the result for display
     */
    private fun formatResult(value: Double): String {
        return when {
            value.isNaN() -> "NaN"
            value.isInfinite() -> if (value > 0) "∞" else "-∞"
            value == value.toLong().toDouble() && abs(value) < 1e15 -> {
                value.toLong().toString()
            }
            abs(value) < 0.0001 || abs(value) >= 1e10 -> {
                String.format("%.6e", value)
            }
            else -> {
                val formatted = String.format("%.10f", value).trimEnd('0').trimEnd('.')
                if (formatted.length > 15) String.format("%.6f", value).trimEnd('0').trimEnd('.')
                else formatted
            }
        }
    }

    /**
     * Main expression evaluator using recursive descent parsing
     */
    private fun evaluateExpression(expr: String): Double {
        val parser = ExpressionParser(preprocessExpression(expr))
        val result = parser.parse()
        if (!parser.isAtEnd()) {
            throw IllegalArgumentException("Unexpected character: ${parser.peek()}")
        }
        return result
    }

    /**
     * Preprocess expression - normalize operators and handle constants
     */
    private fun preprocessExpression(expr: String): String {
        var processed = expr.lowercase().trim()

        // Replace word operators
        processed = processed.replace(" mod ", "%")
        processed = processed.replace("mod", "%")
        processed = processed.replace("**", "^")

        // Replace constants
        for ((name, value) in constants) {
            processed = processed.replace(name, value.toString())
        }

        // Handle implicit multiplication: 2pi -> 2*pi, 2(3) -> 2*(3)
        processed = processed.replace(Regex("(\\d)([a-z(])")) {
            "${it.groupValues[1]}*${it.groupValues[2]}"
        }
        processed = processed.replace(Regex("(\\))([a-z0-9(])")) {
            "${it.groupValues[1]}*${it.groupValues[2]}"
        }

        // Remove spaces
        processed = processed.replace(" ", "")

        return processed
    }

    /**
     * Recursive descent parser for mathematical expressions
     */
    private inner class ExpressionParser(private val expr: String) {
        private var pos = 0

        fun parse(): Double = parseAddSub()

        fun isAtEnd(): Boolean = pos >= expr.length

        fun peek(): Char = if (isAtEnd()) '\u0000' else expr[pos]

        private fun advance(): Char {
            return if (isAtEnd()) '\u0000' else expr[pos++]
        }

        private fun match(c: Char): Boolean {
            if (peek() == c) {
                advance()
                return true
            }
            return false
        }

        // Addition and subtraction (lowest precedence)
        private fun parseAddSub(): Double {
            var result = parseMulDiv()
            while (true) {
                when {
                    match('+') -> result += parseMulDiv()
                    match('-') -> result -= parseMulDiv()
                    else -> return result
                }
            }
        }

        // Multiplication, division, modulo
        private fun parseMulDiv(): Double {
            var result = parsePower()
            while (true) {
                when {
                    match('*') -> result *= parsePower()
                    match('/') -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }
                    match('%') -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                        result %= divisor
                    }
                    else -> return result
                }
            }
        }

        // Power (right associative)
        private fun parsePower(): Double {
            var result = parseUnary()
            if (match('^')) {
                result = result.pow(parsePower()) // Right associative
            }
            return result
        }

        // Unary operators
        private fun parseUnary(): Double {
            return when {
                match('-') -> -parseUnary()
                match('+') -> parseUnary()
                else -> parsePrimary()
            }
        }

        // Primary: numbers, functions, parentheses
        private fun parsePrimary(): Double {
            // Parentheses
            if (match('(')) {
                val result = parseAddSub()
                if (!match(')')) throw IllegalArgumentException("Missing closing parenthesis")
                return result
            }

            // Function or number
            val start = pos

            // Try to read a function name
            while (!isAtEnd() && peek().isLetter()) {
                advance()
            }

            if (pos > start) {
                val name = expr.substring(start, pos)
                if (name in mathFunctions) {
                    return evaluateFunction(name)
                } else {
                    throw IllegalArgumentException("Unknown function: $name")
                }
            }

            // Number
            return parseNumber()
        }

        private fun parseNumber(): Double {
            val start = pos

            // Integer part
            while (!isAtEnd() && (peek().isDigit() || peek() == '.')) {
                advance()
            }

            // Scientific notation
            if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
                advance()
                if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                    advance()
                }
                while (!isAtEnd() && peek().isDigit()) {
                    advance()
                }
            }

            if (pos == start) {
                throw IllegalArgumentException("Expected number at position $pos")
            }

            val numStr = expr.substring(start, pos)
            return numStr.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number: $numStr")
        }

        private fun evaluateFunction(name: String): Double {
            if (!match('(')) throw IllegalArgumentException("Expected '(' after function $name")

            val args = mutableListOf<Double>()
            if (!match(')')) {
                args.add(parseAddSub())
                while (match(',')) {
                    args.add(parseAddSub())
                }
                if (!match(')')) throw IllegalArgumentException("Missing ')' after function arguments")
            }

            return when (name) {
                "sin" -> sin(Math.toRadians(args.getOrElse(0) { 0.0 }))
                "cos" -> cos(Math.toRadians(args.getOrElse(0) { 0.0 }))
                "tan" -> tan(Math.toRadians(args.getOrElse(0) { 0.0 }))
                "asin" -> Math.toDegrees(asin(args.getOrElse(0) { 0.0 }))
                "acos" -> Math.toDegrees(acos(args.getOrElse(0) { 0.0 }))
                "atan" -> Math.toDegrees(atan(args.getOrElse(0) { 0.0 }))
                "sinh" -> sinh(args.getOrElse(0) { 0.0 })
                "cosh" -> cosh(args.getOrElse(0) { 0.0 })
                "tanh" -> tanh(args.getOrElse(0) { 0.0 })
                "sqrt" -> {
                    val arg = args.getOrElse(0) { 0.0 }
                    if (arg < 0) throw ArithmeticException("Cannot take square root of negative number")
                    sqrt(arg)
                }
                "cbrt" -> cbrt(args.getOrElse(0) { 0.0 })
                "log", "log10" -> {
                    val arg = args.getOrElse(0) { 0.0 }
                    if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                    log10(arg)
                }
                "log2" -> {
                    val arg = args.getOrElse(0) { 0.0 }
                    if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                    ln(arg) / ln(2.0)
                }
                "ln" -> {
                    val arg = args.getOrElse(0) { 0.0 }
                    if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                    ln(arg)
                }
                "abs" -> abs(args.getOrElse(0) { 0.0 })
                "floor" -> floor(args.getOrElse(0) { 0.0 })
                "ceil" -> ceil(args.getOrElse(0) { 0.0 })
                "round" -> round(args.getOrElse(0) { 0.0 })
                "exp" -> exp(args.getOrElse(0) { 0.0 })
                "pow" -> {
                    val base = args.getOrElse(0) { 0.0 }
                    val exp = args.getOrElse(1) { 1.0 }
                    base.pow(exp)
                }
                "min" -> args.minOrNull() ?: 0.0
                "max" -> args.maxOrNull() ?: 0.0
                "rad" -> Math.toRadians(args.getOrElse(0) { 0.0 })
                "deg" -> Math.toDegrees(args.getOrElse(0) { 0.0 })
                else -> throw IllegalArgumentException("Unknown function: $name")
            }
        }
    }
}
