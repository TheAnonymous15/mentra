package com.example.mentra.shell.models

/**
 * Represents a parsed shell command
 */
data class ShellCommand(
    val raw: String,                    // Original input
    val verb: String,                   // Primary action (open, call, play, etc.)
    val target: String?,                // Target of action (app name, contact, etc.)
    val entity: String?,                // Specific entity (song name, message text, etc.)
    val params: Map<String, String> = emptyMap(),  // Additional parameters
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents an action to be executed
 */
data class ShellAction(
    val type: ActionType,
    val verb: String = "",               // Original verb (for system commands)
    val target: String?,
    val entity: String?,
    val params: Map<String, String> = emptyMap(),
    val confidence: Float = 1.0f,      // Confidence score (0.0 - 1.0)
    val requiresConfirmation: Boolean = false
)

/**
 * Result of command execution
 */
data class ShellResult(
    val status: ResultStatus,
    val message: String,
    val data: Any? = null,
    val executionTime: Long = 0,
    val error: Throwable? = null
)

/**
 * Types of actions the shell can perform
 */
enum class ActionType {
    // System actions
    OPEN_APP,
    LAUNCH_ACTIVITY,
    OPEN_SETTINGS,

    // Communication
    MAKE_CALL,
    SEND_SMS,
    SEND_MMS,
    OPEN_CONTACTS,

    // Media
    PLAY_MUSIC,
    PLAY_VIDEO,
    PAUSE_MEDIA,
    STOP_MEDIA,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    SET_VOLUME,

    // Navigation
    NAVIGATE_TO,
    SHOW_ROUTE,
    GET_DIRECTIONS,

    // Information queries
    SHOW_BATTERY,
    SHOW_STORAGE,
    SHOW_NETWORK,
    SHOW_APPS,
    SHOW_TIME,
    SHOW_DATE,
    SHOW_STEPS,
    SHOW_DEVICE,

    // File operations
    LIST_FILES,
    READ_FILE,
    WRITE_FILE,
    DELETE_FILE,

    // Shell operations
    SET_VARIABLE,
    GET_VARIABLE,
    RUN_SCRIPT,

    // System-wide operations (via SystemOrchestrator)
    SYSTEM_COMMAND,

    // Unknown
    UNKNOWN
}

/**
 * Result status
 */
enum class ResultStatus {
    SUCCESS,
    FAILURE,
    PARTIAL,
    REQUIRES_PERMISSION,
    REQUIRES_CONFIRMATION,
    NOT_FOUND,
    INVALID_COMMAND
}

/**
 * Action capabilities - what permissions are needed
 */
enum class ActionCapability {
    // No special permissions needed
    BASIC,

    // Android permissions
    CALL_PHONE,
    SEND_SMS,
    READ_CONTACTS,
    WRITE_CONTACTS,
    ACCESS_LOCATION,
    READ_STORAGE,
    WRITE_STORAGE,
    RECORD_AUDIO,
    CAMERA,

    // Shizuku permissions (elevated)
    INSTALL_PACKAGES,
    UNINSTALL_PACKAGES,
    GRANT_PERMISSIONS,
    MODIFY_SETTINGS,

    // Dangerous operations
    DELETE_DATA,
    FACTORY_RESET
}

/**
 * Shell session context
 */
data class ShellContext(
    val sessionId: String,
    val userId: String = "default",
    val workingDirectory: String = "/",
    val environment: MutableMap<String, String> = mutableMapOf(),
    val aliases: MutableMap<String, String> = mutableMapOf(),
    val lastCommand: ShellCommand? = null,
    val lastResult: ShellResult? = null,
    val history: MutableList<ShellCommand> = mutableListOf()
) {
    /**
     * Add command to history
     */
    fun addToHistory(command: ShellCommand) {
        history.add(command)
        if (history.size > 1000) {
            history.removeFirst()
        }
    }

    /**
     * Get last N commands
     */
    fun getRecentHistory(count: Int = 10): List<ShellCommand> {
        return history.takeLast(count)
    }

    /**
     * Set environment variable
     */
    fun setEnv(key: String, value: String) {
        environment[key] = value
    }

    /**
     * Get environment variable
     */
    fun getEnv(key: String): String? {
        return environment[key]
    }

    /**
     * Set alias
     */
    fun setAlias(alias: String, target: String) {
        aliases[alias] = target
    }

    /**
     * Resolve alias
     */
    fun resolveAlias(alias: String): String {
        return aliases[alias] ?: alias
    }
}

/**
 * Command execution options
 */
data class ExecutionOptions(
    val requireConfirmation: Boolean = false,
    val timeout: Long = 30000, // 30 seconds
    val safeMode: Boolean = false,
    val verbose: Boolean = false,
    val dryRun: Boolean = false
)

/**
 * Shell output for display in the terminal
 */
data class ShellOutput(
    val text: String,
    val type: ShellOutputType = ShellOutputType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of shell output
 */
enum class ShellOutputType {
    SUCCESS,     // Green - successful operation
    ERROR,       // Red - error message
    WARNING,     // Yellow - warning message
    INFO,        // White/Gray - information
    PROMPT,      // Cyan - user prompt
    COMMAND      // Blue - command echo
}
