package com.example.mentra.messaging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MessagePreloader - Preloads all messages in background when app starts
 * Keeps messages in memory for instant access
 */
@Singleton
class MessagePreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsManager: SmsManager,
    private val cacheManager: SmsCacheManager
) {
    private val TAG = "MessagePreloader"

    // Background scope for preloading
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Preload status
    private val _isPreloaded = MutableStateFlow(false)
    val isPreloaded: StateFlow<Boolean> = _isPreloaded.asStateFlow()

    private val _preloadProgress = MutableStateFlow(0f)
    val preloadProgress: StateFlow<Float> = _preloadProgress.asStateFlow()

    // Cached data
    private val _cachedConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val cachedConversations: StateFlow<List<Conversation>> = _cachedConversations.asStateFlow()

    private val _cachedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val cachedContacts: StateFlow<List<Contact>> = _cachedContacts.asStateFlow()

    // Message cache per conversation
    private val conversationMessages = mutableMapOf<String, List<SmsMessage>>()

    /**
     * Start preloading all messages in background
     */
    fun startPreloading() {
        if (_isPreloaded.value) {
            Log.d(TAG, "Already preloaded, skipping")
            return
        }

        Log.d(TAG, "Starting message preload...")

        preloadScope.launch {
            try {
                _preloadProgress.value = 0f

                // Step 1: Load from cache first (instant)
                Log.d(TAG, "Loading from cache...")
                val cachedConversations = cacheManager.getCachedConversations()
                if (cachedConversations.isNotEmpty()) {
                    _cachedConversations.value = cachedConversations
                    Log.d(TAG, "Loaded ${cachedConversations.size} conversations from cache")
                }
                _preloadProgress.value = 0.2f

                // Step 2: Load contacts (fast)
                Log.d(TAG, "Loading contacts...")
                smsManager.loadContacts()
                _cachedContacts.value = smsManager.contacts.value
                _preloadProgress.value = 0.4f

                // Step 3: Load all conversations (with latest messages)
                Log.d(TAG, "Loading conversations...")
                smsManager.loadConversations()
                val conversations = smsManager.conversations.value
                _cachedConversations.value = conversations

                // Update cache
                cacheManager.cacheConversations(conversations)
                _preloadProgress.value = 0.6f

                // Step 4: Preload messages for top 10 conversations (most recent)
                Log.d(TAG, "Preloading top conversations...")
                val topConversations = conversations.take(10)
                topConversations.forEachIndexed { index, conversation ->
                    try {
                        smsManager.loadMessages(conversation.address)
                        val messages = smsManager.messages.value
                        conversationMessages[conversation.address] = messages

                        // Update cache for this conversation
                        cacheManager.cacheMessages(conversation.address, messages)

                        val progress = 0.6f + (0.4f * (index + 1) / topConversations.size)
                        _preloadProgress.value = progress
                    } catch (e: Exception) {
                        Log.e(TAG, "Error preloading conversation ${conversation.address}", e)
                    }
                }

                _preloadProgress.value = 1f
                _isPreloaded.value = true
                Log.d(TAG, "Message preload completed! ${conversations.size} conversations loaded")

            } catch (e: Exception) {
                Log.e(TAG, "Error during preload", e)
                _isPreloaded.value = false
            }
        }
    }

    /**
     * Get cached messages for a conversation (instant if preloaded)
     */
    fun getCachedMessages(phoneNumber: String): List<SmsMessage>? {
        // First check in-memory cache
        conversationMessages[phoneNumber]?.let { return it }

        // Then check persistent cache
        return cacheManager.getCachedMessages(phoneNumber)
    }

    /**
     * Check if conversation is preloaded
     */
    fun isConversationPreloaded(phoneNumber: String): Boolean {
        return conversationMessages.containsKey(phoneNumber)
    }

    /**
     * Reload specific conversation (when new message arrives)
     */
    fun reloadConversation(phoneNumber: String) {
        preloadScope.launch {
            try {
                smsManager.loadMessages(phoneNumber)
                val messages = smsManager.messages.value
                conversationMessages[phoneNumber] = messages
                cacheManager.cacheMessages(phoneNumber, messages)
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading conversation $phoneNumber", e)
            }
        }
    }

    /**
     * Clear cache and force reload
     */
    fun clearAndReload() {
        conversationMessages.clear()
        _isPreloaded.value = false
        _preloadProgress.value = 0f
        cacheManager.clearCache()
        startPreloading()
    }
}

