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
 * Manages SMS state and operations
 */
@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val smsManager: SmsManager
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = smsManager.conversations
    val messages: StateFlow<List<SmsMessage>> = smsManager.messages
    val contacts: StateFlow<List<Contact>> = smsManager.contacts

    private val _messageStats = MutableStateFlow<MessageStatistics?>(null)
    val messageStats: StateFlow<MessageStatistics?> = _messageStats.asStateFlow()

    private val _sendingState = MutableStateFlow<SendingState>(SendingState.Idle)
    val sendingState: StateFlow<SendingState> = _sendingState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load all messaging data
     */
    private fun loadData() {
        viewModelScope.launch {
            // Load conversations
            smsManager.loadConversations()

            // Load contacts
            smsManager.loadContacts()

            // Update stats
            updateStats()
        }
    }

    /**
     * Load specific conversation
     */
    fun loadConversation(phoneNumber: String) {
        viewModelScope.launch {
            smsManager.loadMessages(phoneNumber)

            // Mark all as read
            messages.value.forEach { message ->
                if (!message.isRead && message.type == MessageType.RECEIVED) {
                    smsManager.markAsRead(message.id)
                }
            }
        }
    }

    /**
     * Send SMS message
     */
    fun sendMessage(phoneNumber: String, message: String) {
        viewModelScope.launch {
            _sendingState.value = SendingState.Sending

            val result = smsManager.sendSms(phoneNumber, message)

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

