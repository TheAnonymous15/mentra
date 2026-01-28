package com.example.mentra.shell.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.messaging.Contact
import com.example.mentra.shell.core.ShellEngine
import com.example.mentra.shell.messaging.ContactPickerRequest
import com.example.mentra.shell.messaging.ShellMessagingCommandHandler
import com.example.mentra.shell.messaging.ShellMessagingService
import com.example.mentra.shell.messaging.ui.ShellContactPickerDialog
import com.example.mentra.shell.ui.keyboard.TerminalKeyboard
import com.example.mentra.shell.calling.ui.ShellCallingContactPickerDialog
import com.example.mentra.shell.calculator.NexusCalculatorModal
import com.example.mentra.shell.calendar.NexusCalendarModal
import com.example.mentra.shell.models.ShellResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA SHELL - KALI LINUX STYLE TERMINAL
 * Modern futuristic terminal with inline input
 * ═══════════════════════════════════════════════════════════════════
 */

// Terminal color palette - Kali Linux inspired with neon accents
private object TerminalColors {
    val background = Color(0xFF0D0D0D)
    val surface = Color(0xFF1A1A2E)
    val terminalBg = Color(0xFF0F0F0F)

    // Kali Linux colors
    val promptUser = Color(0xFF00FF41)      // Bright green for mentra
    val promptSeparator = Color(0xFFFFFFFF) // White for :
    val promptTime = Color(0xFF00D4FF)      // Cyan for time
    val promptSymbol = Color(0xFFFF2E63)    // Red/Pink for $
    val inputText = Color(0xFFFFFFFF)       // White for user input

    // Output colors
    val success = Color(0xFF00FF41)
    val error = Color(0xFFFF073A)
    val warning = Color(0xFFFFD93D)
    val info = Color(0xFF00D4FF)
    val muted = Color(0xFF6C7A89)
    val command = Color(0xFFB388FF)         // Purple for commands

    // Accent colors
    val neonPink = Color(0xFFFF2E63)
    val neonCyan = Color(0xFF00D4FF)
    val neonGreen = Color(0xFF00FF41)
    val neonPurple = Color(0xFFB388FF)

    // Cursor
    val cursor = Color(0xFF00FF41)
}

// Use monospace font
private val terminalFont = FontFamily.Monospace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    viewModel: ShellViewModel = hiltViewModel(),
    onNavigateToMessages: () -> Unit = {},
    onNavigateToDialer: () -> Unit = {},
    appCacheService: com.example.mentra.shell.apps.AppCacheService? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val contactPickerRequest by viewModel.contactPickerRequest.collectAsState()
    val callingContactPickerRequest by viewModel.callingContactPickerRequest.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val listState = rememberLazyListState()

    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    // Current time state
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }

    // Custom keyboard visibility
    var showKeyboard by remember { mutableStateOf(true) }

    // Cursor position for input
    var cursorPosition by remember { mutableIntStateOf(0) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(1000)
        }
    }

    // Auto-scroll to bottom when new output is added
    LaunchedEffect(uiState.output.size) {
        if (uiState.output.isNotEmpty()) {
            listState.animateScrollToItem(uiState.output.size)
        }
    }

    // Update cursor position when input changes
    LaunchedEffect(uiState.input) {
        cursorPosition = uiState.input.length
    }

    // Check for pending incoming calls when shell becomes visible
    LaunchedEffect(Unit) {
        viewModel.onShellVisible()
    }

    // Settings menu state
    var showSettings by remember { mutableStateOf(false) }

    // Contact Picker Dialog (for messaging)
    contactPickerRequest?.let { request ->
        ShellContactPickerDialog(
            title = request.title,
            contacts = contacts,
            forAlias = request.forAlias,
            isLoading = contacts.isEmpty(),
            onContactSelected = { contact, number ->
                viewModel.onContactSelected(contact, number)
            },
            onDismiss = { viewModel.onContactPickerDismissed() }
        )
    }

    // Contact Picker Dialog (for calling) - with integrated SIM selection
    callingContactPickerRequest?.let { request ->
        ShellCallingContactPickerDialog(
            title = request.title,
            contacts = contacts,
            isLoading = contacts.isEmpty(),
            onCallWithSim = { contact, number, simSlot ->
                viewModel.onCallingContactSelectedWithSim(contact, number, simSlot)
            },
            onDismiss = { viewModel.onCallingContactPickerDismissed() }
        )
    }

    // Settings Dialog
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            shellSettingsManager = viewModel.shellSettingsManager
        )
    }

    // Calculator Dialog - must be displayed as overlay
    val showCalculatorUI by viewModel.showCalculatorUI.collectAsState()

    // Calendar Dialog - must be displayed as overlay
    val showCalendarUI by viewModel.showCalendarUI.collectAsState()

    // App Picker Dialog
    val showAppPicker by viewModel.showAppPicker.collectAsState()

    // Navigation events
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            NavigationEvent.MESSAGES -> {
                onNavigateToMessages()
                viewModel.clearNavigationEvent()
            }
            NavigationEvent.DIALER -> {
                onNavigateToDialer()
                viewModel.clearNavigationEvent()
            }
            null -> { /* No navigation */ }
        }
    }


    // Animated background glow
    val infiniteTransition = rememberInfiniteTransition(label = "terminal_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background)
            .drawBehind {
                // Scanline effect
                for (y in 0 until size.height.toInt() step 3) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.1f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
                // Corner glow effects
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TerminalColors.neonCyan.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.5f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TerminalColors.neonPink.copy(alpha = glowAlpha * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(size.width, size.height),
                        radius = size.width * 0.4f
                    )
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showKeyboard = true
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Terminal Header - Fixed at top
            TerminalHeader(
                onSettingsClick = { showSettings = true },
                onClearClick = { viewModel.clearHistory() }
            )

            // Terminal Content - Output + Current Input Line
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                state = listState,
                reverseLayout = false
            ) {
                // Output history
                items(uiState.output) { entry ->
                    TerminalOutputEntry(entry)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Current input line (inline like real terminal)
                item {
                    CurrentInputLineCustom(
                        currentTime = currentTime,
                        input = uiState.input,
                        cursorPosition = cursorPosition,
                        isExecuting = uiState.isExecuting
                    )
                }
            }

            // Custom Terminal Keyboard
            TerminalKeyboard(
                visible = showKeyboard,
                onKeyPress = { key ->
                    val currentInput = uiState.input
                    // Ensure cursorPosition is within bounds
                    val safePosition = cursorPosition.coerceIn(0, currentInput.length)
                    val newInput = currentInput.substring(0, safePosition) + key + currentInput.substring(safePosition)
                    viewModel.updateInput(newInput)
                    cursorPosition = safePosition + key.length
                },
                onBackspace = {
                    val currentInput = uiState.input
                    // Ensure cursorPosition is within bounds
                    val safePosition = cursorPosition.coerceIn(0, currentInput.length)
                    if (safePosition > 0) {
                        val newInput = currentInput.substring(0, safePosition - 1) + currentInput.substring(safePosition)
                        viewModel.updateInput(newInput)
                        cursorPosition = safePosition - 1
                    }
                },
                onEnter = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.executeCommand()
                    cursorPosition = 0
                },
                onArrowUp = {
                    // Navigate to previous command in history
                    viewModel.navigateHistoryUp()
                },
                onArrowDown = {
                    // Navigate to next command in history
                    viewModel.navigateHistoryDown()
                },
                onArrowLeft = {
                    if (cursorPosition > 0) {
                        cursorPosition--
                    }
                },
                onArrowRight = {
                    if (cursorPosition < uiState.input.length) {
                        cursorPosition++
                    }
                },
                onHome = {
                    cursorPosition = 0
                },
                onEnd = {
                    cursorPosition = uiState.input.length
                },
                onTab = {
                    // TODO: Auto-complete functionality
                    viewModel.updateInput(uiState.input + "    ")
                    cursorPosition += 4
                },
                onCtrlC = {
                    // Cancel current operation
                    viewModel.cancelOperation()
                },
                onClear = {
                    viewModel.clearHistory()
                }
            )
        }

        // Calculator Dialog Overlay
        if (showCalculatorUI) {
            NexusCalculatorModal(
                onClose = { viewModel.dismissCalculatorUI() }
            )
        }

        // Calendar Dialog Overlay
        if (showCalendarUI) {
            NexusCalendarModal(
                onClose = { viewModel.dismissCalendarUI() }
            )
        }

        // App Picker Dialog Overlay
        if (showAppPicker && appCacheService != null) {
            com.example.mentra.shell.apps.NexusAppPickerModal(
                appCacheService = appCacheService,
                onClose = { viewModel.dismissAppPicker() }
            )
        }
    }
}

@Composable
private fun TerminalHeader(
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = TerminalColors.surface.copy(alpha = 0.8f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    TerminalColors.neonCyan.copy(alpha = 0.3f),
                    TerminalColors.neonPurple.copy(alpha = 0.2f),
                    TerminalColors.neonPink.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Terminal title with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Terminal icon with glow
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TerminalColors.neonGreen.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Terminal",
                        tint = TerminalColors.neonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Nexus Mentra Shell ",
                        color = TerminalColors.neonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = terminalFont,
                        letterSpacing = 3.sp
                    )
                }
            }

            // Clear and Settings buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            TerminalColors.warning.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear",
                        tint = TerminalColors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            TerminalColors.neonCyan.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TerminalColors.neonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalOutputEntry(entry: OutputItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Command line with prompt
        if (entry.command.isNotBlank() && entry.command != "welcome" && entry.command != "contact_selected") {
            TerminalPromptLine(
                time = entry.timestamp,
                command = entry.command,
                isHistory = true
            )
        }

        // Result output
        entry.result?.let { result ->
            Text(
                text = result.message,
                color = when (result.status) {
                    com.example.mentra.shell.models.ResultStatus.SUCCESS -> TerminalColors.success
                    com.example.mentra.shell.models.ResultStatus.FAILURE -> TerminalColors.error
                    com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND -> TerminalColors.warning
                    else -> TerminalColors.info
                },
                fontFamily = terminalFont,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun TerminalPromptLine(
    time: String,
    command: String,
    isHistory: Boolean = false
) {
    val annotatedString = buildAnnotatedString {
        // mentra
        withStyle(SpanStyle(color = TerminalColors.promptUser, fontWeight = FontWeight.Bold)) {
            append("mentra")
        }
        // :
        withStyle(SpanStyle(color = TerminalColors.promptSeparator)) {
            append(" : ")
        }
        // time
        withStyle(SpanStyle(color = TerminalColors.promptTime)) {
            append(time)
        }
        // $
        withStyle(SpanStyle(color = TerminalColors.promptSymbol, fontWeight = FontWeight.Bold)) {
            append(" $ ")
        }
        // command
        withStyle(SpanStyle(color = if (isHistory) TerminalColors.command else TerminalColors.inputText)) {
            append(command)
        }
    }

    Text(
        text = annotatedString,
        fontFamily = terminalFont,
        fontSize = 13.sp
    )
}

@Composable
private fun CurrentInputLine(
    currentTime: String,
    input: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    isExecuting: Boolean,
    focusRequester: FocusRequester
) {
    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prompt prefix
        val promptString = buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalColors.promptUser, fontWeight = FontWeight.Bold)) {
                append("mentra")
            }
            withStyle(SpanStyle(color = TerminalColors.promptSeparator)) {
                append(" : ")
            }
            withStyle(SpanStyle(color = TerminalColors.promptTime)) {
                append(currentTime)
            }
            withStyle(SpanStyle(color = TerminalColors.promptSymbol, fontWeight = FontWeight.Bold)) {
                append(" $ ")
            }
        }

        Text(
            text = promptString,
            fontFamily = terminalFont,
            fontSize = 13.sp
        )

        // Input field (inline)
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    color = TerminalColors.inputText,
                    fontFamily = terminalFont,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(TerminalColors.cursor),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onExecute() }
                ),
                singleLine = true,
                enabled = !isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            // Show blinking cursor when empty
            if (input.isEmpty() && !isExecuting) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(16.dp)
                        .alpha(cursorAlpha)
                        .background(TerminalColors.cursor)
                )
            }
        }

        // Loading indicator when executing
        if (isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 8.dp),
                color = TerminalColors.neonCyan,
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Custom input line for use with custom keyboard (no system keyboard)
 * Shows input with cursor at specified position
 */
@Composable
private fun CurrentInputLineCustom(
    currentTime: String,
    input: String,
    cursorPosition: Int,
    isExecuting: Boolean
) {
    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prompt prefix
        val promptString = buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalColors.promptUser, fontWeight = FontWeight.Bold)) {
                append("mentra")
            }
            withStyle(SpanStyle(color = TerminalColors.promptSeparator)) {
                append(" : ")
            }
            withStyle(SpanStyle(color = TerminalColors.promptTime)) {
                append(currentTime)
            }
            withStyle(SpanStyle(color = TerminalColors.promptSymbol, fontWeight = FontWeight.Bold)) {
                append(" $ ")
            }
        }

        Text(
            text = promptString,
            fontFamily = terminalFont,
            fontSize = 13.sp
        )

        // Input with cursor visualization
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ensure cursor position is within bounds
            val safeCursorPosition = cursorPosition.coerceIn(0, input.length)

            // Text before cursor
            val textBeforeCursor = input.take(safeCursorPosition)
            val textAfterCursor = input.drop(safeCursorPosition)

            Text(
                text = textBeforeCursor,
                color = TerminalColors.inputText,
                fontFamily = terminalFont,
                fontSize = 13.sp
            )

            // Blinking cursor
            if (!isExecuting) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .alpha(cursorAlpha)
                        .background(TerminalColors.cursor)
                )
            }

            // Text after cursor
            Text(
                text = textAfterCursor,
                color = TerminalColors.inputText,
                fontFamily = terminalFont,
                fontSize = 13.sp
            )
        }

        // Loading indicator when executing
        if (isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 8.dp),
                color = TerminalColors.neonCyan,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    shellSettingsManager: com.example.mentra.shell.settings.ShellSettingsManager? = null
) {
    // Get settings from manager or use defaults
    val inShellIncomingCall by shellSettingsManager?.inShellIncomingCall?.collectAsState()
        ?: remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalColors.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = TerminalColors.neonCyan
                )
                Text(
                    "Shell Settings",
                    color = TerminalColors.inputText,
                    fontFamily = terminalFont,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // In-Shell Incoming Call Toggle
                SettingsToggleItem(
                    icon = Icons.Default.Phone,
                    title = "In-Shell Incoming Calls",
                    subtitle = "Handle incoming calls via shell commands\nA=Answer R=Reject Q=Quick Reply",
                    checked = inShellIncomingCall,
                    onCheckedChange = { enabled ->
                        shellSettingsManager?.setInShellIncomingCall(enabled)
                    }
                )

                // Divider
                HorizontalDivider(
                    color = TerminalColors.muted.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Other settings
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = "Kali Linux (Default)",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.TextFields,
                    title = "Font Size",
                    subtitle = "13sp",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Default.History,
                    title = "History",
                    subtitle = "Keep last 1000 commands",
                    onClick = { }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TerminalColors.neonCyan
                )
            ) {
                Text("Close", fontFamily = terminalFont)
            }
        }
    )
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TerminalColors.terminalBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) TerminalColors.neonGreen else TerminalColors.neonPurple,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TerminalColors.inputText,
                    fontFamily = terminalFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TerminalColors.muted,
                    fontFamily = terminalFont,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TerminalColors.neonGreen,
                    checkedTrackColor = TerminalColors.neonGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = TerminalColors.muted,
                    uncheckedTrackColor = TerminalColors.terminalBg
                )
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = TerminalColors.terminalBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TerminalColors.neonPurple,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TerminalColors.inputText,
                    fontFamily = terminalFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TerminalColors.muted,
                    fontFamily = terminalFont,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TerminalColors.muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getCurrentTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

/**
 * ViewModel for shell screen with messaging integration
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val shellEngine: ShellEngine,
    private val messagingHandler: ShellMessagingCommandHandler,
    private val messagingService: ShellMessagingService,
    private val callingHandler: com.example.mentra.shell.calling.ShellCallingCommandHandler,
    val shellSettingsManager: com.example.mentra.shell.settings.ShellSettingsManager,
    private val shellIncomingCallHandler: com.example.mentra.shell.calling.ShellIncomingCallHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    private val _contactPickerRequest = MutableStateFlow<ContactPickerRequest?>(null)
    val contactPickerRequest: StateFlow<ContactPickerRequest?> = _contactPickerRequest.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    // Calling contact picker state
    private val _callingContactPickerRequest = MutableStateFlow<CallingContactPickerRequest?>(null)
    val callingContactPickerRequest: StateFlow<CallingContactPickerRequest?> = _callingContactPickerRequest.asStateFlow()

    // Calculator UI state
    private val _showCalculatorUI = MutableStateFlow(false)
    val showCalculatorUI: StateFlow<Boolean> = _showCalculatorUI.asStateFlow()

    // Calendar UI state
    private val _showCalendarUI = MutableStateFlow(false)
    val showCalendarUI: StateFlow<Boolean> = _showCalendarUI.asStateFlow()

    // App Picker UI state
    private val _showAppPicker = MutableStateFlow(false)
    val showAppPicker: StateFlow<Boolean> = _showAppPicker.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // Calling state
    val callState = callingHandler.callState

    // Active call session for terminal display
    val activeCallSession = callingHandler.activeCallSession

    // Command history for navigation
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1
    private var tempInput = "" // Stores current input when navigating history

    fun showCalculator() {
        _showCalculatorUI.value = true
    }

    fun dismissCalculatorUI() {
        _showCalculatorUI.value = false
    }

    fun showCalendar() {
        _showCalendarUI.value = true
    }

    fun dismissCalendarUI() {
        _showCalendarUI.value = false
    }

    fun showAppPicker() {
        _showAppPicker.value = true
    }

    fun dismissAppPicker() {
        _showAppPicker.value = false
    }

    fun navigateToMessages() {
        _navigationEvent.value = NavigationEvent.MESSAGES
    }

    fun navigateToDialer() {
        _navigationEvent.value = NavigationEvent.DIALER
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun endCall() {
        callingHandler.endActiveCall()
    }

    /**
     * Navigate to previous command in history (arrow up)
     */
    fun navigateHistoryUp() {
        if (commandHistory.isEmpty()) return

        if (historyIndex == -1) {
            // Save current input
            tempInput = _uiState.value.input
            historyIndex = commandHistory.size - 1
        } else if (historyIndex > 0) {
            historyIndex--
        }

        _uiState.value = _uiState.value.copy(input = commandHistory[historyIndex])
    }

    /**
     * Navigate to next command in history (arrow down)
     */
    fun navigateHistoryDown() {
        if (historyIndex == -1) return

        if (historyIndex < commandHistory.size - 1) {
            historyIndex++
            _uiState.value = _uiState.value.copy(input = commandHistory[historyIndex])
        } else {
            // Return to original input
            historyIndex = -1
            _uiState.value = _uiState.value.copy(input = tempInput)
        }
    }

    /**
     * Cancel current operation (Ctrl+C)
     */
    fun cancelOperation() {
        // Always reset messaging and calling handlers regardless of isExecuting state
        messagingHandler.reset()
        callingHandler.resetState()

        // Cancel any quick reply in progress
        val quickReplyOutputs = shellIncomingCallHandler.cancelQuickReply()
        quickReplyOutputs.forEach { output ->
            addOutput(OutputItem(
                command = "^C",
                timestamp = getCurrentTimeString(),
                result = ShellResult(
                    status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                    message = output.text
                )
            ))
        }

        // Dismiss any open contact pickers
        _contactPickerRequest.value = null
        _callingContactPickerRequest.value = null

        if (_uiState.value.isExecuting) {
            _uiState.value = _uiState.value.copy(isExecuting = false, input = "")
            addOutput(OutputItem(
                command = "^C",
                timestamp = getCurrentTimeString(),
                result = ShellResult(
                    status = com.example.mentra.shell.models.ResultStatus.FAILURE,
                    message = "Operation cancelled"
                )
            ))
        } else {
            // Check if there's an active conversation state that needs cancellation
            val conversationState = messagingHandler.conversationState.value
            val callingState = callingHandler.callState.value

            if (conversationState != com.example.mentra.shell.messaging.ConversationState.None ||
                callingState != com.example.mentra.shell.calling.CallState.Idle) {
                // Show cancellation message
                addOutput(OutputItem(
                    command = "^C",
                    timestamp = getCurrentTimeString(),
                    result = ShellResult(
                        status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                        message = "Operation cancelled"
                    )
                ))
            }
            // Clear current input
            _uiState.value = _uiState.value.copy(input = "")
        }
        historyIndex = -1
    }

    // Shell incoming call state
    val shellIncomingCallState = shellIncomingCallHandler.shellCallState

    init {
        // Shell starts empty - users can type 'help' for commands

        // Observe contact picker requests from messaging handler
        viewModelScope.launch {
            messagingHandler.showContactPicker.collect { request ->
                if (request != null) {
                    _contactPickerRequest.value = request
                    loadContacts()
                }
            }
        }

        // Observe calling state for ShowContactModal
        viewModelScope.launch {
            callingHandler.callState.collect { state ->
                if (state is com.example.mentra.shell.calling.CallState.ShowContactModal) {
                    _callingContactPickerRequest.value = CallingContactPickerRequest(
                        title = "Select Contact to Call"
                    )
                    loadContacts()
                }
            }
        }

        // Observe call ended events (when other party ends call)
        viewModelScope.launch {
            callingHandler.callEndedEvent.collect { callInfo ->
                // Add call ended message to shell output
                addOutput(OutputItem(
                    command = "",
                    timestamp = getCurrentTimeString(),
                    result = ShellResult(
                        status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                        message = "📵 Call ended by ${callInfo.endedBy}: ${callInfo.contactName ?: callInfo.phoneNumber} [${callInfo.duration}]"
                    )
                ))
            }
        }

        // Observe shell incoming call state changes
        viewModelScope.launch {
            shellIncomingCallHandler.shellCallState.collect { state ->
                // Display incoming call notification in shell if enabled
                if (shellIncomingCallHandler.isShellIncomingCallEnabled()) {
                    val outputs = shellIncomingCallHandler.getCallStateDisplay()
                    outputs.forEach { output ->
                        addOutput(OutputItem(
                            command = "",
                            timestamp = getCurrentTimeString(),
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    com.example.mentra.shell.models.ShellOutputType.PROMPT -> com.example.mentra.shell.models.ResultStatus.REQUIRES_CONFIRMATION
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                }
            }
        }
    }

    /**
     * Called when shell screen becomes visible
     * Checks for any pending incoming calls and displays them
     */
    fun onShellVisible() {
        viewModelScope.launch {
            // Check for any pending incoming call that needs to be displayed
            if (shellIncomingCallHandler.isShellIncomingCallEnabled()) {
                val pendingOutputs = shellIncomingCallHandler.getPendingCallDisplay()
                if (pendingOutputs.isNotEmpty()) {
                    android.util.Log.d("ShellViewModel", "Shell became visible with pending call, displaying ${pendingOutputs.size} outputs")
                    pendingOutputs.forEach { output ->
                        addOutput(OutputItem(
                            command = "",
                            timestamp = getCurrentTimeString(),
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    com.example.mentra.shell.models.ShellOutputType.PROMPT -> com.example.mentra.shell.models.ResultStatus.REQUIRES_CONFIRMATION
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                    // Refresh state to ensure UI is in sync
                    shellIncomingCallHandler.refreshCallState()
                }
            }
        }
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = messagingService.getAllContacts()
        }
    }

    fun onContactSelected(contact: Contact, number: String) {
        viewModelScope.launch {
            messagingHandler.onContactSelected(contact, number)
            _contactPickerRequest.value = null

            // Add feedback to shell
            addOutput(OutputItem(
                command = "contact_selected",
                timestamp = getCurrentTimeString(),
                result = ShellResult(
                    status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                    message = "✓ Selected: ${contact.name} ($number)"
                )
            ))

            // Continue the conversation flow
            val outputs = messagingHandler.handleCommand("")
            outputs.forEach { output ->
                addOutput(OutputItem(
                    command = "",
                    timestamp = getCurrentTimeString(),
                    result = ShellResult(
                        status = when (output.type) {
                            com.example.mentra.shell.messaging.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                            com.example.mentra.shell.messaging.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                            com.example.mentra.shell.messaging.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                            else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                        },
                        message = output.text
                    )
                ))
            }
        }
    }

    fun onContactPickerDismissed() {
        messagingHandler.onContactPickerCancelled()
        _contactPickerRequest.value = null

        addOutput(OutputItem(
            command = "",
            timestamp = getCurrentTimeString(),
            result = ShellResult(
                status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                message = "Contact selection cancelled."
            )
        ))
    }

    /**
     * Handle contact selection for calling
     */
    fun onCallingContactSelected(contact: Contact, number: String) {
        viewModelScope.launch {
            _callingContactPickerRequest.value = null

            // Add feedback to shell
            addOutput(OutputItem(
                command = "contact_selected",
                timestamp = getCurrentTimeString(),
                result = ShellResult(
                    status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                    message = "✓ Selected: ${contact.name} ($number)"
                )
            ))

            // Set state to await SIM selection with the selected contact
            callingHandler.setCallTarget(number, contact.name)

            // Show SIM selection prompt
            addOutput(OutputItem(
                command = "",
                timestamp = getCurrentTimeString(),
                result = ShellResult(
                    status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                    message = "📞 Calling ${contact.name}\nNumber: $number\n\nSelect SIM:\n1. SIM 1\n2. SIM 2"
                )
            ))
        }
    }

    /**
     * Handle contact selection for calling with SIM already selected (from new dialog)
     */
    fun onCallingContactSelectedWithSim(contact: Contact, number: String, simSlot: Int) {
        viewModelScope.launch {
            _callingContactPickerRequest.value = null

            // Place the call directly
            val outputs = callingHandler.placeCallDirectly(number, contact.name, simSlot)

            outputs.forEach { output ->
                addOutput(OutputItem(
                    command = "",
                    timestamp = getCurrentTimeString(),
                    result = ShellResult(
                        status = when (output.type) {
                            com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                            com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                            com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                            else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                        },
                        message = output.text
                    )
                ))
            }
        }
    }

    /**
     * Handle calling contact picker dismissed
     */
    fun onCallingContactPickerDismissed() {
        _callingContactPickerRequest.value = null
        callingHandler.resetState()

        addOutput(OutputItem(
            command = "",
            timestamp = getCurrentTimeString(),
            result = ShellResult(
                status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                message = "Contact selection cancelled."
            )
        ))
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(input = text)
    }

    fun executeCommand() {
        val command = _uiState.value.input.trim()
        if (command.isEmpty()) return

        // Add to command history (avoid duplicates of last command)
        if (commandHistory.isEmpty() || commandHistory.last() != command) {
            commandHistory.add(command)
            // Keep history size reasonable
            if (commandHistory.size > 1000) {
                commandHistory.removeAt(0)
            }
        }
        historyIndex = -1 // Reset history navigation

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExecuting = true)

            try {
                // Handle special commands
                if (command == "clear") {
                    _uiState.value = ShellUiState()
                    messagingHandler.reset()
                    return@launch
                }

                val timestamp = getCurrentTimeString()

                // ═══════════════════════════════════════════════════════════════
                // PRIORITY 1: Check if we have an active shell incoming call
                // If in-shell incoming call is enabled, handle A/R/Q commands
                // ═══════════════════════════════════════════════════════════════
                if (shellIncomingCallHandler.hasActiveIncomingCall()) {
                    val (handled, outputs) = shellIncomingCallHandler.handleCallInput(command)
                    if (handled) {
                        outputs.forEach { output ->
                            addOutput(OutputItem(
                                command = if (outputs.indexOf(output) == 0) command else "",
                                timestamp = timestamp,
                                result = ShellResult(
                                    status = when (output.type) {
                                        com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                        com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                        com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                        com.example.mentra.shell.models.ShellOutputType.PROMPT -> com.example.mentra.shell.models.ResultStatus.REQUIRES_CONFIRMATION
                                        else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    },
                                    message = output.text
                                )
                            ))
                        }
                        _uiState.value = _uiState.value.copy(input = "", isExecuting = false)
                        return@launch
                    }
                }

                // Check if we're in an active call (for call control) - must have BOTH InCall state AND active session
                val isInActiveCall = callState.value is com.example.mentra.shell.calling.CallState.InCall &&
                                     callingHandler.isInActiveCall()

                // Check if we're in a calling conversation (awaiting response like SIM selection)
                val isInCallingConversation = callState.value !is com.example.mentra.shell.calling.CallState.Idle &&
                                              callState.value !is com.example.mentra.shell.calling.CallState.InCall

                if (isInActiveCall) {
                    // Handle call control input (0=end, 1=speaker, 3=mute, etc.)
                    val outputs = callingHandler.handleCallControlInput(command)

                    outputs.forEach { output ->
                        addOutput(OutputItem(
                            command = if (outputs.indexOf(output) == 0) command else "",
                            timestamp = timestamp,
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                } else if (isInCallingConversation) {
                    // Handle follow-up inputs for calling (like SIM selection)
                    val outputs = callingHandler.handleResponse(command)

                    outputs.forEach { output ->
                        addOutput(OutputItem(
                            command = if (outputs.indexOf(output) == 0) command else "",
                            timestamp = timestamp,
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                } else if (callingHandler.isCallingCommand(command)) {
                    // Initial calling command
                    val outputs = callingHandler.handleCommand(command)

                    outputs.forEach { output ->
                        addOutput(OutputItem(
                            command = if (outputs.indexOf(output) == 0) command else "",
                            timestamp = timestamp,
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.models.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.models.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.models.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                } else if (messagingHandler.isInConversation() || messagingHandler.isMessagingCommand(command)) {
                    // Check if we're in a messaging conversation or this is a messaging command
                    val outputs = messagingHandler.handleCommand(command)

                    outputs.forEach { output ->
                        addOutput(OutputItem(
                            command = if (outputs.indexOf(output) == 0) command else "",
                            timestamp = timestamp,
                            result = ShellResult(
                                status = when (output.type) {
                                    com.example.mentra.shell.messaging.ShellOutputType.SUCCESS -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                    com.example.mentra.shell.messaging.ShellOutputType.ERROR -> com.example.mentra.shell.models.ResultStatus.FAILURE
                                    com.example.mentra.shell.messaging.ShellOutputType.WARNING -> com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND
                                    else -> com.example.mentra.shell.models.ResultStatus.SUCCESS
                                },
                                message = output.text
                            )
                        ))
                    }
                } else {
                    // Regular shell command
                    val result = shellEngine.execute(command)

                    // Check if screen should be cleared (from package manager)
                    if (result.data == "clear_screen" || result.message == "CLEAR_SCREEN") {
                        _uiState.value = ShellUiState()
                        messagingHandler.reset()
                        _uiState.value = _uiState.value.copy(isExecuting = false)
                        return@launch
                    }

                    // Check if calculator UI should be shown
                    if (result.data == "SHOW_CALCULATOR_UI") {
                        showCalculator()
                    }

                    // Check if calendar UI should be shown
                    if (result.data == "SHOW_CALENDAR_UI") {
                        showCalendar()
                    }

                    // Check if app picker should be shown
                    if (result.data == "show_app_picker") {
                        showAppPicker()
                        _uiState.value = _uiState.value.copy(input = "")
                        _uiState.value = _uiState.value.copy(isExecuting = false)
                        return@launch
                    }

                    // Check for navigation commands
                    if (result.data == "navigate_messages") {
                        navigateToMessages()
                        _uiState.value = _uiState.value.copy(input = "")
                        _uiState.value = _uiState.value.copy(isExecuting = false)
                        return@launch
                    }

                    if (result.data == "navigate_dialer") {
                        navigateToDialer()
                        _uiState.value = _uiState.value.copy(input = "")
                        _uiState.value = _uiState.value.copy(isExecuting = false)
                        return@launch
                    }

                    addOutput(OutputItem(
                        command = command,
                        timestamp = timestamp,
                        result = result
                    ))
                }

                _uiState.value = _uiState.value.copy(input = "")
            } finally {
                _uiState.value = _uiState.value.copy(isExecuting = false)
            }
        }
    }

    fun clearHistory() {
        _uiState.value = ShellUiState()
        shellEngine.clearHistory()
        messagingHandler.reset()
    }

    private fun addOutput(item: OutputItem) {
        val currentOutput = _uiState.value.output.toMutableList()
        currentOutput.add(item)
        _uiState.value = _uiState.value.copy(output = currentOutput)
    }
}

/**
 * UI state
 */
data class ShellUiState(
    val input: String = "",
    val output: List<OutputItem> = emptyList(),
    val isExecuting: Boolean = false
)

/**
 * Output item with timestamp
 */
data class OutputItem(
    val command: String,
    val timestamp: String = "",
    val result: ShellResult? = null
)

/**
 * Calling contact picker request
 */
data class CallingContactPickerRequest(
    val title: String = "Select Contact to Call"
)

/**
 * Navigation events from shell
 */
enum class NavigationEvent {
    MESSAGES,
    DIALER
}

