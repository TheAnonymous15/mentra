package com.example.mentra.dialer.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.dialer.*

/**
 * Main Dialer Screen
 *
 * System-grade dialer with:
 * - Large touch targets
 * - No heavy animations
 * - Clear visual feedback
 * - Multi-SIM support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    viewModel: DialerViewModel = hiltViewModel(),
    onNavigateToInCall: () -> Unit = {}
) {
    val dialerInput by viewModel.dialerInput.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val callState by viewModel.callState.collectAsState()

    // Navigate to in-call screen when call is active
    LaunchedEffect(callState) {
        if (callState == CallState.DIALING || callState == CallState.ACTIVE || callState == CallState.RINGING) {
            onNavigateToInCall()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF0D1229),
                        Color(0xFF1A1F3A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            DialerHeader()

            // Tab bar
            DialerTabBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                missedCallCount = viewModel.getMissedCallCount()
            )

            // Content based on tab
            when (selectedTab) {
                DialerTab.KEYPAD -> {
                    KeypadContent(
                        input = dialerInput,
                        availableSims = availableSims,
                        selectedSimSlot = selectedSimSlot,
                        onDigitPressed = { viewModel.appendDigit(it) },
                        onDeletePressed = { viewModel.deleteLastDigit() },
                        onDeleteLongPressed = { viewModel.clearInput() },
                        onCallPressed = { viewModel.placeCall() },
                        onSimSelected = { viewModel.selectSim(it) }
                    )
                }
                DialerTab.RECENTS -> {
                    RecentsContent(
                        recentCalls = recentCalls,
                        onCallClick = { entry ->
                            viewModel.placeCallToNumber(entry.number)
                        },
                        onDeleteClick = { entry ->
                            viewModel.deleteCallLogEntry(entry.id)
                        }
                    )
                }
                DialerTab.CONTACTS -> {
                    ContactsContent(
                        onContactClick = { number ->
                            viewModel.setInput(number)
                            viewModel.selectTab(DialerTab.KEYPAD)
                        }
                    )
                }
                DialerTab.FAVORITES -> {
                    FavoritesContent(
                        onFavoriteClick = { number ->
                            viewModel.placeCallToNumber(number)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DialerHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF1A1F3A).copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Phone",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mentra Dialer",
                    color = Color(0xFF4EC9B0),
                    fontSize = 12.sp
                )
            }

            Icon(
                Icons.Default.Phone,
                contentDescription = "Phone",
                tint = Color(0xFF4EC9B0),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun DialerTabBar(
    selectedTab: DialerTab,
    onTabSelected: (DialerTab) -> Unit,
    missedCallCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DialerTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab

            Surface(
                onClick = { onTabSelected(tab) },
                color = if (isSelected) Color(0xFF4EC9B0).copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Icon(
                            imageVector = when (tab) {
                                DialerTab.KEYPAD -> Icons.Default.Dialpad
                                DialerTab.RECENTS -> Icons.Default.History
                                DialerTab.CONTACTS -> Icons.Default.Contacts
                                DialerTab.FAVORITES -> Icons.Default.Star
                            },
                            contentDescription = tab.name,
                            tint = if (isSelected) Color(0xFF4EC9B0) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )

                        // Badge for missed calls
                        if (tab == DialerTab.RECENTS && missedCallCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
                            ) {
                                Text(
                                    text = if (missedCallCount > 9) "9+" else missedCallCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (tab) {
                            DialerTab.KEYPAD -> "Keypad"
                            DialerTab.RECENTS -> "Recents"
                            DialerTab.CONTACTS -> "Contacts"
                            DialerTab.FAVORITES -> "Favorites"
                        },
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadContent(
    input: String,
    availableSims: List<SimAccount>,
    selectedSimSlot: Int,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onDeleteLongPressed: () -> Unit,
    onCallPressed: () -> Unit,
    onSimSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Number display
        NumberDisplay(
            number = input,
            onDelete = onDeletePressed,
            onDeleteLong = onDeleteLongPressed
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad
        Keypad(
            onDigitPressed = onDigitPressed
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SIM selector (if multiple SIMs)
        if (availableSims.size > 1) {
            SimSelector(
                sims = availableSims,
                selectedSlot = selectedSimSlot,
                onSimSelected = onSimSelected
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Call button
        CallButton(
            onClick = onCallPressed,
            enabled = input.isNotEmpty()
        )
    }
}

@Composable
fun NumberDisplay(
    number: String,
    onDelete: () -> Unit,
    onDeleteLong: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Color(0xFF1A1F3A).copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (number.isEmpty()) "Enter number" else number,
            color = if (number.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White,
            fontSize = if (number.length > 12) 24.sp else 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (number.isNotEmpty()) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.combinedClickable(
                    onClick = onDelete,
                    onLongClick = onDeleteLong
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = Color(0xFFCE9178),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Extension for long click
@Composable
private fun Modifier.combinedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    var isLongPressed by remember { mutableStateOf(false) }

    return this
        .clickable { onClick() }
        // Long press would need gesture detector
}

@Composable
fun Keypad(
    onDigitPressed: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    KeypadButton(
                        digit = digit,
                        letters = letters,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDigitPressed(digit)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = Color(0xFF2A2F4A),
        shape = CircleShape,
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SimSelector(
    sims: List<SimAccount>,
    selectedSlot: Int,
    onSimSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        sims.forEachIndexed { index, sim ->
            val isSelected = selectedSlot == sim.slotIndex || (selectedSlot == -1 && index == 0)

            Surface(
                onClick = { onSimSelected(sim.slotIndex) },
                color = if (isSelected) Color(0xFF4EC9B0) else Color(0xFF2A2F4A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SimCard,
                        contentDescription = "SIM",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = sim.getShortLabel(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun CallButton(
    onClick: () -> Unit,
    enabled: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "callScale"
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent,
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled) 12.dp else 4.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF4EC9B0),
                spotColor = Color(0xFF4EC9B0)
            )
            .background(
                brush = if (enabled)
                    Brush.radialGradient(listOf(Color(0xFF4EC9B0), Color(0xFF2A8A7A)))
                else
                    Brush.radialGradient(listOf(Color(0xFF3A3F5A), Color(0xFF2A2F4A))),
                shape = CircleShape
            )
    ) {
        Icon(
            Icons.Default.Phone,
            contentDescription = "Call",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun RecentsContent(
    recentCalls: List<CallLogEntry>,
    onCallClick: (CallLogEntry) -> Unit,
    onDeleteClick: (CallLogEntry) -> Unit
) {
    if (recentCalls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "No recents",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No recent calls",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recentCalls) { entry ->
                RecentCallItem(
                    entry = entry,
                    onClick = { onCallClick(entry) },
                    onDelete = { onDeleteClick(entry) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallItem(
    entry: CallLogEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val callTypeColor = when (entry.callType) {
        CallType.INCOMING -> Color(0xFF4EC9B0)
        CallType.OUTGOING -> Color(0xFF569CD6)
        CallType.MISSED -> Color(0xFFCE9178)
        CallType.REJECTED -> Color(0xFFE57373)
        else -> Color.White.copy(alpha = 0.6f)
    }

    val callTypeIcon = when (entry.callType) {
        CallType.INCOMING -> Icons.Default.CallReceived
        CallType.OUTGOING -> Icons.Default.CallMade
        CallType.MISSED -> Icons.Default.CallMissed
        CallType.REJECTED -> Icons.Default.CallEnd
        else -> Icons.Default.Phone
    }

    Surface(
        onClick = onClick,
        color = Color(0xFF1A1F3A).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call type indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = callTypeColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    callTypeIcon,
                    contentDescription = entry.callType.name,
                    tint = callTypeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.getDisplayName(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.getFormattedDate(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )

                    if (entry.duration > 0) {
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = entry.getFormattedDuration(),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Call button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFF4EC9B0).copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = Color(0xFF4EC9B0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ContactsContent(
    onContactClick: (String) -> Unit
) {
    // Placeholder - would integrate with contacts
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Contacts,
                contentDescription = "Contacts",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Contacts",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
            Text(
                text = "Contact integration coming soon",
                color = Color(0xFF4EC9B0).copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun FavoritesContent(
    onFavoriteClick: (String) -> Unit
) {
    // Placeholder - would integrate with favorites
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Favorites",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Favorites",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
            Text(
                text = "Add contacts to favorites",
                color = Color(0xFF4EC9B0).copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}

