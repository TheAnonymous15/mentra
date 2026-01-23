package com.example.mentra.shell.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * NUMPAD KEYBOARD - Calculator style
 * Operators on LEFT, numbers in CENTER, special on RIGHT
 */

@Composable
fun NumpadKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToLetters: () -> Unit,
    onSwitchToNumpadSymbols: () -> Unit,
    onSwitchToMath: () -> Unit,
    onSwitchToPhysics: () -> Unit
) {
    GlassmorphicKeyboardCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: + | 1 2 3 | %
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatNumpadKey(text = "+", modifier = Modifier.weight(1f), onClick = { onKeyPress("+") }, isOperator = true)
                FlatNumpadKey(text = "1", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("1") }, isNumber = true)
                FlatNumpadKey(text = "2", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("2") }, isNumber = true)
                FlatNumpadKey(text = "3", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("3") }, isNumber = true)
                FlatNumpadKey(text = "%", modifier = Modifier.weight(1f), onClick = { onKeyPress("%") })
            }

            // Row 2: - | 4 5 6 | PHY
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatNumpadKey(text = "−", modifier = Modifier.weight(1f), onClick = { onKeyPress("-") }, isOperator = true)
                FlatNumpadKey(text = "4", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("4") }, isNumber = true)
                FlatNumpadKey(text = "5", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("5") }, isNumber = true)
                FlatNumpadKey(text = "6", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("6") }, isNumber = true)
                FlatNumpadKey(text = "PHY", modifier = Modifier.weight(1f), onClick = onSwitchToPhysics, isMode = true)
            }

            // Row 3: * | 7 8 9 | ⌫
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatNumpadKey(text = "×", modifier = Modifier.weight(1f), onClick = { onKeyPress("*") }, isOperator = true)
                FlatNumpadKey(text = "7", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("7") }, isNumber = true)
                FlatNumpadKey(text = "8", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("8") }, isNumber = true)
                FlatNumpadKey(text = "9", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("9") }, isNumber = true)
                BackspaceKey(modifier = Modifier.weight(1f).height(48.dp), onClick = onBackspace)
            }

            // Row 4: / | , 0 = | .   (0 is directly below 8)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatNumpadKey(text = "÷", modifier = Modifier.weight(1f), onClick = { onKeyPress("/") }, isOperator = true)
                FlatNumpadKey(text = ",", modifier = Modifier.weight(1.5f), onClick = { onKeyPress(",") })
                FlatNumpadKey(text = "0", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("0") }, isNumber = true)
                FlatNumpadKey(text = "=", modifier = Modifier.weight(1.5f), onClick = { onKeyPress("=") })
                FlatNumpadKey(text = ".", modifier = Modifier.weight(1f), onClick = { onKeyPress(".") })
            }

            // Row 5: Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatNumpadKey(text = "MATH", modifier = Modifier.weight(1.2f), onClick = onSwitchToMath, isMode = true)
                ClipboardKey(modifier = Modifier.weight(1f), onPaste = onKeyPress)
                SpaceBar(modifier = Modifier.weight(2.5f), onClick = { onKeyPress(" ") })
                FlatNumpadKey(text = "ABC", modifier = Modifier.weight(1f), onClick = onSwitchToLetters, isMode = true, isHighlighted = true)
                EnterKey(modifier = Modifier.weight(1.2f), onClick = onEnter)
            }
        }
    }
}

/**
 * NUMPAD SYMBOLS KEYBOARD - Special characters from numpad
 */

@Composable
fun NumpadSymbolsKeyboard(
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
            // Row 1: ! @ # $ % ^ & * ( )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 2: ` ~ | \ { } [ ] < >
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("`", "~", "|", "\\", "{", "}", "[", "]", "<", ">").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 3: ' " : ; ? / _ - + =
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("'", "\"", ":", ";", "?", "/", "_", "-", "+", "=").forEach { key ->
                    CharKey(text = key, modifier = Modifier.weight(1f), onClick = { onKeyPress(key) })
                }
            }

            // Row 4: 123 back to numpad + backspace
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

