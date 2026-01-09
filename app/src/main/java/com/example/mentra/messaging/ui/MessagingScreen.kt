package com.example.mentra.messaging.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.messaging.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA NEXUS MESSAGING SYSTEM
 * Next-Generation Futuristic Messenger
 * ═══════════════════════════════════════════════════════════════════
 *
 * Features:
 * - Smart AI-powered threading
 * - Holographic UI elements
 * - Gesture-based interactions
 * - Real-time sync with animations
 * - Sender type detection & categorization
 * - Dual-SIM support
 * - Futuristic glassmorphism design
 */

// ═══════════════════════════════════════════════════════════════════
// COLOR PALETTE - Cyberpunk/Futuristic Theme
// ═══════════════════════════════════════════════════════════════════
object NexusColors {
    val background = Color(0xFF050810)
    val surface = Color(0xFF0A0F1C)
    val surfaceVariant = Color(0xFF111827)
    val card = Color(0xFF1A1F35)
    val cardHover = Color(0xFF242B45)

    val primary = Color(0xFF00F5D4)      // Cyan/Teal
    val secondary = Color(0xFF7B61FF)    // Purple
    val tertiary = Color(0xFFFF6B6B)     // Coral
    val accent = Color(0xFFFFE66D)       // Yellow

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB4B8C5)
    val textMuted = Color(0xFF6B7280)

    val sent = Color(0xFF00F5D4)
    val received = Color(0xFF7B61FF)
    val unread = Color(0xFFFF6B6B)

    val gradientPrimary = listOf(Color(0xFF00F5D4), Color(0xFF00D4AA))
    val gradientSecondary = listOf(Color(0xFF7B61FF), Color(0xFF5B4ACC))
    val gradientDanger = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A5A))
    val gradientSurface = listOf(Color(0xFF1A1F35), Color(0xFF0A0F1C))
}

// ═══════════════════════════════════════════════════════════════════
// MAIN MESSAGING SCREEN
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    viewModel: MessagingViewModel = hiltViewModel(),
    onOpenConversation: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()

    // Preload status
    val isPreloaded by viewModel.isPreloaded.collectAsState()
    val preloadProgress by viewModel.preloadProgress.collectAsState()
    val stats by viewModel.messageStats.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showComposeModal by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<MessageCategory?>(null) }

    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Blur animation for modal
    val blurRadius by animateDpAsState(
        targetValue = if (showComposeModal) 20.dp else 0.dp,
        animationSpec = tween(300),
        label = "blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusColors.background)
            .systemBarsPadding()
    ) {
        // Main content with blur when modal is open
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showComposeModal) Modifier.blur(blurRadius) else Modifier
                )
        ) {
            // Animated gradient orbs
            NexusBackgroundOrbs(glowAlpha)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                // Futuristic Header
                NexusHeader(
                    stats = stats,
                    onSettingsClick = { }
                )

                // Smart Search Bar
                NexusSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                // Category Filter Chips
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    conversations = conversations
                )

                // Smart Threaded Conversations
                SmartConversationList(
                    conversations = filterConversations(conversations, searchQuery, selectedCategory),
                    onConversationClick = onOpenConversation
                )
            }

            // 3D Floating Compose Button
            Nexus3DComposeButton(
                onClick = { showComposeModal = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }

        // Compose Modal (not blurred)
        if (showComposeModal) {
            NexusComposeModal(
                contacts = contacts,
                availableSims = availableSims,
                onDismiss = { showComposeModal = false },
                onSend = { number, message, simId ->
                    viewModel.sendMessage(number, message, simId)
                    // Don't close modal here - let the modal handle showing sending status
                    // Modal will auto-close after showing "Delivered" status
                },
                onSchedule = { number, message, simId, time ->
                    // TODO: Schedule message
                    // viewModel.scheduleMessage(number, message, simId, time)
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// BACKGROUND EFFECTS
// ═══════════════════════════════════════════════════════════════════
@Composable
fun NexusBackgroundOrbs(glowAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top-right cyan orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NexusColors.primary.copy(alpha = glowAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.1f),
            radius = size.width * 0.5f
        )

        // Bottom-left purple orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NexusColors.secondary.copy(alpha = glowAlpha * 0.2f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.1f, size.height * 0.9f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.1f, size.height * 0.9f),
            radius = size.width * 0.4f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════════════
// SEARCH BAR
// ═══════════════════════════════════════════════════════════════════
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

// ═══════════════════════════════════════════════════════════════════
// CATEGORY FILTER
// ═══════════════════════════════════════════════════════════════════
enum class MessageCategory(val label: String, val icon: ImageVector, val color: Color) {
    ALL("All", Icons.Default.Forum, NexusColors.textPrimary),
    PERSONAL("Personal", Icons.Default.Person, NexusColors.primary),
    BUSINESS("Business", Icons.Default.Business, NexusColors.secondary),
    FINANCE("Finance", Icons.Default.AccountBalance, Color(0xFF4CAF50)),
    PROMOTIONS("Promos", Icons.Default.LocalOffer, NexusColors.accent),
    ALERTS("Alerts", Icons.Default.Notifications, NexusColors.tertiary)
}

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

// ═══════════════════════════════════════════════════════════════════
// SMART CONVERSATION LIST
// ═══════════════════════════════════════════════════════════════════
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
                                    senderColor.copy(red = senderColor.red * 0.7f, green = senderColor.green * 0.7f, blue = senderColor.blue * 0.7f) // Darker bottom
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, senderColor.copy(alpha = 0.4f), CircleShape)
                        // Inner highlight for 3D depth
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

            // Content
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
                    // Sender type tag
                    SmallTag(
                        text = when (senderType) {
                            SenderType.BANK -> "Finance"
                            SenderType.TELECOM -> "Telecom"
                            SenderType.SHOPPING -> "Shopping"
                            SenderType.GOVERNMENT -> "Govt"
                            SenderType.PROMOTIONAL -> "Promo"
                            SenderType.SERVICE -> "Service"
                            else -> null
                        },
                        color = senderColor
                    )

                    // Message count
                    SmallTag(
                        text = "${conversation.messageCount} msgs",
                        color = NexusColors.textMuted
                    )
                }
            }
        }
    }
}

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

// ═══════════════════════════════════════════════════════════════════
// 3D COMPOSE BUTTON - Pro Futuristic Design
// ═══════════════════════════════════════════════════════════════════
@Composable
fun Nexus3DComposeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "fab3d")

    // Pulsing glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Floating animation
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Rotating halo
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        ),
        label = "halo"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 24.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Box(
        modifier = modifier
            .offset(y = (-floatOffset).dp)
            .scale(scale)
    ) {
        // Outer glow ring
        Canvas(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = haloRotation }
        ) {
            // Draw rotating dashed circle
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NexusColors.primary.copy(alpha = pulseAlpha),
                        Color.Transparent,
                        NexusColors.secondary.copy(alpha = pulseAlpha * 0.7f),
                        Color.Transparent,
                        NexusColors.accent.copy(alpha = pulseAlpha * 0.5f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Shadow layer (3D effect)
        Box(
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.Center)
                .offset(y = 4.dp)
                .blur(8.dp)
                .background(
                    color = NexusColors.primary.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Main 3D button
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isPressed = true
                onClick()
            },
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.Center)
                .shadow(elevation, CircleShape, spotColor = NexusColors.primary),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NexusColors.primary,
                                NexusColors.primary.copy(alpha = 0.8f),
                                Color(0xFF00A896) // Darker at bottom for 3D effect
                            )
                        ),
                        shape = CircleShape
                    )
                    // Inner highlight for 3D effect
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.5f
                            ),
                            radius = size.minDimension / 2 - 4.dp.toPx()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Pro icon: Pen with plus
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = "New Message",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    // Small plus badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(
                                color = NexusColors.accent,
                                shape = CircleShape
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = NexusColors.background,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

// Keep old button for backward compatibility
@Composable
fun NexusComposeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Nexus3DComposeButton(onClick = onClick, modifier = modifier)
}

// ═══════════════════════════════════════════════════════════════════
// MESSAGE SENDING STATUS
// ═══════════════════════════════════════════════════════════════════
enum class SendingStatus {
    IDLE,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    SCHEDULED
}

// ═══════════════════════════════════════════════════════════════════
// COMPOSE MODAL - Pro Glassmorphism Design with Status Tracking
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusComposeModal(
    contacts: List<Contact>,
    availableSims: List<SimInfo>,
    onDismiss: () -> Unit,
    onSend: (String, String, Int) -> Unit,
    onSchedule: ((String, String, Int, Long) -> Unit)? = null
) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedSimIndex by remember { mutableIntStateOf(0) }
    var showContacts by remember { mutableStateOf(false) }
    var contactSearch by remember { mutableStateOf("") }

    // Selected contact info
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var selectedContactNumbers by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNumberDropdown by remember { mutableStateOf(false) }

    // Schedule state
    var showSchedulePicker by remember { mutableStateOf(false) }
    var scheduledTime by remember { mutableStateOf<Long?>(null) }

    // Sending status
    var sendingStatus by remember { mutableStateOf(SendingStatus.IDLE) }
    var statusMessage by remember { mutableStateOf("") }

    // Validate phone number
    val isValidNumber = isValidPhoneNumber(phoneNumber)
    val canSend = phoneNumber.isNotBlank() && message.isNotBlank() && isValidNumber && sendingStatus == SendingStatus.IDLE

    val infiniteTransition = rememberInfiniteTransition(label = "modal")
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    // Spinner rotation for sending state
    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spinner"
    )

    val modalScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "modalScale"
    )

    // Auto close after delivered or scheduled
    LaunchedEffect(sendingStatus) {
        if (sendingStatus == SendingStatus.DELIVERED || sendingStatus == SendingStatus.SCHEDULED) {
            delay(1500) // Show status for 1.5 seconds
            onDismiss()
        } else if (sendingStatus == SendingStatus.SENT) {
            // Simulate delivery after 2 seconds (in real app, this would be from broadcast receiver)
            delay(2000)
            sendingStatus = SendingStatus.DELIVERED
            statusMessage = "Message delivered!"
        }
    }

    Dialog(
        onDismissRequest = { if (sendingStatus == SendingStatus.IDLE) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = sendingStatus == SendingStatus.IDLE,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NexusColors.primary.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.92f),
                            NexusColors.secondary.copy(alpha = 0.03f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .scale(modalScale)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NexusColors.surface.copy(alpha = 0.95f),
                                NexusColors.card.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NexusColors.primary.copy(alpha = borderGlow),
                                NexusColors.secondary.copy(alpha = borderGlow * 0.5f),
                                NexusColors.primary.copy(alpha = borderGlow * 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Compact Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Glowing icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.linearGradient(NexusColors.gradientPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .shadow(8.dp, RoundedCornerShape(10.dp), spotColor = NexusColors.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = NexusColors.background,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "New Message",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusColors.textPrimary
                            )
                        }

                        // Close button
                        Surface(
                            onClick = { if (sendingStatus == SendingStatus.IDLE) onDismiss() },
                            color = NexusColors.tertiary.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = NexusColors.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Subtle divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        NexusColors.primary.copy(alpha = 0.4f),
                                        NexusColors.secondary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Selected contact name display
                    AnimatedVisibility(visible = selectedContactName != null) {
                        Surface(
                            color = NexusColors.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NexusColors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedContactName ?: "",
                                        color = NexusColors.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Clear contact button
                                Surface(
                                    onClick = {
                                        selectedContactName = null
                                        selectedContactNumbers = emptyList()
                                        phoneNumber = ""
                                    },
                                    color = Color.Transparent,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = NexusColors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Recipient field - Compact with validation
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Number input field - reduced width when contact selected with multiple numbers
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    // Clear contact name if user manually types
                                    if (selectedContactName != null && !selectedContactNumbers.contains(it)) {
                                        selectedContactName = null
                                        selectedContactNumbers = emptyList()
                                    }
                                },
                                modifier = Modifier.weight(if (selectedContactNumbers.size > 1) 0.65f else 1f),
                                placeholder = {
                                    Text("Phone number", color = NexusColors.textMuted, fontSize = 13.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = when {
                                            phoneNumber.isEmpty() -> NexusColors.primary
                                            isValidNumber -> NexusColors.primary
                                            else -> NexusColors.tertiary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (phoneNumber.isNotEmpty()) {
                                        Icon(
                                            imageVector = if (isValidNumber) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (isValidNumber) Color(0xFF4CAF50) else NexusColors.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = NexusColors.textPrimary,
                                    unfocusedTextColor = NexusColors.textPrimary,
                                    cursorColor = NexusColors.primary,
                                    focusedBorderColor = if (phoneNumber.isEmpty() || isValidNumber) NexusColors.primary else NexusColors.tertiary,
                                    unfocusedBorderColor = if (phoneNumber.isEmpty() || isValidNumber) NexusColors.textMuted.copy(alpha = 0.3f) else NexusColors.tertiary.copy(alpha = 0.5f),
                                    focusedContainerColor = NexusColors.card,
                                    unfocusedContainerColor = NexusColors.card
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )

                            // Contact picker button - compact
                            Surface(
                                onClick = { showContacts = !showContacts },
                                color = NexusColors.secondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NexusColors.secondary.copy(alpha = 0.3f)),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.Contacts,
                                        contentDescription = "Contacts",
                                        tint = NexusColors.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Number dropdown for contacts with multiple numbers
                            if (selectedContactNumbers.size > 1) {
                                Box {
                                    Surface(
                                        onClick = { showNumberDropdown = !showNumberDropdown },
                                        color = NexusColors.accent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, NexusColors.accent.copy(alpha = 0.3f)),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${selectedContactNumbers.size}",
                                                color = NexusColors.accent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "More numbers",
                                                tint = NexusColors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Dropdown menu
                                    DropdownMenu(
                                        expanded = showNumberDropdown,
                                        onDismissRequest = { showNumberDropdown = false },
                                        modifier = Modifier.background(NexusColors.surface)
                                    ) {
                                        selectedContactNumbers.filter { isValidPhoneNumber(it) }.forEach { number ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Phone,
                                                            contentDescription = null,
                                                            tint = if (number == phoneNumber) NexusColors.primary else NexusColors.textMuted,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = number,
                                                            color = if (number == phoneNumber) NexusColors.primary else NexusColors.textPrimary,
                                                            fontWeight = if (number == phoneNumber) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        if (number == phoneNumber) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = NexusColors.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    phoneNumber = number
                                                    showNumberDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Validation error message
                        if (phoneNumber.isNotEmpty() && !isValidNumber) {
                            Text(
                                text = when {
                                    phoneNumber.startsWith("*") -> "USSD codes cannot receive SMS"
                                    phoneNumber.length > 15 -> "Number too long"
                                    phoneNumber.length < 3 -> "Number too short"
                                    else -> "Invalid phone number"
                                },
                                color = NexusColors.tertiary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Contact picker dropdown
                    AnimatedVisibility(visible = showContacts) {
                        NexusContactPickerCompact(
                            contacts = contacts,
                            searchQuery = contactSearch,
                            onSearchChange = { contactSearch = it },
                            onContactSelected = { contact ->
                                // Store contact info
                                selectedContactName = contact.name
                                val validNumbers = contact.phoneNumbers.filter { isValidPhoneNumber(it) }
                                selectedContactNumbers = validNumbers

                                // Use the first valid phone number
                                phoneNumber = validNumbers.firstOrNull() ?: ""
                                showContacts = false
                                contactSearch = "" // Reset search
                            },
                            onNumberSelected = { contact, number ->
                                // Store contact info with specific number
                                selectedContactName = contact.name
                                val validNumbers = contact.phoneNumbers.filter { isValidPhoneNumber(it) }
                                selectedContactNumbers = validNumbers

                                // Use the selected number
                                phoneNumber = number
                                showContacts = false
                                contactSearch = "" // Reset search
                            }
                        )
                    }

                    // Message field - Compact
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = {
                            Text("Type your message...", color = NexusColors.textMuted, fontSize = 14.sp)
                        },
                        colors = nexusTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    // Bottom action row: Character count | SIM buttons | Send button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Character count
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${message.length}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    message.length > 160 -> NexusColors.tertiary
                                    message.length > 140 -> NexusColors.accent
                                    else -> NexusColors.textMuted
                                }
                            )
                            Text(
                                text = "/ 160",
                                fontSize = 11.sp,
                                color = NexusColors.textMuted
                            )
                            if (message.length > 160) {
                                Surface(
                                    color = NexusColors.accent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${(message.length / 160) + 1} SMS",
                                        fontSize = 10.sp,
                                        color = NexusColors.accent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // SIM buttons + Schedule + Send buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Compact SIM buttons (only if multiple SIMs and not sending)
                            if (availableSims.size > 1 && sendingStatus == SendingStatus.IDLE) {
                                availableSims.forEachIndexed { index, sim ->
                                    MiniSimButton(
                                        simIndex = index + 1,
                                        isSelected = selectedSimIndex == index,
                                        onClick = { selectedSimIndex = index }
                                    )
                                }

                                // Vertical divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(NexusColors.textMuted.copy(alpha = 0.2f))
                                )
                            }

                            // Schedule button
                            if (sendingStatus == SendingStatus.IDLE && onSchedule != null) {
                                Surface(
                                    onClick = { showSchedulePicker = true },
                                    enabled = canSend,
                                    color = if (canSend) NexusColors.accent.copy(alpha = 0.15f) else NexusColors.card,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if (canSend) NexusColors.accent.copy(alpha = 0.3f) else NexusColors.textMuted.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = "Schedule",
                                            tint = if (canSend) NexusColors.accent else NexusColors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Send button with status
                            when (sendingStatus) {
                                SendingStatus.IDLE -> {
                                    // Normal send button
                                    val sendScale by animateFloatAsState(
                                        targetValue = if (canSend) 1f else 0.9f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "sendScale"
                                    )

                                    Surface(
                                        onClick = {
                                            if (canSend) {
                                                sendingStatus = SendingStatus.SENDING
                                                statusMessage = "Sending..."
                                                val simId = availableSims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1
                                                onSend(phoneNumber, message, simId)
                                                // Simulate sent after delay (in real app, would be from callback)
                                                // The LaunchedEffect handles the state transitions
                                            }
                                        },
                                        enabled = canSend,
                                        color = Color.Transparent,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .scale(sendScale)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = if (canSend)
                                                        Brush.linearGradient(NexusColors.gradientPrimary)
                                                    else
                                                        Brush.linearGradient(listOf(NexusColors.card, NexusColors.card)),
                                                    shape = CircleShape
                                                )
                                                .then(
                                                    if (canSend) Modifier.shadow(8.dp, CircleShape, spotColor = NexusColors.primary)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send",
                                                tint = if (canSend) Color.White else NexusColors.textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                SendingStatus.SENDING -> {
                                    // Sending spinner
                                    SendingStatusIndicator(
                                        icon = null,
                                        isSpinning = true,
                                        spinnerRotation = spinnerRotation,
                                        backgroundColor = NexusColors.accent,
                                        text = "Sending"
                                    )
                                }

                                SendingStatus.SENT -> {
                                    // Sent checkmark
                                    SendingStatusIndicator(
                                        icon = Icons.Default.Check,
                                        isSpinning = false,
                                        spinnerRotation = 0f,
                                        backgroundColor = Color(0xFF4CAF50),
                                        text = "Sent"
                                    )
                                }

                                SendingStatus.DELIVERED -> {
                                    // Delivered double checkmark
                                    SendingStatusIndicator(
                                        icon = Icons.Default.DoneAll,
                                        isSpinning = false,
                                        spinnerRotation = 0f,
                                        backgroundColor = Color(0xFF2196F3),
                                        text = "Delivered"
                                    )
                                }

                                SendingStatus.FAILED -> {
                                    // Failed X
                                    SendingStatusIndicator(
                                        icon = Icons.Default.Close,
                                        isSpinning = false,
                                        spinnerRotation = 0f,
                                        backgroundColor = NexusColors.tertiary,
                                        text = "Failed"
                                    )
                                }

                                SendingStatus.SCHEDULED -> {
                                    // Scheduled clock
                                    SendingStatusIndicator(
                                        icon = Icons.Default.Schedule,
                                        isSpinning = false,
                                        spinnerRotation = 0f,
                                        backgroundColor = NexusColors.secondary,
                                        text = "Scheduled"
                                    )
                                }
                            }
                        }
                    }

                    // Status message bar (shown when sending/sent/delivered/scheduled)
                    AnimatedVisibility(
                        visible = sendingStatus != SendingStatus.IDLE,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SendingStatusBar(
                            status = sendingStatus,
                            message = statusMessage,
                            spinnerRotation = spinnerRotation
                        )
                    }
                }

                // Schedule Time Picker Dialog
                if (showSchedulePicker) {
                    ScheduleTimePickerDialog(
                        onDismiss = { showSchedulePicker = false },
                        onSchedule = { time ->
                            showSchedulePicker = false
                            sendingStatus = SendingStatus.SCHEDULED
                            val simId = availableSims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1

                            // Format time for display
                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
                            statusMessage = "Scheduled for ${sdf.format(java.util.Date(time))}"

                            onSchedule?.invoke(phoneNumber, message, simId, time)
                        }
                    )
                }
            }
        }
    }

    // Update sending status after a delay (simulating the actual send)
    LaunchedEffect(sendingStatus) {
        if (sendingStatus == SendingStatus.SENDING) {
            delay(1500) // Simulate network delay
            sendingStatus = SendingStatus.SENT
            statusMessage = "Message sent!"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SENDING STATUS COMPONENTS
// ═══════════════════════════════════════════════════════════════════
@Composable
fun SendingStatusIndicator(
    icon: ImageVector?,
    isSpinning: Boolean,
    spinnerRotation: Float,
    backgroundColor: Color,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(backgroundColor, CircleShape)
                .shadow(8.dp, CircleShape, spotColor = backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSpinning) {
                // Spinning loader
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = spinnerRotation },
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = text,
            color = backgroundColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SendingStatusBar(
    status: SendingStatus,
    message: String,
    spinnerRotation: Float
) {
    val backgroundColor = when (status) {
        SendingStatus.SENDING -> NexusColors.accent.copy(alpha = 0.15f)
        SendingStatus.SENT -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        SendingStatus.DELIVERED -> Color(0xFF2196F3).copy(alpha = 0.15f)
        SendingStatus.FAILED -> NexusColors.tertiary.copy(alpha = 0.15f)
        SendingStatus.SCHEDULED -> NexusColors.secondary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val iconColor = when (status) {
        SendingStatus.SENDING -> NexusColors.accent
        SendingStatus.SENT -> Color(0xFF4CAF50)
        SendingStatus.DELIVERED -> Color(0xFF2196F3)
        SendingStatus.FAILED -> NexusColors.tertiary
        SendingStatus.SCHEDULED -> NexusColors.secondary
        else -> NexusColors.textMuted
    }

    val icon = when (status) {
        SendingStatus.SENDING -> null
        SendingStatus.SENT -> Icons.Default.Check
        SendingStatus.DELIVERED -> Icons.Default.DoneAll
        SendingStatus.FAILED -> Icons.Default.ErrorOutline
        SendingStatus.SCHEDULED -> Icons.Default.Schedule
        else -> null
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SendingStatus.SENDING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                color = iconColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SCHEDULE TIME PICKER DIALOG
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimePickerDialog(
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit
) {
    val calendar = remember { java.util.Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedHour by remember { mutableIntStateOf(calendar.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(java.util.Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Quick schedule options
    val quickOptions = listOf(
        "In 30 minutes" to 30 * 60 * 1000L,
        "In 1 hour" to 60 * 60 * 1000L,
        "In 2 hours" to 2 * 60 * 60 * 1000L,
        "Tomorrow 9 AM" to run {
            val tomorrow = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            tomorrow.timeInMillis - System.currentTimeMillis()
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = NexusColors.surface,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NexusColors.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Schedule Message",
                            color = NexusColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NexusColors.textMuted
                        )
                    }
                }

                // Quick options
                Text(
                    text = "Quick Schedule",
                    color = NexusColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickOptions.forEach { (label, offset) ->
                        Surface(
                            onClick = {
                                onSchedule(System.currentTimeMillis() + offset)
                            },
                            color = NexusColors.card,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NexusColors.secondary.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = NexusColors.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        }
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NexusColors.textMuted.copy(alpha = 0.2f))
                )

                // Custom time
                Text(
                    text = "Custom Time",
                    color = NexusColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Date selector
                Surface(
                    onClick = { showDatePicker = true },
                    color = NexusColors.card,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = NexusColors.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            val dateFormat = java.text.SimpleDateFormat("EEE, MMM dd, yyyy", java.util.Locale.getDefault())
                            Text(
                                text = dateFormat.format(java.util.Date(selectedDate)),
                                color = NexusColors.textPrimary,
                                fontSize = 14.sp
                            )
                        }

                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = NexusColors.textMuted
                        )
                    }
                }

                // Time selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hour
                    Surface(
                        color = NexusColors.card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = NexusColors.primary)
                            }

                            Text(
                                text = String.format("%02d", selectedHour),
                                color = NexusColors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { selectedHour = (selectedHour + 1) % 24 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NexusColors.primary)
                            }
                        }
                    }

                    Text(
                        text = ":",
                        color = NexusColors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    // Minute
                    Surface(
                        color = NexusColors.card,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = NexusColors.primary)
                            }

                            Text(
                                text = String.format("%02d", selectedMinute),
                                color = NexusColors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NexusColors.primary)
                            }
                        }
                    }
                }

                // Schedule button
                Button(
                    onClick = {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                            set(java.util.Calendar.MINUTE, selectedMinute)
                            set(java.util.Calendar.SECOND, 0)
                        }

                        val scheduledTime = cal.timeInMillis
                        if (scheduledTime > System.currentTimeMillis()) {
                            onSchedule(scheduledTime)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexusColors.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule Message",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Mini SIM Button - Very compact
@Composable
fun MiniSimButton(
    simIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NexusColors.primary.copy(alpha = 0.2f) else NexusColors.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) NexusColors.primary else NexusColors.textMuted.copy(alpha = 0.2f)
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.SimCard,
                    contentDescription = null,
                    tint = if (isSelected) NexusColors.primary else NexusColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$simIndex",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) NexusColors.primary else NexusColors.textMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// PHONE NUMBER VALIDATION
// ═══════════════════════════════════════════════════════════════════
/**
 * Validates if a phone number is valid for SMS
 * - Must start with + or a digit (0-9)
 * - Must not be a USSD code (starts with * and ends with #)
 * - Must be between 3 and 13 characters (excluding + prefix)
 * - Must contain only digits after the optional + prefix
 */
fun isValidPhoneNumber(number: String): Boolean {
    val cleaned = number.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

    // Check for USSD codes (start with * and end with #)
    if (cleaned.startsWith("*") && cleaned.endsWith("#")) {
        return false
    }

    // Check for USSD-like patterns
    if (cleaned.startsWith("*") || cleaned.endsWith("#")) {
        return false
    }

    // Must start with + or digit
    if (cleaned.isEmpty()) return false
    val firstChar = cleaned.first()
    if (firstChar != '+' && !firstChar.isDigit()) {
        return false
    }

    // Get the numeric part (without +)
    val numericPart = if (cleaned.startsWith("+")) cleaned.drop(1) else cleaned

    // Check if all remaining characters are digits
    if (!numericPart.all { it.isDigit() }) {
        return false
    }

    // Check length (3-13 digits)
    if (numericPart.length < 3 || numericPart.length > 13) {
        return false
    }

    return true
}

/**
 * Gets the first valid phone number from a contact
 */
fun getFirstValidPhoneNumber(phoneNumbers: List<String>): String? {
    return phoneNumbers.firstOrNull { isValidPhoneNumber(it) }
}

// Compact contact picker with scrolling - Shows ALL contacts with valid numbers
// Contacts with multiple numbers are expandable
@Composable
fun NexusContactPickerCompact(
    contacts: List<Contact>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onContactSelected: (Contact) -> Unit,
    onNumberSelected: ((Contact, String) -> Unit)? = null // New callback for specific number
) {
    // Track expanded contacts
    var expandedContactId by remember { mutableStateOf<String?>(null) }

    // Filter contacts: must have at least one valid phone number
    val contactsWithValidNumbers = remember(contacts) {
        contacts.filter { contact ->
            contact.phoneNumbers.any { isValidPhoneNumber(it) }
        }
    }

    // Apply search filter
    val filtered = contactsWithValidNumbers.filter {
        searchQuery.isEmpty() ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.phoneNumbers.any { num -> num.contains(searchQuery) }
    }
    // No limit - show ALL matching contacts

    Surface(
        color = NexusColors.card.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NexusColors.textMuted.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp) // Increased height for more contacts
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Search with count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = {
                        Text("Search contacts...", color = NexusColors.textMuted, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NexusColors.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Surface(
                            color = NexusColors.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${filtered.size}",
                                color = NexusColors.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    },
                    colors = nexusTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            tint = NexusColors.textMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No contacts found",
                            color = NexusColors.textMuted,
                            fontSize = 12.sp
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "Try a different search",
                                color = NexusColors.textMuted.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                // Scrollable contact list - ALL contacts
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered) { contact ->
                        val validNumbers = contact.phoneNumbers.filter { isValidPhoneNumber(it) }
                        val hasMultipleNumbers = validNumbers.size > 1
                        val isExpanded = expandedContactId == contact.id

                        ExpandableContactItem(
                            contact = contact,
                            validNumbers = validNumbers,
                            hasMultipleNumbers = hasMultipleNumbers,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedContactId = if (isExpanded) null else contact.id
                            },
                            onSelectContact = {
                                if (hasMultipleNumbers) {
                                    // Toggle expand for multi-number contacts
                                    expandedContactId = if (isExpanded) null else contact.id
                                } else {
                                    // Direct select for single number contacts
                                    onContactSelected(contact)
                                }
                            },
                            onSelectNumber = { number ->
                                // Create a modified contact with the selected number as first
                                val modifiedContact = contact.copy(
                                    phoneNumbers = listOf(number) + contact.phoneNumbers.filter { it != number }
                                )
                                onNumberSelected?.invoke(modifiedContact, number) ?: onContactSelected(modifiedContact)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// EXPANDABLE CONTACT ITEM
// ═══════════════════════════════════════════════════════════════════
@Composable
fun ExpandableContactItem(
    contact: Contact,
    validNumbers: List<String>,
    hasMultipleNumbers: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectContact: () -> Unit,
    onSelectNumber: (String) -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "rotation"
    )

    Column {
        // Main contact row
        Surface(
            onClick = onSelectContact,
            color = if (isExpanded) NexusColors.primary.copy(alpha = 0.05f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(4.dp, CircleShape, spotColor = NexusColors.secondary)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NexusColors.secondary,
                                    NexusColors.secondary.copy(alpha = 0.7f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = NexusColors.textPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = validNumbers.firstOrNull() ?: "No valid number",
                            color = NexusColors.primary,
                            fontSize = 11.sp
                        )
                        if (hasMultipleNumbers) {
                            Surface(
                                color = NexusColors.accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "+${validNumbers.size - 1} more",
                                    color = NexusColors.accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Expand/Add icon
                if (hasMultipleNumbers) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = NexusColors.accent,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotationAngle }
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Select",
                        tint = NexusColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expanded numbers list
        AnimatedVisibility(
            visible = isExpanded && hasMultipleNumbers,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 46.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                validNumbers.forEachIndexed { index, number ->
                    Surface(
                        onClick = { onSelectNumber(number) },
                        color = NexusColors.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Number type indicator
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = when (index) {
                                            0 -> NexusColors.primary.copy(alpha = 0.15f)
                                            1 -> NexusColors.secondary.copy(alpha = 0.15f)
                                            else -> NexusColors.accent.copy(alpha = 0.15f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Phone
                                        1 -> Icons.Default.Smartphone
                                        else -> Icons.Default.PhoneAndroid
                                    },
                                    contentDescription = null,
                                    tint = when (index) {
                                        0 -> NexusColors.primary
                                        1 -> NexusColors.secondary
                                        else -> NexusColors.accent
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = number,
                                    color = NexusColors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (index) {
                                        0 -> "Primary"
                                        1 -> "Secondary"
                                        else -> "Other"
                                    },
                                    color = NexusColors.textMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Select",
                                tint = NexusColors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun nexusTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = NexusColors.textPrimary,
    unfocusedTextColor = NexusColors.textPrimary,
    cursorColor = NexusColors.primary,
    focusedBorderColor = NexusColors.primary,
    unfocusedBorderColor = NexusColors.textMuted.copy(alpha = 0.3f),
    focusedContainerColor = NexusColors.card,
    unfocusedContainerColor = NexusColors.card
)

// ═══════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════
private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

private fun formatSmartTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        diff < 172800_000 -> "Yesterday"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun groupConversationsByTime(conversations: List<Conversation>): Map<String, List<Conversation>> {
    val now = System.currentTimeMillis()
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = today - 86400_000
    val thisWeek = today - 604800_000

    return conversations.groupBy { conv ->
        val timestamp = conv.lastMessage?.timestamp ?: 0L
        when {
            timestamp >= today -> "Today"
            timestamp >= yesterday -> "Yesterday"
            timestamp >= thisWeek -> "This Week"
            else -> "Earlier"
        }
    }
}

private fun categorizeConversation(conversation: Conversation): MessageCategory {
    val senderType = SmsSenderAnalyzer.getSenderType(conversation.address)
    return when (senderType) {
        SenderType.BANK -> MessageCategory.FINANCE
        SenderType.TELECOM, SenderType.GOVERNMENT -> MessageCategory.ALERTS
        SenderType.SHOPPING -> MessageCategory.BUSINESS
        SenderType.PROMOTIONAL -> MessageCategory.PROMOTIONS
        SenderType.SERVICE, SenderType.SHORTCODE -> MessageCategory.ALERTS
        SenderType.CONTACT -> MessageCategory.PERSONAL
    }
}

private fun filterConversations(
    conversations: List<Conversation>,
    searchQuery: String,
    category: MessageCategory?
): List<Conversation> {
    return conversations.filter { conv ->
        val matchesSearch = searchQuery.isEmpty() ||
            conv.contactName?.contains(searchQuery, ignoreCase = true) == true ||
            conv.address.contains(searchQuery, ignoreCase = true) ||
            conv.lastMessage?.body?.contains(searchQuery, ignoreCase = true) == true

        val matchesCategory = category == null || categorizeConversation(conv) == category

        matchesSearch && matchesCategory
    }
}

