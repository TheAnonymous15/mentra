package com.example.mentra.core.common.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mentra.core.common.EventBus
import com.example.mentra.core.common.SystemEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized permission manager for the app
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: EventBus
) {

    private val _permissionState = MutableStateFlow<Map<String, PermissionStatus>>(emptyMap())
    val permissionState: StateFlow<Map<String, PermissionStatus>> = _permissionState.asStateFlow()

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    init {
        updateAllPermissionStates()
    }

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return when {
            // Special: Overlay permission
            permission == android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
            }
            // Special: Manage all files permission (Android 11+)
            permission == android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    // On older versions, check WRITE_EXTERNAL_STORAGE instead
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
            // Special: Install packages permission
            permission == android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.packageManager.canRequestPackageInstalls()
                } else {
                    true
                }
            }
            // Regular runtime permissions
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Check if multiple permissions are granted
     */
    fun arePermissionsGranted(permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(it) }
    }

    /**
     * Check if all critical permissions are granted
     */
    fun areCriticalPermissionsGranted(): Boolean {
        return arePermissionsGranted(MentraPermissions.CRITICAL_PERMISSIONS)
    }

    /**
     * Check if all required permissions are granted
     */
    fun areAllRequiredPermissionsGranted(): Boolean {
        val requiredPermissions = MentraPermissions.PERMISSION_GROUPS
            .filter { it.isRequired }
            .flatMap { it.permissions }
            .distinct()
        return arePermissionsGranted(requiredPermissions)
    }

    /**
     * Get denied permissions from a list
     */
    fun getDeniedPermissions(permissions: List<String>): List<String> {
        return permissions.filter { !isPermissionGranted(it) }
    }

    /**
     * Check if permission should show rationale
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Update permission states for all permissions
     * This includes both runtime and special permissions
     */
    fun updateAllPermissionStates() {
        // Get all permissions (runtime + special)
        val allPermissions = MentraPermissions.getAllRuntimePermissions() + MentraPermissions.SPECIAL_PERMISSIONS

        val stateMap = allPermissions.distinct().associateWith { permission ->
            when {
                isPermissionGranted(permission) -> PermissionStatus.Granted
                else -> PermissionStatus.Denied
            }
        }

        _permissionState.value = stateMap

        // Check if setup is complete
        _setupComplete.value = areAllRequiredPermissionsGranted()
    }

    /**
     * Handle permission result
     */
    suspend fun handlePermissionResult(permission: String, isGranted: Boolean) {
        val currentState = _permissionState.value.toMutableMap()
        currentState[permission] = if (isGranted) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied
        }
        _permissionState.value = currentState

        // Emit event
        if (isGranted) {
            eventBus.emit(SystemEvent.System.PermissionGranted(permission))
        } else {
            eventBus.emit(SystemEvent.System.PermissionDenied(permission))
        }

        // Update setup complete status
        _setupComplete.value = areAllRequiredPermissionsGranted()
    }

    /**
     * Handle multiple permission results
     */
    suspend fun handlePermissionResults(results: Map<String, Boolean>) {
        results.forEach { (permission, isGranted) ->
            handlePermissionResult(permission, isGranted)
        }
    }

    /**
     * Open app settings for manual permission management
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open overlay permission settings
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open all files access settings (MANAGE_EXTERNAL_STORAGE)
     */
    fun openAllFilesAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings
                openAppSettings(context)
            }
        } else {
            // On older Android versions, regular storage permissions are enough
            openAppSettings(context)
        }
    }

    /**
     * Open background location settings (Android 10+)
     * Note: Background location must be requested AFTER foreground location is granted
     */
    fun openBackgroundLocationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, background location requires app settings
            openAppSettings(context)
        }
    }

    /**
     * Open install packages settings
     */
    fun openInstallPackagesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open appropriate settings for a special permission
     */
    fun openSpecialPermissionSettings(context: Context, permission: String) {
        when (permission) {
            android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                openOverlaySettings(context)
            }
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                openAllFilesAccessSettings(context)
            }
            android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                openInstallPackagesSettings(context)
            }
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                openBackgroundLocationSettings(context)
            }
            else -> {
                openAppSettings(context)
            }
        }
    }

    /**
     * Get permission statistics
     */
    fun getPermissionStats(): PermissionStats {
        val allPermissions = MentraPermissions.getAllRuntimePermissions()
        val granted = allPermissions.count { isPermissionGranted(it) }
        val denied = allPermissions.size - granted

        val criticalGranted = MentraPermissions.CRITICAL_PERMISSIONS.count { isPermissionGranted(it) }
        val criticalTotal = MentraPermissions.CRITICAL_PERMISSIONS.size

        return PermissionStats(
            totalPermissions = allPermissions.size,
            grantedPermissions = granted,
            deniedPermissions = denied,
            criticalGranted = criticalGranted,
            criticalTotal = criticalTotal,
            isFullySetup = areAllRequiredPermissionsGranted()
        )
    }

    /**
     * Get permissions grouped by status
     */
    fun getPermissionsByStatus(): Map<PermissionStatus, List<String>> {
        val allPermissions = MentraPermissions.getAllRuntimePermissions()
        return allPermissions.groupBy { permission ->
            if (isPermissionGranted(permission)) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.Denied
            }
        }
    }
}

/**
 * Permission status enum
 */
enum class PermissionStatus {
    Granted,
    Denied,
    PermanentlyDenied
}

/**
 * Permission statistics data class
 */
data class PermissionStats(
    val totalPermissions: Int,
    val grantedPermissions: Int,
    val deniedPermissions: Int,
    val criticalGranted: Int,
    val criticalTotal: Int,
    val isFullySetup: Boolean
) {
    val grantedPercentage: Int
        get() = if (totalPermissions > 0) {
            (grantedPermissions * 100) / totalPermissions
        } else {
            0
        }

    val criticalPercentage: Int
        get() = if (criticalTotal > 0) {
            (criticalGranted * 100) / criticalTotal
        } else {
            0
        }
}

