package com.example.mentra.dialer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.dialer.*
import com.example.mentra.dialer.ussd.UssdService
import com.example.mentra.dialer.ussd.UssdState
import com.example.mentra.dialer.ussd.UssdResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Dialer ViewModel
 *
 * Manages UI state for the dialer screens.
 * Business logic is delegated to DialerManager.
 */
@HiltViewModel
class DialerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dialerManager: DialerManager,
    private val callLogManager: CallLogManager,
    private val ussdService: UssdService,
    private val incomingCallHandler: IncomingCallHandler
) : ViewModel() {

    // Dialer input state
    private val _dialerInput = MutableStateFlow("")
    val dialerInput: StateFlow<String> = _dialerInput.asStateFlow()

    // Call state from DialerManager
    val currentCall: StateFlow<CallInfo?> = dialerManager.currentCall
    val callState: StateFlow<CallState> = dialerManager.callState
    val audioState: StateFlow<AudioRouteState> = dialerManager.audioState
    val availableSims: StateFlow<List<SimAccount>> = dialerManager.availableSims

    // Default dialer status
    val isDefaultDialer: StateFlow<Boolean> = dialerManager.isDefaultDialer

    // Call log state
    val callHistory: StateFlow<List<CallLogEntry>> = callLogManager.callHistory
    val recentCalls: StateFlow<List<CallLogEntry>> = callLogManager.recentCalls

    // USSD state
    val ussdState: StateFlow<UssdState> = ussdService.ussdState
    val ussdHistory = ussdService.ussdHistory

    // Incoming call state
    val incomingCallState: StateFlow<IncomingCallState> = incomingCallHandler.incomingCallState

    // Billing info for call cost display
    val totalCallCost: StateFlow<Double> = dialerManager.totalCallCost
    val isBillingTracking: StateFlow<Boolean> = dialerManager.isBillingTracking

    // UI state
    private val _selectedTab = MutableStateFlow(DialerTab.KEYPAD)
    val selectedTab: StateFlow<DialerTab> = _selectedTab.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow(-1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _callResult = MutableStateFlow<CallResult?>(null)
    val callResult: StateFlow<CallResult?> = _callResult.asStateFlow()

    // Show default dialer prompt
    private val _showDefaultDialerPrompt = MutableStateFlow(false)
    val showDefaultDialerPrompt: StateFlow<Boolean> = _showDefaultDialerPrompt.asStateFlow()

    // Contact match for current input
    private val _contactMatch = MutableStateFlow<ContactMatch?>(null)
    val contactMatch: StateFlow<ContactMatch?> = _contactMatch.asStateFlow()

    // Call type filter for recents
    private val _callTypeFilter = MutableStateFlow(CallTypeFilter.ALL)
    val callTypeFilter: StateFlow<CallTypeFilter> = _callTypeFilter.asStateFlow()

    // Search query for recents
    private val _recentsSearchQuery = MutableStateFlow("")
    val recentsSearchQuery: StateFlow<String> = _recentsSearchQuery.asStateFlow()

    // Show in-call screen
    private val _showInCallScreen = MutableStateFlow(false)
    val showInCallScreen: StateFlow<Boolean> = _showInCallScreen.asStateFlow()

    init {
        // Initialize DialerManagerProvider
        DialerManagerProvider.setDialerManager(dialerManager)

        // Start listening for incoming calls
        incomingCallHandler.startListening()

        // Load initial data
        loadData()

        // Check default dialer status
        checkDefaultDialerStatus()
    }

    override fun onCleared() {
        super.onCleared()
        // Stop listening when ViewModel is destroyed
        incomingCallHandler.stopListening()
    }

    private fun loadData() {
        viewModelScope.launch {
            dialerManager.loadAvailableSims()
            callLogManager.loadCallHistory()
        }
    }

    fun checkDefaultDialerStatus() {
        dialerManager.checkDefaultDialerStatus()
    }

    fun getDefaultDialerIntent(): android.content.Intent? {
        return dialerManager.createDefaultDialerIntent()
    }

    fun dismissDefaultDialerPrompt() {
        _showDefaultDialerPrompt.value = false
    }

    fun showDefaultDialerPrompt() {
        _showDefaultDialerPrompt.value = true
    }

    // ============================================
    // DIALER INPUT
    // ============================================

    fun appendDigit(digit: String) {
        _dialerInput.value += digit
    }

    fun deleteLastDigit() {
        val current = _dialerInput.value
        if (current.isNotEmpty()) {
            _dialerInput.value = current.dropLast(1)
        }
    }

    fun clearInput() {
        _dialerInput.value = ""
    }

    fun setInput(number: String) {
        _dialerInput.value = number
    }

    // ============================================
    // CALL ACTIONS
    // ============================================

    fun placeCall() {
        val number = _dialerInput.value
        if (number.isBlank()) return

        // Check if it's a USSD code - delegate to system dialer
        if (isUssdCode(number)) {
            _dialerInput.value = "" // Clear input immediately before USSD dial
            executeUssd(number)
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val result = dialerManager.placeCall(number, _selectedSimSlot.value)
            _callResult.value = result

            if (result is CallResult.Success) {
                _dialerInput.value = "" // Clear input after successful call
            }
        }
    }

    fun placeCallToNumber(number: String) {
        // Check if it's a USSD code - delegate to system dialer
        if (isUssdCode(number)) {
            _dialerInput.value = "" // Clear input immediately before USSD dial
            executeUssd(number)
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val result = dialerManager.placeCall(number, _selectedSimSlot.value)
            _callResult.value = result

            if (result is CallResult.Success) {
                _dialerInput.value = "" // Clear input after successful call
            }
        }
    }

    fun placeCallWithSim(number: String, simSlot: Int) {
        // Check if it's a USSD code - delegate to system dialer
        if (isUssdCode(number)) {
            _dialerInput.value = "" // Clear input immediately before USSD dial
            viewModelScope.launch {
                ussdService.executeUssd(number, simSlot)
            }
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val result = dialerManager.placeCall(number, simSlot)
            _callResult.value = result

            if (result is CallResult.Success) {
                _dialerInput.value = "" // Clear input after successful call
            }
        }
    }

    /**
     * Filter contacts by phone number for keypad suggestions
     */
    fun filterContactsByNumber(input: String): List<DialerContact> {
        if (input.length < 2) return emptyList()

        return try {
            val contacts = mutableListOf<DialerContact>()
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone._ID,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf("%$input%"),
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 5"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val number = cursor.getString(numberIndex) ?: continue
                    val photo = cursor.getString(photoIndex)
                    contacts.add(DialerContact(id, name, number.replace("\\s".toRegex(), ""), photo))
                }
            }
            contacts.distinctBy { it.phoneNumber.takeLast(10) }.take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun endCall() {
        dialerManager.endCall()
    }

    fun answerCall() {
        dialerManager.answerCall()
    }

    fun rejectCall() {
        dialerManager.rejectCall()
    }

    // ============================================
    // CALL CONTROLS
    // ============================================

    fun toggleMute() {
        dialerManager.toggleMute()
    }

    fun toggleSpeaker() {
        dialerManager.toggleSpeaker()
    }

    fun toggleHold() {
        dialerManager.toggleHold()
    }

    fun setAudioRoute(route: AudioRoute) {
        dialerManager.setAudioRoute(route)
    }

    fun sendDtmf(digit: Char) {
        dialerManager.sendDtmf(digit)
    }

    // ============================================
    // INCOMING CALL HANDLING
    // ============================================

    /**
     * Answer an incoming call
     */
    fun answerIncomingCall() {
        val answered = incomingCallHandler.answerCall()
        if (answered) {
            _showInCallScreen.value = true
        }
    }

    /**
     * Reject an incoming call
     */
    fun rejectIncomingCall() {
        incomingCallHandler.rejectCall()
    }

    /**
     * End the current active call
     */
    fun endActiveCall() {
        incomingCallHandler.endCall()
        _showInCallScreen.value = false
    }

    /**
     * Dismiss incoming call UI
     */
    fun dismissIncomingCallUI() {
        incomingCallHandler.dismissIncomingCallUI()
    }

    /**
     * Show the in-call screen
     */
    fun showInCallScreen() {
        _showInCallScreen.value = true
    }

    /**
     * Hide the in-call screen
     */
    fun hideInCallScreen() {
        _showInCallScreen.value = false
    }

    // ============================================
    // SIM SELECTION
    // ============================================

    fun selectSim(slotIndex: Int) {
        _selectedSimSlot.value = slotIndex
    }

    fun hasMultipleSims(): Boolean = availableSims.value.size > 1

    // ============================================
    // TAB NAVIGATION
    // ============================================

    fun selectTab(tab: DialerTab) {
        _selectedTab.value = tab
    }

    // ============================================
    // CALL LOG
    // ============================================

    fun refreshCallLog() {
        viewModelScope.launch {
            callLogManager.loadCallHistory()
        }
    }

    fun deleteCallLogEntry(id: Long) {
        viewModelScope.launch {
            callLogManager.deleteCallLogEntry(id)
        }
    }

    fun clearCallHistory() {
        viewModelScope.launch {
            callLogManager.clearCallHistory()
        }
    }

    fun markMissedCallsAsRead() {
        viewModelScope.launch {
            callLogManager.markMissedCallsAsRead()
        }
    }

    // ============================================
    // CALL TYPE FILTER & SEARCH
    // ============================================

    fun setCallTypeFilter(filter: CallTypeFilter) {
        _callTypeFilter.value = filter
    }

    fun setRecentsSearchQuery(query: String) {
        _recentsSearchQuery.value = query
    }

    fun getFilteredRecentCalls(): List<CallLogEntry> {
        val calls = callHistory.value
        val filter = _callTypeFilter.value
        val query = _recentsSearchQuery.value.lowercase().trim()

        return calls
            .filter { entry ->
                // Apply call type filter
                when (filter) {
                    CallTypeFilter.ALL -> true
                    CallTypeFilter.INCOMING -> entry.callType == CallType.INCOMING
                    CallTypeFilter.OUTGOING -> entry.callType == CallType.OUTGOING
                    CallTypeFilter.MISSED -> entry.callType == CallType.MISSED
                    CallTypeFilter.BLOCKED -> entry.callType == CallType.BLOCKED || entry.callType == CallType.REJECTED
                    CallTypeFilter.SOCIAL -> entry.isSocialCall() // WhatsApp, Telegram, etc.
                }
            }
            .filter { entry ->
                // Apply search filter
                if (query.isEmpty()) {
                    true
                } else {
                    entry.contactName?.lowercase()?.contains(query) == true ||
                    entry.number.contains(query) ||
                    entry.callSource.displayName.lowercase().contains(query) // Also search by app name
                }
            }
    }

    fun getMissedCallCount(): Int = callLogManager.getMissedCallCount()

    fun getCallStatistics(): CallStatistics = callLogManager.getCallStatistics()

    // ============================================
    // UTILITY
    // ============================================

    fun formatNumber(number: String): String {
        return dialerManager.formatNumberForDisplay(number)
    }

    fun clearCallResult() {
        _callResult.value = null
    }

    // ============================================
    // USSD OPERATIONS
    // ============================================

    /**
     * Check if input is a USSD code
     */
    fun isUssdCode(input: String = _dialerInput.value): Boolean {
        return ussdService.isValidUssdCode(input)
    }

    /**
     * Execute USSD code
     */
    fun executeUssd(code: String = _dialerInput.value) {
        viewModelScope.launch {
            // Check if this is a reply to an interactive session
            val isReply = ussdService.hasActiveSession()
            ussdService.executeUssd(code, _selectedSimSlot.value.coerceAtLeast(0), isReply)
            // Clear the keypad input after dialing
            _dialerInput.value = ""
        }
    }

    /**
     * Send reply to interactive USSD session
     */
    fun sendUssdReply(reply: String) {
        viewModelScope.launch {
            ussdService.sendUssdReply(reply, _selectedSimSlot.value.coerceAtLeast(0))
        }
    }

    /**
     * Dial USSD code (legacy method)
     */
    fun dialUssd(code: String = _dialerInput.value): Boolean {
        val result = ussdService.dialUssd(code)
        if (result) {
            _dialerInput.value = "" // Clear input after successful dial
        }
        return result
    }

    /**
     * Reset USSD state
     */
    fun resetUssdState() {
        ussdService.cancelSession()
    }

    /**
     * Clear USSD history
     */
    fun clearUssdHistory() {
        ussdService.clearHistory()
    }

    /**
     * Handle call button press - decides between call and USSD
     */
    fun handleCallButtonPress() {
        val input = _dialerInput.value
        if (input.isBlank()) return

        if (isUssdCode(input)) {
            // Execute USSD
            executeUssd(input)
        } else {
            // Place regular call
            placeCall()
        }
    }

    // ============================================
    // QUICK MESSAGING
    // ============================================

    /**
     * Send a quick SMS message from the dialer
     */
    fun sendQuickMessage(recipient: String, message: String, simSlot: Int) {
        viewModelScope.launch {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                        ?.createForSubscriptionId(getSubscriptionIdForSlot(simSlot))
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }

                smsManager?.sendTextMessage(
                    recipient,
                    null,
                    message,
                    null,
                    null
                )

                android.util.Log.d("DialerViewModel", "Quick message sent to $recipient")
            } catch (e: Exception) {
                android.util.Log.e("DialerViewModel", "Failed to send quick message", e)
            }
        }
    }

    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(
                    android.content.Context.TELEPHONY_SUBSCRIPTION_SERVICE
                ) as? android.telephony.SubscriptionManager

                subscriptionManager?.activeSubscriptionInfoList?.find {
                    it.simSlotIndex == slotIndex
                }?.subscriptionId ?: android.telephony.SubscriptionManager.getDefaultSubscriptionId()
            } else {
                android.telephony.SubscriptionManager.getDefaultSubscriptionId()
            }
        } catch (e: Exception) {
            android.telephony.SubscriptionManager.getDefaultSubscriptionId()
        }
    }
}

enum class DialerTab {
    KEYPAD,
    CONTACTS,
    CALL,
    RECENTS,
    FAVORITES
}

enum class CallTypeFilter {
    ALL,
    INCOMING,
    OUTGOING,
    MISSED,
    BLOCKED,
    SOCIAL  // WhatsApp, Telegram, etc.
}

