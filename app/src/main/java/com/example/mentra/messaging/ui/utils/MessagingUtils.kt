package com.example.mentra.messaging.ui.utils

import com.example.mentra.messaging.*
import com.example.mentra.messaging.ui.components.MessageCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════
 * MESSAGING UTILITY FUNCTIONS
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Format message timestamp for display
 */
fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneDay = 24 * 60 * 60 * 1000L

    return when {
        diff < oneDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 7 * oneDay -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Format count for display (1000 -> 1K, 1000000 -> 1M)
 */
fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

/**
 * Format timestamp with smart relative time
 */
fun formatSmartTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        diff < 172800_000 -> "Yesterday"
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Group conversations by time period
 */
fun groupConversationsByTime(conversations: List<Conversation>): Map<String, List<Conversation>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = today - 86400_000
    val thisWeek = today - 604800_000

    return conversations.groupBy { conv ->
        val timestamp = conv.lastMessage?.timestamp ?: 0L
        when {
            timestamp >= today -> "Today"
            timestamp >= yesterday -> "Yesterday"
            timestamp >= thisWeek -> "This Week"
            else -> "Earlier"
        }
    }
}

/**
 * Categorize conversation based on sender type
 */
fun categorizeConversation(conversation: Conversation): MessageCategory {
    val senderType = SmsSenderAnalyzer.getSenderType(conversation.address)
    return when (senderType) {
        SenderType.BANK -> MessageCategory.FINANCE
        SenderType.TELECOM, SenderType.GOVERNMENT -> MessageCategory.ALERTS
        SenderType.SHOPPING -> MessageCategory.BUSINESS
        SenderType.PROMOTIONAL -> MessageCategory.PROMOTIONS
        SenderType.SERVICE, SenderType.SHORTCODE -> MessageCategory.ALERTS
        SenderType.CONTACT -> MessageCategory.PERSONAL
    }
}

/**
 * Filter conversations by search query and category
 */
fun filterConversations(
    conversations: List<Conversation>,
    searchQuery: String,
    category: MessageCategory?
): List<Conversation> {
    return conversations.filter { conv ->
        val matchesSearch = searchQuery.isEmpty() ||
            conv.contactName?.contains(searchQuery, ignoreCase = true) == true ||
            conv.address.contains(searchQuery, ignoreCase = true) ||
            conv.lastMessage?.body?.contains(searchQuery, ignoreCase = true) == true

        val matchesCategory = category == null ||
            category == MessageCategory.ALL ||
            categorizeConversation(conv) == category

        matchesSearch && matchesCategory
    }
}

/**
 * Check if a phone number is valid
 */
fun isValidPhoneNumber(number: String): Boolean {
    // Must start with + or digit, not USSD (*xxx#)
    if (number.startsWith("*") && number.endsWith("#")) return false
    if (number.length > 15 || number.length < 3) return false
    val cleaned = number.replace(Regex("[^+0-9]"), "")
    return cleaned.isNotEmpty() && (cleaned.startsWith("+") || cleaned.first().isDigit())
}

/**
 * Get first valid phone number from a list
 */
fun getFirstValidPhoneNumber(phoneNumbers: List<String>): String? {
    return phoneNumbers.firstOrNull { isValidPhoneNumber(it) }
}

