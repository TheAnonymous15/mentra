package com.example.mentra.dialer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mentra.dialer.*
import com.example.mentra.dialer.ussd.UssdResponseModal
import com.example.mentra.dialer.ussd.UssdState

/**
 * ═══════════════════════════════════════════════════════════════════
 * NEXUS DIALER - Main Screen
 * Modular architecture with separate component files
 * ═══════════════════════════════════════════════════════════════════
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    viewModel: DialerViewModel = hiltViewModel(),
    initialNumber: String? = null,
    onNavigateToInCall: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val dialerInput by viewModel.dialerInput.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val contactMatch by viewModel.contactMatch.collectAsState()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()

    // Call filter and search state
    val callTypeFilter by viewModel.callTypeFilter.collectAsState()
    val recentsSearchQuery by viewModel.recentsSearchQuery.collectAsState()

    // UI States
    var showSimModal by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    var pendingCallContactName by remember { mutableStateOf<String?>(null) }
    var pendingCallContactPhotoUri by remember { mutableStateOf<String?>(null) }
    var showQuickMessageModal by remember { mutableStateOf(false) }
    var quickMessageRecipient by remember { mutableStateOf("") }
    var quickMessageRecipientName by remember { mutableStateOf<String?>(null) }
    var isKeypadVisible by remember { mutableStateOf(false) }

    // USSD state
    val ussdState by viewModel.ussdState.collectAsState()

    // Default dialer launcher
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check the status after user makes a choice
        viewModel.checkDefaultDialerStatus()
    }

    // Load initial number
    LaunchedEffect(initialNumber) {
        initialNumber?.let { viewModel.setInput(it) }
    }

    // Note: Call UI is handled by CallForegroundService which launches InCallActivity
    // We don't navigate from here to avoid duplicate call UIs

    // Filter contacts for keypad
    val filteredContacts: List<DialerContact> = remember(dialerInput) {
        if (dialerInput.length < 2) emptyList()
        else viewModel.filterContactsByNumber(dialerInput)
    }

    val filteredCalls: List<CallLogEntry> = remember(dialerInput, recentCalls) {
        if (dialerInput.length < 2) emptyList()
        else recentCalls.filter { it.number.contains(dialerInput) }.take(5)
    }

    // Handle call initiation
    fun initiateCall(number: String) {
        if (availableSims.size > 1) {
            pendingCallNumber = number
            showSimModal = true
        } else {
            viewModel.placeCallToNumber(number)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NexusDialerColors.background,
                        NexusDialerColors.surface,
                        NexusDialerColors.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Main content area
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    DialerTab.KEYPAD -> {
                        DialerKeypadContent(
                            input = dialerInput,
                            contactMatch = contactMatch,
                            filteredContacts = filteredContacts,
                            filteredCalls = filteredCalls,
                            allCallLogs = recentCalls,
                            isKeypadVisible = isKeypadVisible,
                            isDefaultDialer = isDefaultDialer,
                            onDigitPressed = { viewModel.appendDigit(it) },
                            onDeletePressed = { viewModel.deleteLastDigit() },
                            onDeleteLongPressed = { viewModel.clearInput() },
                            onCallPressed = { if (dialerInput.isNotBlank()) initiateCall(dialerInput) },
                            onContactSelected = { contact -> viewModel.setInput(contact.phoneNumber) },
                            onCallLogSelected = { entry -> viewModel.setInput(entry.number) },
                            onCallLogCall = { entry ->
                                // Directly initiate call instead of copying to input
                                if (availableSims.size > 1) {
                                    pendingCallNumber = entry.number
                                    pendingCallContactName = entry.contactName
                                    pendingCallContactPhotoUri = entry.photoUri
                                    showSimModal = true
                                } else {
                                    viewModel.placeCallToNumber(entry.number)
                                }
                            },
                            onCallLogMessage = { entry ->
                                quickMessageRecipient = entry.number
                                quickMessageRecipientName = entry.contactName
                                showQuickMessageModal = true
                            },
                            onCallLogEditBeforeCall = { entry ->
                                viewModel.setInput(entry.number)
                            },
                            onCallLogDelete = { entry ->
                                viewModel.deleteCallLogEntry(entry.id)
                            },
                            onCallLogBlock = { entry ->
                                // TODO: Implement block number functionality
                            },
                            onCallLogAddToContacts = { entry ->
                                // TODO: Open add contact screen
                            },
                            onSetDefaultDialerClick = {
                                val intent = viewModel.getDefaultDialerIntent()
                                if (intent != null) {
                                    defaultDialerLauncher.launch(intent)
                                }
                            }
                        )
                    }
                    DialerTab.RECENTS -> {
                        DialerRecentsContent(
                            recentCalls = recentCalls,
                            availableSims = availableSims,
                            callTypeFilter = callTypeFilter,
                            searchQuery = recentsSearchQuery,
                            onCallTypeFilterChange = { viewModel.setCallTypeFilter(it) },
                            onSearchQueryChange = { viewModel.setRecentsSearchQuery(it) },
                            onCallClick = { entry ->
                                if (availableSims.size > 1) {
                                    pendingCallNumber = entry.number
                                    pendingCallContactName = entry.contactName
                                    pendingCallContactPhotoUri = entry.photoUri
                                    showSimModal = true
                                } else {
                                    viewModel.placeCallToNumber(entry.number)
                                }
                            },
                            onMessageClick = { entry ->
                                quickMessageRecipient = entry.number
                                quickMessageRecipientName = entry.contactName
                                showQuickMessageModal = true
                            }
                        )
                    }
                    DialerTab.CONTACTS -> {
                        DialerContactsContent(
                            onContactClick = { number ->
                                // No longer used - contact details modal handles this
                            },
                            onMessageClick = { contact ->
                                quickMessageRecipient = contact.phoneNumber
                                quickMessageRecipientName = contact.name
                                showQuickMessageModal = true
                            },
                            availableSims = availableSims,
                            onCallWithSim = { number, simSlot ->
                                viewModel.placeCallWithSim(number, simSlot)
                            }
                        )
                    }
                    DialerTab.FAVORITES -> {
                        DialerFavoritesContent(
                            onContactClick = { number ->
                                if (availableSims.size > 1) {
                                    pendingCallNumber = number
                                    showSimModal = true
                                } else {
                                    viewModel.placeCallToNumber(number)
                                }
                            },
                            onMessageClick = { contact ->
                                quickMessageRecipient = contact.phoneNumber
                                quickMessageRecipientName = contact.name
                                showQuickMessageModal = true
                            }
                        )
                    }
                    DialerTab.CALL -> {
                        // Handled by center button
                    }
                }
            }

            // Bottom Navigation
            DialerBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == DialerTab.KEYPAD) {
                        if (selectedTab == DialerTab.KEYPAD) {
                            isKeypadVisible = !isKeypadVisible
                        } else {
                            viewModel.selectTab(tab)
                            isKeypadVisible = true
                        }
                    } else {
                        viewModel.selectTab(tab)
                    }
                },
                missedCallCount = recentCalls.count { it.callType == CallType.MISSED },
                dialerInput = dialerInput,
                onCallPressed = { if (dialerInput.isNotBlank()) initiateCall(dialerInput) }
            )
        }
    }

    // Modals
    if (showSimModal && pendingCallNumber != null) {
        SimSelectionModal(
            sims = availableSims,
            phoneNumber = pendingCallNumber!!,
            contactName = pendingCallContactName,
            contactPhotoUri = pendingCallContactPhotoUri,
            onSimSelected = { simSlot ->
                showSimModal = false
                viewModel.placeCallWithSim(pendingCallNumber!!, simSlot)
                pendingCallNumber = null
                pendingCallContactName = null
                pendingCallContactPhotoUri = null
            },
            onDismiss = {
                showSimModal = false
                pendingCallNumber = null
                pendingCallContactName = null
                pendingCallContactPhotoUri = null
            }
        )
    }

    if (showQuickMessageModal) {
        QuickMessageModal(
            recipientNumber = quickMessageRecipient,
            recipientName = quickMessageRecipientName,
            availableSims = availableSims,
            onSend = { message, simSlot ->
                viewModel.sendQuickMessage(quickMessageRecipient, message, simSlot)
                showQuickMessageModal = false
            },
            onDismiss = { showQuickMessageModal = false }
        )
    }

    // USSD Modal
    if (ussdState != UssdState.Idle) {
        UssdResponseModal(
            ussdState = ussdState,
            onReply = { reply -> viewModel.sendUssdReply(reply) },
            onDismiss = { viewModel.resetUssdState() }
        )
    }
}

