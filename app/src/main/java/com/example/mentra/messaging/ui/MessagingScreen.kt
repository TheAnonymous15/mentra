package com.example.mentra.messaging.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Messaging Screen - Superior SMS App
 * Features:
 * - Beautiful conversation list
 * - Real-time message updates
 * - Contact integration
 * - Search & filter
 * - Unread badges
 * - Smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    viewModel: MessagingViewModel = hiltViewModel(),
    onOpenConversation: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val stats by viewModel.messageStats.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showNewMessage by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with stats
            MessagingHeader(
                stats = stats,
                onSearchClick = { /* Show search */ },
                onNewMessageClick = { showNewMessage = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { viewModel.searchMessages(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Conversations list
            ConversationsList(
                conversations = conversations.filter { conversation ->
                    searchQuery.isEmpty() ||
                    conversation.contactName?.contains(searchQuery, ignoreCase = true) == true ||
                    conversation.address.contains(searchQuery, ignoreCase = true) ||
                    conversation.lastMessage?.body?.contains(searchQuery, ignoreCase = true) == true
                },
                onConversationClick = onOpenConversation
            )
        }

        // New message FAB
        FloatingActionButton(
            onClick = { showNewMessage = true },
            containerColor = Color(0xFF4EC9B0),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(64.dp)
        ) {
            Icon(
                Icons.Default.Create,
                contentDescription = "New Message",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }

        // New message sheet
        if (showNewMessage) {
            NewMessageSheet(
                onDismiss = { showNewMessage = false },
                onSendMessage = { number, message ->
                    viewModel.sendMessage(number, message)
                    showNewMessage = false
                }
            )
        }
    }
}

@Composable
fun MessagingHeader(
    stats: com.example.mentra.messaging.MessageStatistics?,
    onSearchClick: () -> Unit,
    onNewMessageClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF1A1F3A).copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MESSAGES",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF4EC9B0),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    stats?.let {
                        Text(
                            text = "${it.conversationCount} conversations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFF569CD6).copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF569CD6)
                        )
                    }
                }
            }

            // Stats row
            stats?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBadge(
                        icon = Icons.Default.MailOutline,
                        count = it.totalMessages,
                        label = "Total",
                        color = Color(0xFF4EC9B0)
                    )
                    StatBadge(
                        icon = Icons.Default.MarkEmailUnread,
                        count = it.unreadCount,
                        label = "Unread",
                        color = Color(0xFFCE9178)
                    )
                    StatBadge(
                        icon = Icons.Default.Send,
                        count = it.sentCount,
                        label = "Sent",
                        color = Color(0xFF569CD6)
                    )
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.scale(if (count > 0) pulse else 1f)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = {
            Text(
                "Search messages, contacts...",
                color = Color.White.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "Search",
                tint = Color(0xFF4EC9B0)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        "Clear",
                        tint = Color.White
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF4EC9B0),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch(query) }
        )
    )
}

@Composable
fun ConversationsList(
    conversations: List<com.example.mentra.messaging.Conversation>,
    onConversationClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(conversations) { conversation ->
            ConversationItem(
                conversation = conversation,
                onClick = { onConversationClick(conversation.address) }
            )
        }
    }
}

@Composable
fun ConversationItem(
    conversation: com.example.mentra.messaging.Conversation,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "itemScale"
    )

    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        color = Color(0xFF1A1F3A).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = Color(0xFF4EC9B0).copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4EC9B0),
                                    Color(0xFF2A7A6F)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (conversation.contactName?.firstOrNull()
                            ?: conversation.address.firstOrNull()
                            ?: '?').toString().uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Unread badge
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .background(
                                color = Color(0xFFCE9178),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.coerceAtMost(99).toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Name / Number
                Text(
                    text = conversation.contactName ?: conversation.address,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Last message
                conversation.lastMessage?.let { message ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.type == com.example.mentra.messaging.MessageType.SENT) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Sent",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF569CD6)
                            )
                        }

                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isRead)
                                Color.White.copy(alpha = 0.6f)
                            else
                                Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold
                        )
                    }
                }
            }

            // Time
            conversation.lastMessage?.let { message ->
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4EC9B0),
                        fontSize = 11.sp
                    )

                    if (conversation.messageCount > 1) {
                        Text(
                            text = "${conversation.messageCount} msgs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageSheet(
    onDismiss: () -> Unit,
    onSendMessage: (String, String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1F3A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NEW MESSAGE",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4EC9B0),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, "Phone", tint = Color(0xFF4EC9B0))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4EC9B0),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF4EC9B0),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                label = { Text("Message") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4EC9B0),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Color(0xFF4EC9B0),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Button(
                onClick = {
                    if (phoneNumber.isNotBlank() && messageText.isNotBlank()) {
                        onSendMessage(phoneNumber, messageText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4EC9B0)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.Send, "Send", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Message", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

