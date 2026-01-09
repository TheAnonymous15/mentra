package com.example.mentra.infrastructure.shizuku

import android.content.pm.PackageManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles privileged operations via Shizuku
 * System-level actions that require elevated permissions
 */
@Singleton
class PrivilegedActions @Inject constructor(
    private val shizukuBridge: ShizukuBridge
) {

    /**
     * Install APK from path
     */
    suspend fun installApp(apkPath: String): Result<String> {
        return shizukuBridge.executeCommand("pm install -r $apkPath")
    }

    /**
     * Uninstall app by package name
     */
    suspend fun uninstallApp(packageName: String): Result<String> {
        return shizukuBridge.executeCommand("pm uninstall $packageName")
    }

    /**
     * Grant permission to app
     */
    suspend fun grantPermission(packageName: String, permission: String): Result<String> {
        return shizukuBridge.executeCommand("pm grant $packageName $permission")
    }

    /**
     * Revoke permission from app
     */
    suspend fun revokePermission(packageName: String, permission: String): Result<String> {
        return shizukuBridge.executeCommand("pm revoke $packageName $permission")
    }

    /**
     * List all installed packages
     */
    suspend fun listPackages(): Result<List<String>> {
        return shizukuBridge.executeCommand("pm list packages").mapCatching { output ->
            output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
        }
    }

    /**
     * Clear app data
     */
    suspend fun clearAppData(packageName: String): Result<String> {
        return shizukuBridge.executeCommand("pm clear $packageName")
    }

    /**
     * Force stop app
     */
    suspend fun forceStopApp(packageName: String): Result<String> {
        return shizukuBridge.executeCommand("am force-stop $packageName")
    }

    /**
     * Start activity
     */
    suspend fun startActivity(packageName: String, activityName: String): Result<String> {
        return shizukuBridge.executeCommand("am start -n $packageName/$activityName")
    }

    /**
     * Broadcast intent
     */
    suspend fun sendBroadcast(action: String): Result<String> {
        return shizukuBridge.executeCommand("am broadcast -a $action")
    }

    /**
     * Set system setting
     */
    suspend fun putSystemSetting(namespace: String, key: String, value: String): Result<String> {
        return shizukuBridge.executeCommand("settings put $namespace $key $value")
    }

    /**
     * Get system setting
     */
    suspend fun getSystemSetting(namespace: String, key: String): Result<String> {
        return shizukuBridge.executeCommand("settings get $namespace $key")
    }

    /**
     * Reboot device (dangerous!)
     */
    suspend fun rebootDevice(): Result<String> {
        return shizukuBridge.executeCommand("reboot")
    }

    /**
     * Take screenshot and save to path
     */
    suspend fun takeScreenshot(outputPath: String): Result<String> {
        return shizukuBridge.executeCommand("screencap -p $outputPath")
    }

    /**
     * Record screen (requires API 29+)
     */
    suspend fun recordScreen(outputPath: String, duration: Int): Result<String> {
        return shizukuBridge.executeCommand("screenrecord --time-limit $duration $outputPath")
    }

    /**
     * Get battery stats
     */
    suspend fun getBatteryStats(): Result<String> {
        return shizukuBridge.executeCommand("dumpsys battery")
    }

    /**
     * Get network stats
     */
    suspend fun getNetworkStats(): Result<String> {
        return shizukuBridge.executeCommand("dumpsys netstats")
    }

    /**
     * Get memory info
     */
    suspend fun getMemoryInfo(): Result<String> {
        return shizukuBridge.executeCommand("dumpsys meminfo")
    }

    /**
     * List running services
     */
    suspend fun listServices(): Result<List<String>> {
        return shizukuBridge.executeCommand("dumpsys activity services").mapCatching { output ->
            output.lines()
                .filter { it.contains("ServiceRecord") }
                .map { it.trim() }
        }
    }

    /**
     * Kill process by PID
     */
    suspend fun killProcess(pid: Int): Result<String> {
        return shizukuBridge.executeCommand("kill $pid")
    }

    /**
     * List running processes
     */
    suspend fun listProcesses(): Result<List<String>> {
        return shizukuBridge.executeCommand("ps").mapCatching { output ->
            output.lines().drop(1) // Skip header
        }
    }

    /**
     * Change file permissions
     */
    suspend fun chmod(path: String, permissions: String): Result<String> {
        return shizukuBridge.executeCommand("chmod $permissions $path")
    }

    /**
     * Copy file (with elevated permissions)
     */
    suspend fun copyFile(source: String, destination: String): Result<String> {
        return shizukuBridge.executeCommand("cp $source $destination")
    }

    /**
     * Move file
     */
    suspend fun moveFile(source: String, destination: String): Result<String> {
        return shizukuBridge.executeCommand("mv $source $destination")
    }

    /**
     * Delete file/directory
     */
    suspend fun deleteFile(path: String, recursive: Boolean = false): Result<String> {
        val flag = if (recursive) "-rf" else "-f"
        return shizukuBridge.executeCommand("rm $flag $path")
    }

    /**
     * Create directory
     */
    suspend fun createDirectory(path: String): Result<String> {
        return shizukuBridge.executeCommand("mkdir -p $path")
    }

    /**
     * List directory contents
     */
    suspend fun listDirectory(path: String): Result<List<String>> {
        return shizukuBridge.executeCommand("ls -la $path").mapCatching { output ->
            output.lines().filter { it.isNotEmpty() }
        }
    }

    /**
     * Read file content
     */
    suspend fun readFile(path: String): Result<String> {
        return shizukuBridge.executeCommand("cat $path")
    }

    /**
     * Write to file
     */
    suspend fun writeFile(path: String, content: String): Result<String> {
        // Escape quotes in content
        val escaped = content.replace("\"", "\\\"")
        return shizukuBridge.executeCommand("echo \"$escaped\" > $path")
    }

    /**
     * Get file/directory info
     */
    suspend fun getFileInfo(path: String): Result<String> {
        return shizukuBridge.executeCommand("stat $path")
    }

    /**
     * Set device brightness
     */
    suspend fun setBrightness(level: Int): Result<String> {
        val brightness = level.coerceIn(0, 255)
        return putSystemSetting("system", "screen_brightness", brightness.toString())
    }

    /**
     * Set volume
     */
    suspend fun setVolume(streamType: Int, level: Int): Result<String> {
        return shizukuBridge.executeCommand("media volume --stream $streamType --set $level")
    }

    /**
     * Enable/disable airplane mode
     */
    suspend fun setAirplaneMode(enabled: Boolean): Result<String> {
        val value = if (enabled) "1" else "0"
        return putSystemSetting("global", "airplane_mode_on", value)
    }

    /**
     * Enable/disable WiFi
     */
    suspend fun setWifiEnabled(enabled: Boolean): Result<String> {
        val command = if (enabled) "svc wifi enable" else "svc wifi disable"
        return shizukuBridge.executeCommand(command)
    }

    /**
     * Enable/disable mobile data
     */
    suspend fun setMobileDataEnabled(enabled: Boolean): Result<String> {
        val command = if (enabled) "svc data enable" else "svc data disable"
        return shizukuBridge.executeCommand(command)
    }

    /**
     * Get logcat output
     */
    suspend fun getLogcat(maxLines: Int = 100): Result<String> {
        return shizukuBridge.executeCommand("logcat -d -t $maxLines")
    }

    /**
     * Clear logcat
     */
    suspend fun clearLogcat(): Result<String> {
        return shizukuBridge.executeCommand("logcat -c")
    }
}

