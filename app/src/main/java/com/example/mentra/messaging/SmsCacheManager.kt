package com.example.mentra.messaging

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SMS CACHE MANAGER
 * Caches messages to avoid reloading on every app open
 * Only fetches new messages since last sync
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class SmsCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "mentra_sms_cache"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_CACHED_CONVERSATIONS = "cached_conversations"
        private const val KEY_CACHED_MESSAGES_PREFIX = "cached_messages_"
        private const val KEY_MESSAGE_COUNT = "message_count"

        // Cache expiry time (5 minutes for conversations list)
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // In-memory cache
    private val _cachedConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val cachedConversations: StateFlow<List<Conversation>> = _cachedConversations.asStateFlow()

    private val _cachedMessages = mutableMapOf<String, MutableList<SmsMessage>>()

    init {
        loadCachedConversations()
    }

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0)
    }

    /**
     * Update last sync timestamp
     */
    fun updateLastSyncTime() {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Check if cache is valid (not expired)
     */
    fun isCacheValid(): Boolean {
        val lastSync = getLastSyncTime()
        return System.currentTimeMillis() - lastSync < CACHE_EXPIRY_MS
    }

    /**
     * Check if we have any cached data
     */
    fun hasCachedData(): Boolean {
        return _cachedConversations.value.isNotEmpty()
    }

    /**
     * Cache conversations
     */
    fun cacheConversations(conversations: List<Conversation>) {
        _cachedConversations.value = conversations

        // Persist to SharedPreferences
        try {
            val json = gson.toJson(conversations)
            prefs.edit().putString(KEY_CACHED_CONVERSATIONS, json).apply()
            updateLastSyncTime()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load cached conversations from storage
     */
    private fun loadCachedConversations() {
        try {
            val json = prefs.getString(KEY_CACHED_CONVERSATIONS, null)
            if (json != null) {
                val type = object : TypeToken<List<Conversation>>() {}.type
                val conversations: List<Conversation> = gson.fromJson(json, type)
                _cachedConversations.value = conversations
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get cached conversations
     */
    fun getCachedConversations(): List<Conversation> {
        return _cachedConversations.value
    }

    /**
     * Cache messages for a conversation
     */
    fun cacheMessages(address: String, messages: List<SmsMessage>) {
        _cachedMessages[address] = messages.toMutableList()

        // Persist to SharedPreferences
        try {
            val json = gson.toJson(messages)
            prefs.edit().putString("$KEY_CACHED_MESSAGES_PREFIX$address", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get cached messages for a conversation
     */
    fun getCachedMessages(address: String): List<SmsMessage>? {
        // Check in-memory cache first
        _cachedMessages[address]?.let { return it }

        // Load from storage
        try {
            val json = prefs.getString("$KEY_CACHED_MESSAGES_PREFIX$address", null)
            if (json != null) {
                val type = object : TypeToken<List<SmsMessage>>() {}.type
                val messages: List<SmsMessage> = gson.fromJson(json, type)
                _cachedMessages[address] = messages.toMutableList()
                return messages
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Add new message to cache
     */
    fun addMessageToCache(address: String, message: SmsMessage) {
        val messages = _cachedMessages.getOrPut(address) { mutableListOf() }
        messages.add(message)

        // Update conversation in cache
        updateConversationWithNewMessage(address, message)

        // Persist
        cacheMessages(address, messages)
    }

    /**
     * Update conversation when new message arrives
     */
    private fun updateConversationWithNewMessage(address: String, message: SmsMessage) {
        val conversations = _cachedConversations.value.toMutableList()
        val index = conversations.indexOfFirst { it.address == address }

        if (index >= 0) {
            val conversation = conversations[index]
            conversations[index] = conversation.copy(
                lastMessage = message,
                messageCount = conversation.messageCount + 1,
                unreadCount = if (message.type == MessageType.RECEIVED) conversation.unreadCount + 1 else conversation.unreadCount
            )
            // Move to top
            val updated = conversations[index]
            conversations.removeAt(index)
            conversations.add(0, updated)
        } else {
            // New conversation
            conversations.add(0, Conversation(
                address = address,
                contactName = null,
                contactPhoto = null,
                lastMessage = message,
                messageCount = 1,
                unreadCount = if (message.type == MessageType.RECEIVED) 1 else 0,
                messages = listOf(message)
            ))
        }

        _cachedConversations.value = conversations
        cacheConversations(conversations)
    }

    /**
     * Mark conversation as read in cache
     */
    fun markConversationAsRead(address: String) {
        val conversations = _cachedConversations.value.toMutableList()
        val index = conversations.indexOfFirst { it.address == address }

        if (index >= 0) {
            conversations[index] = conversations[index].copy(unreadCount = 0)
            _cachedConversations.value = conversations
            cacheConversations(conversations)
        }
    }

    /**
     * Clear all cache
     */
    fun clearCache() {
        _cachedConversations.value = emptyList()
        _cachedMessages.clear()
        prefs.edit().clear().apply()
    }

    /**
     * Get timestamp of newest message in cache for incremental sync
     */
    fun getNewestMessageTimestamp(address: String? = null): Long {
        return if (address != null) {
            _cachedMessages[address]?.maxOfOrNull { it.timestamp } ?: 0
        } else {
            _cachedConversations.value.mapNotNull { it.lastMessage?.timestamp }.maxOrNull() ?: 0
        }
    }
}

