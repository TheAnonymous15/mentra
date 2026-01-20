package com.example.mentra.dialer.ui.call

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class for call log entry
 */
data class CallLogEntry(
    val name: String?,
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long
)

/**
 * Data class for contact entry
 */
data class ContactEntry(
    val name: String,
    val number: String,
    val photoUri: String? = null
)

/**
 * Tab options for Add Call Modal
 */
private enum class AddCallTab {
    RECENTS,
    CONTACTS,
    KEYPAD
}

/**
 * Add Call Modal - Shows recents, contacts and keypad to select number for conference
 */
@Composable
fun AddCallModal(
    onDismiss: () -> Unit,
    onCallNumber: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AddCallTab.RECENTS) }
    var searchQuery by remember { mutableStateOf("") }
    var dialpadNumber by remember { mutableStateOf("") }

    // Load data
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    var contacts by remember { mutableStateOf<List<ContactEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load call logs and contacts
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            callLogs = loadCallLogs(context)
            contacts = loadContacts(context)
        }
        isLoading = false
    }

    // Filter based on search
    val filteredCallLogs = remember(callLogs, searchQuery) {
        if (searchQuery.isBlank()) callLogs
        else callLogs.filter {
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.number.contains(searchQuery)
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.number.contains(searchQuery)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CallColors.voidBlack.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .shadow(32.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CallColors.glassCore,
                                    CallColors.deepSpace.copy(alpha = 0.98f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CallColors.cyberCyan.copy(alpha = 0.5f),
                                    CallColors.neonPurple.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        AddCallModalHeader(
                            onDismiss = onDismiss
                        )

                        // Search bar
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Tabs
                        AddCallTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        // Content based on selected tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = CallColors.cyberCyan,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            } else {
                                when (selectedTab) {
                                    AddCallTab.RECENTS -> {
                                        RecentsTab(
                                            callLogs = filteredCallLogs,
                                            onSelectNumber = { onCallNumber(it) }
                                        )
                                    }
                                    AddCallTab.CONTACTS -> {
                                        ContactsTab(
                                            contacts = filteredContacts,
                                            onSelectNumber = { onCallNumber(it) }
                                        )
                                    }
                                    AddCallTab.KEYPAD -> {
                                        KeypadTab(
                                            number = dialpadNumber,
                                            onNumberChange = { dialpadNumber = it },
                                            onCall = {
                                                if (dialpadNumber.isNotBlank()) {
                                                    onCallNumber(dialpadNumber)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCallModalHeader(
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = CallColors.cyberCyan,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Add Call",
                color = CallColors.textPure,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CallColors.rejectRed.copy(alpha = 0.2f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = CallColors.rejectRed),
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = CallColors.rejectRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                CallColors.glassSurface.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
            .border(
                1.dp,
                CallColors.borderSubtle,
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = CallColors.textMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = CallColors.textPure,
                    fontSize = 15.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(CallColors.cyberCyan),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search contacts or numbers...",
                                color = CallColors.textDim,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = CallColors.textMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
private fun AddCallTabs(
    selectedTab: AddCallTab,
    onTabSelected: (AddCallTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AddCallTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) CallColors.cyberCyan.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CallColors.cyberCyan.copy(alpha = 0.6f)
                               else CallColors.borderSubtle.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = CallColors.cyberCyan),
                        onClick = { onTabSelected(tab) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        when (tab) {
                            AddCallTab.RECENTS -> Icons.Default.History
                            AddCallTab.CONTACTS -> Icons.Default.Contacts
                            AddCallTab.KEYPAD -> Icons.Default.Dialpad
                        },
                        contentDescription = null,
                        tint = if (isSelected) CallColors.cyberCyan else CallColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (tab) {
                            AddCallTab.RECENTS -> "Recents"
                            AddCallTab.CONTACTS -> "Contacts"
                            AddCallTab.KEYPAD -> "Keypad"
                        },
                        color = if (isSelected) CallColors.cyberCyan else CallColors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentsTab(
    callLogs: List<CallLogEntry>,
    onSelectNumber: (String) -> Unit
) {
    if (callLogs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            message = "No recent calls"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(callLogs) { entry ->
                CallLogItem(
                    entry = entry,
                    onClick = { onSelectNumber(entry.number) }
                )
            }
        }
    }
}

@Composable
private fun CallLogItem(
    entry: CallLogEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CallColors.glassSurface.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CallColors.cyberCyan),
                onClick = onClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.neonPurple.copy(alpha = 0.3f),
                            CallColors.cyberCyan.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (entry.name?.firstOrNull() ?: entry.number.firstOrNull())
                    ?.uppercase()?.toString() ?: "?",
                color = CallColors.textPure,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name ?: entry.number,
                color = CallColors.textPure,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    when (entry.type) {
                        CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                        CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                        else -> Icons.Default.Call
                    },
                    contentDescription = null,
                    tint = when (entry.type) {
                        CallLog.Calls.MISSED_TYPE -> CallColors.rejectRed
                        else -> CallColors.textMuted
                    },
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = formatCallTime(entry.date),
                    color = CallColors.textDim,
                    fontSize = 12.sp
                )
            }
        }

        // Call button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CallColors.answerGreen.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, CallColors.answerGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Add to call",
                tint = CallColors.answerGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ContactsTab(
    contacts: List<ContactEntry>,
    onSelectNumber: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Contacts,
            message = "No contacts found"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onSelectNumber(contact.number) }
                )
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: ContactEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CallColors.glassSurface.copy(alpha = 0.3f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CallColors.cyberCyan),
                onClick = onClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CallColors.cyberCyan.copy(alpha = 0.3f),
                            CallColors.neonPurple.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.uppercase()?.toString() ?: "?",
                color = CallColors.textPure,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = CallColors.textPure,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.number,
                color = CallColors.textMuted,
                fontSize = 13.sp
            )
        }

        // Call button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CallColors.answerGreen.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, CallColors.answerGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Add to call",
                tint = CallColors.answerGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun KeypadTab(
    number: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Number display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    CallColors.glassSurface.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .border(1.dp, CallColors.borderSubtle, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = number.ifEmpty { "Enter number" },
                    color = if (number.isEmpty()) CallColors.textDim else CallColors.textPure,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (number.isNotEmpty()) {
                    Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = CallColors.textMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if (number.isNotEmpty()) {
                                    onNumberChange(number.dropLast(1))
                                }
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onNumberChange(number + key) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Call button
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(8.dp, CircleShape, ambientColor = CallColors.answerGreen.copy(alpha = 0.5f))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CallColors.answerGreen,
                            CallColors.answerGreen.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onCall,
                    enabled = number.isNotEmpty()
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(CallColors.glassSurface.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CallColors.cyberCyan),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            color = CallColors.textPure,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = CallColors.textDim,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = CallColors.textDim,
                fontSize = 14.sp
            )
        }
    }
}

// Helper functions

private fun loadCallLogs(context: Context): List<CallLogEntry> {
    val logs = mutableListOf<CallLogEntry>()
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

            var count = 0
            while (it.moveToNext() && count < 50) {
                logs.add(
                    CallLogEntry(
                        name = if (nameIdx >= 0) it.getString(nameIdx) else null,
                        number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else "",
                        type = if (typeIdx >= 0) it.getInt(typeIdx) else 0,
                        date = if (dateIdx >= 0) it.getLong(dateIdx) else 0,
                        duration = if (durationIdx >= 0) it.getLong(durationIdx) else 0
                    )
                )
                count++
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return logs
}

private fun loadContacts(context: Context): List<ContactEntry> {
    val contacts = mutableListOf<ContactEntry>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            val seen = mutableSetOf<String>()
            while (it.moveToNext()) {
                val number = if (numberIdx >= 0) it.getString(numberIdx)?.replace("\\s".toRegex(), "") ?: "" else ""
                if (number.isNotBlank() && number !in seen) {
                    seen.add(number)
                    contacts.add(
                        ContactEntry(
                            name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown",
                            number = number,
                            photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return contacts
}

private fun formatCallTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

