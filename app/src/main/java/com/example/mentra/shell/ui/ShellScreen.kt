package com.example.mentra.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.shell.core.ShellEngine
import com.example.mentra.shell.models.ShellResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shell terminal screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    viewModel: ShellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(uiState.output.size) {
        if (uiState.output.isNotEmpty()) {
            listState.animateScrollToItem(uiState.output.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentra Shell") },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Output area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                state = listState
            ) {
                items(uiState.output) { entry ->
                    OutputEntry(entry)
                }
            }

            // Input area
            InputArea(
                input = uiState.input,
                onInputChange = { viewModel.updateInput(it) },
                onExecute = { viewModel.executeCommand() },
                isExecuting = uiState.isExecuting
            )
        }
    }
}

@Composable
private fun OutputEntry(entry: OutputItem) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // Command
        Text(
            text = "$ ${entry.command}",
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium
        )

        // Result
        if (entry.result != null) {
            Text(
                text = entry.result.message,
                color = when (entry.result.status) {
                    com.example.mentra.shell.models.ResultStatus.SUCCESS -> Color.White
                    com.example.mentra.shell.models.ResultStatus.FAILURE -> Color.Red
                    com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND -> Color.Yellow
                    else -> Color.Gray
                },
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun InputArea(
    input: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    isExecuting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$",
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )

        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onExecute() }
            ),
            singleLine = true,
            enabled = !isExecuting
        )

        IconButton(
            onClick = onExecute,
            enabled = !isExecuting && input.isNotBlank()
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Green
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Execute",
                    tint = Color.Green
                )
            }
        }
    }
}

/**
 * ViewModel for shell screen
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val shellEngine: ShellEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    init {
        // Add welcome message
        addOutput(OutputItem(
            command = "welcome",
            result = ShellResult(
                status = com.example.mentra.shell.models.ResultStatus.SUCCESS,
                message = "Mentra AI Shell v1.0\nType 'help' for available commands."
            )
        ))
    }

    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(input = text)
    }

    fun executeCommand() {
        val command = _uiState.value.input.trim()
        if (command.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExecuting = true)

            try {
                // Handle special commands
                if (command == "clear") {
                    _uiState.value = ShellUiState()
                    return@launch
                }

                val result = shellEngine.execute(command)

                addOutput(OutputItem(
                    command = command,
                    result = result
                ))

                _uiState.value = _uiState.value.copy(input = "")
            } finally {
                _uiState.value = _uiState.value.copy(isExecuting = false)
            }
        }
    }

    fun clearHistory() {
        _uiState.value = ShellUiState()
        shellEngine.clearHistory()
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
 * Output item
 */
data class OutputItem(
    val command: String,
    val result: ShellResult? = null
)

