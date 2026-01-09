package com.example.mentra.messaging

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS Manager
 * Handles SMS reading, sending, and contact integration
 *
 * Features:
 * - Read SMS messages (inbox, sent, drafts)
 * - Send SMS (single & multiple recipients)
 * - Contact integration
 * - Conversation threading
 * - Search & filter
 * - Message statistics
 */
@Singleton
class SmsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val smsManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.SmsManager

    /**
     * Load all SMS conversations
     * Groups messages by phone number/contact
     */
    suspend fun loadConversations() = withContext(Dispatchers.IO) {
        try {
            val conversationMap = mutableMapOf<String, MutableList<SmsMessage>>()

            // Query SMS inbox and sent
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.THREAD_ID
            )

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                val threadIdIndex = it.getColumnIndex(Telephony.Sms.THREAD_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val address = it.getString(addressIndex) ?: "Unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)
                    val type = it.getInt(typeIndex)
                    val read = it.getInt(readIndex) == 1
                    val threadId = it.getLong(threadIdIndex)

                    val message = SmsMessage(
                        id = id,
                        address = address,
                        body = body,
                        timestamp = date,
                        type = when (type) {
                            Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageType.RECEIVED
                            Telephony.Sms.MESSAGE_TYPE_SENT -> MessageType.SENT
                            Telephony.Sms.MESSAGE_TYPE_DRAFT -> MessageType.DRAFT
                            else -> MessageType.RECEIVED
                        },
                        isRead = read,
                        threadId = threadId
                    )

                    conversationMap.getOrPut(address) { mutableListOf() }.add(message)
                }
            }

            // Convert to conversations
            val conversations = conversationMap.map { (address, messages) ->
                val contact = getContactByPhone(address)
                val lastMessage = messages.firstOrNull() // Already sorted by date DESC
                val unreadCount = messages.count { !it.isRead && it.type == MessageType.RECEIVED }

                Conversation(
                    address = address,
                    contactName = contact?.name,
                    contactPhoto = contact?.photoUri,
                    lastMessage = lastMessage,
                    messageCount = messages.size,
                    unreadCount = unreadCount,
                    messages = messages.sortedByDescending { it.timestamp }
                )
            }.sortedByDescending { it.lastMessage?.timestamp ?: 0 }

            _conversations.value = conversations

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load messages for specific conversation
     */
    suspend fun loadMessages(address: String) = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<SmsMessage>()

            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.THREAD_ID
            )

            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(address),
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
                val readIndex = it.getColumnIndex(Telephony.Sms.READ)
                val threadIdIndex = it.getColumnIndex(Telephony.Sms.THREAD_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)
                    val type = it.getInt(typeIndex)
                    val read = it.getInt(readIndex) == 1
                    val threadId = it.getLong(threadIdIndex)

                    messages.add(
                        SmsMessage(
                            id = id,
                            address = address,
                            body = body,
                            timestamp = date,
                            type = when (type) {
                                Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageType.RECEIVED
                                Telephony.Sms.MESSAGE_TYPE_SENT -> MessageType.SENT
                                Telephony.Sms.MESSAGE_TYPE_DRAFT -> MessageType.DRAFT
                                else -> MessageType.RECEIVED
                            },
                            isRead = read,
                            threadId = threadId
                        )
                    )
                }
            }

            _messages.value = messages

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Send SMS message
     */
    suspend fun sendSms(
        phoneNumber: String,
        message: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // For long messages, divide into parts
            val parts = smsManager.divideMessage(message)

            if (parts.size == 1) {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            } else {
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    null,
                    null
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Send SMS to multiple recipients
     */
    suspend fun sendBulkSms(
        phoneNumbers: List<String>,
        message: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            phoneNumbers.forEach { number ->
                val result = sendSms(number, message)
                if (result.isSuccess) successCount++
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark message as read
     */
    suspend fun markAsRead(messageId: Long) = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
            }

            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString())
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Delete message
     */
    suspend fun deleteMessage(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString())
            )
            deleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load contacts from phone
     */
    suspend fun loadContacts() = withContext(Dispatchers.IO) {
        try {
            val contacts = mutableListOf<Contact>()

            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            )

            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val photoUri = it.getString(photoIndex)
                    val hasPhone = it.getInt(hasPhoneIndex) > 0

                    if (hasPhone) {
                        val phoneNumbers = getPhoneNumbers(id)
                        if (phoneNumbers.isNotEmpty()) {
                            contacts.add(
                                Contact(
                                    id = id,
                                    name = name,
                                    phoneNumbers = phoneNumbers,
                                    photoUri = photoUri
                                )
                            )
                        }
                    }
                }
            }

            _contacts.value = contacts

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get phone numbers for contact
     */
    private fun getPhoneNumbers(contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                phoneNumbers.add(it.getString(numberIndex))
            }
        }

        return phoneNumbers
    }

    /**
     * Get contact by phone number
     */
    private fun getContactByPhone(phoneNumber: String): Contact? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUri = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI))

                return Contact(
                    id = id,
                    name = name,
                    phoneNumbers = listOf(phoneNumber),
                    photoUri = photoUri
                )
            }
        }

        return null
    }

    /**
     * Search messages
     */
    fun searchMessages(query: String): List<SmsMessage> {
        return _messages.value.filter { message ->
            message.body.contains(query, ignoreCase = true) ||
            message.address.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get message statistics
     */
    fun getMessageStats(): MessageStatistics {
        val allMessages = _conversations.value.flatMap { it.messages }

        return MessageStatistics(
            totalMessages = allMessages.size,
            receivedCount = allMessages.count { it.type == MessageType.RECEIVED },
            sentCount = allMessages.count { it.type == MessageType.SENT },
            unreadCount = allMessages.count { !it.isRead && it.type == MessageType.RECEIVED },
            conversationCount = _conversations.value.size
        )
    }
}

/**
 * SMS Message
 */
data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: MessageType,
    val isRead: Boolean,
    val threadId: Long
)

/**
 * Conversation (grouped messages)
 */
data class Conversation(
    val address: String,
    val contactName: String?,
    val contactPhoto: String?,
    val lastMessage: SmsMessage?,
    val messageCount: Int,
    val unreadCount: Int,
    val messages: List<SmsMessage>
)

/**
 * Contact
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>,
    val photoUri: String?
)

/**
 * Message statistics
 */
data class MessageStatistics(
    val totalMessages: Int,
    val receivedCount: Int,
    val sentCount: Int,
    val unreadCount: Int,
    val conversationCount: Int
)

/**
 * Message types
 */
enum class MessageType {
    RECEIVED,
    SENT,
    DRAFT
}

