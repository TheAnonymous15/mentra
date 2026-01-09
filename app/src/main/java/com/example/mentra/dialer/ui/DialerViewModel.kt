package com.example.mentra.dialer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.dialer.*
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val dialerManager: DialerManager,
    private val callLogManager: CallLogManager
) : ViewModel() {

    // Dialer input state
    private val _dialerInput = MutableStateFlow("")
    val dialerInput: StateFlow<String> = _dialerInput.asStateFlow()

    // Call state from DialerManager
    val currentCall: StateFlow<CallInfo?> = dialerManager.currentCall
    val callState: StateFlow<CallState> = dialerManager.callState
    val audioState: StateFlow<AudioRouteState> = dialerManager.audioState
    val availableSims: StateFlow<List<SimAccount>> = dialerManager.availableSims

    // Call log state
    val callHistory: StateFlow<List<CallLogEntry>> = callLogManager.callHistory
    val recentCalls: StateFlow<List<CallLogEntry>> = callLogManager.recentCalls

    // UI state
    private val _selectedTab = MutableStateFlow(DialerTab.KEYPAD)
    val selectedTab: StateFlow<DialerTab> = _selectedTab.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow(-1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _callResult = MutableStateFlow<CallResult?>(null)
    val callResult: StateFlow<CallResult?> = _callResult.asStateFlow()

    init {
        // Initialize DialerManagerProvider
        DialerManagerProvider.setDialerManager(dialerManager)

        // Load initial data
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            dialerManager.loadAvailableSims()
            callLogManager.loadCallHistory()
        }
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

        viewModelScope.launch {
            val result = dialerManager.placeCall(number, _selectedSimSlot.value)
            _callResult.value = result

            if (result is CallResult.Success) {
                // Clear input after successful call initiation
                // _dialerInput.value = ""
            }
        }
    }

    fun placeCallToNumber(number: String) {
        viewModelScope.launch {
            val result = dialerManager.placeCall(number, _selectedSimSlot.value)
            _callResult.value = result
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
}

enum class DialerTab {
    KEYPAD,
    RECENTS,
    CONTACTS,
    FAVORITES
}

