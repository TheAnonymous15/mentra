package com.example.mentra.shell.core

import com.example.mentra.shell.models.ShellCommand
import com.example.mentra.shell.models.ShellContext
import com.example.mentra.shell.models.ShellResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages shell session context
 * Handles environment variables, aliases, history, and session state
 */
@Singleton
class ContextManager @Inject constructor() {

    private val _currentContext = MutableStateFlow(createNewContext())
    val currentContext: StateFlow<ShellContext> = _currentContext.asStateFlow()

    /**
     * Create a new shell context
     */
    private fun createNewContext(): ShellContext {
        return ShellContext(
            sessionId = UUID.randomUUID().toString(),
            workingDirectory = "/",
            environment = getDefaultEnvironment(),
            aliases = getDefaultAliases()
        )
    }

    /**
     * Get default environment variables
     */
    private fun getDefaultEnvironment(): MutableMap<String, String> {
        return mutableMapOf(
            "HOME" to "/",
            "USER" to "default",
            "SHELL" to "mentra",
            "LANG" to "en_US",
            "PATH" to "/bin:/usr/bin",
            "PWD" to "/"
        )
    }

    /**
     * Get default aliases
     */
    private fun getDefaultAliases(): MutableMap<String, String> {
        return mutableMapOf(
            "ll" to "ls -la",
            "la" to "ls -a",
            ".." to "cd ..",
            "~" to "cd /",
            "h" to "history",
            "c" to "clear"
        )
    }

    /**
     * Add command to history
     */
    fun addToHistory(command: ShellCommand) {
        val context = _currentContext.value
        context.addToHistory(command)
        _currentContext.value = context.copy(lastCommand = command)
    }

    /**
     * Update last result
     */
    fun updateLastResult(result: ShellResult) {
        val context = _currentContext.value
        _currentContext.value = context.copy(lastResult = result)
    }

    /**
     * Get recent history
     */
    fun getHistory(count: Int = 10): List<ShellCommand> {
        return _currentContext.value.getRecentHistory(count)
    }

    /**
     * Get full history
     */
    fun getAllHistory(): List<ShellCommand> {
        return _currentContext.value.history.toList()
    }

    /**
     * Clear history
     */
    fun clearHistory() {
        val context = _currentContext.value
        context.history.clear()
        _currentContext.value = context.copy()
    }

    /**
     * Set environment variable
     */
    fun setEnv(key: String, value: String) {
        val context = _currentContext.value
        context.setEnv(key, value)
        _currentContext.value = context.copy()
    }

    /**
     * Get environment variable
     */
    fun getEnv(key: String): String? {
        return _currentContext.value.getEnv(key)
    }

    /**
     * Get all environment variables
     */
    fun getAllEnv(): Map<String, String> {
        return _currentContext.value.environment.toMap()
    }

    /**
     * Set alias
     */
    fun setAlias(alias: String, target: String) {
        val context = _currentContext.value
        context.setAlias(alias, target)
        _currentContext.value = context.copy()
    }

    /**
     * Resolve alias
     */
    fun resolveAlias(alias: String): String {
        return _currentContext.value.resolveAlias(alias)
    }

    /**
     * Get all aliases
     */
    fun getAllAliases(): Map<String, String> {
        return _currentContext.value.aliases.toMap()
    }

    /**
     * Remove alias
     */
    fun removeAlias(alias: String) {
        val context = _currentContext.value
        context.aliases.remove(alias)
        _currentContext.value = context.copy()
    }

    /**
     * Change working directory
     */
    fun changeDirectory(path: String) {
        val context = _currentContext.value
        val newPath = resolvePath(context.workingDirectory, path)
        _currentContext.value = context.copy(workingDirectory = newPath)
    }

    /**
     * Get current working directory
     */
    fun getWorkingDirectory(): String {
        return _currentContext.value.workingDirectory
    }

    /**
     * Resolve relative path to absolute
     */
    private fun resolvePath(current: String, path: String): String {
        return when {
            path == "/" -> "/"
            path == "~" -> "/"
            path == ".." -> {
                val parts = current.split("/").filter { it.isNotEmpty() }
                if (parts.isEmpty()) "/" else "/" + parts.dropLast(1).joinToString("/")
            }
            path.startsWith("/") -> path
            else -> {
                if (current == "/") "/$path" else "$current/$path"
            }
        }
    }

    /**
     * Reset context (new session)
     */
    fun reset() {
        _currentContext.value = createNewContext()
    }

    /**
     * Get session ID
     */
    fun getSessionId(): String {
        return _currentContext.value.sessionId
    }

    /**
     * Get last command
     */
    fun getLastCommand(): ShellCommand? {
        return _currentContext.value.lastCommand
    }

    /**
     * Get last result
     */
    fun getLastResult(): ShellResult? {
        return _currentContext.value.lastResult
    }

    /**
     * Repeat last command (!! in shell)
     */
    fun getRepeatCommand(): ShellCommand? {
        return _currentContext.value.lastCommand
    }

    /**
     * Export context to string (for saving)
     */
    fun exportContext(): String {
        val context = _currentContext.value
        val sb = StringBuilder()

        // Environment variables
        context.environment.forEach { (key, value) ->
            sb.appendLine("export $key=\"$value\"")
        }

        // Aliases
        context.aliases.forEach { (alias, target) ->
            sb.appendLine("alias $alias=\"$target\"")
        }

        return sb.toString()
    }

    /**
     * Import context from string
     */
    fun importContext(data: String) {
        val lines = data.lines()
        val context = _currentContext.value

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("export ") -> {
                    // Parse: export KEY="value"
                    val parts = trimmed.substring(7).split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"")
                        context.setEnv(key, value)
                    }
                }
                trimmed.startsWith("alias ") -> {
                    // Parse: alias name="value"
                    val parts = trimmed.substring(6).split("=", limit = 2)
                    if (parts.size == 2) {
                        val alias = parts[0].trim()
                        val target = parts[1].trim().removeSurrounding("\"")
                        context.setAlias(alias, target)
                    }
                }
            }
        }

        _currentContext.value = context.copy()
    }
}

