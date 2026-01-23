package com.example.mentra.shell.apps

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS APP PICKER MODAL
 * Futuristic glassmorphic app picker with beautiful grid layout
 * ═══════════════════════════════════════════════════════════════════
 */

private object NexusAppColors {
    val voidBlack = Color(0xFF0A0A0F)
    val deepSpace = Color(0xFF12121A)
    val glassSurface = Color(0xFF1A1A2E)
    val glassHighlight = Color(0xFF2A2A4E)

    val neonCyan = Color(0xFF00D4FF)
    val neonPink = Color(0xFFFF2E63)
    val neonGreen = Color(0xFF00FF88)
    val neonPurple = Color(0xFFB388FF)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0C0)
    val textMuted = Color(0xFF6C7A89)

    val borderGlow = Color(0xFF00D4FF)
}

@Composable
fun NexusAppPickerModal(
    appCacheService: AppCacheService,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val apps by appCacheService.cachedApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }

    // Filter apps based on search and category
    val filteredApps = remember(apps, searchQuery, selectedCategory) {
        apps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                AppCategory.ALL -> true
                AppCategory.USER -> !app.isSystemApp
                AppCategory.SYSTEM -> app.isSystemApp
            }

            matchesSearch && matchesCategory && app.isLaunchable
        }
    }

    // Animate entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Background with blur effect
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusAppColors.voidBlack.copy(alpha = 0.85f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        // Main modal content
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) +
                   scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)) +
                  scaleOut(targetScale = 0.9f, animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(enabled = false, onClick = {})
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NexusAppColors.glassSurface.copy(alpha = 0.95f),
                                    NexusAppColors.deepSpace.copy(alpha = 0.98f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NexusAppColors.neonCyan.copy(alpha = 0.5f),
                                    NexusAppColors.neonPurple.copy(alpha = 0.3f),
                                    NexusAppColors.neonPink.copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    // Header
                    AppPickerHeader(
                        onClose = onClose,
                        appCount = filteredApps.size
                    )

                    // Search bar
                    AppPickerSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    // Category tabs
                    AppPickerCategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { selectedCategory = it }
                    )

                    // Apps grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            AppGridItem(
                                app = app,
                                context = context,
                                onClick = {
                                    launchApp(context, app.packageName)
                                    onClose()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerHeader(
    onClose: () -> Unit,
    appCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon with glow effect
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexusAppColors.neonCyan.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = NexusAppColors.neonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column {
                Text(
                    text = "Applications",
                    style = TextStyle(
                        color = NexusAppColors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$appCount apps available",
                    style = TextStyle(
                        color = NexusAppColors.textMuted,
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(40.dp)
                .background(
                    NexusAppColors.neonPink.copy(alpha = 0.15f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = NexusAppColors.neonPink,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AppPickerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NexusAppColors.glassHighlight.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    NexusAppColors.borderGlow.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = NexusAppColors.neonCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = NexusAppColors.textPrimary,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search apps...",
                                    color = NexusAppColors.textMuted,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NexusAppColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerCategoryTabs(
    selectedCategory: AppCategory,
    onCategorySelect: (AppCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NexusAppColors.neonCyan.copy(alpha = 0.3f),
                                    NexusAppColors.neonPurple.copy(alpha = 0.3f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NexusAppColors.glassHighlight.copy(alpha = 0.3f),
                                    NexusAppColors.glassHighlight.copy(alpha = 0.3f)
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) NexusAppColors.neonCyan.copy(alpha = 0.5f)
                        else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onCategorySelect(category) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.displayName,
                    style = TextStyle(
                        color = if (isSelected) NexusAppColors.neonCyan
                               else NexusAppColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: CachedApp,
    context: Context,
    onClick: () -> Unit
) {
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Load app icon
    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(app.packageName)
                iconBitmap = drawableToBitmap(drawable)
            } catch (e: Exception) {
                // Use default icon
            }
        }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = NexusAppColors.neonCyan.copy(alpha = 0.3f),
                    spotColor = NexusAppColors.neonPurple.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(NexusAppColors.glassHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!.asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = NexusAppColors.neonGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App name
        Text(
            text = app.name,
            style = TextStyle(
                color = NexusAppColors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

enum class AppCategory(val displayName: String) {
    ALL("All Apps"),
    USER("User"),
    SYSTEM("System")
}

