package com.example.mentra.dialer.ui

import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mentra.dialer.CallLogEntry
import com.example.mentra.dialer.CallType
import com.example.mentra.dialer.SimAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NEXUS DIALER - Modals
 * SIM Selection, Quick Message, and Alien Search modals
 */

// ═══════════════════════════════════════════════════════════════════
// SIM SELECTION MODAL
// ═══════════════════════════════════════════════════════════════════

@Composable
fun SimSelectionModal(
    sims: List<SimAccount>,
    phoneNumber: String,
    contactName: String? = null,
    contactPhotoUri: String? = null,
    onSimSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "simModal")

    // Load contact photo - try provided URI first, then look up by phone number
    var contactBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(contactPhotoUri, phoneNumber) {
        contactBitmap = withContext(Dispatchers.IO) {
            // First try the provided photo URI
            if (contactPhotoUri != null) {
                try {
                    val uri = Uri.parse(contactPhotoUri)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                // Try to look up contact photo by phone number
                try {
                    val contactUri = Uri.withAppendedPath(
                        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(phoneNumber)
                    )
                    context.contentResolver.query(
                        contactUri,
                        arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val photoUriStr = cursor.getString(
                                cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                            )
                            if (photoUriStr != null) {
                                val photoUri = Uri.parse(photoUriStr)
                                context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                                    android.graphics.BitmapFactory.decodeStream(inputStream)
                                }
                            } else null
                        } else null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    // Rotating ring animation for avatar border
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 300.dp)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0D1117),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            NexusDialerColors.primary.copy(alpha = 0.6f),
                            NexusDialerColors.secondary.copy(alpha = 0.3f)
                        )
                    )
                ),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact avatar with animated ring
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotating outer ring
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(ringRotation)
                        ) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        NexusDialerColors.primary,
                                        NexusDialerColors.secondary,
                                        Color.Transparent,
                                        NexusDialerColors.primary
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Contact photo or initial
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            NexusDialerColors.primary.copy(alpha = 0.3f),
                                            NexusDialerColors.secondary.copy(alpha = 0.3f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = contactBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Contact Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Show initial letter or icon
                                if (contactName != null) {
                                    Text(
                                        contactName.firstOrNull()?.uppercase()?.toString() ?: "#",
                                        color = NexusDialerColors.primary,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NexusDialerColors.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Contact info - compact
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (contactName != null) {
                            Text(
                                contactName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NexusDialerColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            phoneNumber,
                            fontSize = 13.sp,
                            color = NexusDialerColors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }


                    // SIM cards in single card container
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NexusDialerColors.cardGlass.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, NexusDialerColors.textMuted.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Select SIM" label
                            Text(
                                "Select SIM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = NexusDialerColors.textMuted,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sims.forEachIndexed { index, sim ->
                                    val simColor = if (index == 0) NexusDialerColors.simBlue else NexusDialerColors.simPurple
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.95f else 1f,
                                        animationSpec = spring(dampingRatio = 0.5f),
                                        label = "scale"
                                    )

                                    Surface(
                                        onClick = { onSimSelected(sim.slotIndex) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .scale(scale),
                                        shape = RoundedCornerShape(10.dp),
                                        color = simColor.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, simColor.copy(alpha = 0.3f)),
                                        interactionSource = interactionSource
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // SIM number badge - smaller
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(
                                                        simColor.copy(alpha = 0.2f),
                                                        RoundedCornerShape(6.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${index + 1}",
                                                    color = simColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // SIM label - smaller
                                            Text(
                                                "SIM ${index + 1}",
                                                color = NexusDialerColors.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            // Network/Carrier - smaller
                                            Text(
                                                sim.carrierName.ifEmpty { "Unknown" },
                                                color = simColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cancel button - compact
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = NexusDialerColors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ALIEN SEARCH MODAL
// ═══════════════════════════════════════════════════════════════════

data class SearchResults(
    val callLogs: List<CallLogEntry>,
    val contacts: List<DialerContact>
)

@Composable
fun AlienSearchModal(
    allCallLogs: List<CallLogEntry>,
    allContacts: List<DialerContact>,
    onCallLogSelected: (CallLogEntry) -> Unit,
    onContactSelected: (DialerContact) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "alienSearch")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "glowPulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(15000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "ringRotation"
    )

    val searchResults = remember(searchQuery, allCallLogs, allContacts) {
        if (searchQuery.length < 2) {
            SearchResults(emptyList(), emptyList())
        } else {
            val query = searchQuery.lowercase()
            val matchingCalls = allCallLogs.filter { it.number.contains(query) || it.contactName?.lowercase()?.contains(query) == true }.distinctBy { it.number }.take(10)
            val matchingContacts = if (matchingCalls.size < 5) {
                allContacts.filter { it.phoneNumber.contains(query) || it.name.lowercase().contains(query) }
                    .filterNot { contact -> matchingCalls.any { it.number.takeLast(10) == contact.phoneNumber.takeLast(10) } }
                    .take(10 - matchingCalls.size)
            } else emptyList()
            SearchResults(matchingCalls, matchingContacts)
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Background glow effect
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2.5f
                for (i in 1..3) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = glowPulse * 0.15f / i),
                                Color(0xFF7C4DFF).copy(alpha = glowPulse * 0.1f / i),
                                Color(0xFFFF4081).copy(alpha = glowPulse * 0.05f / i),
                                Color(0xFF00E5FF).copy(alpha = glowPulse * 0.15f / i)
                            ),
                            center = Offset(centerX, centerY)
                        ),
                        radius = 200.dp.toPx() + (i * 50.dp.toPx()),
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Modal content
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.7f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(listOf(Color(0xFF0A1628).copy(alpha = 0.95f), Color(0xFF050D18).copy(alpha = 0.98f))),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF7C4DFF).copy(alpha = 0.4f), Color(0xFFFF4081).copy(alpha = 0.3f))), RoundedCornerShape(32.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier.size(36.dp).rotate(ringRotation)
                                        .background(Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color.Transparent, Color(0xFF00E5FF))), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(30.dp).background(Color(0xFF0A1628), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Search, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text("NEXUS SEARCH", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), letterSpacing = 3.sp)
                            }
                            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onDismiss() }) {
                                Icon(Icons.Default.Close, "Close", tint = Color(0xFFFF4081), modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Search input
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0D1A2D).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF).copy(alpha = if (searchQuery.isNotEmpty()) 0.8f else 0.3f), Color(0xFF7C4DFF).copy(alpha = if (searchQuery.isNotEmpty()) 0.6f else 0.2f))))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = Color(0xFF00E5FF).copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) Text("Search calls & contacts...", color = Color(0xFF6B7A99), fontSize = 16.sp)
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, "Clear", tint = Color(0xFF6B7A99), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Results
                        if (searchQuery.length >= 2) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RESULTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF), letterSpacing = 2.sp)
                                Text("${searchResults.callLogs.size + searchResults.contacts.size} found", fontSize = 10.sp, color = Color(0xFF6B7A99))
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (searchResults.callLogs.isNotEmpty()) {
                                item { Text("📞 CALL HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                                items(searchResults.callLogs) { entry ->
                                    SearchResultItem(
                                        title = entry.contactName ?: entry.number,
                                        subtitle = if (entry.contactName != null) entry.number else null,
                                        callType = entry.callType,
                                        timestamp = entry.timestamp,
                                        isCallLog = true,
                                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCallLogSelected(entry) }
                                    )
                                }
                            }

                            if (searchResults.contacts.isNotEmpty()) {
                                item { Text("👤 CONTACTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF), letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                                items(searchResults.contacts) { contact ->
                                    SearchResultItem(
                                        title = contact.name,
                                        subtitle = contact.phoneNumber,
                                        callType = null,
                                        timestamp = null,
                                        isCallLog = false,
                                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onContactSelected(contact) }
                                    )
                                }
                            }

                            if (searchQuery.length >= 2 && searchResults.callLogs.isEmpty() && searchResults.contacts.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.SearchOff, null, tint = Color(0xFF6B7A99), modifier = Modifier.size(48.dp))
                                            Text("No results found", color = Color(0xFF6B7A99), fontSize = 14.sp)
                                        }
                                    }
                                }
                            }

                            if (searchQuery.length < 2) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("🔮", fontSize = 48.sp)
                                            Text("Type to search", color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Text("Search by name or number\nCall logs are prioritized", color = Color(0xFF6B7A99), fontSize = 12.sp, textAlign = TextAlign.Center)
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
}

@Composable
private fun SearchResultItem(
    title: String,
    subtitle: String?,
    callType: CallType?,
    timestamp: Long?,
    isCallLog: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

    val callTypeIcon = when (callType) {
        CallType.INCOMING -> Icons.Default.CallReceived
        CallType.OUTGOING -> Icons.Default.CallMade
        CallType.MISSED -> Icons.Default.CallMissed
        else -> null
    }

    val callTypeColor = when (callType) {
        CallType.INCOMING -> Color(0xFF00E676)
        CallType.OUTGOING -> Color(0xFF00E5FF)
        CallType.MISSED -> Color(0xFFFF5252)
        else -> Color(0xFF7C4DFF)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0D1A2D).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isCallLog) callTypeColor.copy(alpha = 0.3f) else Color(0xFF7C4DFF).copy(alpha = 0.2f)),
        interactionSource = interactionSource
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            if (isCallLog) listOf(callTypeColor.copy(alpha = 0.3f), callTypeColor.copy(alpha = 0.1f))
                            else listOf(Color(0xFF7C4DFF).copy(alpha = 0.3f), Color(0xFF7C4DFF).copy(alpha = 0.1f))
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, if (isCallLog) callTypeColor.copy(alpha = 0.5f) else Color(0xFF7C4DFF).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCallLog && callTypeIcon != null) {
                    Icon(callTypeIcon, null, tint = callTypeColor, modifier = Modifier.size(20.dp))
                } else {
                    Text(title.firstOrNull()?.uppercase()?.toString() ?: "#", color = Color(0xFF7C4DFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, color = Color(0xFF6B7A99), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (timestamp != null) Text(formatRelativeTime(timestamp), color = Color(0xFF4A5568), fontSize = 10.sp)
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF4A5568), modifier = Modifier.size(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// QUICK MESSAGE MODAL
// ═══════════════════════════════════════════════════════════════════

@Composable
fun QuickMessageModal(
    recipientNumber: String,
    recipientName: String?,
    availableSims: List<SimAccount>,
    onSend: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var selectedSimSlot by remember { mutableIntStateOf(availableSims.firstOrNull()?.slotIndex ?: 0) }
    var isSending by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 32.dp),
                shape = RoundedCornerShape(28.dp),
                color = NexusDialerColors.cardGlass,
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(NexusDialerColors.primary.copy(alpha = 0.4f), NexusDialerColors.secondary.copy(alpha = 0.2f))))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).background(Brush.linearGradient(NexusDialerColors.gradientPrimary), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Message, null, tint = NexusDialerColors.background, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("Quick Message", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NexusDialerColors.textPrimary)
                                Text(recipientName ?: recipientNumber, fontSize = 12.sp, color = NexusDialerColors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { if (!isSending) onDismiss() }) {
                            Icon(Icons.Default.Close, "Close", tint = NexusDialerColors.textMuted)
                        }
                    }

                    // Message input
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = NexusDialerColors.card,
                        border = BorderStroke(1.dp, NexusDialerColors.primary.copy(alpha = 0.2f))
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textStyle = LocalTextStyle.current.copy(color = NexusDialerColors.textPrimary, fontSize = 14.sp),
                            decorationBox = { innerTextField ->
                                if (messageText.isEmpty()) Text("Type your message...", color = NexusDialerColors.textMuted, fontSize = 14.sp)
                                innerTextField()
                            }
                        )
                    }

                    // SIM selection
                    if (availableSims.size > 1) {
                        Text("Send via", fontSize = 12.sp, color = NexusDialerColors.textMuted, fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            availableSims.forEachIndexed { index, sim ->
                                val isSelected = sim.slotIndex == selectedSimSlot
                                val simColor = if (index == 0) NexusDialerColors.simBlue else NexusDialerColors.simPurple
                                Surface(
                                    onClick = { selectedSimSlot = sim.slotIndex },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) simColor.copy(alpha = 0.2f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) simColor else NexusDialerColors.textMuted.copy(alpha = 0.3f))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(if (isSelected) simColor else NexusDialerColors.textMuted, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(sim.getLabel().take(12), fontSize = 12.sp, color = if (isSelected) simColor else NexusDialerColors.textSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Send button
                    Button(
                        onClick = { if (messageText.isNotBlank()) { isSending = true; onSend(messageText, selectedSimSlot) } },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NexusDialerColors.secondary, disabledContainerColor = NexusDialerColors.textMuted.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Message", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// NEXUS SIM SELECTION MODAL (Simple - for placing calls from other screens)
// ═══════════════════════════════════════════════════════════════════

@Composable
fun NexusSimSelectionModal(
    phoneNumber: String,
    contactName: String? = null,
    availableSims: List<SimInfo>,
    onDismiss: () -> Unit,
    onSimSelected: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "simSelect")
    
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Main card
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 320.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Prevent dismiss on card click */ },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A1520)
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            NexusDialerColors.secondary.copy(alpha = glowPulse),
                            NexusDialerColors.accent.copy(alpha = glowPulse * 0.5f),
                            NexusDialerColors.secondary.copy(alpha = glowPulse)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Phone icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        NexusDialerColors.secondary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = NexusDialerColors.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact info
                    Text(
                        text = contactName ?: phoneNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    if (contactName != null) {
                        Text(
                            text = phoneNumber,
                            fontSize = 14.sp,
                            color = NexusDialerColors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Select SIM",
                        fontSize = 12.sp,
                        color = NexusDialerColors.textMuted,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SIM buttons
                    if (availableSims.size == 1) {
                        // Single SIM - just show call button
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSimSelected(availableSims[0].slotIndex)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexusDialerColors.secondary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Multiple SIMs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            availableSims.forEach { sim ->
                                val simColor = if (sim.slotIndex == 0) 
                                    NexusDialerColors.secondary else NexusDialerColors.accent
                                
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSimSelected(sim.slotIndex)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = simColor.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(1.dp, simColor.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "SIM ${sim.slotIndex + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = simColor
                                        )
                                        Text(
                                            text = sim.carrierName.take(10),
                                            fontSize = 11.sp,
                                            color = NexusDialerColors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
                            color = NexusDialerColors.textMuted
                        )
                    }
                }
            }
        }
    }
}

