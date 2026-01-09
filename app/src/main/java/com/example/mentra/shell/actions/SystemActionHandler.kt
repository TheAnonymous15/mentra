package com.example.mentra.shell.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.mentra.shell.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles system-level actions (open app, settings, etc.)
 */
@Singleton
class SystemActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : ActionHandler {

    override suspend fun handle(action: ShellAction): ShellResult {
        return when (action.type) {
            ActionType.OPEN_APP -> openApp(action.target)
            ActionType.OPEN_SETTINGS -> openSettings(action.target)
            ActionType.LAUNCH_ACTIVITY -> launchActivity(action.target, action.entity)
            else -> ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "Unsupported system action: ${action.type}"
            )
        }
    }

    /**
     * Open app by name or package
     */
    private fun openApp(appIdentifier: String?): ShellResult {
        if (appIdentifier == null) {
            return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "App name required"
            )
        }

        val packageName = resolveAppPackage(appIdentifier)

        if (packageName == null) {
            return ShellResult(
                status = ResultStatus.NOT_FOUND,
                message = "App not found: $appIdentifier"
            )
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ShellResult(
                    status = ResultStatus.SUCCESS,
                    message = "Opened $appIdentifier"
                )
            } else {
                ShellResult(
                    status = ResultStatus.FAILURE,
                    message = "Cannot launch $appIdentifier"
                )
            }
        } catch (e: Exception) {
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "Failed to open app: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Open Android settings
     */
    private fun openSettings(settingType: String?): ShellResult {
        val intent = when (settingType?.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "network" -> Settings.ACTION_WIRELESS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            context.startActivity(Intent(intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            ShellResult(
                status = ResultStatus.SUCCESS,
                message = "Opened settings"
            )
        } catch (e: Exception) {
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "Failed to open settings: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Launch specific activity
     */
    private fun launchActivity(packageName: String?, activityName: String?): ShellResult {
        if (packageName == null || activityName == null) {
            return ShellResult(
                status = ResultStatus.INVALID_COMMAND,
                message = "Package and activity name required"
            )
        }

        return try {
            val intent = Intent().apply {
                setClassName(packageName, activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ShellResult(
                status = ResultStatus.SUCCESS,
                message = "Launched $packageName/$activityName"
            )
        } catch (e: Exception) {
            ShellResult(
                status = ResultStatus.FAILURE,
                message = "Failed to launch activity: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Resolve app name to package name
     */
    private fun resolveAppPackage(appName: String): String? {
        // Direct package name
        if (appName.contains(".")) {
            return if (isPackageInstalled(appName)) appName else null
        }

        // Common app aliases
        val commonApps = mapOf(
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "settings" to "com.android.settings",
            "camera" to "com.android.camera2",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "mail" to "com.google.android.gm",
            "youtube" to "com.google.android.youtube",
            "play" to "com.android.vending",
            "playstore" to "com.android.vending",
            "phone" to "com.android.dialer",
            "contacts" to "com.android.contacts",
            "messages" to "com.google.android.apps.messaging",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "files" to "com.google.android.apps.nbu.files"
        )

        return commonApps[appName.lowercase()]
    }

    /**
     * Check if package is installed
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

