package com.example.mentra.infrastructure.apis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native Android API Executor
 * Uses standard Android APIs - works on ALL devices without root/Shizuku
 * Limited functionality but guaranteed to work
 */
@Singleton
class AndroidAPIExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Execute command using Android APIs
     * Returns success if command was handled, failure otherwise
     */
    suspend fun execute(command: String, params: Map<String, String> = emptyMap()): Result<String> {
        return when (command.lowercase()) {
            // Settings operations
            "wifi" -> openWiFiSettings()
            "bluetooth", "bt" -> openBluetoothSettings()
            "airplane", "airplanemode" -> openAirplaneSettings()
            "brightness" -> openDisplaySettings()
            "location" -> openLocationSettings()
            "volume" -> openSoundSettings()
            "developermode", "devmode" -> openDeveloperSettings()
            "settings" -> openSettings(params["type"])

            // App operations
            "open" -> openApp(params["app"] ?: "")

            // System info
            "battery" -> getBatteryInfo()
            "device" -> getDeviceInfo()
            "storage" -> getStorageInfo()

            else -> Result.failure(UnsupportedOperationException(
                "Command '$command' requires Shizuku/Root. Try: open settings for $command"
            ))
        }
    }

    // ============================================
    // SETTINGS OPERATIONS (Works without privileges)
    // ============================================

    private fun openWiFiSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened WiFi settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openBluetoothSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Bluetooth settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openAirplaneSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Airplane mode settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openDisplaySettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Display settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openLocationSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Location settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openSoundSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Sound settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openDeveloperSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened Developer settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openSettings(type: String?): Result<String> {
        return try {
            val action = when (type?.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "network" -> Settings.ACTION_WIRELESS_SETTINGS
                "developer", "dev" -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }

            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened ${type ?: "main"} settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // APP OPERATIONS
    // ============================================

    private fun openApp(appName: String): Result<String> {
        if (appName.isEmpty()) {
            return Result.failure(IllegalArgumentException("App name required"))
        }

        return try {
            // Try to launch by package name first
            val intent = if (appName.contains(".")) {
                context.packageManager.getLaunchIntentForPackage(appName)
            } else {
                // Search by app name
                null
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.success("Opened $appName")
            } else {
                Result.failure(IllegalArgumentException("App not found: $appName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // SYSTEM INFO (Works without privileges)
    // ============================================

    private fun getBatteryInfo(): Result<String> {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)

            val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val temp = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val voltage = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
            val health = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1) ?: -1

            val statusText = when (status) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            val healthText = when (health) {
                android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            val info = buildString {
                appendLine("Battery Status:")
                appendLine("  Level: $level%")
                appendLine("  Status: $statusText")
                if (temp > 0) appendLine("  Temperature: ${temp / 10.0}°C")
                if (voltage > 0) appendLine("  Voltage: ${voltage / 1000.0}V")
                appendLine("  Health: $healthText")
            }

            Result.success(info.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDeviceInfo(): Result<String> {
        return try {
            val info = """
                Device Information:
                  Manufacturer: ${Build.MANUFACTURER}
                  Model: ${Build.MODEL}
                  Device: ${Build.DEVICE}
                  Product: ${Build.PRODUCT}
                  Android Version: ${Build.VERSION.RELEASE}
                  SDK Level: ${Build.VERSION.SDK_INT}
                  Build ID: ${Build.ID}
                  Build Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(Build.TIME))}
            """.trimIndent()

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getStorageInfo(): Result<String> {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes

            fun formatBytes(bytes: Long): String {
                val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                return String.format("%.2f GB", gb)
            }

            val usedPercent = (usedBytes * 100.0 / totalBytes).toInt()

            val info = """
                Storage Information:
                  Total: ${formatBytes(totalBytes)}
                  Used: ${formatBytes(usedBytes)} ($usedPercent%)
                  Available: ${formatBytes(availableBytes)}
            """.trimIndent()

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

