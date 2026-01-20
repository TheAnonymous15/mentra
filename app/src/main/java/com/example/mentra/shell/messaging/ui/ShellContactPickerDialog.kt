package com.example.mentra.shell.messaging.ui

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
import androidx.compose.ui.draw.blur
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
import com.example.mentra.messaging.ui.theme.NexusColors
import com.example.mentra.shell.messaging.AliasInfo
import com.example.mentra.shell.messaging.ContactAliasManager

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL CONTACT PICKER DIALOG
 * Futuristic contact selection popup for shell messaging
 * ═══════════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellContactPickerDialog(
    title: String,
    contacts: List<Contact>,
    forAlias: String? = null,
    isLoading: Boolean = false,
    onContactSelected: (Contact, String) -> Unit,
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

    val aliasInfo = forAlias?.let {
        ContactAliasManager.SUGGESTED_ALIASES.find { info ->
            info.alias.equals(it, ignoreCase = true)
        }
    }

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
            // Main card
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f)
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = NexusColors.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    ContactPickerHeader(
                        title = title,
                        aliasInfo = aliasInfo,
                        glowAlpha = glowAlpha,
                        onClose = onDismiss
                    )

                    // Search bar
                    ContactSearchBar(
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
                            CircularProgressIndicator(
                                color = NexusColors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else if (filteredContacts.isEmpty()) {
                        EmptyContactsState(searchQuery)
                    } else {
                        ContactsList(
                            contacts = filteredContacts,
                            selectedContact = selectedContact,
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
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Action buttons
                    ContactPickerActions(
                        selectedContact = selectedContact,
                        selectedNumber = selectedNumber,
                        onConfirm = {
                            selectedContact?.let { contact ->
                                selectedNumber?.let { number ->
                                    onContactSelected(contact, number)
                                }
                            }
                        },
                        onCancel = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactPickerHeader(
    title: String,
    aliasInfo: AliasInfo?,
    glowAlpha: Float,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NexusColors.primary.copy(alpha = glowAlpha * 0.2f),
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
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(NexusColors.gradientPrimary),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NexusColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (aliasInfo != null) {
                        Text(
                            text = aliasInfo.emoji,
                            fontSize = 22.sp
                        )
                    } else {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = null,
                            tint = NexusColors.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusColors.textPrimary
                    )
                    if (aliasInfo != null) {
                        Text(
                            text = aliasInfo.description,
                            fontSize = 12.sp,
                            color = NexusColors.textMuted
                        )
                    }
                }
            }

            // Close button
            Surface(
                onClick = onClose,
                color = NexusColors.tertiary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NexusColors.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = NexusColors.card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
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
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search contacts...",
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
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = NexusColors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    selectedContact: Contact?,
    onContactClick: (Contact) -> Unit,
    onNumberSelected: (Contact, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(contacts, key = { it.id }) { contact ->
            ContactListItem(
                contact = contact,
                isSelected = contact == selectedContact,
                onClick = { onContactClick(contact) },
                onNumberSelected = { number -> onNumberSelected(contact, number) }
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNumberSelected: (String) -> Unit
) {
    val hasMultipleNumbers = contact.phoneNumbers.size > 1
    var expanded by remember { mutableStateOf(false) }

    Column {
        Surface(
            onClick = {
                if (hasMultipleNumbers) {
                    expanded = !expanded
                } else {
                    onClick()
                }
            },
            color = if (isSelected) NexusColors.primary.copy(alpha = 0.15f) else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(1.dp, NexusColors.primary) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                if (isSelected) NexusColors.gradientPrimary
                                else listOf(NexusColors.card, NexusColors.cardHover)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        color = if (isSelected) NexusColors.background else NexusColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Name and number
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = NexusColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasMultipleNumbers) {
                            "${contact.phoneNumbers.size} numbers"
                        } else {
                            contact.phoneNumbers.firstOrNull() ?: ""
                        },
                        color = NexusColors.textMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expand indicator for multiple numbers
                if (hasMultipleNumbers) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NexusColors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = NexusColors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Expandable number list
        AnimatedVisibility(
            visible = expanded && hasMultipleNumbers,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                contact.phoneNumbers.forEach { number ->
                    Surface(
                        onClick = { onNumberSelected(number) },
                        color = NexusColors.card,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = number,
                                color = NexusColors.textSecondary,
                                fontSize = 13.sp
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
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
private fun EmptyContactsState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                tint = NexusColors.textMuted,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No contacts found for \"$searchQuery\""
                } else {
                    "No contacts available"
                },
                color = NexusColors.textSecondary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ContactPickerActions(
    selectedContact: Contact?,
    selectedNumber: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val canConfirm = selectedContact != null && selectedNumber != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NexusColors.card.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NexusColors.textSecondary
                ),
                border = BorderStroke(1.dp, NexusColors.textMuted.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            // Confirm button
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexusColors.primary,
                    disabledContainerColor = NexusColors.textMuted.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canConfirm) "Select" else "Choose Contact",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

