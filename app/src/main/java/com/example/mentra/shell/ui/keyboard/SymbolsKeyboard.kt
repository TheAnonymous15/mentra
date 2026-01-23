package com.example.mentra.shell.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SYMBOLS KEYBOARD - Extended symbols
 */

@Composable
fun SymbolsKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToLetters: () -> Unit,
    onSwitchToNumbers: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: ~ ` | · √ π ÷ × § ∆
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("~", "`", "|", "·", "√", "π", "÷", "×", "§", "∆").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 2: £ ¢ € ¥ ^ ° = { } \
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 3: ?123 % © ® ™ ✓ [ ] Backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(
                    text = "?123",
                    modifier = Modifier.weight(1.2f),
                    onClick = onSwitchToNumbers
                )
                listOf("%", "©", "®", "™", "✓", "[", "]").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
                BackspaceKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onBackspace
                )
            }

            // Bottom row: ABC < Space > Enter
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
                CharKey(
                    text = "<",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyPress("<") }
                )
                SpaceBar(
                    modifier = Modifier.weight(4f),
                    onClick = { onKeyPress(" ") }
                )
                CharKey(
                    text = ">",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyPress(">") }
                )
                EnterKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onEnter
                )
            }
        }
    }
}

