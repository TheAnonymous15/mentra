package com.example.mentra.shell.calling.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mentra.messaging.Contact

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL CALLING CONTACT PICKER DIALOG
 * Adaptive contact picker with integrated SIM selection for calling
 * ═══════════════════════════════════════════════════════════════════
 */

// Color palette
private object CallPickerColors {
    val background = Color(0xFF0D0D0D)
    val surface = Color(0xFF1A1A2E)
    val card = Color(0xFF16213E)
    val primary = Color(0xFF00FF41)      // Green for calls
    val secondary = Color(0xFF00D4FF)    // Cyan
    val tertiary = Color(0xFFFF2E63)     // Red/Pink
    val textPrimary = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF6C7A89)
    val sim1Color = Color(0xFF00D4FF)    // Cyan for SIM 1
    val sim2Color = Color(0xFFB388FF)    // Purple for SIM 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellCallingContactPickerDialog(
    title: String,
    contacts: List<Contact>,
    isLoading: Boolean = false,
    onCallWithSim: (Contact, String, Int) -> Unit, // contact, number, simSlot (0 or 1)
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedNumber by remember { mutableStateOf<String?>(null) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { contact ->
            contact.name.contains(searchQuery, ignoreCase = true) ||
            contact.phoneNumbers.any { it.contains(searchQuery) }
        }
    }

    // Calculate adaptive height based on content
    val contentHeight = remember(filteredContacts.size, isLoading, selectedContact) {
        when {
            isLoading -> 0.4f
            filteredContacts.isEmpty() -> 0.35f
            filteredContacts.size <= 3 -> 0.45f
            filteredContacts.size <= 6 -> 0.55f
            filteredContacts.size <= 10 -> 0.7f
            else -> 0.85f
        }
    }

    // Glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Main card with adaptive height
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(contentHeight)
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = CallPickerColors.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    CallingPickerHeader(
                        title = title,
                        glowAlpha = glowAlpha,
                        onClose = onDismiss
                    )

                    // Search bar
                    CallingSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    // Contact list or loading
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = CallPickerColors.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Loading contacts...",
                                    color = CallPickerColors.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else if (filteredContacts.isEmpty()) {
                        EmptyContactsState(searchQuery)
                    } else {
                        CallingContactsList(
                            contacts = filteredContacts,
                            selectedContact = selectedContact,
                            selectedNumber = selectedNumber,
                            onContactClick = { contact ->
                                if (contact.phoneNumbers.size == 1) {
                                    selectedContact = contact
                                    selectedNumber = contact.phoneNumbers.first()
                                } else {
                                    selectedContact = if (selectedContact == contact) null else contact
                                    selectedNumber = null
                                }
                            },
                            onNumberSelected = { contact, number ->
                                selectedContact = contact
                                selectedNumber = number
                            },
                            onCallWithSim = onCallWithSim,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Cancel button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = CallPickerColors.card,
                        shape = RoundedCornerShape(12.dp),
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "Cancel",
                            color = CallPickerColors.tertiary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(vertical = 14.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallingPickerHeader(
    title: String,
    glowAlpha: Float,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CallPickerColors.primary.copy(alpha = glowAlpha * 0.2f),
                        Color.Transparent
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(CallPickerColors.primary, CallPickerColors.secondary)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = CallPickerColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        tint = CallPickerColors.background,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallPickerColors.textPrimary
                    )
                    Text(
                        text = "Select contact & SIM to call",
                        fontSize = 12.sp,
                        color = CallPickerColors.textMuted
                    )
                }
            }

            // Close button
            Surface(
                onClick = onClose,
                color = CallPickerColors.tertiary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CallPickerColors.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = CallPickerColors.card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    CallPickerColors.primary.copy(alpha = 0.3f),
                    CallPickerColors.secondary.copy(alpha = 0.3f)
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
                tint = CallPickerColors.textMuted,
                modifier = Modifier.size(20.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalTextStyle.current.copy(
                    color = CallPickerColors.textPrimary,
                    fontSize = 15.sp
                ),
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search contacts...",
                                color = CallPickerColors.textMuted,
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
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = CallPickerColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallingContactsList(
    contacts: List<Contact>,
    selectedContact: Contact?,
    selectedNumber: String?,
    onContactClick: (Contact) -> Unit,
    onNumberSelected: (Contact, String) -> Unit,
    onCallWithSim: (Contact, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            CallingContactItem(
                contact = contact,
                isSelected = contact == selectedContact,
                selectedNumber = if (contact == selectedContact) selectedNumber else null,
                onClick = { onContactClick(contact) },
                onNumberSelected = { number -> onNumberSelected(contact, number) },
                onCallWithSim = { number, simSlot -> onCallWithSim(contact, number, simSlot) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallingContactItem(
    contact: Contact,
    isSelected: Boolean,
    selectedNumber: String?,
    onClick: () -> Unit,
    onNumberSelected: (String) -> Unit,
    onCallWithSim: (String, Int) -> Unit
) {
    val hasMultipleNumbers = contact.phoneNumbers.size > 1

    Surface(
        onClick = onClick,
        color = if (isSelected) CallPickerColors.primary.copy(alpha = 0.1f) else CallPickerColors.card,
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(1.dp, CallPickerColors.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Contact info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(CallPickerColors.primary, CallPickerColors.secondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        color = CallPickerColors.background,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Name and number info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = CallPickerColors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!hasMultipleNumbers) {
                        Text(
                            text = contact.phoneNumbers.firstOrNull() ?: "",
                            color = CallPickerColors.textMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "${contact.phoneNumbers.size} numbers",
                            color = CallPickerColors.secondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // SIM buttons for single number contacts (or when number is selected)
                if (!hasMultipleNumbers || selectedNumber != null) {
                    val numberToCall = selectedNumber ?: contact.phoneNumbers.firstOrNull() ?: ""
                    SimCallButtons(
                        onCallSim1 = { onCallWithSim(numberToCall, 0) },
                        onCallSim2 = { onCallWithSim(numberToCall, 1) }
                    )
                } else {
                    // Expand indicator for multiple numbers
                    Icon(
                        imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CallPickerColors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Number selection for contacts with multiple numbers
            AnimatedVisibility(
                visible = isSelected && hasMultipleNumbers,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = CallPickerColors.textMuted.copy(alpha = 0.2f))

                    Text(
                        text = "Select a number:",
                        color = CallPickerColors.textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    contact.phoneNumbers.forEach { number ->
                        NumberWithSimButtons(
                            number = number,
                            isSelected = number == selectedNumber,
                            onSelect = { onNumberSelected(number) },
                            onCallSim1 = { onCallWithSim(number, 0) },
                            onCallSim2 = { onCallWithSim(number, 1) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberWithSimButtons(
    number: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCallSim1: () -> Unit,
    onCallSim2: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = if (isSelected) CallPickerColors.secondary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) BorderStroke(1.dp, CallPickerColors.secondary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = CallPickerColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = number,
                    color = CallPickerColors.textPrimary,
                    fontSize = 14.sp
                )
            }

            SimCallButtons(
                onCallSim1 = onCallSim1,
                onCallSim2 = onCallSim2,
                compact = true
            )
        }
    }
}

@Composable
private fun SimCallButtons(
    onCallSim1: () -> Unit,
    onCallSim2: () -> Unit,
    compact: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        // SIM 1 Button
        Surface(
            onClick = onCallSim1,
            color = CallPickerColors.sim1Color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CallPickerColors.sim1Color.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 8.dp else 12.dp,
                    vertical = if (compact) 6.dp else 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SimCard,
                    contentDescription = null,
                    tint = CallPickerColors.sim1Color,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
                Text(
                    text = "1",
                    color = CallPickerColors.sim1Color,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // SIM 2 Button
        Surface(
            onClick = onCallSim2,
            color = CallPickerColors.sim2Color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CallPickerColors.sim2Color.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 8.dp else 12.dp,
                    vertical = if (compact) 6.dp else 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SimCard,
                    contentDescription = null,
                    tint = CallPickerColors.sim2Color,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
                Text(
                    text = "2",
                    color = CallPickerColors.sim2Color,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyContactsState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = CallPickerColors.textMuted,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (searchQuery.isBlank()) "No contacts found" else "No results for \"$searchQuery\"",
                color = CallPickerColors.textMuted,
                fontSize = 15.sp
            )
        }
    }
}

