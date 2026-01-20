package com.example.mentra.dialer.ussd

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * ═══════════════════════════════════════════════════════════════════
 * USSD RESPONSE MODAL - Displays USSD responses in a futuristic dialog
 * ═══════════════════════════════════════════════════════════════════
 */

private object UssdColors {
    val background = Color(0xFF0A0E14)
    val surface = Color(0xFF141C2D)
    val surfaceLight = Color(0xFF1A2332)

    val primary = Color(0xFF00E5FF)
    val secondary = Color(0xFF9D4EDD)
    val success = Color(0xFF00E676)
    val error = Color(0xFFFF5252)
    val warning = Color(0xFFFFD740)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0BEC5)
    val textMuted = Color(0xFF607D8B)

    val gradientPrimary = listOf(Color(0xFF00E5FF), Color(0xFF00B8D4))
    val gradientSuccess = listOf(Color(0xFF00E676), Color(0xFF00C853))
    val gradientError = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
}

/**
 * USSD Response Modal - Shows the response from a USSD request
 */
@Composable
fun UssdResponseModal(
    ussdState: UssdState,
    onDismiss: () -> Unit,
    onReply: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    when (ussdState) {
        is UssdState.Idle -> { /* Don't show modal */ }
        is UssdState.Executing -> {
            UssdLoadingModal(code = ussdState.code, onDismiss = onDismiss)
        }
        is UssdState.Success -> {
            UssdSuccessModal(
                response = ussdState.response,
                onDismiss = onDismiss,
                onReply = onReply,
                isInteractive = false
            )
        }
        is UssdState.Interactive -> {
            UssdSuccessModal(
                response = ussdState.response,
                onDismiss = onDismiss,
                onReply = onReply,
                isInteractive = true
            )
        }
        is UssdState.Error -> {
            UssdErrorModal(
                message = ussdState.message,
                onDismiss = onDismiss,
                onRetry = onRetry
            )
        }
        is UssdState.LegacyDialing -> {
            // Auto-dismiss when system dialer opens - no modal needed
            LaunchedEffect(Unit) {
                onDismiss()
            }
        }
    }
}

/**
 * Loading state modal
 */
@Composable
private fun UssdLoadingModal(
    code: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = UssdColors.surface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Animated loading indicator
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing)
                    ),
                    label = "rotation"
                )

                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = UssdColors.primary,
                        strokeWidth = 3.dp
                    )
                    Icon(
                        Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = UssdColors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Executing USSD",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UssdColors.textPrimary
                )

                Text(
                    text = code,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = UssdColors.primary,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Please wait for response...",
                    fontSize = 13.sp,
                    color = UssdColors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Success response modal
 */
@Composable
private fun UssdSuccessModal(
    response: UssdResponse,
    onDismiss: () -> Unit,
    onReply: ((String) -> Unit)? = null,
    isInteractive: Boolean = false
) {
    var replyText by remember { mutableStateOf("") }
    // Automatically show reply field for interactive sessions
    val showReplyField = isInteractive || response.isInteractive

    Dialog(
        onDismissRequest = if (isInteractive) {
            {} // Don't allow dismissal during interactive session by clicking outside
        } else {
            onDismiss
        },
        properties = DialogProperties(
            dismissOnBackPress = !isInteractive, // Don't allow back during interactive session
            dismissOnClickOutside = !isInteractive, // Don't allow outside click during interactive
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = UssdColors.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        UssdColors.success.copy(alpha = 0.4f),
                        UssdColors.primary.copy(alpha = 0.2f)
                    )
                )
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    brush = Brush.linearGradient(UssdColors.gradientSuccess),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "USSD Response",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = UssdColors.textPrimary
                            )
                            Text(
                                text = response.code,
                                fontSize = 12.sp,
                                color = UssdColors.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = UssdColors.textMuted
                        )
                    }
                }

                // Response content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = UssdColors.background
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = response.response,
                            fontSize = 14.sp,
                            color = UssdColors.textPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Reply section (for interactive sessions)
                if (showReplyField && onReply != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Interactive session indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = UssdColors.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                UssdColors.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QuestionAnswer,
                                    contentDescription = null,
                                    tint = UssdColors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "This menu requires your response",
                                    fontSize = 12.sp,
                                    color = UssdColors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Reply input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = {
                                    Text("Enter your choice (e.g., 1, 2, 3...)", color = UssdColors.textMuted)
                                },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UssdColors.primary,
                                    unfocusedBorderColor = UssdColors.textMuted.copy(alpha = 0.3f),
                                    focusedTextColor = UssdColors.textPrimary,
                                    unfocusedTextColor = UssdColors.textPrimary,
                                    cursorColor = UssdColors.primary,
                                    focusedContainerColor = UssdColors.background.copy(alpha = 0.5f),
                                    unfocusedContainerColor = UssdColors.background.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (replyText.isNotBlank()) {
                                            onReply(replyText)
                                            replyText = ""
                                        }
                                    }
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        onReply(replyText)
                                        replyText = ""
                                    }
                                },
                                enabled = replyText.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = if (replyText.isNotBlank()) {
                                            Brush.linearGradient(UssdColors.gradientPrimary)
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    UssdColors.textMuted.copy(alpha = 0.3f),
                                                    UssdColors.textMuted.copy(alpha = 0.2f)
                                                )
                                            )
                                        },
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (replyText.isNotBlank()) Color.White else UssdColors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Footer with action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isInteractive) {
                        Arrangement.SpaceBetween
                    } else {
                        Arrangement.End
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show timestamp
                    if (!isInteractive) {
                        Text(
                            text = response.getFormattedTime(),
                            fontSize = 11.sp,
                            color = UssdColors.textMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cancel button for interactive sessions
                        if (isInteractive) {
                            OutlinedButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = UssdColors.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    UssdColors.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Session")
                            }
                        } else {
                            // Done button for non-interactive
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UssdColors.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", color = UssdColors.background)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper extension for formatting time
 */
private fun UssdResponse.getFormattedTime(): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Error modal
 */
@Composable
private fun UssdErrorModal(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = UssdColors.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                UssdColors.error.copy(alpha = 0.4f)
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(UssdColors.gradientError),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "USSD Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UssdColors.textPrimary
                )

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = UssdColors.textSecondary,
                    textAlign = TextAlign.Center
                )

                // Help text for users
                if (message.contains("request failed", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = UssdColors.primary.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            UssdColors.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = UssdColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Some USSD codes may not work with the app. Try using your device's dialer.",
                                fontSize = 11.sp,
                                color = UssdColors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onRetry != null) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = UssdColors.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                UssdColors.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UssdColors.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Legacy dialing modal (when callback API is not available)
 */
@Composable
private fun UssdLegacyModal(
    code: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = UssdColors.surface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(UssdColors.gradientPrimary),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneForwarded,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Opening in System Dialer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UssdColors.textPrimary
                )

                Text(
                    text = code,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = UssdColors.primary,
                    fontFamily = FontFamily.Monospace
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = UssdColors.primary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        UssdColors.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = UssdColors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "USSD Menu Guide:",
                                fontSize = 12.sp,
                                color = UssdColors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "• Your device's dialer will handle this USSD code\n• Navigate menus by entering numbers (1, 2, 3...)\n• You can interact with all options there",
                            fontSize = 11.sp,
                            color = UssdColors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UssdColors.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("OK", color = UssdColors.background)
                }
            }
        }
    }
}

/**
 * USSD History List
 */
@Composable
fun UssdHistoryList(
    history: List<UssdHistoryEntry>,
    onEntryClick: (UssdHistoryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = UssdColors.textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No USSD history",
                    color = UssdColors.textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(history) { entry ->
                UssdHistoryItem(
                    entry = entry,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun UssdHistoryItem(
    entry: UssdHistoryEntry,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = UssdColors.surfaceLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (entry.isSuccess)
                            UssdColors.success.copy(alpha = 0.15f)
                        else
                            UssdColors.error.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (entry.isSuccess) UssdColors.success else UssdColors.error,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.code,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = UssdColors.textPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = entry.response.take(50) + if (entry.response.length > 50) "..." else "",
                    fontSize = 12.sp,
                    color = UssdColors.textMuted,
                    maxLines = 1
                )
            }

            Text(
                text = entry.getFormattedTime(),
                fontSize = 11.sp,
                color = UssdColors.textMuted
            )
        }
    }
}

