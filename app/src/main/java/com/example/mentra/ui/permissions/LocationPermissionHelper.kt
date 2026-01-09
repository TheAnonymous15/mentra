package com.example.mentra.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.mentra.core.common.permissions.PermissionManager
import com.example.mentra.core.common.permissions.PermissionStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for handling complex location permission flows
 * Especially for background location which requires special handling
 */
@Singleton
class LocationPermissionHelper @Inject constructor(
    private val permissionManager: PermissionManager
) {

    /**
     * Check if foreground location permissions are granted
     */
    fun hasForegroundLocation(): Boolean {
        return permissionManager.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permissionManager.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Check if background location is granted
     */
    fun hasBackgroundLocation(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionManager.isPermissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // On older Android versions, background location is included with foreground
            hasForegroundLocation()
        }
    }

    /**
     * Get list of foreground location permissions to request
     */
    fun getForegroundLocationPermissions(): List<String> {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Determine what location permissions should be requested
     * Returns a LocationPermissionRequest with the appropriate action
     */
    fun getLocationPermissionRequest(
        permissionStates: Map<String, PermissionStatus>
    ): LocationPermissionRequest {
        val hasForeground = hasForegroundLocation()
        val hasBackground = hasBackgroundLocation()
        val needsBackground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        return when {
            // No foreground location - request it first
            !hasForeground -> {
                LocationPermissionRequest.RequestForeground(
                    permissions = getForegroundLocationPermissions(),
                    message = "Location access is required for navigation and activity tracking."
                )
            }

            // Has foreground, needs background (Android 10+)
            hasForeground && !hasBackground && needsBackground -> {
                LocationPermissionRequest.RequestBackground(
                    message = "Background location allows tracking your activity even when the app is closed. This is required for continuous health and activity monitoring."
                )
            }

            // All location permissions granted
            hasForeground && (hasBackground || !needsBackground) -> {
                LocationPermissionRequest.AllGranted
            }

            else -> LocationPermissionRequest.AllGranted
        }
    }

    /**
     * Open settings for background location permission
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun openBackgroundLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get user-friendly explanation for background location
     */
    fun getBackgroundLocationExplanation(): String {
        return """
            Background location access allows Mentra to:
            • Track your steps and activity throughout the day
            • Provide accurate distance measurements
            • Detect different activities (walking, running, cycling)
            • Record your routes and navigation history
            
            Your privacy is important. All location data stays on your device and is never shared.
        """.trimIndent()
    }

    /**
     * Check if we should show background location rationale
     */
    fun shouldShowBackgroundLocationRationale(): Boolean {
        return hasForegroundLocation() &&
               !hasBackgroundLocation() &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}

/**
 * Sealed class representing different location permission request states
 */
sealed class LocationPermissionRequest {
    /**
     * Need to request foreground location first
     */
    data class RequestForeground(
        val permissions: List<String>,
        val message: String
    ) : LocationPermissionRequest()

    /**
     * Foreground granted, need to request background
     */
    data class RequestBackground(
        val message: String
    ) : LocationPermissionRequest()

    /**
     * All location permissions are granted
     */
    data object AllGranted : LocationPermissionRequest()
}

