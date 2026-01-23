package com.example.mentra.shell.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * MATH KEYBOARD - Mathematical symbols
 */

@Composable
fun MathKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToNumpad: () -> Unit,
    onSwitchToLetters: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: Basic math operators
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("÷", "×", "−", "+", "±", "=", "≠", "≈", "∞", "√").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 2: Comparison & fractions
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("<", ">", "≤", "≥", "½", "⅓", "¼", "⅔", "¾", "⅛").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 3: Greek letters & constants
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("π", "θ", "α", "β", "γ", "δ", "λ", "σ", "Σ", "Δ").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 4: More math symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("∫", "∂", "∇", "∑", "∏", "∈", "∉", "⊂", "⊃", "∪").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 5: Superscripts & subscripts
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("²", "³", "⁴", "ⁿ", "₀", "₁", "₂", "°", "′", "″").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 6: 123 back to numpad + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(text = "123", modifier = Modifier.weight(1.2f), onClick = onSwitchToNumpad)
                Spacer(modifier = Modifier.weight(3f))
                BackspaceKey(modifier = Modifier.weight(1.4f), onClick = onBackspace)
            }

            // Bottom row: ABC | space | Enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(
                    text = "ABC",
                    modifier = Modifier.weight(1.2f),
                    onClick = onSwitchToLetters,
                    isHighlighted = true
                )
                SpaceBar(
                    modifier = Modifier.weight(4f),
                    onClick = { onKeyPress(" ") }
                )
                EnterKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onEnter
                )
            }
        }
    }
}

/**
 * PHYSICS KEYBOARD - Physics symbols and units
 */

@Composable
fun PhysicsKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToNumpad: () -> Unit,
    onSwitchToLetters: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: Common physics symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("Ω", "μ", "ε", "ρ", "ω", "τ", "φ", "ψ", "η", "ν").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 2: Units and constants
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("ℏ", "ℓ", "Å", "℃", "℉", "K", "J", "W", "V", "A").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 3: Vector and field symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("→", "⃗", "∥", "⊥", "∠", "∝", "≡", "⇌", "↔", "⟂").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 4: More physics symbols
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("λ", "γ", "β", "α", "κ", "χ", "ξ", "ζ", "Φ", "Ψ").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 5: SI prefixes and special
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("n", "μ", "m", "k", "M", "G", "T", "Hz", "Pa", "N").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 6: 123 back to numpad + backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(text = "123", modifier = Modifier.weight(1.2f), onClick = onSwitchToNumpad)
                Spacer(modifier = Modifier.weight(3f))
                BackspaceKey(modifier = Modifier.weight(1.4f), onClick = onBackspace)
            }

            // Bottom row: ABC | space | Enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(
                    text = "ABC",
                    modifier = Modifier.weight(1.2f),
                    onClick = onSwitchToLetters,
                    isHighlighted = true
                )
                SpaceBar(
                    modifier = Modifier.weight(4f),
                    onClick = { onKeyPress(" ") }
                )
                EnterKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onEnter
                )
            }
        }
    }
}

