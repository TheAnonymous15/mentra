package com.example.mentra.shell.actions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.mentra.infrastructure.sensors.StepCounterSensor
import com.example.mentra.infrastructure.storage.StorageManager
import com.example.mentra.shell.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles query/information actions (show battery, storage, etc.)
 */
@Singleton
class QueryActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager,
    private val stepCounter: StepCounterSensor
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        return when (action.type) {
            ActionType.SHOW_BATTERY -> showBattery()
            ActionType.SHOW_STORAGE -> showStorage()
            ActionType.SHOW_TIME -> showTime()
            ActionType.SHOW_DATE -> showDate()
            ActionType.SHOW_NETWORK -> showNetwork()
            else -> {
                // Try to determine from target
                when (action.target?.lowercase()) {
                    "battery" -> showBattery()
                    "storage", "disk", "space" -> showStorage()
                    "time" -> showTime()
                    "date" -> showDate()
                    "steps" -> showSteps()
                    "network", "wifi" -> showNetwork()
                    "device", "info" -> showDeviceInfo()
                    else -> ShellResult(
                        status = ResultStatus.INVALID_COMMAND,
                        message = "Unknown query: ${action.target}"
                    )
                }
            }
        }
    }

    /**
     * Show battery information
     */
    private fun showBattery(): ShellResult {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            -1
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = temperature / 10.0

        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val voltageV = voltage / 1000.0

        val message = buildString {
            appendLine("Battery Status:")
            appendLine("  Level: $batteryPct%")
            appendLine("  Status: ${if (isCharging) "Charging" else "Discharging"}")
            appendLine("  Temperature: ${tempCelsius}°C")
            appendLine("  Voltage: ${voltageV}V")
            appendLine("  Health: ${getBatteryHealthString(health)}")
        }

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = message.trim(),
            data = mapOf(
                "level" to batteryPct,
                "charging" to isCharging,
                "temperature" to tempCelsius,
                "voltage" to voltageV
            )
        )
    }

    /**
     * Show storage information
     */
    private fun showStorage(): ShellResult {
        val internal = storageManager.getInternalStorageInfo()
        val external = storageManager.getExternalStorageInfo()

        val message = buildString {
            appendLine("Storage Information:")
            appendLine("\nInternal Storage:")
            appendLine("  Total: %.2f GB".format(internal.totalGB))
            appendLine("  Used: %.2f GB (%d%%)".format(internal.usedGB, internal.percentUsed))
            appendLine("  Available: %.2f GB".format(internal.availableGB))

            external?.let {
                appendLine("\nExternal Storage:")
                appendLine("  Total: %.2f GB".format(it.totalGB))
                appendLine("  Used: %.2f GB (%d%%)".format(it.usedGB, it.percentUsed))
                appendLine("  Available: %.2f GB".format(it.availableGB))
            }
        }

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = message.trim(),
            data = mapOf(
                "internal" to internal,
                "external" to external
            )
        )
    }

    /**
     * Show current time
     */
    private fun showTime(): ShellResult {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = timeFormat.format(Date())

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = "Current time: $time"
        )
    }

    /**
     * Show current date
     */
    private fun showDate(): ShellResult {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val date = dateFormat.format(Date())

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = "Current date: $date"
        )
    }

    /**
     * Show steps
     */
    private fun showSteps(): ShellResult {
        val steps = stepCounter.dailySteps.value

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = "Steps today: $steps",
            data = steps
        )
    }

    /**
     * Show network information
     */
    private fun showNetwork(): ShellResult {
        // Basic network info - can be expanded
        val message = "Network information (basic)"

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = message
        )
    }

    /**
     * Show device information
     */
    private fun showDeviceInfo(): ShellResult {
        val message = buildString {
            appendLine("Device Information:")
            appendLine("  Model: ${Build.MODEL}")
            appendLine("  Manufacturer: ${Build.MANUFACTURER}")
            appendLine("  Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("  Device: ${Build.DEVICE}")
            appendLine("  Product: ${Build.PRODUCT}")
        }

        return ShellResult(
            status = ResultStatus.SUCCESS,
            message = message.trim()
        )
    }

    /**
     * Get battery health string
     */
    private fun getBatteryHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
}

/**
 * Handles phone call actions with comprehensive calling features
 */
@Singleton
class CallActionHandler @Inject constructor(
    private val callingHandler: com.example.mentra.shell.calling.ShellCallingCommandHandler
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        // Build command string from action
        val command = buildCallCommand(action)

        // Handle via calling command handler
        val outputs = callingHandler.handleCommand(command)

        // Convert outputs to shell result
        val message = outputs.joinToString("\n") { it.text }
        val status = outputs.firstOrNull()?.type?.let {
            when (it) {
                ShellOutputType.SUCCESS -> ResultStatus.SUCCESS
                ShellOutputType.ERROR -> ResultStatus.FAILURE
                else -> ResultStatus.SUCCESS
            }
        } ?: ResultStatus.SUCCESS

        return ShellResult(
            status = status,
            message = message,
            data = outputs
        )
    }

    private fun buildCallCommand(action: ShellAction): String {
        return when {
            action.target != null -> "call ${action.target}"
            action.entity != null -> "call ${action.entity}"
            else -> "call"
        }
    }
}

/**
 * Handles SMS/message actions with natural language support
 */
@Singleton
class MessageActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        val phoneNumber = action.target
        val messageText = action.entity

        if (phoneNumber == null) {
            return ShellResult(
                status = ResultStatus.REQUIRES_CONFIRMATION,
                message = "📨 New Message\n\nWho would you like to send to?\n\nOptions:\n  1. Enter phone number\n  2. Search contacts\n  3. Use an alias (wife, mom, boss, etc.)\n\nEnter choice or type a name/number:",
                data = mapOf("type" to "need_recipient", "message" to messageText)
            )
        }

        return try {
            // Check if it's a valid phone number or might be an alias
            if (!phoneNumber.matches(Regex("^\\+?[0-9]{7,15}$"))) {
                // Might be an alias or contact name - needs resolution
                return ShellResult(
                    status = ResultStatus.REQUIRES_CONFIRMATION,
                    message = "📱 Looking up \"$phoneNumber\"...\nIf this is an alias, use: alias $phoneNumber = [contact name]",
                    data = mapOf("type" to "lookup_recipient", "query" to phoneNumber, "message" to messageText)
                )
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("sms:$phoneNumber")
                if (messageText != null) {
                    putExtra("sms_body", messageText)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            ShellResult(
                status = ResultStatus.SUCCESS,
                message = "📱 Opening messaging for $phoneNumber${if (messageText != null) "\n📝 Message: \"$messageText\"" else ""}"
            )
        } catch (e: Exception) {
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "Failed to open messaging: ${e.message}",
                error = e
            )
        }
    }
}

/**
 * Handles media playback actions
 */
@Singleton
class MediaActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        // Basic implementation - opens music app
        // Can be enhanced with actual media control

        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "audio/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            ShellResult(
                status = ResultStatus.SUCCESS,
                message = "Opening music player"
            )
        } catch (e: Exception) {
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "Failed to open music player: ${e.message}",
                error = e
            )
        }
    }
}

/**
 * Handles file operations
 */
@Singleton
class FileActionHandler @Inject constructor(
    private val storageManager: StorageManager
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        return when (action.type) {
            ActionType.LIST_FILES -> listFiles(action.target ?: "/")
            ActionType.READ_FILE -> readFile(action.target)
            ActionType.WRITE_FILE -> writeFile(action.target, action.entity)
            ActionType.DELETE_FILE -> deleteFile(action.target)
            else -> ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "Unsupported file action"
            )
        }
    }

    private suspend fun listFiles(path: String): ShellResult {
        return storageManager.listFiles(path).fold(
            onSuccess = { files ->
                val listing = files.joinToString("\n") { file ->
                    val type = if (file.isDirectory) "d" else "-"
                    val size = if (file.isFile) "${file.length()} bytes" else ""
                    "$type ${file.name} $size"
                }

                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = listing.ifEmpty { "Empty directory" },
                    data = files
                )
            },
            onFailure = { error ->
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Failed to list files: ${error.message}",
                    error = error
                )
            }
        )
    }

    private suspend fun readFile(path: String?): ShellResult {
        if (path == null) {
            return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "File path required"
            )
        }

        return storageManager.readFile(path).fold(
            onSuccess = { content ->
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = content,
                    data = content
                )
            },
            onFailure = { error ->
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Failed to read file: ${error.message}",
                    error = error
                )
            }
        )
    }

    private suspend fun writeFile(path: String?, content: String?): ShellResult {
        if (path == null || content == null) {
            return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "File path and content required"
            )
        }

        return storageManager.writeFile(path, content).fold(
            onSuccess = {
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "File written successfully"
                )
            },
            onFailure = { error ->
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Failed to write file: ${error.message}",
                    error = error
                )
            }
        )
    }

    private suspend fun deleteFile(path: String?): ShellResult {
        if (path == null) {
            return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "File path required"
            )
        }

        return storageManager.delete(path).fold(
            onSuccess = { deleted ->
                if (deleted) {
                    ShellResult(
                        status = ResultStatus.SUCCESS,
                        message = "File deleted successfully"
                    )
                } else {
                    ShellResult(
                        status = ResultStatus.FAILURE,
                        message = "Failed to delete file"
                    )
                }
            },
            onFailure = { error ->
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Failed to delete file: ${error.message}",
                    error = error
                )
            }
        )
    }
}

