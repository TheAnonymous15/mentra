package com.example.mentra.shell.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.mentra.shell.actions.ActionRouter
import com.example.mentra.shell.apps.AppCacheService
import com.example.mentra.shell.apps.CacheState
import com.example.mentra.shell.apps.LaunchResult
import com.example.mentra.shell.calculator.ShellCalculator
import com.example.mentra.shell.models.*
import com.example.mentra.shell.packages.PackageManager as MentraPackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes parsed shell commands
 * Routes commands to appropriate action handlers
 */
@Singleton
class CommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: CommandParser,
    private val contextManager: ContextManager,
    private val actionRouter: ActionRouter,
    private val appCacheService: AppCacheService,
    private val calculator: ShellCalculator,
    private val packageManager: MentraPackageManager
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

            // Handle package manager commands (pip, npm, pkg, ls, cd, etc.)
            if (packageManager.isPackageCommand(commandText)) {
                val outputs = packageManager.handleCommand(commandText)

                // Check for special commands like 'clear'
                val clearCommand = outputs.any { it.text == "CLEAR_SCREEN" }
                if (clearCommand) {
                    return ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = "CLEAR_SCREEN",
                        data = "clear_screen",
                        executionTime = System.currentTimeMillis() - startTime
                    )
                }

                val message = outputs.joinToString("\n") { it.text }
                val hasError = outputs.any { it.type == ShellOutputType.ERROR }
                return ShellResult(
                    status = if (hasError) ResultStatus.FAILURE else ResultStatus.SUCCESS,
                    message = message,
                    data = outputs,
                    executionTime = System.currentTimeMillis() - startTime
                )
            }

            // Handle built-in shell commands FIRST (before validation)
            // This allows special commands like 'sms --ui' to bypass strict validation
            val builtInResult = handleBuiltInCommands(command)
            if (builtInResult != null) {
                contextManager.updateLastResult(builtInResult)
                return builtInResult
            }

            // Validate command (only for non-built-in commands)
            if (!parser.validate(command)) {
                return ShellResult(
                    status = ResultStatus.INVALID_COMMAND,
                    message = "Invalid command syntax",
                    executionTime = System.currentTimeMillis() - startTime
                )
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
        // First check for multi-word commands
        val rawLower = command.raw.lowercase().trim()

        // Handle calendar UI trigger
        if (rawLower == "calendar" || rawLower == "cal" || rawLower == "date") {
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = "Opening Calendar...",
                data = "SHOW_CALENDAR_UI"  // Signal to show calendar UI
            )
        }

        // Handle calculator UI trigger (just "calc" or "calculator")
        if (calculator.shouldShowUI(command.raw)) {
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = "Opening Calculator...",
                data = "SHOW_CALCULATOR_UI"  // Signal to show calculator UI
            )
        }

        // Handle calculator commands with expressions (calc 2+2)
        if (calculator.isCalculatorCommand(command.raw)) {
            val outputs = calculator.handleCommand(command.raw)
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = outputs.joinToString("\n") { it.text },
                data = outputs
            )
        }

        // Auto-detect math expressions (e.g., 1+1, sqrt(144), 2^10)
        if (calculator.isMathExpression(command.raw)) {
            val outputs = calculator.evaluate(command.raw)
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = outputs.joinToString("\n") { it.text },
                data = outputs
            )
        }

        // Handle "list apps", "show apps", "apps", "app" commands
        if (rawLower == "apps" || rawLower == "app" || rawLower == "list apps" || rawLower == "show apps" ||
            rawLower.startsWith("apps ") || rawLower.startsWith("app ") ||
            rawLower.startsWith("list apps ") || rawLower.startsWith("show apps ")) {
            val filter = when {
                rawLower.startsWith("list apps ") -> rawLower.removePrefix("list apps ").trim()
                rawLower.startsWith("show apps ") -> rawLower.removePrefix("show apps ").trim()
                rawLower.startsWith("apps ") -> rawLower.removePrefix("apps ").trim()
                rawLower.startsWith("app ") -> rawLower.removePrefix("app ").trim()
                else -> null
            }
            // Check for --ui flag
            if (filter?.contains("--ui") == true) {
                return ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "SHOW_APP_PICKER",
                    data = "show_app_picker"
                )
            }
            return handleListApps(filter)
        }

        // Handle "sms --ui", "messages --ui" to open messaging UI
        if (rawLower == "sms --ui" || rawLower == "messages --ui" ||
            rawLower == "open messages" || rawLower == "open sms") {
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = "NAVIGATE_MESSAGES",
                data = "navigate_messages"
            )
        }

        // Handle "dialer", "phone", "dial" UI commands
        if (rawLower == "dialer" || rawLower == "phone" || rawLower == "dialer --ui" ||
            rawLower == "open dialer" || rawLower == "open phone") {
            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = "NAVIGATE_DIALER",
                data = "navigate_dialer"
            )
        }

        // Handle "open", "launch", "start" app commands
        if (rawLower.startsWith("open ") || rawLower.startsWith("launch ") || rawLower.startsWith("start ")) {
            val appName = when {
                rawLower.startsWith("open ") -> command.raw.substring(5).trim()
                rawLower.startsWith("launch ") -> command.raw.substring(7).trim()
                rawLower.startsWith("start ") -> command.raw.substring(6).trim()
                else -> ""
            }
            if (appName.isNotEmpty()) {
                return handleOpenApp(appName)
            }
        }

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
                    
                    ═══════════════════════════════════════
                    CALCULATOR:
                    ═══════════════════════════════════════
                    calc <expr>       Calculate expression
                    1+1               Auto-detect math
                    calc sqrt(144)    Functions supported
                    calc 2^10         Power operations
                    calc 17 mod 5     Modulo operations
                    calc pi * 2       Constants: pi, e
                    show steps        Step count today
                    sysinfo           Complete system info
                    
                    ═══════════════════════════════════════
                    APP CONTROL:
                    ═══════════════════════════════════════
                    apps              List installed apps
                    apps --ui         Show app picker UI
                    apps --all        List all apps (user + system)
                    apps --system     List system apps only
                    apps [search]     Search apps by name
                    list apps         Same as 'apps'
                    show apps         Same as 'apps'
                    open <app>        Open application
                    settings [type]   Open settings (wifi/bluetooth/etc)
                    freeze <pkg>      Freeze/disable app (Shizuku)
                    unfreeze <pkg>    Unfreeze app (Shizuku)
                    hide <pkg>        Hide app from launcher (Shizuku)
                    unhide <pkg>      Unhide app (Shizuku)
                    
                    ═══════════════════════════════════════
                    NAVIGATION:
                    ═══════════════════════════════════════
                    sms --ui          Open messaging UI
                    dialer            Open dialer/phone UI
                    phone             Same as 'dialer'
                    
                    ═══════════════════════════════════════
                    PERFORMANCE (Requires Shizuku):
                    ═══════════════════════════════════════
                    performance high/balanced/powersave
                    batterysaver --state=on/off
                    clearram          Clear RAM/kill background
                    clearcache        Clear all app caches
                    
                    ═══════════════════════════════════════
                    MESSAGING:
                    ═══════════════════════════════════════
                    inbox             View recent messages
                    inbox [name]      Open contact's inbox
                    inbox [name] [n]  Show last n messages
                                      e.g: inbox mpesa 5
                    unread            Show unread count
                    read [contact]    Read conversation
                    chat [contact]    Same as read
                    reply [message]   Quick reply to open chat
                    
                    message [contact] [text]  Send message
                    text [contact] [text]     Send message  
                    sms [number] [text]       Send to number
                    
                    alias [name] [contact]    Set contact alias
                    Example: alias wife Jane Doe
                    
                    ═══════════════════════════════════════
                    CALLING:
                    ═══════════════════════════════════════
                    call <number>     Make a call
                    call [alias]      Call using alias
                    dial <number>     Open dialer
                    
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
                    
                    Total: 80+ commands available!
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

    /**
     * Handle opening an app by name
     */
    private fun handleOpenApp(appName: String): ShellResult {
        // Use cache service to launch app
        return when (val result = appCacheService.launchApp(appName)) {
            is LaunchResult.Success -> {
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "✅ Launched ${result.app.name}"
                )
            }
            is LaunchResult.NotFound -> {
                // Try to find similar apps
                val suggestions = appCacheService.searchApps(appName).take(5)
                val suggestionText = if (suggestions.isNotEmpty()) {
                    "\n\n💡 Did you mean:\n" + suggestions.joinToString("\n") { "   • ${it.name}" }
                } else {
                    "\n\n💡 Tip: Type 'apps' to see all installed apps"
                }

                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "❌ App not found: $appName$suggestionText"
                )
            }
            is LaunchResult.NotLaunchable -> {
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "❌ ${result.appName} cannot be launched (no launcher activity)"
                )
            }
            is LaunchResult.NoLaunchIntent -> {
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "❌ Cannot launch ${result.appName}: No launch intent available"
                )
            }
            is LaunchResult.Error -> {
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "❌ Failed to launch ${result.appName}: ${result.message}"
                )
            }
        }
    }

    /**
     * Handle listing installed apps
     * Supports: apps, apps --system, apps --user, apps --all, apps [search]
     */
    private fun handleListApps(filter: String?): ShellResult {
        try {
            // Check cache state
            val cacheState = appCacheService.cacheState.value
            if (cacheState is CacheState.NotInitialized || cacheState is CacheState.Loading) {
                // Fall back to direct query if cache not ready
                return handleListAppsDirect(filter)
            }

            // Determine which apps to show
            val showSystem = filter?.contains("--system") == true || filter?.contains("-s") == true
            val showUser = filter?.contains("--user") == true || filter?.contains("-u") == true
            val showAll = filter?.contains("--all") == true || filter?.contains("-a") == true

            // Get search term if provided (not a flag)
            val searchTerm = filter?.split(" ")
                ?.filterNot { it.startsWith("-") }
                ?.joinToString(" ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            // Get apps from cache
            val appsList = when {
                showAll -> appCacheService.getAllApps()
                showSystem -> appCacheService.getSystemApps()
                else -> appCacheService.getUserApps()
            }.let { apps ->
                if (searchTerm != null) {
                    apps.filter { app ->
                        app.name.contains(searchTerm, ignoreCase = true) ||
                        app.packageName.contains(searchTerm, ignoreCase = true)
                    }
                } else {
                    apps
                }
            }.sortedBy { it.name.lowercase() }

            if (appsList.isEmpty()) {
                return ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "📦 No apps found${if (searchTerm != null) " matching '$searchTerm'" else ""}"
                )
            }

            // Build output
            val header = buildString {
                appendLine("╔══════════════════════════════════════════════════════════════╗")
                appendLine("║              📱 INSTALLED APPLICATIONS                        ║")
                appendLine("╠══════════════════════════════════════════════════════════════╣")
                val type = when {
                    showAll -> "All"
                    showSystem -> "System"
                    else -> "User"
                }
                appendLine("║  Type: $type Apps | Total: ${appsList.size} apps")
                if (searchTerm != null) {
                    appendLine("║  Filter: '$searchTerm'")
                }
                appendLine("╚══════════════════════════════════════════════════════════════╝")
                appendLine()
            }

            val appsOutput = appsList.mapIndexed { index, app ->
                val icon = if (app.isSystemApp) "⚙️" else "📱"
                val launchable = if (app.isLaunchable) "" else " [not launchable]"
                val num = String.format("%3d", index + 1)
                "$icon $num. ${app.name}$launchable\n      └─ ${app.packageName} (v${app.version})"
            }.joinToString("\n\n")

            val footer = buildString {
                appendLine()
                appendLine("─".repeat(60))
                appendLine("💡 Usage:")
                appendLine("   apps              - List user apps")
                appendLine("   apps --ui         - Show app picker")
                appendLine("   apps --all        - List all apps")
                appendLine("   apps --system     - List system apps")
                appendLine("   apps [name]       - Search apps by name")
                appendLine("   open <app>        - Open an app by name")
            }

            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = header + appsOutput + footer,
                data = appsList
            )

        } catch (e: Exception) {
            return ShellResult(
                status = ResultStatus.FAILURE,
                message = "❌ Failed to list apps: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Direct app listing (fallback when cache not ready)
     */
    private fun handleListAppsDirect(filter: String?): ShellResult {
        try {
            val pm = context.packageManager

            // Determine which apps to show
            val showSystem = filter?.contains("--system") == true || filter?.contains("-s") == true
            val showUser = filter?.contains("--user") == true || filter?.contains("-u") == true
            val showAll = filter?.contains("--all") == true || filter?.contains("-a") == true

            // Get search term if provided (not a flag)
            val searchTerm = filter?.split(" ")
                ?.filterNot { it.startsWith("-") }
                ?.joinToString(" ")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            // Get installed apps
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val appsList = installedApps
                .asSequence()
                .map { appInfo ->
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val version = try {
                        pm.getPackageInfo(packageName, 0).versionName ?: "?"
                    } catch (e: Exception) {
                        "?"
                    }

                    AppInfo(appName, packageName, version, isSystemApp)
                }
                .filter { app ->
                    when {
                        showAll -> true
                        showSystem -> app.isSystemApp
                        showUser -> !app.isSystemApp
                        else -> !app.isSystemApp // Default: show user apps
                    }
                }
                .filter { app ->
                    if (searchTerm != null) {
                        app.name.contains(searchTerm, ignoreCase = true) ||
                        app.packageName.contains(searchTerm, ignoreCase = true)
                    } else {
                        true
                    }
                }
                .sortedBy { it.name.lowercase() }
                .toList()

            if (appsList.isEmpty()) {
                return ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "📦 No apps found${if (searchTerm != null) " matching '$searchTerm'" else ""}"
                )
            }

            // Build output
            val header = buildString {
                appendLine("╔══════════════════════════════════════════════════════════════╗")
                appendLine("║              📱 INSTALLED APPLICATIONS                        ║")
                appendLine("╠══════════════════════════════════════════════════════════════╣")
                val type = when {
                    showAll -> "All"
                    showSystem -> "System"
                    else -> "User"
                }
                appendLine("║  Type: $type Apps | Total: ${appsList.size} apps (loading cache...)")
                if (searchTerm != null) {
                    appendLine("║  Filter: '$searchTerm'")
                }
                appendLine("╚══════════════════════════════════════════════════════════════╝")
                appendLine()
            }

            val appsOutput = appsList.mapIndexed { index, app ->
                val icon = if (app.isSystemApp) "⚙️" else "📱"
                val num = String.format("%3d", index + 1)
                "$icon $num. ${app.name}\n      └─ ${app.packageName} (v${app.version})"
            }.joinToString("\n\n")

            val footer = buildString {
                appendLine()
                appendLine("─".repeat(60))
                appendLine("💡 Usage:")
                appendLine("   apps              - List user apps")
                appendLine("   apps --ui         - Show app picker")
                appendLine("   apps --all        - List all apps")
                appendLine("   apps --system     - List system apps")
                appendLine("   apps [name]       - Search apps by name")
                appendLine("   open <app>        - Open an app")
            }

            return ShellResult(
                status = ResultStatus.SUCCESS,
                message = header + appsOutput + footer,
                data = appsList
            )

        } catch (e: Exception) {
            return ShellResult(
                status = ResultStatus.FAILURE,
                message = "❌ Failed to list apps: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Data class for app info
     */
    private data class AppInfo(
        val name: String,
        val packageName: String,
        val version: String,
        val isSystemApp: Boolean
    )
}
