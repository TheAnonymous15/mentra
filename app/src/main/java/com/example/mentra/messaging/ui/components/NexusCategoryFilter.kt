package com.example.mentra.messaging.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.messaging.Conversation
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.messaging.ui.utils.categorizeConversation
import com.example.mentra.messaging.ui.utils.formatCount

/**
 * ═══════════════════════════════════════════════════════════════════
 * CATEGORY FILTER COMPONENTS
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Message category enum with display properties
 */
enum class MessageCategory(val label: String, val icon: ImageVector, val color: Color) {
    ALL("All", Icons.Default.Forum, NexusColors.textPrimary),
    PERSONAL("Personal", Icons.Default.Person, NexusColors.primary),
    BUSINESS("Business", Icons.Default.Business, NexusColors.secondary),
    FINANCE("Finance", Icons.Default.AccountBalance, Color(0xFF4CAF50)),
    PROMOTIONS("Promos", Icons.Default.LocalOffer, NexusColors.accent),
    ALERTS("Alerts", Icons.Default.Notifications, NexusColors.tertiary)
}

/**
 * Horizontal scrollable category filter row
 */
@Composable
fun CategoryFilterRow(
    selectedCategory: MessageCategory?,
    onCategorySelected: (MessageCategory?) -> Unit,
    conversations: List<Conversation>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(MessageCategory.entries) { category ->
            val isSelected = selectedCategory == category || (selectedCategory == null && category == MessageCategory.ALL)
            val count = when (category) {
                MessageCategory.ALL -> conversations.size
                else -> conversations.count {
                    categorizeConversation(it) == category
                }
            }

            CategoryChip(
                category = category,
                count = count,
                isSelected = isSelected,
                onClick = {
                    onCategorySelected(if (category == MessageCategory.ALL) null else category)
                }
            )
        }
    }
}

/**
 * Individual category filter chip
 */
@Composable
fun CategoryChip(
    category: MessageCategory,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        color = if (isSelected) category.color.copy(alpha = 0.2f) else NexusColors.surface,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, category.color) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = if (isSelected) category.color else NexusColors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category.label,
                color = if (isSelected) category.color else NexusColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (count > 0) {
                Text(
                    text = formatCount(count),
                    color = if (isSelected) category.color else NexusColors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

