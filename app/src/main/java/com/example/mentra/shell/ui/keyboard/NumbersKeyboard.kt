package com.example.mentra.shell.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * NUMBERS KEYBOARD - Calculator style
 */

@Composable
fun NumbersKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToLetters: () -> Unit,
    onSwitchToSymbols: () -> Unit,
    onSwitchToNumpad: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: 1 2 3 4 5 6 7 8 9 0
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) },
                        isLarge = true
                    )
                }
            }

            // Row 2: @ # $ _ & - + ( ) /
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").forEach { key ->
                    CharKey(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyPress(key) }
                    )
                }
            }

            // Row 3: =\< * " ' : ; ! ? Backspace
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ModeKey(
                    text = "=\\<",
                    modifier = Modifier.weight(1.2f),
                    onClick = onSwitchToSymbols
                )
                listOf("*", "\"", "'", ":", ";", "!", "?").forEach { key ->
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

            // Bottom row: ABC , 1234 Space . Enter
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
                    text = ",",
                    modifier = Modifier.weight(0.7f),
                    onClick = { onKeyPress(",") }
                )
                ModeKey(
                    text = "12\n34",
                    modifier = Modifier.weight(1f),
                    onClick = onSwitchToNumpad
                )
                SpaceBar(
                    modifier = Modifier.weight(3f),
                    onClick = { onKeyPress(" ") }
                )
                CharKey(
                    text = ".",
                    modifier = Modifier.weight(0.7f),
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

