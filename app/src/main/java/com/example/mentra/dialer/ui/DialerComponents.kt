package com.example.mentra.dialer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NEXUS DIALER - Shared UI Components
 * Bottom navigation, empty states, and common UI elements
 */

// ═══════════════════════════════════════════════════════════════════
// BOTTOM NAVIGATION BAR
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerBottomNavBar(
    selectedTab: DialerTab,
    onTabSelected: (DialerTab) -> Unit,
    missedCallCount: Int,
    dialerInput: String = "",
    onCallPressed: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = NexusDialerColors.cardGlass.copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    NexusDialerColors.primary.copy(alpha = 0.25f),
                    NexusDialerColors.secondary.copy(alpha = 0.15f)
                )
            )
        ),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(DialerTab.KEYPAD, selectedTab == DialerTab.KEYPAD, 0) { onTabSelected(DialerTab.KEYPAD) }
            NavItem(DialerTab.CONTACTS, selectedTab == DialerTab.CONTACTS, 0) { onTabSelected(DialerTab.CONTACTS) }
            CenterCallButton(dialerInput.isNotBlank(), onCallPressed)
            NavItem(DialerTab.RECENTS, selectedTab == DialerTab.RECENTS, missedCallCount) { onTabSelected(DialerTab.RECENTS) }
            NavItem(DialerTab.FAVORITES, selectedTab == DialerTab.FAVORITES, 0) { onTabSelected(DialerTab.FAVORITES) }
        }
    }
}

@Composable
private fun CenterCallButton(hasInput: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "call_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = EaseInOut), repeatMode = RepeatMode.Reverse),
        label = "glow_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (hasInput) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(60.dp)
            .offset(y = (-8).dp)
            .scale(scale)
            .drawBehind {
                if (hasInput) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(NexusDialerColors.callGreen.copy(alpha = glowAlpha), Color.Transparent)),
                        radius = size.minDimension * 0.7f
                    )
                }
            }
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    if (hasInput) listOf(NexusDialerColors.callGreen, NexusDialerColors.callGreen.copy(alpha = 0.7f))
                    else listOf(NexusDialerColors.callGreen.copy(alpha = 0.6f), NexusDialerColors.callGreen.copy(alpha = 0.4f))
                )
            )
            .border(2.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), NexusDialerColors.callGreen.copy(alpha = 0.2f))), CircleShape)
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Phone, "Call", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun NavItem(tab: DialerTab, isSelected: Boolean, badgeCount: Int, onClick: () -> Unit) {
    if (tab == DialerTab.CALL) return

    val icon = when (tab) {
        DialerTab.KEYPAD -> Icons.Default.Dialpad
        DialerTab.RECENTS -> Icons.Default.History
        DialerTab.CONTACTS -> Icons.Default.People
        DialerTab.FAVORITES -> Icons.Default.Star
        DialerTab.CALL -> Icons.Default.Phone
    }
    val label = when (tab) {
        DialerTab.KEYPAD -> "Keypad"; DialerTab.RECENTS -> "Recents"
        DialerTab.CONTACTS -> "Contacts"; DialerTab.FAVORITES -> "Favorites"; DialerTab.CALL -> "Call"
    }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )

    Column(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp).scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        BadgedBox(badge = {
            if (badgeCount > 0) Badge(containerColor = NexusDialerColors.accent, contentColor = Color.White) {
                Text(if (badgeCount > 9) "9+" else badgeCount.toString(), fontSize = 9.sp)
            }
        }) {
            Box(
                modifier = Modifier.size(36.dp).background(if (isSelected) NexusDialerColors.primary.copy(alpha = 0.12f) else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = if (isSelected) NexusDialerColors.primary else NexusDialerColors.textMuted, modifier = Modifier.size(20.dp))
            }
        }
        Text(label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal, color = if (isSelected) NexusDialerColors.primary else NexusDialerColors.textMuted)
    }
}

// ═══════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerEmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(80.dp).background(NexusDialerColors.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = NexusDialerColors.primary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
            }
            Text(title, color = NexusDialerColors.textSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = NexusDialerColors.textMuted, fontSize = 14.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

fun formatCallDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, secs) else String.format("%02d:%02d", minutes, secs)
}

