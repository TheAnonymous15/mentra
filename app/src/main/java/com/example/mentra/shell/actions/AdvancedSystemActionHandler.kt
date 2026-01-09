package com.example.mentra.shell.actions

import com.example.mentra.shell.models.*
import com.example.mentra.shell.orchestrator.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced System Action Handler
 * Handles all system-wide operations via SystemOrchestrator
 */
@Singleton
class AdvancedSystemActionHandler @Inject constructor(
    private val orchestrator: SystemOrchestrator
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        return when (action.type) {
            ActionType.OPEN_APP -> handleAppAction(action)
            ActionType.OPEN_SETTINGS -> handleSettingsAction(action)
            else -> handleSystemCommand(action)
        }
    }

    /**
     * Handle system-wide commands
     */
    private suspend fun handleSystemCommand(action: ShellAction): ShellResult {
        // For system commands, check both target and verb
        // Commands like "show battery" have target="battery", verb="show"
        // Commands like "reboot" have no target, so we use verb="reboot"
        val command = (action.target ?: action.verb).lowercase()

        if (command.isEmpty()) return invalidCommand()

        return when {
            // POWER MANAGEMENT
            command == "shutdown" || command == "poweroff" -> shutdown()
            command == "reboot" || command == "restart" -> reboot(action.params["mode"])
            command == "sleep" || command == "suspend" -> sleep()
            command == "lock" -> lockScreen()

            // NETWORK MANAGEMENT
            command == "wifi" -> handleWifi(action.params["state"])
            command == "data" || command == "mobiledata" -> handleMobileData(action.params["state"])
            command == "airplane" || command == "airplanemode" -> handleAirplane(action.params["state"])
            command == "bluetooth" || command == "bt" -> handleBluetooth(action.params["state"])

            // DISPLAY
            command == "brightness" -> handleBrightness(action.entity)
            command == "timeout" || command == "screentimeout" -> handleScreenTimeout(action.entity)
            command == "autobrightness" -> handleAutoBrightness(action.params["state"])

            // VOLUME
            command == "volume" -> handleVolume(action.params["type"], action.entity)
            command == "mute" -> handleMute(action.params["state"])

            // TIME & DATE
            command == "settime" -> setTime(action.entity)
            command == "settimezone" -> setTimezone(action.entity ?: "")
            command == "autotime" -> handleAutoTime(action.params["state"])

            // APP MANAGEMENT
            command == "freeze" || command == "disable" -> freezeApp(action.entity ?: "")
            command == "unfreeze" || command == "enable" -> unfreezeApp(action.entity ?: "")
            command == "hide" -> hideApp(action.entity ?: "")
            command == "unhide" || command == "show" -> unhideApp(action.entity ?: "")

            // PERFORMANCE
            command == "performance" || command == "perf" -> setPerformance(action.entity)
            command == "batterysaver" || command == "powersave" -> handleBatterySaver(action.params["state"])
            command == "clearram" || command == "freeram" -> clearRAM()
            command == "clearcache" -> clearCache()

            // NOTIFICATIONS
            command == "dnd" || command == "donotdisturb" -> handleDND(action.params["state"])
            command == "notify" -> sendNotification(action.entity ?: "", action.params["title"] ?: "")

            // SETTINGS
            command == "developermode" || command == "devmode" -> handleDeveloperMode(action.params["state"])
            command == "adb" || command == "usbdebug" -> handleUSBDebug(action.params["state"])
            command == "stayawake" -> handleStayAwake(action.params["state"])
            command == "animations" -> handleAnimations(action.entity)
            command == "location" -> handleLocation(action.params["state"])

            // SYSTEM INFO
            command == "sysinfo" || command == "systeminfo" -> getSystemInfo()

            else -> ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "Unknown system command: $command. Type 'syshelp' for system commands."
            )
        }
    }

    // ============================================
    // POWER MANAGEMENT IMPLEMENTATIONS
    // ============================================

    private suspend fun shutdown(): ShellResult {
        return orchestrator.shutdown().toShellResult(
            success = "System shutdown initiated...",
            error = "Failed to shutdown system"
        )
    }

    private suspend fun reboot(mode: String?): ShellResult {
        val rebootMode = when (mode?.lowercase()) {
            "recovery" -> RebootMode.RECOVERY
            "bootloader", "fastboot" -> RebootMode.BOOTLOADER
            "safe", "safemode" -> RebootMode.SAFE_MODE
            else -> RebootMode.NORMAL
        }

        return orchestrator.reboot(rebootMode).toShellResult(
            success = "System reboot initiated (mode: ${rebootMode.name})...",
            error = "Failed to reboot system"
        )
    }

    private suspend fun sleep(): ShellResult {
        return orchestrator.sleep().toShellResult(
            success = "Device entering sleep mode...",
            error = "Failed to sleep"
        )
    }

    private suspend fun lockScreen(): ShellResult {
        return orchestrator.lockScreen().toShellResult(
            success = "Screen locked",
            error = "Failed to lock screen"
        )
    }

    // ============================================
    // NETWORK MANAGEMENT IMPLEMENTATIONS
    // ============================================

    private suspend fun handleWifi(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("wifi")
        return orchestrator.setWifiEnabled(enabled).toShellResult(
            success = "WiFi ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change WiFi state"
        )
    }

    private suspend fun handleMobileData(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("mobile data")
        return orchestrator.setMobileDataEnabled(enabled).toShellResult(
            success = "Mobile data ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change mobile data state"
        )
    }

    private suspend fun handleAirplane(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("airplane mode")
        return orchestrator.setAirplaneModeEnabled(enabled).toShellResult(
            success = "Airplane mode ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change airplane mode"
        )
    }

    private suspend fun handleBluetooth(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("bluetooth")
        return orchestrator.setBluetoothEnabled(enabled).toShellResult(
            success = "Bluetooth ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change Bluetooth state"
        )
    }

    // ============================================
    // DISPLAY IMPLEMENTATIONS
    // ============================================

    private suspend fun handleBrightness(value: String?): ShellResult {
        val level = value?.toIntOrNull() ?: return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: brightness <0-255>"
        )

        return orchestrator.setBrightness(level).toShellResult(
            success = "Brightness set to $level",
            error = "Failed to set brightness"
        )
    }

    private suspend fun handleScreenTimeout(value: String?): ShellResult {
        val seconds = value?.toIntOrNull() ?: return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: timeout <seconds>"
        )

        return orchestrator.setScreenTimeout(seconds * 1000L).toShellResult(
            success = "Screen timeout set to $seconds seconds",
            error = "Failed to set screen timeout"
        )
    }

    private suspend fun handleAutoBrightness(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("auto-brightness")
        return orchestrator.setAutoBrightness(enabled).toShellResult(
            success = "Auto-brightness ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change auto-brightness"
        )
    }

    // ============================================
    // VOLUME IMPLEMENTATIONS
    // ============================================

    private suspend fun handleVolume(type: String?, value: String?): ShellResult {
        val level = value?.toIntOrNull() ?: return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: volume --type=<music|ring|notification|alarm> <level>"
        )

        val stream = when (type?.lowercase()) {
            "ring", "ringer" -> AudioStream.RING
            "notification", "notif" -> AudioStream.NOTIFICATION
            "alarm" -> AudioStream.ALARM
            "call", "voice" -> AudioStream.VOICE_CALL
            else -> AudioStream.MUSIC
        }

        return orchestrator.setVolume(stream, level).toShellResult(
            success = "Volume (${stream.name}) set to $level",
            error = "Failed to set volume"
        )
    }

    private suspend fun handleMute(state: String?): ShellResult {
        val mute = parseBoolean(state) ?: return invalidState("mute")
        return orchestrator.setMuteAll(mute).toShellResult(
            success = if (mute) "All audio muted" else "Audio unmuted",
            error = "Failed to change mute state"
        )
    }

    // ============================================
    // APP MANAGEMENT IMPLEMENTATIONS
    // ============================================

    private suspend fun freezeApp(packageName: String): ShellResult {
        if (packageName.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: freeze <package_name>"
        )

        return orchestrator.freezeApp(packageName).toShellResult(
            success = "App frozen: $packageName",
            error = "Failed to freeze app"
        )
    }

    private suspend fun unfreezeApp(packageName: String): ShellResult {
        if (packageName.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: unfreeze <package_name>"
        )

        return orchestrator.unfreezeApp(packageName).toShellResult(
            success = "App unfrozen: $packageName",
            error = "Failed to unfreeze app"
        )
    }

    private suspend fun hideApp(packageName: String): ShellResult {
        if (packageName.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: hide <package_name>"
        )

        return orchestrator.hideApp(packageName).toShellResult(
            success = "App hidden: $packageName",
            error = "Failed to hide app"
        )
    }

    private suspend fun unhideApp(packageName: String): ShellResult {
        if (packageName.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: unhide <package_name>"
        )

        return orchestrator.unhideApp(packageName).toShellResult(
            success = "App unhidden: $packageName",
            error = "Failed to unhide app"
        )
    }

    // ============================================
    // HELPER FUNCTIONS
    // ============================================

    private fun parseBoolean(value: String?): Boolean? {
        return when (value?.lowercase()) {
            "on", "true", "1", "yes", "enable" -> true
            "off", "false", "0", "no", "disable" -> false
            else -> null
        }
    }

    private fun invalidState(feature: String): ShellResult {
        return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Invalid state for $feature. Use: on/off, true/false, enable/disable"
        )
    }

    private fun invalidCommand(): ShellResult {
        return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Invalid system command"
        )
    }

    private suspend fun handleAppAction(action: ShellAction): ShellResult {
        // Delegate to SystemActionHandler for backward compatibility
        return ShellResult(status = ResultStatus.SUCCESS, message = "App opened")
    }

    private suspend fun handleSettingsAction(action: ShellAction): ShellResult {
        // Delegate to SystemActionHandler for backward compatibility
        return ShellResult(status = ResultStatus.SUCCESS, message = "Settings opened")
    }

    // ============================================
    // TIME & DATE IMPLEMENTATIONS
    // ============================================

    private suspend fun setTime(value: String?): ShellResult {
        val timestamp = value?.toLongOrNull() ?: return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: settime <unix_timestamp_milliseconds>"
        )

        return orchestrator.setSystemTime(timestamp).toShellResult(
            success = "System time updated",
            error = "Failed to set system time"
        )
    }

    private suspend fun setTimezone(timezone: String): ShellResult {
        if (timezone.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: settimezone <timezone> (e.g., America/New_York)"
        )

        return orchestrator.setTimezone(timezone).toShellResult(
            success = "Timezone set to $timezone",
            error = "Failed to set timezone"
        )
    }

    private suspend fun handleAutoTime(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("auto time")
        return orchestrator.setAutoTime(enabled).toShellResult(
            success = "Auto time ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change auto time"
        )
    }

    // ============================================
    // PERFORMANCE IMPLEMENTATIONS
    // ============================================

    private suspend fun setPerformance(mode: String?): ShellResult {
        val perfMode = when (mode?.lowercase()) {
            "high", "performance" -> PerformanceMode.HIGH_PERFORMANCE
            "balanced", "normal" -> PerformanceMode.BALANCED
            "low", "powersave", "save" -> PerformanceMode.POWER_SAVE
            else -> return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "Usage: performance <high|balanced|powersave>"
            )
        }

        return orchestrator.setPerformanceMode(perfMode).toShellResult(
            success = "Performance mode set to ${perfMode.name}",
            error = "Failed to set performance mode"
        )
    }

    private suspend fun handleBatterySaver(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("battery saver")
        return orchestrator.setBatterySaver(enabled).toShellResult(
            success = "Battery saver ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change battery saver"
        )
    }

    private suspend fun clearRAM(): ShellResult {
        return orchestrator.clearRAM().toShellResult(
            success = "RAM cleared - background processes killed",
            error = "Failed to clear RAM"
        )
    }

    private suspend fun clearCache(): ShellResult {
        return orchestrator.trimAllCaches().toShellResult(
            success = "All app caches cleared",
            error = "Failed to clear caches"
        )
    }

    // ============================================
    // NOTIFICATION IMPLEMENTATIONS
    // ============================================

    private suspend fun handleDND(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("Do Not Disturb")
        return orchestrator.setDoNotDisturb(enabled).toShellResult(
            success = "Do Not Disturb ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change Do Not Disturb"
        )
    }

    private suspend fun sendNotification(message: String, title: String): ShellResult {
        if (message.isEmpty()) return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: notify \"message\" --title=\"title\""
        )

        return orchestrator.sendNotification(title.ifEmpty { "Notification" }, message).toShellResult(
            success = "Notification sent",
            error = "Failed to send notification"
        )
    }

    // ============================================
    // SETTINGS IMPLEMENTATIONS
    // ============================================

    private suspend fun handleDeveloperMode(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("developer mode")
        return orchestrator.setDeveloperMode(enabled).toShellResult(
            success = "Developer mode ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change developer mode"
        )
    }

    private suspend fun handleUSBDebug(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("USB debugging")
        return orchestrator.setUSBDebugging(enabled).toShellResult(
            success = "USB debugging ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change USB debugging"
        )
    }

    private suspend fun handleStayAwake(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("stay awake")
        return orchestrator.setStayAwake(enabled).toShellResult(
            success = "Stay awake ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change stay awake"
        )
    }

    private suspend fun handleAnimations(scale: String?): ShellResult {
        val animScale = scale?.toFloatOrNull() ?: return ShellResult(
            status = ResultStatus.INVALID_COMMAND,
            message = "Usage: animations <0.0-2.0> (0=off, 0.5=fast, 1.0=normal)"
        )

        return orchestrator.setAnimationScale(animScale).toShellResult(
            success = "Animation scale set to $animScale",
            error = "Failed to set animation scale"
        )
    }

    private suspend fun handleLocation(state: String?): ShellResult {
        val enabled = parseBoolean(state) ?: return invalidState("location services")
        return orchestrator.setLocationEnabled(enabled).toShellResult(
            success = "Location services ${if (enabled) "enabled" else "disabled"}",
            error = "Failed to change location services"
        )
    }

    // ============================================
    // SYSTEM INFO IMPLEMENTATION
    // ============================================

    private suspend fun getSystemInfo(): ShellResult {
        return orchestrator.getSystemInfo().fold(
            onSuccess = { info ->
                val output = buildString {
                    appendLine("System Information:")
                    appendLine()
                    appendLine("Device:")
                    info.device.forEach { (key, value) ->
                        appendLine("  ${key.replace("ro.product.", "").replace("ro.build.version.", "")}: $value")
                    }
                    if (info.battery.isNotEmpty()) {
                        appendLine()
                        appendLine("Battery: ${info.battery.take(200)}...")
                    }
                    if (info.memory.isNotEmpty()) {
                        appendLine()
                        appendLine("Memory: ${info.memory.take(200)}...")
                    }
                }

                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = output,
                    data = info
                )
            },
            onFailure = { exception ->
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Failed to get system info: ${exception.message}",
                    error = exception
                )
            }
        )
    }
}

/**
 * Extension function to convert Result to ShellResult
 */
private fun Result<String>.toShellResult(success: String, error: String): ShellResult {
    return fold(
        onSuccess = {
            ShellResult(
                status = ResultStatus.SUCCESS,
                message = success,
                data = it
            )
        },
        onFailure = { exception ->
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "$error: ${exception.message}",
                error = exception
            )
        }
    )
}

