package com.example.mentra.core.common.permissions

import android.Manifest
import android.os.Build

/**
 * Defines all permissions required by the Mentra app
 */
object MentraPermissions {

    /**
     * Critical permissions required for core functionality
     */
    val CRITICAL_PERMISSIONS = listOf(
        // Location permissions
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,

        // Activity recognition
        Manifest.permission.ACTIVITY_RECOGNITION,

        // Storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },

        // Post notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    ).filterNotNull()

    /**
     * Important permissions for enhanced functionality
     */
    val IMPORTANT_PERMISSIONS = listOf(
        // Background location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        },

        // Additional media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            null
        },

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            null
        },

        // Camera
        Manifest.permission.CAMERA,

        // Audio recording
        Manifest.permission.RECORD_AUDIO,

        // Body sensors
        Manifest.permission.BODY_SENSORS
    ).filterNotNull()

    /**
     * Optional permissions for phone/messaging features
     */
    val OPTIONAL_PHONE_PERMISSIONS = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG
    )

    /**
     * Special location permissions (background location requires special handling on Android 10+)
     */
    val SPECIAL_LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyList()
    }

    /**
     * Special storage permissions (require Settings intent)
     */
    val SPECIAL_STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        emptyList()
    }

    /**
     * Optional permissions for system-level features (require Settings intent)
     */
    val OPTIONAL_SYSTEM_PERMISSIONS = listOf(
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.REQUEST_INSTALL_PACKAGES
    )

    /**
     * All special permissions that require Settings navigation
     */
    val SPECIAL_PERMISSIONS = SPECIAL_LOCATION_PERMISSIONS + SPECIAL_STORAGE_PERMISSIONS + OPTIONAL_SYSTEM_PERMISSIONS

    /**
     * All permissions grouped by category
     */
    data class PermissionGroup(
        val name: String,
        val description: String,
        val permissions: List<String>,
        val icon: String,
        val isRequired: Boolean
    )

    val PERMISSION_GROUPS = listOf(
        PermissionGroup(
            name = "Location",
            description = "Required for navigation, activity tracking, and location-based features. Background location allows tracking while the app is not in use (requires Settings on Android 10+).",
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else null
            ).filterNotNull(),
            icon = "location_on",
            isRequired = true
        ),
        PermissionGroup(
            name = "Activity & Sensors",
            description = "Required for step counting, activity recognition, and health tracking",
            permissions = listOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.BODY_SENSORS
            ),
            icon = "directions_run",
            isRequired = true
        ),
        PermissionGroup(
            name = "Storage & Media",
            description = "Required for media player, file management, and offline maps. Full storage access enables complete file management.",
            permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )
            } else {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            },
            icon = "perm_media",
            isRequired = true
        ),
        PermissionGroup(
            name = "Notifications",
            description = "Required for foreground services and important alerts",
            permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            },
            icon = "notifications",
            isRequired = true
        ),
        PermissionGroup(
            name = "Phone & Messaging",
            description = "Optional: Enable phone calls and messaging via AI shell",
            permissions = OPTIONAL_PHONE_PERMISSIONS,
            icon = "phone",
            isRequired = false
        ),
        PermissionGroup(
            name = "Camera & Audio",
            description = "Optional: Enable camera and voice commands",
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            icon = "camera",
            isRequired = false
        ),
        PermissionGroup(
            name = "System Access",
            description = "Optional: Enable overlay and system-level features",
            permissions = listOf(
                Manifest.permission.SYSTEM_ALERT_WINDOW
            ),
            icon = "settings",
            isRequired = false
        )
    )

    /**
     * Get all permissions that need runtime requests (excluding special permissions)
     */
    fun getAllRuntimePermissions(): List<String> {
        return (CRITICAL_PERMISSIONS + IMPORTANT_PERMISSIONS + OPTIONAL_PHONE_PERMISSIONS)
            .distinct()
            .filterNot { it in SPECIAL_PERMISSIONS }
    }

    /**
     * Check if a permission is a special permission that requires Settings intent
     */
    fun isSpecialPermission(permission: String): Boolean {
        return permission in SPECIAL_PERMISSIONS
    }

    /**
     * Get permission group for a specific permission
     */
    fun getGroupForPermission(permission: String): PermissionGroup? {
        return PERMISSION_GROUPS.find { it.permissions.contains(permission) }
    }
}

