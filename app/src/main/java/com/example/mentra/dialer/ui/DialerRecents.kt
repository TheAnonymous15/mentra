package com.example.mentra.dialer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.dialer.CallLogEntry
import com.example.mentra.dialer.CallSource
import com.example.mentra.dialer.CallType
import com.example.mentra.dialer.SimAccount
import com.example.mentra.dialer.ui.CallTypeFilter

/**
 * NEXUS DIALER - Recents Content
 * Call history with filtering, search, and message integration
 */

// ═══════════════════════════════════════════════════════════════════
// RECENTS CONTENT
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerRecentsContent(
    recentCalls: List<CallLogEntry>,
    availableSims: List<SimAccount>,
    callTypeFilter: CallTypeFilter,
    searchQuery: String,
    onCallTypeFilterChange: (CallTypeFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCallClick: (CallLogEntry) -> Unit,
    onMessageClick: (CallLogEntry) -> Unit
) {
    val filteredCalls = remember(recentCalls, callTypeFilter, searchQuery) {
        recentCalls
            .filter { entry ->
                when (callTypeFilter) {
                    CallTypeFilter.ALL -> true
                    CallTypeFilter.INCOMING -> entry.callType == CallType.INCOMING
                    CallTypeFilter.OUTGOING -> entry.callType == CallType.OUTGOING
                    CallTypeFilter.MISSED -> entry.callType == CallType.MISSED
                    CallTypeFilter.BLOCKED -> entry.callType == CallType.BLOCKED || entry.callType == CallType.REJECTED
                    CallTypeFilter.SOCIAL -> entry.isSocialCall() // WhatsApp, Telegram, etc.
                }
            }
            .filter { entry ->
                if (searchQuery.isEmpty()) true
                else {
                    val query = searchQuery.lowercase()
                    entry.contactName?.lowercase()?.contains(query) == true ||
                    entry.number.contains(query) ||
                    entry.callSource.displayName.lowercase().contains(query) // Search by app name too
                }
            }
            .sortedByDescending { it.timestamp }
    }

    if (recentCalls.isEmpty()) {
        DialerEmptyState(
            icon = Icons.Default.History,
            title = "No recent calls",
            subtitle = "Call history appears here"
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            RecentsSearchBar(searchQuery = searchQuery, onSearchQueryChange = onSearchQueryChange)

            Spacer(modifier = Modifier.height(12.dp))

            CallTypeFilterTabs(selectedFilter = callTypeFilter, onFilterSelected = onCallTypeFilterChange)

            Spacer(modifier = Modifier.height(8.dp))

            // Count indicators
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total: ${filteredCalls.size} calls", color = NexusDialerColors.textMuted, fontSize = 12.sp)
                Text("All: ${recentCalls.size}", color = NexusDialerColors.primary.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            if (filteredCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = NexusDialerColors.textMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No calls found", color = NexusDialerColors.textMuted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filteredCalls, key = { it.id }) { entry ->
                        RecentCallItem(
                            entry = entry,
                            onCallClick = { onCallClick(entry) },
                            onMessageClick = { onMessageClick(entry) }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SEARCH BAR
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun RecentsSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = NexusDialerColors.card,
        border = BorderStroke(1.dp, NexusDialerColors.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, "Search", tint = NexusDialerColors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(color = NexusDialerColors.textPrimary, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) Text("Search calls...", color = NexusDialerColors.textMuted, fontSize = 14.sp)
                    innerTextField()
                },
                singleLine = true
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Clear, "Clear", tint = NexusDialerColors.textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FILTER TABS
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CallTypeFilterTabs(
    selectedFilter: CallTypeFilter,
    onFilterSelected: (CallTypeFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CallTypeFilter.entries) { filter ->
            val isSelected = filter == selectedFilter
            val filterColor = when (filter) {
                CallTypeFilter.ALL -> NexusDialerColors.primary
                CallTypeFilter.INCOMING -> NexusDialerColors.success
                CallTypeFilter.OUTGOING -> NexusDialerColors.simBlue
                CallTypeFilter.MISSED -> NexusDialerColors.callRed
                CallTypeFilter.BLOCKED -> NexusDialerColors.textMuted
                CallTypeFilter.SOCIAL -> Color(0xFF25D366) // WhatsApp green
            }

            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) filterColor.copy(alpha = 0.15f) else Color.Transparent,
                border = BorderStroke(1.dp, if (isSelected) filterColor else NexusDialerColors.textMuted.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        when (filter) {
                            CallTypeFilter.ALL -> Icons.Default.AllInclusive
                            CallTypeFilter.INCOMING -> Icons.Default.CallReceived
                            CallTypeFilter.OUTGOING -> Icons.Default.CallMade
                            CallTypeFilter.MISSED -> Icons.Default.CallMissed
                            CallTypeFilter.BLOCKED -> Icons.Default.Block
                            CallTypeFilter.SOCIAL -> Icons.Default.Chat // Social apps icon
                        },
                        null,
                        tint = if (isSelected) filterColor else NexusDialerColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) filterColor else NexusDialerColors.textSecondary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// RECENT CALL ITEM
// ═══════════════════════════════════════════════════════════════════

@Composable
fun RecentCallItem(
    entry: CallLogEntry,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAddToFavorites: () -> Unit = {},
    onBlockNumber: () -> Unit = {},
    onDeleteCallLog: () -> Unit = {},
    onEditBeforeCall: () -> Unit = {},
    onAddToContacts: () -> Unit = {},
    onCopyNumber: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val callTypeIcon = when (entry.callType) {
        CallType.INCOMING -> Icons.Default.CallReceived
        CallType.OUTGOING -> Icons.Default.CallMade
        CallType.MISSED -> Icons.Default.CallMissed
        CallType.BLOCKED, CallType.REJECTED -> Icons.Default.Block
        else -> Icons.Default.Phone
    }

    val callTypeColor = when (entry.callType) {
        CallType.INCOMING -> NexusDialerColors.success
        CallType.OUTGOING -> NexusDialerColors.primary
        CallType.MISSED -> NexusDialerColors.callRed
        CallType.BLOCKED, CallType.REJECTED -> NexusDialerColors.textMuted
        else -> NexusDialerColors.textSecondary
    }

    // Social app badge color
    val socialAppColor = when {
        entry.callSource.displayName.contains("WhatsApp", ignoreCase = true) -> Color(0xFF25D366)
        entry.callSource.displayName.contains("Telegram", ignoreCase = true) -> Color(0xFF0088CC)
        entry.callSource.displayName.contains("Messenger", ignoreCase = true) -> Color(0xFF0084FF)
        entry.callSource.displayName.contains("Viber", ignoreCase = true) -> Color(0xFF665CAC)
        entry.callSource.displayName.contains("Signal", ignoreCase = true) -> Color(0xFF3A76F0)
        entry.callSource.displayName.contains("Discord", ignoreCase = true) -> Color(0xFF5865F2)
        entry.callSource.displayName.contains("Zoom", ignoreCase = true) -> Color(0xFF2D8CFF)
        entry.callSource.displayName.contains("Duo", ignoreCase = true) -> Color(0xFF1A73E8)
        entry.callSource.displayName.contains("Meet", ignoreCase = true) -> Color(0xFF00897B)
        entry.isSocialCall() -> Color(0xFF9C27B0) // Generic purple for other VoIP
        else -> null
    }

    Surface(
        onClick = onCallClick,
        shape = RoundedCornerShape(16.dp),
        color = NexusDialerColors.card.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, callTypeColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar/Icon with social badge
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(callTypeColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.contactName != null) {
                        Text(
                            entry.contactName.firstOrNull()?.uppercase()?.toString() ?: "#",
                            color = callTypeColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(callTypeIcon, null, tint = callTypeColor, modifier = Modifier.size(22.dp))
                    }
                }

                // Social app badge (bottom-right corner)
                if (socialAppColor != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .background(socialAppColor, CircleShape)
                            .border(1.5.dp, NexusDialerColors.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        entry.contactName ?: entry.number,
                        color = NexusDialerColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Social app label badge
                    if (entry.isSocialCall()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (socialAppColor ?: NexusDialerColors.secondary).copy(alpha = 0.2f)
                        ) {
                            Text(
                                entry.callSource.displayName,
                                color = socialAppColor ?: NexusDialerColors.secondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(callTypeIcon, null, tint = callTypeColor, modifier = Modifier.size(14.dp))
                    if (entry.contactName != null) {
                        Text(entry.number, color = NexusDialerColors.textMuted, fontSize = 12.sp)
                    }
                    Text(formatRelativeTime(entry.timestamp), color = NexusDialerColors.textMuted.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                if (entry.duration > 0) {
                    Text(formatCallDuration(entry.duration), color = NexusDialerColors.textMuted, fontSize = 11.sp)
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Message button
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(36.dp).background(NexusDialerColors.secondary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Message, "Message", tint = NexusDialerColors.secondary, modifier = Modifier.size(16.dp))
                }

                // Call button
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(36.dp).background(NexusDialerColors.callGreen.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = NexusDialerColors.callGreen, modifier = Modifier.size(16.dp))
                }

                // Context menu button
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, "More options", tint = NexusDialerColors.textMuted, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to favorites", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onAddToFavorites() },
                            leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit before calling", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onEditBeforeCall() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                        if (entry.contactName == null) {
                            DropdownMenuItem(
                                text = { Text("Add to contacts", fontSize = 13.sp) },
                                onClick = { showContextMenu = false; onAddToContacts() },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Copy number", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onCopyNumber() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block number", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onBlockNumber() },
                            leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete from call log", fontSize = 13.sp, color = NexusDialerColors.callRed) },
                            onClick = { showContextMenu = false; onDeleteCallLog() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexusDialerColors.callRed, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }
}

