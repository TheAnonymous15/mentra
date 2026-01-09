package com.example.mentra.shell.core

import com.example.mentra.shell.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main shell engine
 * Orchestrates all shell components
 */
@Singleton
class ShellEngine @Inject constructor(
    private val parser: CommandParser,
    private val executor: CommandExecutor,
    private val contextManager: ContextManager
) {

    private val _lastResult = MutableStateFlow<ShellResult?>(null)
    val lastResult: StateFlow<ShellResult?> = _lastResult.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    /**
     * Execute command
     */
    suspend fun execute(
        command: String,
        options: ExecutionOptions = ExecutionOptions()
    ): ShellResult {
        _isExecuting.value = true

        return try {
            val result = executor.execute(command, options)
            _lastResult.value = result
            result
        } finally {
            _isExecuting.value = false
        }
    }

    /**
     * Execute multiple commands
     */
    suspend fun executeMultiple(commands: List<String>): List<ShellResult> {
        return commands.map { execute(it) }
    }

    /**
     * Get command history
     */
    fun getHistory(count: Int = 10): List<ShellCommand> {
        return contextManager.getHistory(count)
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        contextManager.clearHistory()
    }

    /**
     * Get environment variables
     */
    fun getEnvironment(): Map<String, String> {
        return contextManager.getAllEnv()
    }

    /**
     * Set environment variable
     */
    fun setEnv(key: String, value: String) {
        contextManager.setEnv(key, value)
    }

    /**
     * Get aliases
     */
    fun getAliases(): Map<String, String> {
        return contextManager.getAllAliases()
    }

    /**
     * Set alias
     */
    fun setAlias(alias: String, command: String) {
        contextManager.setAlias(alias, command)
    }

    /**
     * Get session ID
     */
    fun getSessionId(): String {
        return contextManager.getSessionId()
    }

    /**
     * Reset session
     */
    fun reset() {
        contextManager.reset()
        _lastResult.value = null
    }

    /**
     * Export session context
     */
    fun exportContext(): String {
        return contextManager.exportContext()
    }

    /**
     * Import session context
     */
    fun importContext(data: String) {
        contextManager.importContext(data)
    }
}

