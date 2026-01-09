package com.example.mentra.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.messaging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Messaging ViewModel
 * Manages SMS state and operations with preloading support
 */
@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val smsManager: SmsManager,
    private val simCardManager: SimCardManager,
    private val messagePreloader: MessagePreloader
) : ViewModel() {

    // Use preloaded conversations for instant display
    val conversations: StateFlow<List<Conversation>> = messagePreloader.cachedConversations
    val messages: StateFlow<List<SmsMessage>> = smsManager.messages
    val contacts: StateFlow<List<Contact>> = messagePreloader.cachedContacts

    // SIM card management
    val availableSims: StateFlow<List<SimInfo>> = simCardManager.availableSims
    val selectedSimSlot: StateFlow<Int?> = simCardManager.selectedSimSlot

    // Preload status
    val isPreloaded: StateFlow<Boolean> = messagePreloader.isPreloaded
    val preloadProgress: StateFlow<Float> = messagePreloader.preloadProgress

    private val _messageStats = MutableStateFlow<MessageStatistics?>(null)
    val messageStats: StateFlow<MessageStatistics?> = _messageStats.asStateFlow()

    private val _sendingState = MutableStateFlow<SendingState>(SendingState.Idle)
    val sendingState: StateFlow<SendingState> = _sendingState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load all messaging data - uses preloaded data if available
     */
    private fun loadData() {
        viewModelScope.launch {
            // Check if data is already preloaded
            if (!messagePreloader.isPreloaded.value) {
                // If not preloaded yet, load normally
                smsManager.loadConversations()
                smsManager.loadContacts()
            }

            // Always load SIMs (lightweight)
            simCardManager.loadAvailableSims()

            // Update stats
            updateStats()
        }
    }

    /**
     * Load specific conversation - uses cached data for instant display
     */
    fun loadConversation(phoneNumber: String) {
        viewModelScope.launch {
            // Try to get cached messages first (instant)
            val cachedMessages = messagePreloader.getCachedMessages(phoneNumber)

            if (cachedMessages != null && cachedMessages.isNotEmpty()) {
                // Use cached data immediately
                (smsManager.messages as? MutableStateFlow)?.value = cachedMessages

                // Then refresh in background
                smsManager.loadMessages(phoneNumber)
            } else {
                // No cache, load normally
                smsManager.loadMessages(phoneNumber)
            }

            // Mark all as read
            messages.value.forEach { message ->
                if (!message.isRead && message.type == MessageType.RECEIVED) {
                    smsManager.markAsRead(message.id)
                }
            }
        }
    }

    /**
     * Select SIM for sending
     */
    fun selectSim(slotIndex: Int) {
        simCardManager.selectSim(slotIndex)
    }

    /**
     * Check if multiple SIMs available
     */
    fun hasMultipleSims(): Boolean = simCardManager.hasMultipleSims()

    /**
     * Send SMS message with optional SIM selection
     */
    fun sendMessage(phoneNumber: String, message: String, subscriptionId: Int = -1) {
        viewModelScope.launch {
            _sendingState.value = SendingState.Sending

            val result = smsManager.sendSms(phoneNumber, message, subscriptionId)

            if (result.isSuccess) {
                _sendingState.value = SendingState.Success
                // Reload to show sent message
                smsManager.loadConversations()
                smsManager.loadMessages(phoneNumber)
            } else {
                _sendingState.value = SendingState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send"
                )
            }

            // Reset state after delay
            kotlinx.coroutines.delay(2000)
            _sendingState.value = SendingState.Idle
        }
    }

    /**
     * Send bulk SMS
     */
    fun sendBulkMessage(phoneNumbers: List<String>, message: String) {
        viewModelScope.launch {
            _sendingState.value = SendingState.Sending

            val result = smsManager.sendBulkSms(phoneNumbers, message)

            if (result.isSuccess) {
                _sendingState.value = SendingState.Success
                smsManager.loadConversations()
            } else {
                _sendingState.value = SendingState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send bulk"
                )
            }

            kotlinx.coroutines.delay(2000)
            _sendingState.value = SendingState.Idle
        }
    }

    /**
     * Search messages
     */
    fun searchMessages(query: String) {
        viewModelScope.launch {
            val results = smsManager.searchMessages(query)
            // Handle search results
        }
    }

    /**
     * Delete message
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            val deleted = smsManager.deleteMessage(messageId)
            if (deleted) {
                smsManager.loadConversations()
                updateStats()
            }
        }
    }

    /**
     * Mark as read
     */
    fun markAsRead(messageId: Long) {
        viewModelScope.launch {
            smsManager.markAsRead(messageId)
            updateStats()
        }
    }

    /**
     * Update statistics
     */
    private fun updateStats() {
        _messageStats.value = smsManager.getMessageStats()
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadData()
    }
}

/**
 * Sending state
 */
sealed class SendingState {
    object Idle : SendingState()
    object Sending : SendingState()
    object Success : SendingState()
    data class Error(val message: String) : SendingState()
}

