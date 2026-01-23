package com.example.mentra.shell.messaging

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.database.getStringOrNull
import com.example.mentra.messaging.Contact
import com.example.mentra.messaging.SimInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL MESSAGING SERVICE
 * Natural language SMS handling for the Mentra Shell
 * ═══════════════════════════════════════════════════════════════════
 *
 * Supports commands like:
 * - "send my wife a message"
 * - "text mom hello"
 * - "sms +1234567890 meeting at 5pm"
 * - "message dad I'll be late"
 */
@Singleton
class ShellMessagingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aliasManager: ContactAliasManager
) {

    private val _currentState = MutableStateFlow<MessagingState>(MessagingState.Idle)
    val currentState: StateFlow<MessagingState> = _currentState.asStateFlow()

    private val _pendingMessage = MutableStateFlow<PendingMessage?>(null)
    val pendingMessage: StateFlow<PendingMessage?> = _pendingMessage.asStateFlow()

    // Intent keywords for message sending
    private val sendKeywords = listOf(
        "send", "text", "sms", "message", "msg", "write", "compose"
    )

    // Relationship aliases that users commonly use
    private val relationshipKeywords = listOf(
        "wife", "husband", "mom", "mother", "dad", "father", "son", "daughter",
        "brother", "sister", "bro", "sis", "boss", "friend", "bestie", "bff",
        "gf", "girlfriend", "bf", "boyfriend", "babe", "honey", "love",
        "grandma", "grandpa", "grandmother", "grandfather", "uncle", "aunt",
        "cousin", "nephew", "niece", "partner", "spouse", "fiancé", "fiancee"
    )

    /**
     * Parse natural language input and determine messaging intent
     */
    suspend fun parseCommand(input: String): MessagingIntent {
        val lowercaseInput = input.lowercase().trim()
        val words = lowercaseInput.split(Regex("\\s+"))

        // Check if this is a messaging command
        val hasSendKeyword = words.any { it in sendKeywords }
        if (!hasSendKeyword) {
            return MessagingIntent.NotAMessageCommand
        }

        // Extract recipient and message
        return parseMessageIntent(input, words)
    }

    private suspend fun parseMessageIntent(input: String, words: List<String>): MessagingIntent {
        // Supported patterns:
        // "send message to 0712123123"
        // "send 0712123123 hello"
        // "text my wife hello"
        // "sms +254712123123 meeting at 5"
        // "message mom I'll be late"
        // "send a message to wife saying hello" // "message x hello" (where x is a custom alias)

        var recipient: String? = null
        var messageContent: String? = null
        var isAlias = false

        // First, check if any word in the input is a registered alias
        // This allows custom aliases like "x", "dk", etc.
        for (word in words) {
            val cleanWord = word.lowercase().replace(Regex("[^a-z0-9]"), "")
            if (cleanWord.isNotEmpty() && cleanWord !in sendKeywords && cleanWord != "a" && cleanWord != "my" && cleanWord != "to") {
                val resolvedContact = aliasManager.getContactByAlias(cleanWord)
                if (resolvedContact != null) {
                    recipient = cleanWord
                    isAlias = true
                    messageContent = extractMessageContent(input, recipient)
                    return MessagingIntent.SendToAlias(
                        alias = recipient,
                        contact = resolvedContact,
                        message = messageContent
                    )
                }
            }
        }

        // Pattern: "to [number/alias]" - handles "send message to 0712123123"
        val toPattern = Regex("\\bto\\s+([+]?[0-9]{7,15}|\\w+)", RegexOption.IGNORE_CASE)
        val toMatch = toPattern.find(input)
        if (toMatch != null) {
            val potentialRecipient = toMatch.groupValues[1]
            if (isValidPhoneNumber(potentialRecipient)) {
                recipient = potentialRecipient
                isAlias = false
            } else {
                // Check alias database first, then relationship keywords
                val resolvedContact = aliasManager.getContactByAlias(potentialRecipient.lowercase())
                if (resolvedContact != null) {
                    messageContent = extractMessageContent(input, potentialRecipient)
                    return MessagingIntent.SendToAlias(
                        alias = potentialRecipient.lowercase(),
                        contact = resolvedContact,
                        message = messageContent
                    )
                } else if (potentialRecipient.lowercase() in relationshipKeywords) {
                    recipient = potentialRecipient.lowercase()
                    isAlias = true
                } else {
                    // Treat as potential alias to set up
                    recipient = potentialRecipient.lowercase()
                    isAlias = true
                }
            }
        }

        // Pattern: "my [relationship]" - handles "send my wife a message"
        if (recipient == null) {
            val myPattern = Regex("\\bmy\\s+(\\w+)", RegexOption.IGNORE_CASE)
            val myMatch = myPattern.find(input)
            if (myMatch != null) {
                val potentialAlias = myMatch.groupValues[1].lowercase()
                // Check alias database first
                val resolvedContact = aliasManager.getContactByAlias(potentialAlias)
                if (resolvedContact != null) {
                    messageContent = extractMessageContent(input, potentialAlias)
                    return MessagingIntent.SendToAlias(
                        alias = potentialAlias,
                        contact = resolvedContact,
                        message = messageContent
                    )
                } else if (potentialAlias in relationshipKeywords) {
                    recipient = potentialAlias
                    isAlias = true
                }
            }
        }

        // Pattern: Direct phone number anywhere in the input
        if (recipient == null) {
            val phonePattern = Regex("\\b([+]?[0-9]{7,15})\\b")
            val phoneMatch = phonePattern.find(input)
            if (phoneMatch != null) {
                recipient = phoneMatch.groupValues[1]
                isAlias = false
            }
        }

        // Pattern: Direct alias without "my" (e.g., "text wife hello")
        if (recipient == null) {
            for (word in words) {
                val cleanWord = word.lowercase().replace(Regex("[^a-z]"), "")
                if (cleanWord in relationshipKeywords) {
                    recipient = cleanWord
                    isAlias = true
                    break
                }
            }
        }

        // Extract message content - everything after the recipient
        messageContent = extractMessageContent(input, recipient)

        // If we found an alias, check if it's registered
        if (isAlias && recipient != null) {
            val resolvedContact = aliasManager.getContactByAlias(recipient)
            if (resolvedContact != null) {
                return MessagingIntent.SendToAlias(
                    alias = recipient,
                    contact = resolvedContact,
                    message = messageContent
                )
            } else {
                return MessagingIntent.AliasNotFound(alias = recipient)
            }
        }

        // If we have a phone number
        if (recipient != null && !isAlias) {
            if (isValidPhoneNumber(recipient)) {
                return MessagingIntent.SendToNumber(
                    phoneNumber = recipient,
                    message = messageContent
                )
            } else {
                return MessagingIntent.InvalidNumber(recipient)
            }
        }

        // No recipient specified - ask user
        return MessagingIntent.NeedRecipient(message = messageContent)
    }

    /**
     * Extract message content from input, removing command words and recipient
     */
    private fun extractMessageContent(input: String, recipient: String?): String? {
        // Common patterns for message content
        val contentPatterns = listOf(
            // "saying [message]" or "say [message]"
            Regex("\\b(?:saying|say)\\s+(.+)$", RegexOption.IGNORE_CASE),
            // "message: [content]" or "text: [content]"
            Regex("(?:message|text|sms):\\s*(.+)$", RegexOption.IGNORE_CASE),
            // "that [message]"
            Regex("\\bthat\\s+(.+)$", RegexOption.IGNORE_CASE),
            // After recipient, everything else is the message
            // e.g., "text 0712123123 hello world" -> "hello world"
            // e.g., "sms wife I'll be late" -> "I'll be late"
        )

        for (pattern in contentPatterns) {
            val match = pattern.find(input)
            if (match != null) {
                return match.groupValues[1].trim().takeIf { it.isNotBlank() }
            }
        }

        // Try to extract content after recipient
        if (recipient != null) {
            val recipientIndex = input.indexOf(recipient, ignoreCase = true)
            if (recipientIndex != -1) {
                val afterRecipient = input.substring(recipientIndex + recipient.length).trim()
                // Remove common filler words at the start
                val cleanedContent = afterRecipient
                    .removePrefix("a message")
                    .removePrefix("a text")
                    .removePrefix("an sms")
                    .removePrefix("message")
                    .removePrefix("text")
                    .removePrefix("sms")
                    .trim()
                if (cleanedContent.isNotBlank()) {
                    return cleanedContent
                }
            }
        }

        return null
    }

    /**
     * Set the current messaging state
     */
    fun setState(state: MessagingState) {
        _currentState.value = state
    }

    /**
     * Set pending message for later sending
     */
    fun setPendingMessage(message: PendingMessage?) {
        _pendingMessage.value = message
    }

    /**
     * Send SMS message
     */
    suspend fun sendMessage(
        phoneNumber: String,
        message: String,
        simSlot: Int = 0
    ): SendResult = withContext(Dispatchers.IO) {
        try {
            if (!isValidPhoneNumber(phoneNumber)) {
                return@withContext SendResult.InvalidNumber
            }

            if (message.isBlank()) {
                return@withContext SendResult.EmptyMessage
            }

            @Suppress("DEPRECATION")
            val smsManager = if (simSlot > 0) {
                SmsManager.getSmsManagerForSubscriptionId(simSlot)
            } else {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            }

            // Split long messages
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }

            SendResult.Success
        } catch (e: Exception) {
            SendResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Get all contacts from the phone
     */
    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            val seenIds = mutableSetOf<String>()

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                if (id in seenIds) continue
                seenIds.add(id)

                val name = it.getStringOrNull(nameIndex) ?: continue
                val number = it.getStringOrNull(numberIndex) ?: continue
                val photoUri = it.getStringOrNull(photoIndex)

                contacts.add(
                    Contact(
                        id = id,
                        name = name,
                        phoneNumbers = listOf(number),
                        photoUri = photoUri
                    )
                )
            }
        }

        contacts
    }

    /**
     * Search contacts by name or number
     */
    suspend fun searchContacts(query: String): List<Contact> {
        val allContacts = getAllContacts()
        val lowercaseQuery = query.lowercase()

        return allContacts.filter { contact ->
            contact.name.lowercase().contains(lowercaseQuery) ||
            contact.phoneNumbers.any { it.contains(query) }
        }
    }

    /**
     * Validate phone number format
     */
    fun isValidPhoneNumber(number: String): Boolean {
        val cleaned = number.replace(Regex("[^+0-9]"), "")
        // Must start with + or digit, not USSD (*xxx#)
        if (number.startsWith("*") && number.endsWith("#")) return false
        if (cleaned.length !in 7..15) return false
        return cleaned.isNotEmpty() && (cleaned.startsWith("+") || cleaned.first().isDigit())
    }

    /**
     * Reset the messaging service state
     */
    fun reset() {
        _currentState.value = MessagingState.Idle
        _pendingMessage.value = null
    }

    /**
     * Get recent conversations (inbox summary)
     */
    suspend fun getInbox(limit: Int = 10): List<InboxConversation> = withContext(Dispatchers.IO) {
        val conversations = mutableListOf<InboxConversation>()

        try {
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE
            )

            // Get latest message per conversation
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            val seenAddresses = mutableSetOf<String>()

            cursor?.use {
                while (it.moveToNext() && conversations.size < limit) {
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: continue
                    val normalizedAddress = normalizeNumber(address)

                    if (normalizedAddress in seenAddresses) continue
                    seenAddresses.add(normalizedAddress)

                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    val isRead = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                    // Lookup contact name
                    val contactName = getContactName(normalizedAddress)

                    // Count unread messages from this address
                    val unreadCount = countUnreadFrom(normalizedAddress)

                    conversations.add(InboxConversation(
                        address = normalizedAddress,
                        contactName = contactName,
                        lastMessage = body,
                        lastMessageTime = date,
                        unreadCount = unreadCount,
                        isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT
                    ))
                }
            }
        } catch (e: Exception) {
            // Handle permission or other errors
        }

        conversations
    }

    /**
     * Get unread message count
     */
    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("COUNT(*)"),
                "${Telephony.Sms.READ} = 0",
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return@withContext it.getInt(0)
                }
            }
        } catch (e: Exception) {
            // Handle errors
        }
        0
    }

    /**
     * Count unread messages from a specific address
     */
    private fun countUnreadFrom(address: String): Int {
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("COUNT(*)"),
                "${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf("%${address.takeLast(10)}%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getInt(0)
                }
            }
        } catch (e: Exception) {
            // Handle errors
        }
        return 0
    }

    /**
     * Read messages from a specific contact/number
     */
    suspend fun readMessagesFrom(addressOrAlias: String, limit: Int = 20): List<ShellMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<ShellMessage>()

        // Resolve alias if needed
        val resolvedAddress = aliasManager.getContactByAlias(addressOrAlias.lowercase())
            ?.phoneNumbers?.firstOrNull() ?: addressOrAlias

        try {
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

            // Match by last digits of phone number
            val selection = "${Telephony.Sms.ADDRESS} LIKE ?"
            val selectionArgs = arrayOf("%${resolvedAddress.takeLast(10)}%")

            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                    val isRead = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1

                    messages.add(ShellMessage(
                        id = id,
                        body = body,
                        timestamp = date,
                        isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT,
                        isRead = isRead
                    ))
                }
            }

            // Mark as read after reading
            markMessagesAsRead(resolvedAddress)
        } catch (e: Exception) {
            // Handle errors
        }

        // Return in chronological order (oldest first)
        messages.reversed()
    }

    /**
     * Mark messages from an address as read
     */
    private fun markMessagesAsRead(address: String) {
        try {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }
            context.contentResolver.update(
                Telephony.Sms.Inbox.CONTENT_URI,
                values,
                "${Telephony.Sms.ADDRESS} LIKE ? AND ${Telephony.Sms.READ} = 0",
                arrayOf("%${address.takeLast(10)}%")
            )
        } catch (e: Exception) {
            // Handle errors (might need default SMS app permission)
        }
    }

    /**
     * Get contact name from phone number
     */
    private fun getContactName(phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0)
                }
            }
        } catch (e: Exception) {
            // Handle errors
        }
        return null
    }

    /**
     * Normalize phone number for comparison
     */
    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^+0-9]"), "")
    }

    /**
     * Search for threads matching a keyword (contact name or address)
     * Returns list of matching conversations
     */
    suspend fun searchThreadsByKeyword(keyword: String, limit: Int = 10): List<InboxConversation> = withContext(Dispatchers.IO) {
        val allConversations = getInbox(50) // Get more to search through
        val lowerKeyword = keyword.lowercase()

        allConversations.filter { conv ->
            val nameMatch = conv.contactName?.lowercase()?.contains(lowerKeyword) == true
            val addressMatch = conv.address.lowercase().contains(lowerKeyword)
            nameMatch || addressMatch
        }.take(limit)
    }
}

/**
 * Messaging intent types parsed from natural language
 */
sealed class MessagingIntent {
    object NotAMessageCommand : MessagingIntent()

    data class SendToAlias(
        val alias: String,
        val contact: Contact,
        val message: String?
    ) : MessagingIntent()

    data class SendToNumber(
        val phoneNumber: String,
        val message: String?
    ) : MessagingIntent()

    data class AliasNotFound(
        val alias: String
    ) : MessagingIntent()

    data class InvalidNumber(
        val number: String
    ) : MessagingIntent()

    data class NeedRecipient(
        val message: String?
    ) : MessagingIntent()
}

/**
 * Current state of the messaging flow
 */
sealed class MessagingState {
    object Idle : MessagingState()
    object WaitingForRecipientChoice : MessagingState()
    object WaitingForPhoneNumber : MessagingState()
    object WaitingForContactSearch : MessagingState()
    object WaitingForMessage : MessagingState()
    object WaitingForAliasSetup : MessagingState()
    data class WaitingForConfirmation(val summary: String) : MessagingState()
    object Sending : MessagingState()
    data class Error(val message: String) : MessagingState()
}

/**
 * Pending message waiting to be sent
 */
data class PendingMessage(
    val recipient: String,
    val recipientName: String?,
    val message: String,
    val simSlot: Int = 0
)

/**
 * Result of sending a message
 */
sealed class SendResult {
    object Success : SendResult()
    object InvalidNumber : SendResult()
    object EmptyMessage : SendResult()
    data class Failed(val error: String) : SendResult()
}

/**
 * Inbox conversation summary
 */
data class InboxConversation(
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isOutgoing: Boolean
)

/**
 * Message for shell display
 */
data class ShellMessage(
    val id: Long,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isRead: Boolean
)
