package com.example.mentra.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main home screen - Dashboard after permission setup
 */
@Composable
fun HomeScreen(
    onNavigateToFeature: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            HomeTopBar()
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
            }

            item {
                WelcomeCard()
            }

            item {
                QuickStatsCard()
            }

            item {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                FeatureGrid(onFeatureClick = onNavigateToFeature)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mentra",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Open settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun WelcomeCard() {
    val currentTime = remember { LocalDateTime.now() }
    val greeting = when (currentTime.hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val timeString = remember {
        currentTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your personal Android ecosystem",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Steps",
                    value = "0",
                    color = MaterialTheme.colorScheme.primary
                )
                QuickStatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Calories",
                    value = "0",
                    color = MaterialTheme.colorScheme.error
                )
                QuickStatItem(
                    icon = Icons.Default.Timer,
                    label = "Active",
                    value = "0m",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureGrid(onFeatureClick: (String) -> Unit) {
    val features = remember {
        listOf(
            FeatureItem(
                id = "launcher",
                title = "Launcher",
                description = "Custom home screen",
                icon = Icons.Default.Apps,
                color = Color(0xFF6200EE),
                available = false
            ),
            FeatureItem(
                id = "health",
                title = "Health",
                description = "Activity tracking",
                icon = Icons.Default.FavoriteBorder,
                color = Color(0xFFE91E63),
                available = true
            ),
            FeatureItem(
                id = "navigation",
                title = "Navigation",
                description = "Maps & routes",
                icon = Icons.Default.Navigation,
                color = Color(0xFF2196F3),
                available = true
            ),
            FeatureItem(
                id = "media",
                title = "Media",
                description = "Music & videos",
                icon = Icons.Default.MusicNote,
                color = Color(0xFF4CAF50),
                available = false
            ),
            FeatureItem(
                id = "ai_shell",
                title = "AI Shell",
                description = "Smart commands",
                icon = Icons.Default.Terminal,
                color = Color(0xFFFF9800),
                available = true  // Now available!
            ),
            FeatureItem(
                id = "camera",
                title = "Camera",
                description = "Photo & video",
                icon = Icons.Default.CameraAlt,
                color = Color(0xFF9C27B0),
                available = false
            ),
            FeatureItem(
                id = "messaging",
                title = "Messaging",
                description = "SMS & texts",
                icon = Icons.Default.Message,
                color = Color(0xFF00BCD4),
                available = true
            ),
            FeatureItem(
                id = "dialer",
                title = "Phone",
                description = "Calls & dialer",
                icon = Icons.Default.Phone,
                color = Color(0xFF4EC9B0),
                available = true
            ),
            FeatureItem(
                id = "utilities",
                title = "Utilities",
                description = "System tools",
                icon = Icons.Default.Build,
                color = Color(0xFF607D8B),
                available = false
            )
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(((features.size / 2 + features.size % 2) * 150).dp)
    ) {
        items(features) { feature ->
            FeatureCard(
                feature = feature,
                onClick = { onFeatureClick(feature.id) }
            )
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = feature.available, onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (feature.available) 2.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (feature.available) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(feature.color.copy(alpha = if (feature.available) 0.15f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (feature.available) feature.color else feature.color.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (feature.available) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feature.available) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                }
            }

            if (!feature.available) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private data class FeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val available: Boolean
)

