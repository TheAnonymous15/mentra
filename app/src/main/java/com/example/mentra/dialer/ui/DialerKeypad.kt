package com.example.mentra.dialer.ui

import android.content.Context
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.dialer.CallLogEntry
import com.example.mentra.dialer.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NEXUS DIALER - Keypad Components
 * Number input, keypad grid, and suggestions panel
 */

// ═══════════════════════════════════════════════════════════════════
// KEYPAD CONTENT
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerKeypadContent(
    input: String,
    contactMatch: ContactMatch?,
    filteredContacts: List<DialerContact>,
    filteredCalls: List<CallLogEntry>,
    allCallLogs: List<CallLogEntry>,
    isKeypadVisible: Boolean,
    isDefaultDialer: Boolean,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onDeleteLongPressed: () -> Unit,
    onCallPressed: () -> Unit,
    onContactSelected: (DialerContact) -> Unit,
    onCallLogSelected: (CallLogEntry) -> Unit,
    onCallLogCall: (CallLogEntry) -> Unit = onCallLogSelected,
    onCallLogMessage: (CallLogEntry) -> Unit = {},
    onCallLogEditBeforeCall: (CallLogEntry) -> Unit = {},
    onCallLogDelete: (CallLogEntry) -> Unit = {},
    onCallLogBlock: (CallLogEntry) -> Unit = {},
    onCallLogAddToContacts: (CallLogEntry) -> Unit = {}
) {
    val showFiltered = input.length >= 2 && (filteredContacts.isNotEmpty() || filteredCalls.isNotEmpty())
    val showAllLogs = !isKeypadVisible && allCallLogs.isNotEmpty()
    val showRecentCalls = isKeypadVisible && input.isEmpty() && allCallLogs.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Suggestions Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showFiltered || showRecentCalls || showAllLogs,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.9f)
            ) {
                KeypadSuggestionsPanel(
                    filteredContacts = if (showFiltered) filteredContacts else emptyList(),
                    filteredCalls = if (showFiltered) filteredCalls else emptyList(),
                    recentCalls = allCallLogs,
                    showRecentCalls = showRecentCalls || showAllLogs,
                    isDefaultDialer = isDefaultDialer,
                    onContactSelected = onContactSelected,
                    onCallLogSelected = onCallLogSelected,
                    onCallLogCall = onCallLogCall,
                    onCallLogMessage = onCallLogMessage,
                    onCallLogEditBeforeCall = onCallLogEditBeforeCall,
                    onCallLogDelete = onCallLogDelete,
                    onCallLogBlock = onCallLogBlock,
                    onCallLogAddToContacts = onCallLogAddToContacts
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keypad Card
        AnimatedVisibility(
            visible = isKeypadVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = NexusDialerColors.card.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            NexusDialerColors.primary.copy(alpha = 0.2f),
                            NexusDialerColors.secondary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    NumberDisplayBox(
                        input = input,
                        contactMatch = contactMatch,
                        onDeletePressed = onDeletePressed,
                        onDeleteLongPressed = onDeleteLongPressed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    KeypadGrid(onDigitPressed = onDigitPressed)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════
// NUMBER DISPLAY
// ═══════════════════════════════════════════════════════════════════

@Composable
fun NumberDisplayBox(
    input: String,
    contactMatch: ContactMatch?,
    onDeletePressed: () -> Unit,
    onDeleteLongPressed: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (contactMatch != null) {
                Text(
                    text = contactMatch.name,
                    color = NexusDialerColors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = input.ifEmpty { "Enter number" },
                color = if (input.isEmpty())
                    NexusDialerColors.textMuted
                else
                    NexusDialerColors.textPrimary,
                fontSize = if (input.length > 12) 22.sp else 28.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }


        if (input.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NexusDialerColors.callRed.copy(alpha = 0.12f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeletePressed()
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteLongPressed()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = NexusDialerColors.callRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// KEYPAD GRID
// ═══════════════════════════════════════════════════════════════════

@Composable
fun KeypadGrid(onDigitPressed: (String) -> Unit) {
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    KeypadButton(
                        digit = digit,
                        letters = letters,
                        onClick = { onDigitPressed(digit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                color = NexusDialerColors.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    color = NexusDialerColors.textMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// SUGGESTIONS PANEL
// ═══════════════════════════════════════════════════════════════════

@Composable
fun KeypadSuggestionsPanel(
    filteredContacts: List<DialerContact>,
    filteredCalls: List<CallLogEntry>,
    recentCalls: List<CallLogEntry>,
    showRecentCalls: Boolean,
    isDefaultDialer: Boolean,
    onContactSelected: (DialerContact) -> Unit,
    onCallLogSelected: (CallLogEntry) -> Unit,
    onCallLogCall: (CallLogEntry) -> Unit = onCallLogSelected,
    onCallLogMessage: (CallLogEntry) -> Unit = {},
    onCallLogEditBeforeCall: (CallLogEntry) -> Unit = {},
    onCallLogDelete: (CallLogEntry) -> Unit = {},
    onCallLogBlock: (CallLogEntry) -> Unit = {},
    onCallLogAddToContacts: (CallLogEntry) -> Unit = {}
) {
    val context = LocalContext.current
    var showSearchModal by remember { mutableStateOf(false) }
    var allContacts by remember { mutableStateOf<List<DialerContact>>(emptyList()) }

    LaunchedEffect(Unit) {
        allContacts = loadDialerContacts(context)
    }

    val displayCalls = if (showRecentCalls) recentCalls else filteredCalls
    val hasContent = filteredContacts.isNotEmpty() || displayCalls.isNotEmpty()

    if (!hasContent) return

    if (showSearchModal) {
        AlienSearchModal(
            allCallLogs = recentCalls,
            allContacts = allContacts,
            onCallLogSelected = { entry ->
                showSearchModal = false
                onCallLogCall(entry)
            },
            onContactSelected = { contact ->
                showSearchModal = false
                onContactSelected(contact)
            },
            onDismiss = { showSearchModal = false }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NexusDialerColors.cardGlass.copy(alpha = 0.8f),
                            NexusDialerColors.card.copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NexusDialerColors.primary.copy(alpha = 0.5f),
                            NexusDialerColors.secondary.copy(alpha = 0.3f),
                            NexusDialerColors.primary.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                SuggestionsPanelHeader(
                    showRecentCalls = showRecentCalls,
                    isDefaultDialer = isDefaultDialer,
                    displayCount = displayCalls.size,
                    onSearchClick = { showSearchModal = true }
                )

                // Items
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredContacts.take(3)) { contact ->
                        SuggestionContactItem(contact = contact, onClick = { onContactSelected(contact) })
                    }

                    items(displayCalls) { entry ->
                        SuggestionCallItem(
                            entry = entry,
                            onClick = { onCallLogCall(entry) },
                            onMessageClick = { onCallLogMessage(entry) },
                            onEditBeforeCall = { onCallLogEditBeforeCall(entry) },
                            onDeleteCallLog = { onCallLogDelete(entry) },
                            onBlockNumber = { onCallLogBlock(entry) },
                            onAddToContacts = { onCallLogAddToContacts(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsPanelHeader(
    showRecentCalls: Boolean,
    isDefaultDialer: Boolean,
    displayCount: Int,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(NexusDialerColors.primary, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showRecentCalls) "CALL HISTORY" else "SUGGESTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NexusDialerColors.primary,
                letterSpacing = 1.2.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Search button
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(10.dp),
                color = NexusDialerColors.secondary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, NexusDialerColors.secondary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Search, null, tint = NexusDialerColors.secondary, modifier = Modifier.size(14.dp))
                    Text("SEARCH", fontSize = 9.sp, color = NexusDialerColors.secondary, fontWeight = FontWeight.Bold)
                }
            }

            // Default dialer badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDefaultDialer) NexusDialerColors.success.copy(alpha = 0.15f) else NexusDialerColors.callRed.copy(alpha = 0.15f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(5.dp).background(if (isDefaultDialer) NexusDialerColors.success else NexusDialerColors.callRed, CircleShape))
                    Text(if (isDefaultDialer) "DEFAULT" else "NOT DEFAULT", fontSize = 8.sp, color = if (isDefaultDialer) NexusDialerColors.success else NexusDialerColors.callRed, fontWeight = FontWeight.Bold)
                }
            }

            // Count badge
            Surface(shape = RoundedCornerShape(10.dp), color = NexusDialerColors.primary.copy(alpha = 0.12f)) {
                Text("$displayCount", fontSize = 10.sp, color = NexusDialerColors.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun SuggestionContactItem(contact: DialerContact, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = NexusDialerColors.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, NexusDialerColors.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(
                    brush = Brush.linearGradient(listOf(NexusDialerColors.primary, NexusDialerColors.secondary)),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(contact.name.firstOrNull()?.uppercase()?.toString() ?: "#", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, color = NexusDialerColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(contact.phoneNumber, color = NexusDialerColors.textMuted, fontSize = 12.sp)
            }

            Icon(Icons.Default.ChevronRight, null, tint = NexusDialerColors.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SuggestionCallItem(
    entry: CallLogEntry,
    onClick: () -> Unit,
    onMessageClick: () -> Unit = {},
    onEditBeforeCall: () -> Unit = {},
    onDeleteCallLog: () -> Unit = {},
    onBlockNumber: () -> Unit = {},
    onAddToContacts: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val callTypeIcon = when (entry.callType) {
        CallType.INCOMING -> Icons.Default.CallReceived
        CallType.OUTGOING -> Icons.Default.CallMade
        CallType.MISSED -> Icons.Default.CallMissed
        else -> Icons.Default.Phone
    }

    val callTypeColor = when (entry.callType) {
        CallType.INCOMING -> NexusDialerColors.success
        CallType.OUTGOING -> NexusDialerColors.primary
        CallType.MISSED -> NexusDialerColors.callRed
        else -> NexusDialerColors.textMuted
    }

    Surface(
        onClick = onClick,
        color = NexusDialerColors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, callTypeColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(callTypeColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(callTypeIcon, null, tint = callTypeColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.contactName ?: entry.number,
                    color = NexusDialerColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.contactName != null) {
                        Text(entry.number, color = NexusDialerColors.textMuted, fontSize = 11.sp)
                    }
                    Text(formatRelativeTime(entry.timestamp), color = NexusDialerColors.textMuted.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Message button
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Message, "Message", tint = NexusDialerColors.secondary, modifier = Modifier.size(16.dp))
                }

                // Context menu button
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, "More options", tint = NexusDialerColors.textMuted, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit before calling", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onEditBeforeCall() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete from call log", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onDeleteCallLog() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block number", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onBlockNumber() },
                            leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) }
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
                            onClick = {
                                showContextMenu = false
                                // Copy to clipboard handled externally
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DATA CLASSES & HELPERS
// ═══════════════════════════════════════════════════════════════════

data class DialerContact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class ContactMatch(
    val name: String,
    val photoUri: String? = null
)

suspend fun loadDialerContacts(context: Context): List<DialerContact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<DialerContact>()

    try {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: continue
                val number = cursor.getString(numberIndex) ?: continue
                val photo = cursor.getString(photoIndex)

                contacts.add(DialerContact(id, name, number.replace("\\s".toRegex(), ""), photo))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    contacts.distinctBy { it.phoneNumber.takeLast(10) }
}

