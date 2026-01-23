package com.example.mentra.messaging.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.mentra.messaging.*
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.dialer.ui.NexusSimSelectionModal
import com.example.mentra.dialer.DialerManagerProvider
import java.text.SimpleDateFormat
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS CONVERSATION SCREEN
 * Full-Width Stunning Messages with Text Zoom
 * Background-Sorted, Paginated for Instant View
 * ═══════════════════════════════════════════════════════════════════
 */

// Pagination constants
private const val INITIAL_PAGE_SIZE = 50  // Load last 50 messages initially
private const val PAGE_SIZE = 30          // Load 30 more when scrolling up

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    phoneNumber: String,
    viewModel: MessagingViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val allMessages by viewModel.messages.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()
    var messageText by remember { mutableStateOf("") }

    // Context for making calls
    val context = LocalContext.current

    // Call modal state
    var showCallModal by remember { mutableStateOf(false) }

    // Use reverseLayout - this is the KEY to instant bottom view
    // Messages are displayed bottom-to-top, so index 0 is at the bottom
    val listState = rememberLazyListState()

    // Text zoom state (only affects message text size)
    var textZoom by remember { mutableFloatStateOf(1f) }

    // Pagination state
    var loadedMessageCount by remember { mutableIntStateOf(INITIAL_PAGE_SIZE) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // Pre-sorted messages (background sorted, newest LAST in list, but shown at BOTTOM due to reverseLayout)
    val sortedMessages = remember(allMessages) {
        allMessages.sortedBy { it.timestamp }
    }

    // Paginated messages - take the LAST N messages (most recent)
    val displayedMessages = remember(sortedMessages, loadedMessageCount) {
        val startIndex = maxOf(0, sortedMessages.size - loadedMessageCount)
        sortedMessages.subList(startIndex, sortedMessages.size)
    }

    // Check if there are more messages to load
    val hasMoreMessages = sortedMessages.size > loadedMessageCount

    // Sender analysis
    val senderType = remember(phoneNumber) { SmsSenderAnalyzer.getSenderType(phoneNumber) }
    val senderColor = remember(senderType) { Color(SmsSenderAnalyzer.getColorForSenderType(senderType)) }
    val senderIcon = remember(senderType) { SmsSenderAnalyzer.getIconForSenderType(senderType) }
    val isUnreplyable = remember(phoneNumber) { SmsSenderAnalyzer.isUnreplyable(phoneNumber) }

    // Load conversation
    LaunchedEffect(phoneNumber) {
        viewModel.loadConversation(phoneNumber)
    }

    // Load more messages when user scrolls to top (which is actually the end in reverse layout)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

                // If user has scrolled to see older messages (near the end of the list in reverse)
                // and there are more messages to load
                if (lastVisibleItem >= totalItems - 5 && hasMoreMessages && !isLoadingMore) {
                    isLoadingMore = true
                    // Load more messages
                    loadedMessageCount += PAGE_SIZE
                    isLoadingMore = false
                }
            }
    }

    // When new message is sent/received, ensure we see it (scroll to bottom = index 0 in reverse)
    val previousMessageCount = remember { mutableIntStateOf(allMessages.size) }
    LaunchedEffect(allMessages.size) {
        if (allMessages.size > previousMessageCount.intValue && allMessages.isNotEmpty()) {
            // New message arrived - scroll to bottom (index 0 in reverse layout)
            listState.animateScrollToItem(0)
        }
        previousMessageCount.intValue = allMessages.size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusColors.background)
            .statusBarsPadding() // Top padding for status bar
            .navigationBarsPadding() // Bottom padding for nav bar
    ) {
        // Animated Background
        ConversationBackground()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ConversationHeader(
                phoneNumber = phoneNumber,
                senderType = senderType,
                senderColor = senderColor,
                senderIcon = senderIcon,
                isUnreplyable = isUnreplyable,
                currentZoom = textZoom,
                onZoomReset = { textZoom = 1f },
                onBack = onBack,
                onCallPressed = { showCallModal = true }
            )

            // Messages with pinch-to-zoom for TEXT ONLY
            Box(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            // Only change text zoom, not page scale
                            textZoom = (textZoom * zoom).coerceIn(0.7f, 2.5f)
                        }
                    }
            ) {
                // Using reverseLayout so newest messages are at the bottom (index 0)
                // User starts at bottom and scrolls UP to see older messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true, // KEY: Start from bottom, scroll up for older
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Loading indicator at top (shown when scrolling to load more)
                    if (isLoadingMore) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NexusColors.primary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // "Load more" indicator when there are more messages
                    if (hasMoreMessages && !isLoadingMore) {
                        item(key = "load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↑ Scroll up for older messages",
                                    color = NexusColors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Messages in REVERSE order (newest first in list = at bottom visually)
                    // Since reverseLayout is true, we need to reverse our sorted list
                    val reversedMessages = displayedMessages.reversed()

                    // Group by date for display
                    val grouped = groupMessagesByDateReversed(reversedMessages)

                    grouped.forEach { (date, msgs) ->
                        // Messages first (they appear above the date due to reverse)
                        items(msgs, key = { it.id }) { message ->
                            FullWidthMessageBubble(
                                message = message,
                                textZoom = textZoom
                            )
                        }

                        // Date divider (appears below messages in this group due to reverse)
                        item(key = "date_$date") {
                            DateDivider(date = date)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Zoom indicator
                if (textZoom != 1f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        ZoomIndicator(
                            scale = textZoom,
                            onReset = { textZoom = 1f }
                        )
                    }
                }
            }

            // Input or unreplyable banner - sits flush with keyboard
            if (isUnreplyable) {
                UnreplyableBanner(senderType = senderType, senderIcon = senderIcon)
            } else {
                MessageInputBar(
                        text = messageText,
                        onTextChange = { messageText = it },
                        availableSims = availableSims,
                        onSend = { simId ->
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(phoneNumber, messageText, simId)
                                messageText = ""
                            }
                        },
                        onSchedule = { simId, scheduledTime ->
                            if (messageText.isNotBlank()) {
                                // TODO: Schedule message via viewModel
                                // viewModel.scheduleMessage(phoneNumber, messageText, simId, scheduledTime)
                                messageText = ""
                            }
                        }
                    )
                }
        }
    }

    // Call Modal - SIM selection only
    if (showCallModal) {
        val contactName = allMessages.firstOrNull()?.let {
            // Try to get contact name from the first message
            null // This would come from a contact lookup
        }

        // Convert availableSims to SimInfo list for call modal
        val simInfoList = availableSims.map { sim ->
            com.example.mentra.dialer.ui.SimInfo(
                slotIndex = sim.simSlotIndex,
                carrierName = sim.carrierName,
                phoneNumber = sim.phoneNumber
            )
        }

        // Only show SIM selection modal - close after call is placed
        // InCallActivity will handle the in-call UI
        NexusSimSelectionModal(
            phoneNumber = phoneNumber,
            contactName = contactName,
            availableSims = simInfoList,
            onDismiss = {
                showCallModal = false
            },
            onSimSelected = { simSlot ->
                // Place the call and close modal immediately
                // CallForegroundService will launch InCallActivity after 0.9s
                val dialerManager = DialerManagerProvider.getDialerManager()
                dialerManager?.placeCall(phoneNumber, simSlot)
                showCallModal = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// BACKGROUND
// ═══════════════════════════════════════════════════════════════════
@Composable
fun ConversationBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle grid
        val gridSize = 40.dp.toPx()
        for (x in 0..(size.width / gridSize).toInt()) {
            drawLine(
                color = Color.White.copy(alpha = 0.02f),
                start = Offset(x * gridSize, 0f),
                end = Offset(x * gridSize, size.height),
                strokeWidth = 0.5f
            )
        }
        for (y in 0..(size.height / gridSize).toInt()) {
            drawLine(
                color = Color.White.copy(alpha = 0.02f),
                start = Offset(0f, y * gridSize),
                end = Offset(size.width, y * gridSize),
                strokeWidth = 0.5f
            )
        }

        // Accent glows
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NexusColors.primary.copy(alpha = pulse), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.15f),
                radius = size.width * 0.35f
            ),
            center = Offset(size.width * 0.85f, size.height * 0.15f),
            radius = size.width * 0.35f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NexusColors.secondary.copy(alpha = pulse * 0.5f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.85f),
                radius = size.width * 0.3f
            ),
            center = Offset(size.width * 0.1f, size.height * 0.85f),
            radius = size.width * 0.3f
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════════════
@Composable
fun ConversationHeader(
    phoneNumber: String,
    senderType: SenderType,
    senderColor: Color,
    senderIcon: String,
    isUnreplyable: Boolean,
    currentZoom: Float,
    onZoomReset: () -> Unit,
    onBack: () -> Unit,
    onCallPressed: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NexusColors.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NexusColors.textPrimary
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(senderColor, senderColor.copy(alpha = 0.7f))),
                        shape = CircleShape
                    )
                    .border(1.5.dp, senderColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (senderType != SenderType.CONTACT) {
                    Text(text = senderIcon, fontSize = 18.sp)
                } else {
                    Text(
                        text = phoneNumber.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = phoneNumber,
                        color = NexusColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isUnreplyable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = NexusColors.tertiary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NO REPLY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusColors.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = getSenderLabel(senderType),
                    color = senderColor,
                    fontSize = 11.sp
                )
            }

            // Zoom reset
            if (currentZoom != 1f) {
                Surface(
                    onClick = onZoomReset,
                    color = NexusColors.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ZoomOutMap,
                            contentDescription = null,
                            tint = NexusColors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${(currentZoom * 100).toInt()}%",
                            color = NexusColors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isUnreplyable) {
                IconButton(onClick = { onCallPressed() }) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = NexusColors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DATE DIVIDER
// ═══════════════════════════════════════════════════════════════════
@Composable
fun DateDivider(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, NexusColors.textMuted.copy(alpha = 0.3f))
                        )
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(NexusColors.textMuted.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )
        }

        // Date pill
        Surface(
            color = NexusColors.surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(0.5.dp, NexusColors.textMuted.copy(alpha = 0.2f))
        ) {
            Text(
                text = date,
                color = NexusColors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DYNAMIC SIZE MESSAGE BUBBLE WITH TEXT ZOOM
// ═══════════════════════════════════════════════════════════════════
@Composable
fun FullWidthMessageBubble(
    message: SmsMessage,
    textZoom: Float = 1f
) {
    val isSent = message.type == MessageType.SENT

    // Calculate zoomed text size
    val baseTextSize = 15.sp
    val zoomedTextSize = (baseTextSize.value * textZoom).sp
    val zoomedLineHeight = (22f * textZoom).sp

    // Dynamic max width based on message length
    val maxWidth = when {
        message.body.length < 20 -> 150.dp  // Very short messages
        message.body.length < 50 -> 200.dp  // Short messages
        message.body.length < 100 -> 260.dp // Medium messages
        else -> 320.dp                       // Long messages
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        // Message bubble - DYNAMIC WIREFRAME DESIGN (fits content)
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isSent) 14.dp else 4.dp,
                bottomEnd = if (isSent) 4.dp else 14.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSent) NexusColors.primary.copy(alpha = 0.5f) else NexusColors.textMuted.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(min = 60.dp, max = maxWidth)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Sender indicator for received (only for longer messages)
                if (!isSent && message.body.length > 30) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(NexusColors.secondary, CircleShape)
                        )
                        Text(
                            text = "Received",
                            color = NexusColors.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Message text with zoom
                Text(
                    text = message.body,
                    color = if (isSent) NexusColors.primary else NexusColors.textPrimary,
                    fontSize = zoomedTextSize,
                    lineHeight = zoomedLineHeight
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Footer - compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = NexusColors.textMuted,
                        fontSize = 9.sp
                    )

                    if (isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // Delivery status
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = NexusColors.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ZOOM INDICATOR
// ═══════════════════════════════════════════════════════════════════
@Composable
fun ZoomIndicator(scale: Float, onReset: () -> Unit) {
    Surface(
        onClick = onReset,
        color = NexusColors.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NexusColors.primary.copy(alpha = 0.5f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ZoomIn,
                contentDescription = null,
                tint = NexusColors.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${(scale * 100).toInt()}%",
                color = NexusColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Tap to reset",
                color = NexusColors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// UNREPLYABLE BANNER
// ═══════════════════════════════════════════════════════════════════
@Composable
fun UnreplyableBanner(senderType: SenderType, senderIcon: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NexusColors.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NexusColors.tertiary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = senderIcon, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Automated Message",
                    color = NexusColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "${getSenderLabel(senderType)} • Cannot reply",
                    color = NexusColors.tertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// MESSAGE INPUT BAR WITH SCHEDULING
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    availableSims: List<SimInfo>,
    onSend: (Int) -> Unit,
    onSchedule: ((Int, Long) -> Unit)? = null
) {
    var selectedSimIndex by remember { mutableIntStateOf(0) }
    var showSchedulePicker by remember { mutableStateOf(false) }
    val hasMultipleSims = availableSims.size > 1
    val canSend = text.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NexusColors.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message...", color = NexusColors.textMuted, fontSize = 14.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NexusColors.textPrimary,
                    unfocusedTextColor = NexusColors.textPrimary,
                    cursorColor = NexusColors.primary,
                    focusedBorderColor = NexusColors.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = NexusColors.textMuted.copy(alpha = 0.2f),
                    focusedContainerColor = NexusColors.card,
                    unfocusedContainerColor = NexusColors.card
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            val simId = availableSims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1
                            onSend(simId)
                        }
                    }
                )
            )

            // Compact SIM buttons (if multiple) - 3D style, same height as input
            if (hasMultipleSims) {
                availableSims.forEachIndexed { index, _ ->
                    MiniSimSelector3D(
                        index = index + 1,
                        isSelected = selectedSimIndex == index,
                        onClick = { selectedSimIndex = index }
                    )
                }
            }

            // Schedule button - 3D style, same height as input, orange/yellow theme
            if (onSchedule != null) {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .width(48.dp)
                        .shadow(
                            elevation = if (canSend) 6.dp else 1.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = if (canSend) Color(0xFFFFE66D) else Color.Transparent
                        )
                        .background(
                            brush = if (canSend) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFE66D).copy(alpha = 0.5f),
                                        Color(0xFFFFD93D).copy(alpha = 0.3f),
                                        Color(0xFFFFCC00).copy(alpha = 0.2f)
                                    ),
                                    center = Offset(0.3f, 0.3f),
                                    radius = 180f
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NexusColors.card,
                                        NexusColors.card.copy(alpha = 0.8f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.5.dp,
                            if (canSend) Color(0xFFFFE66D).copy(alpha = 0.5f) else NexusColors.textMuted.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canSend) { showSchedulePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Schedule",
                        tint = if (canSend) Color(0xFFFFE66D) else NexusColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Send button - 3D style, same height as input, green/cyan theme
            val sendScale by animateFloatAsState(
                targetValue = if (canSend) 1f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "send"
            )

            Box(
                modifier = Modifier
                    .height(56.dp)
                    .width(48.dp)
                    .scale(sendScale)
                    .shadow(
                        elevation = if (canSend) 8.dp else 1.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = if (canSend) Color(0xFF00F5D4) else Color.Transparent
                    )
                    .background(
                        brush = if (canSend) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00F5D4),
                                    Color(0xFF00D4AA),
                                    Color(0xFF00B38A)
                                ),
                                center = Offset(0.3f, 0.3f),
                                radius = 200f
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    NexusColors.card,
                                    NexusColors.card.copy(alpha = 0.8f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.5.dp,
                        if (canSend) Color(0xFF00F5D4).copy(alpha = 0.3f) else NexusColors.textMuted.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = canSend) {
                        if (canSend) {
                            val simId = availableSims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1
                            onSend(simId)
                        }
                    },
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

    // Schedule picker dialog
    if (showSchedulePicker && onSchedule != null) {
        ReplySchedulePickerDialog(
            onDismiss = { showSchedulePicker = false },
            onSchedule = { time ->
                showSchedulePicker = false
                val simId = availableSims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1
                onSchedule(simId, time)
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// REPLY SCHEDULE PICKER DIALOG (Compact version)
// ═══════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplySchedulePickerDialog(
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE)) }

    // Quick options
    val quickOptions = listOf(
        "30m" to 30 * 60 * 1000L,
        "1h" to 60 * 60 * 1000L,
        "2h" to 2 * 60 * 60 * 1000L,
        "Tomorrow" to run {
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }
            tomorrow.timeInMillis - System.currentTimeMillis()
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexusColors.surface,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = NexusColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Schedule Reply",
                    color = NexusColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick options
                Text(
                    "Quick Schedule",
                    color = NexusColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.forEach { (label, offset) ->
                        Surface(
                            onClick = { onSchedule(System.currentTimeMillis() + offset) },
                            color = NexusColors.card,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NexusColors.accent.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = NexusColors.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
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
                    "Custom Time",
                    color = NexusColors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // Time selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { selectedHour = (selectedHour + 1) % 24 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = NexusColors.primary)
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            color = NexusColors.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = NexusColors.primary)
                        }
                    }

                    Text(
                        ":",
                        color = NexusColors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { selectedMinute = (selectedMinute + 5) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = NexusColors.primary)
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = NexusColors.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = NexusColors.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        // If time is in the past today, schedule for tomorrow
                        if (timeInMillis < System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    onSchedule(cal.timeInMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexusColors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NexusColors.textMuted)
            }
        }
    )
}

@Composable
fun MiniSimSelector3D(
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(56.dp)
            .width(42.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isSelected) Color(0xFF00F5D4) else Color.Transparent
            )
            .background(
                brush = if (isSelected) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F5D4).copy(alpha = 0.4f),
                            Color(0xFF00D4AA).copy(alpha = 0.2f),
                            Color(0xFF00B38A).copy(alpha = 0.1f)
                        ),
                        center = Offset(0.3f, 0.3f),
                        radius = 200f
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            NexusColors.card,
                            NexusColors.card.copy(alpha = 0.7f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                1.5.dp,
                if (isSelected) Color(0xFF00F5D4).copy(alpha = 0.5f) else NexusColors.textMuted.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SimCard,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00F5D4) else NexusColors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$index",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF00F5D4) else NexusColors.textMuted
            )
        }
    }
}

@Composable
fun MiniSimSelector(
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) NexusColors.primary.copy(alpha = 0.2f) else NexusColors.card,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) NexusColors.primary else NexusColors.textMuted.copy(alpha = 0.2f)
        ),
        modifier = Modifier.size(32.dp)
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
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$index",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) NexusColors.primary else NexusColors.textMuted
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════════
private fun getSenderLabel(senderType: SenderType): String {
    return when (senderType) {
        SenderType.BANK -> "💰 Finance"
        SenderType.TELECOM -> "📱 Telecom"
        SenderType.SHOPPING -> "🛒 Shopping"
        SenderType.GOVERNMENT -> "🏛️ Government"
        SenderType.PROMOTIONAL -> "📢 Promo"
        SenderType.SHORTCODE -> "📟 Shortcode"
        SenderType.SERVICE -> "🏢 Service"
        SenderType.CONTACT -> "💬 Personal"
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun groupMessagesByDate(messages: List<SmsMessage>): Map<String, List<SmsMessage>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = today - 86400_000

    // First, sort all messages by timestamp ascending (oldest first, newest last/at bottom)
    val sortedMessages = messages.sortedBy { it.timestamp }

    // Group by date label
    val grouped = sortedMessages.groupBy { msg ->
        when {
            msg.timestamp >= today -> "Today"
            msg.timestamp >= yesterday -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(msg.timestamp))
        }
    }

    // Sort groups by the earliest timestamp in each group (chronological order)
    return grouped.toSortedMap(compareBy { dateLabel ->
        grouped[dateLabel]?.firstOrNull()?.timestamp ?: 0L
    })
}

/**
 * Groups messages by date for REVERSE layout display.
 * Messages within each group are kept in the provided order (already reversed).
 * Groups are sorted by newest first (for reverse layout - newest at bottom visually).
 */
private fun groupMessagesByDateReversed(messages: List<SmsMessage>): List<Pair<String, List<SmsMessage>>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = today - 86400_000

    // Group by date label (messages are already in reversed order - newest first)
    val grouped = messages.groupBy { msg ->
        when {
            msg.timestamp >= today -> "Today"
            msg.timestamp >= yesterday -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(msg.timestamp))
        }
    }

    // Sort groups by newest first (for reverse layout)
    // In reverse layout: first groups in list appear at BOTTOM, so newest should be first
    return grouped.toList().sortedByDescending { (_, msgs) ->
        msgs.firstOrNull()?.timestamp ?: 0L
    }
}

