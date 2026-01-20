package com.example.mentra.messaging.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.messaging.*
import com.example.mentra.messaging.ui.components.*
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.messaging.ui.utils.filterConversations
// Import compose modal from legacy (will be extracted later)
import com.example.mentra.messaging.ui.legacy.NexusComposeModal

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA NEXUS MESSAGING SCREEN
 * Main entry point for the messaging UI
 * ═══════════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    viewModel: MessagingViewModel = hiltViewModel(),
    onOpenConversation: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()

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

    FuturisticBackground {
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
                FuturisticHeader(
                    onSearch = { /* TODO: Open search */ }
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
                },
                onSchedule = { number, message, simId, time ->
                    // TODO: Schedule message implementation
                }
            )
        }
    }
}

