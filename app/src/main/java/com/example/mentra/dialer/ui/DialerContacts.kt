package com.example.mentra.dialer.ui

import android.accounts.AccountManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NEXUS DIALER - Contacts & Favorites Content
 * Contact list with search, favorites management
 */

// Data class for device accounts
data class DeviceAccount(
    val name: String,
    val type: String,
    val displayName: String
)

// Get available accounts on the device
fun getDeviceAccounts(context: Context): List<DeviceAccount> {
    val accounts = mutableListOf<DeviceAccount>()

    // Add Phone storage option
    accounts.add(DeviceAccount("Phone", "local", "Phone Storage"))

    try {
        val accountManager = AccountManager.get(context)
        val deviceAccounts = accountManager.accounts

        deviceAccounts.forEach { account ->
            val displayName = when {
                account.type.contains("google", ignoreCase = true) -> "Google - ${account.name}"
                account.type.contains("samsung", ignoreCase = true) -> "Samsung - ${account.name}"
                account.type.contains("microsoft", ignoreCase = true) -> "Microsoft - ${account.name}"
                account.type.contains("whatsapp", ignoreCase = true) -> "WhatsApp - ${account.name}"
                else -> "${account.type.substringAfterLast(".")} - ${account.name}"
            }
            accounts.add(DeviceAccount(account.name, account.type, displayName))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Add SIM options
    accounts.add(DeviceAccount("SIM1", "sim", "SIM Card 1"))
    accounts.add(DeviceAccount("SIM2", "sim", "SIM Card 2"))

    return accounts
}

// ═══════════════════════════════════════════════════════════════════
// CONTACTS CONTENT
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerContactsContent(
    onContactClick: (String) -> Unit,
    onMessageClick: (DialerContact) -> Unit,
    onAddContactClick: () -> Unit = {},
    availableSims: List<com.example.mentra.dialer.SimAccount> = emptyList(),
    onCallWithSim: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<DialerContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddContactModal by remember { mutableStateOf(false) }
    var selectedContactForDetails by remember { mutableStateOf<DialerContact?>(null) }
    var showSimSelectionFor by remember { mutableStateOf<String?>(null) }
    var simSelectionContactName by remember { mutableStateOf<String?>(null) }
    var simSelectionContactPhotoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        contacts = loadDialerContacts(context)
        isLoading = false
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isEmpty()) contacts
        else {
            val query = searchQuery.lowercase()
            contacts.filter {
                it.name.lowercase().contains(query) || it.phoneNumber.contains(query)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            ContactsSearchBar(searchQuery = searchQuery, onSearchQueryChange = { searchQuery = it })

            Spacer(modifier = Modifier.height(12.dp))

            // Count
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${filteredContacts.size} contacts", color = NexusDialerColors.textMuted, fontSize = 12.sp)
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NexusDialerColors.primary, modifier = Modifier.size(40.dp))
                }
            } else if (contacts.isEmpty()) {
                DialerEmptyState(icon = Icons.Default.People, title = "No contacts", subtitle = "Your contacts will appear here")
            } else if (filteredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = NexusDialerColors.textMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No contacts found", color = NexusDialerColors.textMuted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = filteredContacts, key = { it.id }) { contact ->
                        ContactItemWithContextMenu(
                            contact = contact,
                            onContactClick = { selectedContactForDetails = contact },
                            onCallClick = {
                                if (availableSims.size > 1) {
                                    showSimSelectionFor = contact.phoneNumber
                                    simSelectionContactName = contact.name
                                    simSelectionContactPhotoUri = contact.photoUri
                                } else {
                                    onCallWithSim(contact.phoneNumber, 0)
                                }
                            },
                            onMessageClick = { onMessageClick(contact) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to add contacts
        FloatingActionButton(
            onClick = { showAddContactModal = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 8.dp),
            containerColor = NexusDialerColors.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.PersonAdd, "Add Contact", modifier = Modifier.size(24.dp))
        }
    }

    // SIM Selection Modal
    if (showSimSelectionFor != null) {
        SimSelectionModal(
            sims = availableSims,
            phoneNumber = showSimSelectionFor!!,
            contactName = simSelectionContactName,
            contactPhotoUri = simSelectionContactPhotoUri,
            onSimSelected = { simSlot ->
                onCallWithSim(showSimSelectionFor!!, simSlot)
                showSimSelectionFor = null
                simSelectionContactName = null
                simSelectionContactPhotoUri = null
            },
            onDismiss = {
                showSimSelectionFor = null
                simSelectionContactName = null
                simSelectionContactPhotoUri = null
            }
        )
    }

    // Contact Details Modal
    if (selectedContactForDetails != null) {
        ContactDetailsModal(
            contact = selectedContactForDetails!!,
            availableSims = availableSims,
            onDismiss = { selectedContactForDetails = null },
            onCallClick = { number ->
                if (availableSims.size > 1) {
                    showSimSelectionFor = number
                    simSelectionContactName = selectedContactForDetails?.name
                    simSelectionContactPhotoUri = selectedContactForDetails?.photoUri
                    selectedContactForDetails = null
                } else {
                    onCallWithSim(number, 0)
                    selectedContactForDetails = null
                }
            },
            onMessageClick = {
                onMessageClick(selectedContactForDetails!!)
                selectedContactForDetails = null
            }
        )
    }

    // Add Contact Modal
    if (showAddContactModal) {
        AddContactModal(
            context = context,
            onDismiss = { showAddContactModal = false },
            onSaveContact = { name, numbers, saveLocation ->
                // Save contact using ContentResolver
                saveContact(context, name, numbers, saveLocation)
                showAddContactModal = false
                // Refresh contacts list
                kotlinx.coroutines.MainScope().launch {
                    contacts = loadDialerContacts(context)
                }
            }
        )
    }
}

@Composable
private fun ContactsSearchBar(
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
                    if (searchQuery.isEmpty()) Text("Search contacts...", color = NexusDialerColors.textMuted, fontSize = 14.sp)
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

@Composable
fun ContactItemWithContextMenu(
    contact: DialerContact,
    onContactClick: () -> Unit,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit,
    onAddToFavorites: () -> Unit = {},
    onBlockNumber: () -> Unit = {},
    onEditContact: () -> Unit = {},
    onDeleteContact: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onContactClick,
        shape = RoundedCornerShape(16.dp),
        color = NexusDialerColors.card.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, NexusDialerColors.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(48.dp).background(
                    brush = Brush.linearGradient(listOf(NexusDialerColors.primary, NexusDialerColors.secondary)),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.firstOrNull()?.uppercase()?.toString() ?: "#",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    color = NexusDialerColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(contact.phoneNumber, color = NexusDialerColors.textMuted, fontSize = 13.sp)
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Message, "Message", tint = NexusDialerColors.secondary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = NexusDialerColors.callGreen, modifier = Modifier.size(18.dp))
                }

                // Context menu
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
                            text = { Text("Edit contact", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onEditContact() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block number", fontSize = 13.sp) },
                            onClick = { showContextMenu = false; onBlockNumber() },
                            leadingIcon = { Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share contact", fontSize = 13.sp) },
                            onClick = { showContextMenu = false },
                            leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete contact", fontSize = 13.sp, color = NexusDialerColors.callRed) },
                            onClick = { showContextMenu = false; onDeleteContact() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexusDialerColors.callRed, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: DialerContact,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Surface(
        onClick = onCallClick,
        shape = RoundedCornerShape(16.dp),
        color = NexusDialerColors.card.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, NexusDialerColors.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(48.dp).background(
                    brush = Brush.linearGradient(listOf(NexusDialerColors.primary, NexusDialerColors.secondary)),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.firstOrNull()?.uppercase()?.toString() ?: "#",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    color = NexusDialerColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(contact.phoneNumber, color = NexusDialerColors.textMuted, fontSize = 13.sp)
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Message, "Message", tint = NexusDialerColors.secondary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = NexusDialerColors.callGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// FAVORITES CONTENT
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DialerFavoritesContent(
    onContactClick: (String) -> Unit,
    onMessageClick: (DialerContact) -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf<List<DialerContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        favorites = loadFavoriteContacts(context)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NexusDialerColors.primary, modifier = Modifier.size(40.dp))
        }
    } else if (favorites.isEmpty()) {
        DialerEmptyState(
            icon = Icons.Default.Star,
            title = "No favorites yet",
            subtitle = "Star contacts to add them here"
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, null, tint = NexusDialerColors.accent, modifier = Modifier.size(20.dp))
                    Text("Favorites", color = NexusDialerColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("${favorites.size} contacts", color = NexusDialerColors.textMuted, fontSize = 12.sp)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = favorites, key = { it.id }) { contact ->
                    FavoriteContactItem(
                        contact = contact,
                        onCallClick = { onContactClick(contact.phoneNumber) },
                        onMessageClick = { onMessageClick(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteContactItem(
    contact: DialerContact,
    onCallClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Surface(
        onClick = onCallClick,
        shape = RoundedCornerShape(16.dp),
        color = NexusDialerColors.card.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, NexusDialerColors.accent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with star indicator
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.linearGradient(listOf(NexusDialerColors.accent, NexusDialerColors.secondary)),
                        shape = CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        contact.name.firstOrNull()?.uppercase()?.toString() ?: "#",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Star badge
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).background(NexusDialerColors.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, null, tint = NexusDialerColors.accent, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    color = NexusDialerColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(contact.phoneNumber, color = NexusDialerColors.textMuted, fontSize = 13.sp)
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(40.dp).background(NexusDialerColors.secondary.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Message, "Message", tint = NexusDialerColors.secondary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(40.dp).background(NexusDialerColors.callGreen.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, "Call", tint = NexusDialerColors.callGreen, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════

suspend fun loadFavoriteContacts(context: Context): List<DialerContact> = withContext(Dispatchers.IO) {
    val favorites = mutableListOf<DialerContact>()

    try {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = ?"
        val selectionArgs = arrayOf("1")

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
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

                favorites.add(DialerContact(id, name, number.replace("\\s".toRegex(), ""), photo))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    favorites.distinctBy { it.phoneNumber.takeLast(10) }
}

/**
 * Save a new contact to the device
 */
fun saveContact(
    context: Context,
    name: String,
    numbers: List<ContactPhoneNumber>,
    saveLocation: ContactSaveLocation
) {
    try {
        val ops = ArrayList<android.content.ContentProviderOperation>()

        // Create raw contact
        val rawContactInsertIndex = ops.size
        ops.add(
            android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        // Add name
        ops.add(
            android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        // Add phone numbers
        numbers.forEach { phoneNumber ->
            val phoneType = when (phoneNumber.type.lowercase()) {
                "mobile" -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
            }

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                    .build()
            )
        }

        // Execute batch operation
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

        android.widget.Toast.makeText(context, "Contact saved successfully", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Failed to save contact: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ═══════════════════════════════════════════════════════════════════
// CONTACT DETAILS MODAL
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ContactDetailsModal(
    contact: DialerContact,
    availableSims: List<com.example.mentra.dialer.SimAccount>,
    onDismiss: () -> Unit,
    onCallClick: (String) -> Unit,
    onMessageClick: () -> Unit
) {
    val context = LocalContext.current

    // Load contact photo if available
    val contactBitmap = remember(contact.photoUri) {
        if (contact.photoUri != null) {
            try {
                val uri = Uri.parse(contact.photoUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexusDialerColors.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = NexusDialerColors.textMuted)
                    }
                }

                // Avatar with photo support
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(NexusDialerColors.primary, NexusDialerColors.secondary)),
                            shape = CircleShape
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    NexusDialerColors.primary.copy(alpha = 0.8f),
                                    NexusDialerColors.secondary.copy(alpha = 0.8f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (contactBitmap != null) {
                        Image(
                            bitmap = contactBitmap.asImageBitmap(),
                            contentDescription = "Contact Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            contact.name.firstOrNull()?.uppercase()?.toString() ?: "#",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Name
                Text(
                    contact.name,
                    color = NexusDialerColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Phone Number
                Text(
                    contact.phoneNumber,
                    color = NexusDialerColors.textMuted,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Call Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = { onCallClick(contact.phoneNumber) },
                            containerColor = NexusDialerColors.callGreen,
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Call, "Call", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Call", color = NexusDialerColors.textSecondary, fontSize = 12.sp)
                    }

                    // Message Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = onMessageClick,
                            containerColor = NexusDialerColors.secondary,
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Message, "Message", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Message", color = NexusDialerColors.textSecondary, fontSize = 12.sp)
                    }

                    // Video Call Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = { /* TODO: Video call */ },
                            containerColor = NexusDialerColors.primary,
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.VideoCall, "Video Call", modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Video", color = NexusDialerColors.textSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Additional actions
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NexusDialerColors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Add to favorites", color = NexusDialerColors.textPrimary) },
                            leadingContent = { Icon(Icons.Default.Star, null, tint = NexusDialerColors.accent) },
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                        HorizontalDivider(color = NexusDialerColors.textMuted.copy(alpha = 0.1f))
                        ListItem(
                            headlineContent = { Text("Block number", color = NexusDialerColors.textPrimary) },
                            leadingContent = { Icon(Icons.Default.Block, null, tint = NexusDialerColors.textMuted) },
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                        HorizontalDivider(color = NexusDialerColors.textMuted.copy(alpha = 0.1f))
                        ListItem(
                            headlineContent = { Text("Share contact", color = NexusDialerColors.textPrimary) },
                            leadingContent = { Icon(Icons.Default.Share, null, tint = NexusDialerColors.textMuted) },
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// ADD CONTACT MODAL
// ═══════════════════════════════════════════════════════════════════

enum class ContactSaveLocation {
    PHONE, SIM1, SIM2, GOOGLE
}

data class ContactPhoneNumber(
    val number: String,
    val type: String = "Mobile" // Mobile, Home, Work, etc.
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactModal(
    context: Context,
    onDismiss: () -> Unit,
    onSaveContact: (name: String, numbers: List<ContactPhoneNumber>, saveLocation: ContactSaveLocation) -> Unit,
    initialNumber: String? = null
) {
    var contactName by remember { mutableStateOf("") }
    var phoneNumbers by remember { mutableStateOf(listOf(ContactPhoneNumber(initialNumber ?: ""))) }
    var showSaveLocationDropdown by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    // Get device accounts
    val deviceAccounts = remember { getDeviceAccounts(context) }
    var selectedAccount by remember { mutableStateOf(deviceAccounts.firstOrNull()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NexusDialerColors.card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Contact",
                        color = NexusDialerColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = NexusDialerColors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Photo and Name Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo Picker
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedPhotoUri == null)
                                    Brush.linearGradient(listOf(NexusDialerColors.primary.copy(alpha = 0.3f), NexusDialerColors.secondary.copy(alpha = 0.3f)))
                                else
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(NexusDialerColors.primary, NexusDialerColors.secondary)),
                                shape = CircleShape
                            )
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPhotoUri != null) {
                            // Load bitmap from URI
                            val bitmap = remember(selectedPhotoUri) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val source = ImageDecoder.createSource(context.contentResolver, selectedPhotoUri!!)
                                        ImageDecoder.decodeBitmap(source)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(context.contentResolver, selectedPhotoUri)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Contact Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            // Edit overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    "Add Photo",
                                    tint = NexusDialerColors.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    "Add",
                                    color = NexusDialerColors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Contact Name TextField
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = NexusDialerColors.primary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexusDialerColors.primary,
                            unfocusedBorderColor = NexusDialerColors.textMuted.copy(alpha = 0.3f),
                            focusedLabelColor = NexusDialerColors.primary,
                            unfocusedLabelColor = NexusDialerColors.textMuted
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Numbers
                Text(
                    "Phone Numbers",
                    color = NexusDialerColors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                phoneNumbers.forEachIndexed { index, phoneNumber ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phoneNumber.number,
                            onValueChange = { newNumber ->
                                phoneNumbers = phoneNumbers.toMutableList().apply {
                                    this[index] = phoneNumber.copy(number = newNumber)
                                }
                            },
                            label = { Text(phoneNumber.type) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = NexusDialerColors.secondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NexusDialerColors.secondary,
                                unfocusedBorderColor = NexusDialerColors.textMuted.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )

                        if (phoneNumbers.size > 1) {
                            IconButton(
                                onClick = {
                                    phoneNumbers = phoneNumbers.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, "Remove", tint = NexusDialerColors.callRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add another number button
                TextButton(
                    onClick = {
                        val types = listOf("Mobile", "Home", "Work", "Other")
                        val usedTypes = phoneNumbers.map { it.type }
                        val nextType = types.firstOrNull { it !in usedTypes } ?: "Other"
                        phoneNumbers = phoneNumbers + ContactPhoneNumber("", nextType)
                    }
                ) {
                    Icon(Icons.Default.Add, null, tint = NexusDialerColors.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add another number", color = NexusDialerColors.primary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Location - Show actual device accounts
                Text(
                    "Save to",
                    color = NexusDialerColors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Surface(
                        onClick = { showSaveLocationDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = NexusDialerColors.surface,
                        border = BorderStroke(1.dp, NexusDialerColors.textMuted.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    when {
                                        selectedAccount?.type == "local" -> Icons.Default.PhoneAndroid
                                        selectedAccount?.type == "sim" -> Icons.Default.SimCard
                                        selectedAccount?.type?.contains("google", ignoreCase = true) == true -> Icons.Default.Email
                                        selectedAccount?.type?.contains("samsung", ignoreCase = true) == true -> Icons.Default.Cloud
                                        selectedAccount?.type?.contains("microsoft", ignoreCase = true) == true -> Icons.Default.Cloud
                                        else -> Icons.Default.AccountCircle
                                    },
                                    null,
                                    tint = NexusDialerColors.primary
                                )
                                Text(
                                    selectedAccount?.displayName ?: "Select account",
                                    color = NexusDialerColors.textPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, null, tint = NexusDialerColors.textMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = showSaveLocationDropdown,
                        onDismissRequest = { showSaveLocationDropdown = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        deviceAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        account.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    selectedAccount = account
                                    showSaveLocationDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when {
                                            account.type == "local" -> Icons.Default.PhoneAndroid
                                            account.type == "sim" -> Icons.Default.SimCard
                                            account.type.contains("google", ignoreCase = true) -> Icons.Default.Email
                                            account.type.contains("samsung", ignoreCase = true) -> Icons.Default.Cloud
                                            account.type.contains("microsoft", ignoreCase = true) -> Icons.Default.Cloud
                                            else -> Icons.Default.AccountCircle
                                        },
                                        null,
                                        tint = when {
                                            account.type.contains("google", ignoreCase = true) -> Color(0xFFDB4437)
                                            account.type.contains("samsung", ignoreCase = true) -> Color(0xFF1428A0)
                                            account.type.contains("microsoft", ignoreCase = true) -> Color(0xFF00A4EF)
                                            else -> NexusDialerColors.textMuted
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusDialerColors.textSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (contactName.isNotBlank() && phoneNumbers.any { it.number.isNotBlank() }) {
                                // Convert selected account to save location for backward compatibility
                                val saveLocation = when {
                                    selectedAccount?.type == "sim" && selectedAccount?.name == "SIM1" -> ContactSaveLocation.SIM1
                                    selectedAccount?.type == "sim" && selectedAccount?.name == "SIM2" -> ContactSaveLocation.SIM2
                                    selectedAccount?.type?.contains("google", ignoreCase = true) == true -> ContactSaveLocation.GOOGLE
                                    else -> ContactSaveLocation.PHONE
                                }
                                onSaveContact(contactName, phoneNumbers.filter { it.number.isNotBlank() }, saveLocation)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NexusDialerColors.primary),
                        enabled = contactName.isNotBlank() && phoneNumbers.any { it.number.isNotBlank() }
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

