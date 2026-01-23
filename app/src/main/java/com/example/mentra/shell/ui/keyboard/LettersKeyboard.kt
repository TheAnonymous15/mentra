package com.example.mentra.shell.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * LETTERS KEYBOARD - QWERTY Layout
 */

@Composable
fun LettersKeyboard(
    isShiftActive: Boolean,
    isCtrlActive: Boolean,
    onKeyPress: (String) -> Unit,
    onShiftToggle: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToNumbers: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Number row: 1 2 3 4 5 6 7 8 9 0
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 1: q w e r t y u i o p
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").forEach { key ->
                    CharKey(
                        text = if (isShiftActive) key.uppercase() else key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 2: a s d f g h j k l (slightly indented)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").forEach { key ->
                    CharKey(
                        text = if (isShiftActive) key.uppercase() else key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 3: Shift z x c v b n m Backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ShiftKey(
                    isActive = isShiftActive,
                    modifier = Modifier.weight(1.4f),
                    onClick = onShiftToggle
                )
                listOf("z", "x", "c", "v", "b", "n", "m").forEach { key ->
                    CharKey(
                        text = if (isShiftActive) key.uppercase() else key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) },
                        // Highlight 'C' key when CTRL is active to indicate Ctrl+C
                        isHighlighted = isCtrlActive && key == "c"
                    )
                }
                BackspaceKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onBackspace
                )
            }

            // Bottom row: ?123 , Space . Enter
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(
                    text = "?123",
                    modifier = Modifier.weight(1.2f),
                    onClick = onSwitchToNumbers
                )
                CharKey(
                    text = ",",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyPress(",") }
                )
                SpaceBar(
                    modifier = Modifier.weight(4f),
                    onClick = { onKeyPress(" ") }
                )
                CharKey(
                    text = ".",
                    modifier = Modifier.weight(0.8f),
                    onClick = { onKeyPress(".") }
                )
                EnterKey(
                    modifier = Modifier.weight(1.4f),
                    onClick = onEnter
                )
            }
        }
    }
}
