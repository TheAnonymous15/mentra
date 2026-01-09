package com.example.mentra.launcher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

/**
 * Mentra Futuristic Launcher Screen
 * Ultra-modern design with glassmorphism, animations, and stunning visuals
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel(),
    onNavigateToShell: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    var showAppDrawer by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A),
                        Color(0xFF0F1729)
                    ),
                    startY = gradientOffset * 1000,
                    endY = gradientOffset * 2000
                )
            )
    ) {
        // Floating particles background
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Futuristic Status Bar
            FuturisticStatusBar(
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                onQuickSettingsClick = { showQuickSettings = true }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Holographic Search Bar
            HolographicSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onShellClick = onNavigateToShell
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Neon Quick Actions
            NeonQuickActions(
                onActionClick = viewModel::handleQuickAction
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glowing App Grid
            GlowingAppGrid(
                apps = apps.take(12),
                onAppClick = viewModel::launchApp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Futuristic App Drawer Button
            FuturisticDrawerButton(
                onClick = { showAppDrawer = true }
            )
        }

        // Futuristic App Drawer
        AnimatedVisibility(
            visible = showAppDrawer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FuturisticAppDrawer(
                apps = apps.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                },
                onDismiss = { showAppDrawer = false },
                onAppClick = { app ->
                    viewModel.launchApp(app)
                    showAppDrawer = false
                }
            )
        }

        // Holographic Quick Settings
        AnimatedVisibility(
            visible = showQuickSettings,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            HolographicQuickSettings(
                onDismiss = { showQuickSettings = false },
                onSettingClick = viewModel::handleQuickSetting
            )
        }
    }
}

@Composable
fun FloatingParticles() {
    // Animated floating particles for ambiance
    repeat(15) { index ->
        val infiniteTransition = rememberInfiniteTransition(label = "particle$index")
        val offsetY by infiniteTransition.animateFloat(
            initialValue = (index * 60).toFloat(),
            targetValue = (index * 60 + 800).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 8000 + (index * 500),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "particleY$index"
        )

        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particleAlpha$index"
        )

        Box(
            modifier = Modifier
                .offset(
                    x = (index * 50 % 350).dp,
                    y = (offsetY % 900).dp
                )
                .size((4 + index % 3).dp)
                .alpha(alpha)
                .background(
                    color = Color(0xFF4EC9B0),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun FuturisticStatusBar(
    currentTime: String,
    batteryLevel: Int,
    onQuickSettingsClick: () -> Unit
) {
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1F3A).copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Holographic Time
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale)
            )

            // Status Icons with glow
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onQuickSettingsClick() }
            ) {
                // Battery with neon glow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = Color(0xFF4EC9B0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$batteryLevel%",
                        color = Color(0xFF4EC9B0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // WiFi with pulse
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = "WiFi",
                    tint = Color(0xFF4EC9B0),
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolographicSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onShellClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Glowing search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4EC9B0),
                            Color(0xFF569CD6)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            placeholder = {
                Text(
                    "Search apps...",
                    color = Color(0xFF4EC9B0).copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    "Search",
                    tint = Color(0xFF4EC9B0)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1A1F3A).copy(alpha = 0.6f),
                unfocusedContainerColor = Color(0xFF1A1F3A).copy(alpha = 0.4f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        // Pulsing shell button
        val shellScale by rememberInfiniteTransition(label = "shell").animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shellScale"
        )

        IconButton(
            onClick = onShellClick,
            modifier = Modifier
                .size(56.dp)
                .scale(shellScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4EC9B0),
                            Color(0xFF2A7A6F)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = "AI Shell",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun NeonQuickActions(
    onActionClick: (QuickAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            Triple(Icons.Default.Wifi, "WiFi", QuickAction.WIFI) to Color(0xFF4EC9B0),
            Triple(Icons.Default.Bluetooth, "BT", QuickAction.BLUETOOTH) to Color(0xFF569CD6),
            Triple(Icons.Default.Lightbulb, "Light", QuickAction.BRIGHTNESS) to Color(0xFFDCDCAA),
            Triple(Icons.Default.VolumeUp, "Sound", QuickAction.VOLUME) to Color(0xFFC586C0),
            Triple(Icons.Default.AirplanemodeActive, "Plane", QuickAction.AIRPLANE) to Color(0xFFCE9178)
        ).forEach { (action, color) ->
            NeonActionButton(
                icon = action.first,
                label = action.second,
                color = color,
                onClick = { onActionClick(action.third) }
            )
        }
    }
}

@Composable
fun NeonActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
                isPressed = false
            }
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            color.copy(alpha = 0.1f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = color,
                    shape = CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = color.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlowingAppGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "FAVORITES",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF4EC9B0),
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.height(280.dp)
        ) {
            items(apps) { app ->
                GlowingAppIcon(
                    app = app,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlowingAppIcon(
    app: AppInfo,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "appScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .combinedClickable(
                onClick = {
                    isPressed = true
                    onClick()
                },
                onLongClick = { }
            )
    ) {
        // App icon with holographic glow
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .blur(8.dp)
                    .background(
                        color = Color(0xFF4EC9B0).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            // Icon
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap(64, 64).asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF4EC9B0).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App name with glow
        Text(
            text = app.name,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(70.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FuturisticDrawerButton(
    onClick: () -> Unit
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring animation
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF4EC9B0).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main button
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4EC9B0),
                            Color(0xFF2A7A6F)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFF4EC9B0),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Apps,
                contentDescription = "App Drawer",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FuturisticAppDrawer(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter),
            color = Color(0xFF1A1F3A).copy(alpha = 0.95f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Drawer handle
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(5.dp)
                        .background(
                            color = Color(0xFF4EC9B0).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(3.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Header with glow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALL APPS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4EC9B0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )

                    Text(
                        text = "${apps.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4EC9B0).copy(alpha = 0.7f),
                        modifier = Modifier
                            .background(
                                color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Glowing grid of all apps
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(apps) { app ->
                        GlowingAppIcon(
                            app = app,
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicQuickSettings(
    onDismiss: () -> Unit,
    onSettingClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            color = Color(0xFF1A1F3A).copy(alpha = 0.95f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUICK SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF4EC9B0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF4EC9B0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Settings grid
                val settings = listOf(
                    Triple("WiFi", Icons.Default.Wifi, Color(0xFF4EC9B0)),
                    Triple("Bluetooth", Icons.Default.Bluetooth, Color(0xFF569CD6)),
                    Triple("Brightness", Icons.Default.Lightbulb, Color(0xFFDCDCAA)),
                    Triple("Volume", Icons.Default.VolumeUp, Color(0xFFC586C0)),
                    Triple("Battery", Icons.Default.BatteryFull, Color(0xFF4EC9B0)),
                    Triple("Display", Icons.Default.Smartphone, Color(0xFF569CD6)),
                    Triple("Network", Icons.Default.NetworkCell, Color(0xFFDCDCAA)),
                    Triple("Developer", Icons.Default.Code, Color(0xFFCE9178))
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(settings.size) { index ->
                        val (name, icon, color) = settings[index]
                        HolographicSettingCard(
                            name = name,
                            icon = icon,
                            color = color,
                            onClick = {
                                onSettingClick(name)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicSettingCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale),
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Rest of the components remain the same
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onShellClick: () -> Unit
) {
    // Legacy component - keep for compatibility
}

@Composable
fun LauncherQuickActions(
    onActionClick: (QuickAction) -> Unit
) {
    // Legacy component - keep for compatibility
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    // Legacy component - keep for compatibility
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherFavoriteApps(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    // Legacy component - keep for compatibility
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    // Legacy component - keep for compatibility
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Legacy component - keep for compatibility
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    onDismiss: () -> Unit,
    onSettingClick: (String) -> Unit
) {
    // Legacy component - keep for compatibility
}

// Data classes
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isFavorite: Boolean = false,
    val isSystemApp: Boolean = false
)

enum class QuickAction {
    WIFI,
    BLUETOOTH,
    BRIGHTNESS,
    VOLUME,
    AIRPLANE
}

