package com.example.mentra.messaging.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.messaging.MessageStatistics
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.messaging.ui.utils.formatCount

/**
 * ═══════════════════════════════════════════════════════════════════
 * HEADER COMPONENTS - Various header styles for messaging UI
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Floating glassmorphic header with logo and search
 */
@Composable
fun FuturisticHeader(onSearch: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                ),
                alpha = 0.8f
            )
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .blur(16.dp)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "App Logo",
                tint = Color.Cyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Mentra Messenger",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(Color.Cyan, Offset(0f, 2f), 8f)
                )
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Cyan)
            }
        }
    }
}

/**
 * Nexus-style header with stats
 */
@Composable
fun NexusHeader(
    stats: MessageStatistics?,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animated logo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(NexusColors.gradientPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = NexusColors.background,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "MENTRA NEXUS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = NexusColors.textPrimary,
                        letterSpacing = 4.sp
                    )
                }

                Text(
                    text = "${stats?.totalMessages ?: 0} messages • ${stats?.unreadCount ?: 0} unread",
                    fontSize = 12.sp,
                    color = NexusColors.textMuted,
                    modifier = Modifier.padding(start = 50.dp)
                )
            }

            // Stats badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    icon = Icons.Default.CallReceived,
                    count = stats?.receivedCount ?: 0,
                    color = NexusColors.received
                )
                StatPill(
                    icon = Icons.Default.CallMade,
                    count = stats?.sentCount ?: 0,
                    color = NexusColors.sent
                )
            }
        }
    }
}

/**
 * Stat display pill
 */
@Composable
fun StatPill(
    icon: ImageVector,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = formatCount(count),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Nexus-style search bar
 */
@Composable
fun NexusSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = NexusColors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    NexusColors.primary.copy(alpha = 0.3f),
                    NexusColors.secondary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = NexusColors.textMuted,
                modifier = Modifier.size(20.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(
                    color = NexusColors.textPrimary,
                    fontSize = 15.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search messages, contacts...",
                                color = NexusColors.textMuted,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = NexusColors.textMuted
                    )
                }
            }
        }
    }
}

