package com.example.mentra.ui.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.mentra.core.common.permissions.MentraPermissions
import com.example.mentra.core.common.permissions.PermissionStatus

/**
 * Permission setup screen for first-time app launch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: PermissionSetupViewModel = hiltViewModel(),
    locationHelper: LocationPermissionHelper = hiltViewModel<PermissionSetupViewModel>().locationHelper
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for showing background location explanation dialog
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }

    // Refresh permissions when app resumes (e.g., returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Multi-permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.handlePermissionResults(results)
    }

    // Special permission launcher (for Settings intents)
    val specialPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updatePermissionStates()
    }

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopBar(
                title = { Text("Welcome to Mentra") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            PermissionSetupBottomBar(
                stats = uiState.stats,
                onGrantAll = {
                    val deniedPermissions = viewModel.getDeniedPermissions()
                    if (deniedPermissions.isNotEmpty()) {
                        permissionLauncher.launch(deniedPermissions.toTypedArray())
                    }
                },
                onSkip = {
                    if (uiState.canSkip) {
                        onSetupComplete()
                    }
                },
                canSkip = uiState.canSkip
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PermissionSetupHeader()
            }

            item {
                PermissionStatsCard(stats = uiState.stats)
            }

            items(MentraPermissions.PERMISSION_GROUPS) { group ->
                PermissionGroupCard(
                    group = group,
                    permissionStates = uiState.permissionStates,
                    onRequestPermissions = { permissions ->
                        // Special handling for location permissions
                        if (group.name == "Location") {
                            val locationRequest = locationHelper.getLocationPermissionRequest(uiState.permissionStates)

                            when (locationRequest) {
                                is LocationPermissionRequest.RequestForeground -> {
                                    // Request foreground location first
                                    permissionLauncher.launch(locationRequest.permissions.toTypedArray())
                                }
                                is LocationPermissionRequest.RequestBackground -> {
                                    // Show explanation dialog before requesting background location
                                    showBackgroundLocationDialog = true
                                }
                                is LocationPermissionRequest.AllGranted -> {
                                    // All granted, nothing to do
                                }
                            }
                            return@PermissionGroupCard
                        }

                        // Check if any permissions are special permissions
                        val specialPerms = permissions.filter { MentraPermissions.isSpecialPermission(it) }
                        val regularPerms = permissions.filterNot { MentraPermissions.isSpecialPermission(it) }

                        // Request regular permissions first
                        if (regularPerms.isNotEmpty()) {
                            permissionLauncher.launch(regularPerms.toTypedArray())
                        }

                        // Handle special permissions one by one
                        specialPerms.firstOrNull()?.let { specialPerm ->
                            val intent = when (specialPerm) {
                                android.Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    } else null
                                }
                                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    } else null
                                }
                                android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Intent(
                                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    } else null
                                }
                                else -> null
                            }
                            intent?.let { specialPermissionLauncher.launch(it) }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Background Location Explanation Dialog
    if (showBackgroundLocationDialog) {
        BackgroundLocationDialog(
            onDismiss = { showBackgroundLocationDialog = false },
            onConfirm = {
                showBackgroundLocationDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    locationHelper.openBackgroundLocationSettings(context)
                }
            },
            explanation = locationHelper.getBackgroundLocationExplanation()
        )
    }
}

@Composable
private fun PermissionSetupHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "App Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mentra needs certain permissions to provide you with the best experience. Your privacy is important to us - all data stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionStatsCard(stats: com.example.mentra.core.common.permissions.PermissionStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Setup Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stats.grantedPermissions}/${stats.totalPermissions} permissions granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "${stats.grantedPercentage}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { stats.grantedPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

@Composable
private fun PermissionGroupCard(
    group: MentraPermissions.PermissionGroup,
    permissionStates: Map<String, PermissionStatus>,
    onRequestPermissions: (List<String>) -> Unit
) {
    val allGranted = group.permissions.all {
        permissionStates[it] == PermissionStatus.Granted
    }

    val hasSpecialPermissions = group.permissions.any { MentraPermissions.isSpecialPermission(it) }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (allGranted) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForGroup(group.icon),
                            contentDescription = null,
                            tint = if (allGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (group.isRequired) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (allGranted) {
                                "All permissions granted"
                            } else {
                                "${group.permissions.size} permissions${if (hasSpecialPermissions) " • Requires Settings" else ""}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (allGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    FilledTonalButton(
                        onClick = { onRequestPermissions(group.permissions) }
                    ) {
                        Text("Grant")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = group.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expandable permission details
            if (!allGranted) {
                TextButton(
                    onClick = { expanded = !expanded }
                ) {
                    Text(if (expanded) "Hide details" else "Show details")
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column {
                        group.permissions.forEach { permission ->
                            val isGranted = permissionStates[permission] == PermissionStatus.Granted
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = permission.substringAfterLast('.'),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isGranted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSetupBottomBar(
    stats: com.example.mentra.core.common.permissions.PermissionStats,
    onGrantAll: () -> Unit,
    onSkip: () -> Unit,
    canSkip: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canSkip) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip for now")
                }
            }

            Button(
                onClick = onGrantAll,
                modifier = Modifier.weight(if (canSkip) 1f else 2f),
                enabled = stats.deniedPermissions > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (stats.deniedPermissions > 0) {
                        "Grant ${stats.deniedPermissions} permissions"
                    } else {
                        "Continue"
                    }
                )
            }
        }
    }
}

@Composable
private fun CenterAlignedTopBar(
    title: @Composable () -> Unit,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
) {
    CenterAlignedTopAppBar(
        title = title,
        colors = colors
    )
}

private fun getIconForGroup(iconName: String): ImageVector {
    return when (iconName) {
        "location_on" -> Icons.Default.LocationOn
        "directions_run" -> Icons.Default.DirectionsRun
        "perm_media" -> Icons.Default.PermMedia
        "notifications" -> Icons.Default.Notifications
        "phone" -> Icons.Default.Phone
        "camera" -> Icons.Default.CameraAlt
        "settings" -> Icons.Default.Settings
        else -> Icons.Default.Security
    }
}

@Composable
private fun BackgroundLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    explanation: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Background Location Access",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Mentra needs to access your location even when the app is closed.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Step",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "In Settings, select \"Allow all the time\" for location access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
