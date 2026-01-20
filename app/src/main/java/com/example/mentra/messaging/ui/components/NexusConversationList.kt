package com.example.mentra.messaging.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.messaging.*
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.messaging.ui.utils.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * CONVERSATION LIST COMPONENTS
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Smart conversation list with time grouping
 */
@Composable
fun SmartConversationList(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    if (conversations.isEmpty()) {
        EmptyState()
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group by time (Today, Yesterday, This Week, etc.)
            val grouped = groupConversationsByTime(conversations)

            grouped.forEach { (timeGroup, convos) ->
                item {
                    TimeGroupHeader(timeGroup)
                }

                items(convos, key = { it.address }) { conversation ->
                    NexusConversationCard(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.address) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * Time group header divider
 */
@Composable
fun TimeGroupHeader(timeGroup: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, NexusColors.textMuted.copy(alpha = 0.3f))
                    )
                )
        )
        Text(
            text = timeGroup,
            color = NexusColors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(NexusColors.textMuted.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Conversation card with 3D avatar and smart categorization
 */
@Composable
fun NexusConversationCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val senderType = SmsSenderAnalyzer.getSenderType(conversation.address)
    val senderColor = Color(SmsSenderAnalyzer.getColorForSenderType(senderType))
    val senderIcon = SmsSenderAnalyzer.getIconForSenderType(senderType)
    val isUnreplyable = SmsSenderAnalyzer.isUnreplyable(conversation.address)
    val category = categorizeConversation(conversation)

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        color = NexusColors.card,
        shape = RoundedCornerShape(20.dp),
        border = if (conversation.unreadCount > 0) {
            BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.5f))
        } else null,
        shadowElevation = if (conversation.unreadCount > 0) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 3D Avatar with glow effect
            ConversationAvatar(
                conversation = conversation,
                senderType = senderType,
                senderColor = senderColor,
                senderIcon = senderIcon,
                category = category
            )

            // Content
            ConversationContent(
                conversation = conversation,
                senderType = senderType,
                senderColor = senderColor,
                isUnreplyable = isUnreplyable
            )
        }
    }
}

@Composable
private fun ConversationAvatar(
    conversation: Conversation,
    senderType: SenderType,
    senderColor: Color,
    senderIcon: String,
    category: MessageCategory
) {
    Box {
        // Glow layer
        if (conversation.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .blur(12.dp)
                    .background(senderColor.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Shadow for 3D effect
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(y = 3.dp)
                .blur(6.dp)
                .background(senderColor.copy(alpha = 0.3f), CircleShape)
        )

        // Main 3D Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, CircleShape, spotColor = senderColor)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            senderColor,
                            senderColor.copy(alpha = 0.85f),
                            senderColor.copy(
                                red = senderColor.red * 0.7f,
                                green = senderColor.green * 0.7f,
                                blue = senderColor.blue * 0.7f
                            )
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.5.dp, senderColor.copy(alpha = 0.4f), CircleShape)
                .drawWithContent {
                    drawContent()
                    // Top highlight
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.4f
                        ),
                        radius = size.minDimension / 2 - 3.dp.toPx()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (senderType != SenderType.CONTACT) {
                Text(
                    text = senderIcon,
                    fontSize = 22.sp
                )
            } else {
                Text(
                    text = (conversation.contactName?.firstOrNull()
                        ?: conversation.address.firstOrNull())
                        ?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Category indicator
        Box(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.BottomEnd)
                .background(category.color, CircleShape)
                .border(2.dp, NexusColors.card, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
private fun RowScope.ConversationContent(
    conversation: Conversation,
    senderType: SenderType,
    senderColor: Color,
    isUnreplyable: Boolean
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Name row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.contactName ?: conversation.address,
                    color = NexusColors.textPrimary,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )

                if (isUnreplyable) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "No reply",
                        tint = NexusColors.tertiary.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = conversation.lastMessage?.let { formatSmartTime(it.timestamp) } ?: "",
                color = if (conversation.unreadCount > 0) NexusColors.primary else NexusColors.textMuted,
                fontSize = 11.sp
            )
        }

        // Message preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conversation.lastMessage?.body ?: "No messages",
                color = NexusColors.textSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )

            // Unread badge
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                UnreadBadge(count = conversation.unreadCount)
            }
        }

        // Tags
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            SmallTag(
                text = when (senderType) {
                    SenderType.BANK -> "Finance"
                    SenderType.TELECOM -> "Telecom"
                    SenderType.SHOPPING -> "Shopping"
                    SenderType.GOVERNMENT -> "Promo"
                    SenderType.PROMOTIONAL -> "Promo"
                    SenderType.SERVICE -> "Service"
                    else -> null
                },
                color = senderColor
            )

            SmallTag(
                text = "${conversation.messageCount} msgs",
                color = NexusColors.textMuted
            )
        }
    }
}

/**
 * Unread count badge
 */
@Composable
fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.linearGradient(NexusColors.gradientPrimary),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = NexusColors.background,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Small tag chip
 */
@Composable
fun SmallTag(text: String?, color: Color) {
    if (text == null) return

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Empty state for no conversations
 */
@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NexusColors.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = NexusColors.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "No conversations",
                color = NexusColors.textSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Tap + to start a new message",
                color = NexusColors.textMuted,
                fontSize = 14.sp
            )
        }
    }
}

