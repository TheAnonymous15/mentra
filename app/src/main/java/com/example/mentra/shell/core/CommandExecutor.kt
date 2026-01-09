package com.example.mentra.shell.core

import com.example.mentra.shell.actions.ActionRouter
import com.example.mentra.shell.models.*
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes parsed shell commands
 * Routes commands to appropriate action handlers
 */
@Singleton
class CommandExecutor @Inject constructor(
    private val parser: CommandParser,
    private val contextManager: ContextManager,
    private val actionRouter: ActionRouter
) {

    /**
     * Execute a command string
     */
    suspend fun execute(
        commandText: String,
        options: ExecutionOptions = ExecutionOptions()
    ): ShellResult {
        val startTime = System.currentTimeMillis()

        try {
            // Parse command
            val command = parser.parse(commandText)

            // Add to history
            contextManager.addToHistory(command)

            // Validate command
            if (!parser.validate(command)) {
                return ShellResult(
                    status = ResultStatus.INVALID_COMMAND,
                    message = "Invalid command syntax",
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Handle built-in shell commands
            val builtInResult = handleBuiltInCommands(command)
            if (builtInResult != null) {
                contextManager.updateLastResult(builtInResult)
                return builtInResult
            }

            // Convert to action
            val action = commandToAction(command)

            // Check if confirmation required
            if (options.requireConfirmation || action.requiresConfirmation) {
                return ShellResult(
                    status = ResultStatus.REQUIRES_CONFIRMATION,
                    message = "Command requires confirmation: ${command.raw}",
                    data = action,
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Dry run mode
            if (options.dryRun) {
                return ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "Dry run: Would execute ${action.type}",
                    data = action,
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Execute with timeout
            val result = withTimeout(options.timeout) {
                actionRouter.route(action)
            }

            contextManager.updateLastResult(result)
            return result.copy(executionTime = System.currentTimeMillis() - startTime)

        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            return ShellResult(
                status = ResultStatus.FAILURE,
                message = "Command timed out after ${options.timeout}ms",
                executionTime = System.currentTimeMillis() - startTime,
                error = e
            )
        } catch (e: Exception) {
            return ShellResult(
                status = ResultStatus.FAILURE,
                message = "Execution failed: ${e.message}",
                executionTime = System.currentTimeMillis() - startTime,
                error = e
            )
        }
    }

    /**
     * Handle built-in shell commands (cd, ls, history, etc.)
     */
    private fun handleBuiltInCommands(command: ShellCommand): ShellResult? {
        return when (command.verb.lowercase()) {
            "cd" -> {
                val path = command.target ?: "/"
                contextManager.changeDirectory(path)
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "Changed directory to ${contextManager.getWorkingDirectory()}"
                )
            }

            "pwd" -> {
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = contextManager.getWorkingDirectory()
                )
            }

            "history", "h" -> {
                val count = command.target?.toIntOrNull() ?: 10
                val history = contextManager.getHistory(count)
                val historyText = history.mapIndexed { index, cmd ->
                    "${index + 1}. ${cmd.raw}"
                }.joinToString("\n")

                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = historyText,
                    data = history
                )
            }

            "clear", "c" -> {
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "clear_screen",
                    data = "clear"
                )
            }

            "export" -> {
                // export VAR=value
                if (command.target?.contains("=") == true) {
                    val parts = command.target.split("=", limit = 2)
                    contextManager.setEnv(parts[0], parts.getOrNull(1) ?: "")
                    ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = "Set ${parts[0]}=${parts.getOrNull(1)}"
                    )
                } else {
                    ShellResult(
                        status = ResultStatus.INVALID_COMMAND,
                        message = "Usage: export VAR=value"
                    )
                }
            }

            "env" -> {
                val env = contextManager.getAllEnv()
                val envText = env.entries.joinToString("\n") { "${it.key}=${it.value}" }
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = envText,
                    data = env
                )
            }

            "alias" -> {
                if (command.target == null) {
                    // List aliases
                    val aliases = contextManager.getAllAliases()
                    val aliasText = aliases.entries.joinToString("\n") {
                        "alias ${it.key}='${it.value}'"
                    }
                    ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = aliasText,
                        data = aliases
                    )
                } else if (command.target.contains("=")) {
                    // Set alias
                    val parts = command.target.split("=", limit = 2)
                    contextManager.setAlias(parts[0], parts.getOrNull(1) ?: "")
                    ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = "Set alias ${parts[0]}='${parts.getOrNull(1)}'"
                    )
                } else {
                    ShellResult(
                        status = ResultStatus.INVALID_COMMAND,
                        message = "Usage: alias name=value"
                    )
                }
            }

            "!!" -> {
                // Repeat last command
                val lastCmd = contextManager.getLastCommand()
                if (lastCmd != null) {
                    ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = "repeat_last",
                        data = lastCmd.raw
                    )
                } else {
                    ShellResult(
                        status = ResultStatus.FAILURE,
                        message = "No previous command"
                    )
                }
            }

            "help", "?" -> {
                val helpText = """
                    Mentra AI Shell v1.0 - System Administrator
                    
                    ═══════════════════════════════════════
                    BUILT-IN COMMANDS:
                    ═══════════════════════════════════════
                    cd [path]         Change directory
                    pwd               Print working directory
                    ls [path]         List files
                    history [n]       Show command history
                    clear             Clear screen
                    export VAR=val    Set environment variable
                    env               Show all environment variables
                    alias name=cmd    Create command alias
                    !!                Repeat last command
                    
                    ═══════════════════════════════════════
                    POWER MANAGEMENT (Requires Shizuku):
                    ═══════════════════════════════════════
                    shutdown          Shutdown device
                    reboot            Reboot device
                    reboot --mode=recovery    Reboot to recovery
                    reboot --mode=bootloader  Reboot to bootloader
                    sleep             Put device to sleep
                    lock              Lock screen
                    
                    ═══════════════════════════════════════
                    NETWORK CONTROL (Requires Shizuku):
                    ═══════════════════════════════════════
                    wifi --state=on/off       Control WiFi
                    data --state=on/off       Control mobile data
                    airplane --state=on/off   Control airplane mode
                    bluetooth --state=on/off  Control Bluetooth
                    
                    ═══════════════════════════════════════
                    DISPLAY & BRIGHTNESS (Requires Shizuku):
                    ═══════════════════════════════════════
                    brightness <0-255>        Set brightness
                    timeout <seconds>         Set screen timeout
                    autobrightness --state=on/off  Auto-brightness
                    
                    ═══════════════════════════════════════
                    VOLUME & AUDIO (Requires Shizuku):
                    ═══════════════════════════════════════
                    volume --type=music/ring/notification <0-15>
                    mute --state=on/off       Mute all audio
                    
                    ═══════════════════════════════════════
                    INFORMATION QUERIES (No Shizuku needed):
                    ═══════════════════════════════════════
                    show battery      Battery status
                    show storage      Storage information
                    show device       Device information
                    show time         Current time
                    show date         Current date
                    show steps        Step count today
                    sysinfo           Complete system info
                    
                    ═══════════════════════════════════════
                    APP CONTROL:
                    ═══════════════════════════════════════
                    open <app>        Open application
                    settings [type]   Open settings (wifi/bluetooth/etc)
                    freeze <pkg>      Freeze/disable app (Shizuku)
                    unfreeze <pkg>    Unfreeze app (Shizuku)
                    hide <pkg>        Hide app from launcher (Shizuku)
                    unhide <pkg>      Unhide app (Shizuku)
                    
                    ═══════════════════════════════════════
                    PERFORMANCE (Requires Shizuku):
                    ═══════════════════════════════════════
                    performance high/balanced/powersave
                    batterysaver --state=on/off
                    clearram          Clear RAM/kill background
                    clearcache        Clear all app caches
                    
                    ═══════════════════════════════════════
                    COMMUNICATION:
                    ═══════════════════════════════════════
                    call <number>     Open dialer
                    message <number> "text"   Send SMS
                    
                    ═══════════════════════════════════════
                    FILE OPERATIONS:
                    ═══════════════════════════════════════
                    ls [path]         List files
                    cat <file>        Read file
                    rm <file>         Delete file
                    
                    ═══════════════════════════════════════
                    SETTINGS (Requires Shizuku):
                    ═══════════════════════════════════════
                    developermode --state=on/off
                    adb --state=on/off           USB debugging
                    animations <0.0-2.0>         Animation scale
                    location --state=on/off      Location services
                    dnd --state=on/off           Do Not Disturb
                    
                    ═══════════════════════════════════════
                    TIPS:
                    ═══════════════════════════════════════
                    • Use quotes for multi-word arguments
                    • Chain commands with ; or &&
                    • Most system commands require Shizuku
                    • Install Shizuku from Play Store for full power
                    
                    Total: 75+ commands available!
                    Type 'syshelp' for detailed system command guide.
                """.trimIndent()

                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = helpText
                )
            }

            "syshelp" -> {
                val sysHelpText = """
                    ═══════════════════════════════════════
                    MENTRA SYSTEM SHELL - COMPLETE GUIDE
                    ═══════════════════════════════════════
                    
                    ⚡ POWER COMMANDS:
                    shutdown                  Shutdown device
                    reboot                    Reboot device  
                    reboot --mode=recovery    Reboot to recovery
                    reboot --mode=bootloader  Reboot to bootloader/fastboot
                    reboot --mode=safe        Reboot in safe mode
                    sleep                     Put device to sleep
                    lock                      Lock screen
                    
                    🌐 NETWORK COMMANDS:
                    wifi --state=on           Enable WiFi
                    wifi --state=off          Disable WiFi
                    data --state=on           Enable mobile data
                    data --state=off          Disable mobile data
                    airplane --state=on       Enable airplane mode
                    airplane --state=off      Disable airplane mode
                    bluetooth --state=on      Enable Bluetooth
                    bluetooth --state=off     Disable Bluetooth
                    
                    💡 DISPLAY COMMANDS:
                    brightness 128            Set brightness (0-255)
                    brightness 255            Max brightness
                    autobrightness --state=on Enable auto-brightness
                    timeout 30                Screen timeout 30 seconds
                    
                    🔊 AUDIO COMMANDS:
                    volume --type=music 10    Set music volume
                    volume --type=ring 15     Set ring volume
                    volume --type=notification 8
                    mute --state=on           Mute all audio
                    
                    📱 APP MANAGEMENT:
                    freeze com.example.app    Disable/freeze app
                    unfreeze com.example.app  Enable/unfreeze app
                    hide com.example.app      Hide from launcher
                    unhide com.example.app    Unhide app
                    
                    🚀 PERFORMANCE:
                    performance high          High performance mode
                    performance balanced      Balanced mode
                    performance powersave     Power saving mode
                    batterysaver --state=on   Enable battery saver
                    clearram                  Clear RAM
                    clearcache                Clear all app caches
                    
                    ⚙️ SETTINGS:
                    developermode --state=on  Enable developer mode
                    adb --state=on            Enable USB debugging
                    animations 0.5            Set animation scale
                    location --state=off      Disable location
                    dnd --state=on            Enable Do Not Disturb
                    
                    📊 SYSTEM INFO:
                    show battery              Battery status
                    show storage              Storage info
                    show device               Device info
                    show time                 Current time
                    show date                 Current date
                    sysinfo                   Complete system info
                    
                    💾 FILE OPERATIONS:
                    ls /sdcard                List files
                    cat /sdcard/file.txt      Read file
                    rm /sdcard/file.txt       Delete file
                    
                    ⚠️  NOTE: Most system commands require Shizuku
                    Install Shizuku from Play Store for full power!
                    
                    Type 'help' for quick reference.
                """.trimIndent()

                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = sysHelpText
                )
            }

            else -> null // Not a built-in command
        }
    }

    /**
     * Convert ShellCommand to ShellAction
     */
    private fun commandToAction(command: ShellCommand): ShellAction {
        // Resolve aliases
        val target = command.target?.let { contextManager.resolveAlias(it) }

        // Map verb to action type
        val actionType = mapVerbToActionType(command.verb)

        return ShellAction(
            type = actionType,
            verb = command.verb,
            target = target,
            entity = command.entity,
            params = command.params,
            confidence = 1.0f,
            requiresConfirmation = isConfirmationRequired(actionType)
        )
    }

    /**
     * Map command verb to action type
     */
    private fun mapVerbToActionType(verb: String): ActionType {
        return when (verb.lowercase()) {
            // App & Settings
            "open", "launch", "start" -> ActionType.OPEN_APP
            "settings" -> ActionType.OPEN_SETTINGS

            // Communication
            "call", "dial" -> ActionType.MAKE_CALL
            "message", "sms", "text" -> ActionType.SEND_SMS

            // Media
            "play" -> ActionType.PLAY_MUSIC
            "pause" -> ActionType.PAUSE_MEDIA
            "stop" -> ActionType.STOP_MEDIA
            "next" -> ActionType.NEXT_TRACK
            "previous", "prev" -> ActionType.PREVIOUS_TRACK

            // Navigation
            "navigate", "goto", "go" -> ActionType.NAVIGATE_TO

            // Information queries
            "show", "display", "get" -> ActionType.SYSTEM_COMMAND // Will be handled by target

            // File operations
            "ls", "list" -> ActionType.LIST_FILES
            "cat", "read" -> ActionType.READ_FILE
            "write", "echo" -> ActionType.WRITE_FILE
            "rm", "delete", "del" -> ActionType.DELETE_FILE

            // System commands (all routed to AdvancedSystemActionHandler)
            "shutdown", "poweroff",
            "reboot", "restart",
            "sleep", "suspend",
            "lock",
            "wifi",
            "data", "mobiledata",
            "airplane", "airplanemode",
            "bluetooth", "bt",
            "brightness",
            "timeout", "screentimeout",
            "autobrightness",
            "volume",
            "mute",
            "settime",
            "settimezone",
            "autotime",
            "freeze", "disable",
            "unfreeze", "enable",
            "hide",
            "unhide",
            "performance", "perf",
            "batterysaver", "powersave",
            "clearram", "freeram",
            "clearcache",
            "dnd", "donotdisturb",
            "notify",
            "developermode", "devmode",
            "adb", "usbdebug",
            "stayawake",
            "animations",
            "location",
            "sysinfo", "systeminfo" -> ActionType.SYSTEM_COMMAND

            else -> ActionType.UNKNOWN
        }
    }

    /**
     * Determine query type based on context
     */
    private fun determineQueryType(verb: String): ActionType {
        // Legacy - no longer used, queries go through SYSTEM_COMMAND
        return ActionType.SHOW_BATTERY
    }

    /**
     * Check if action requires confirmation
     */
    private fun isConfirmationRequired(actionType: ActionType): Boolean {
        return when (actionType) {
            ActionType.DELETE_FILE,
            ActionType.SEND_SMS -> true
            else -> false
        }
    }
}

