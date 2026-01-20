package com.example.mentra.shell.messaging

import com.example.mentra.messaging.Contact
import com.example.mentra.shell.models.ShellResult
import com.example.mentra.shell.models.ResultStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shell output with color support
 */
data class ShellOutput(
    val text: String,
    val type: ShellOutputType = ShellOutputType.INFO,
    val color: String? = null
)

enum class ShellOutputType {
    INFO,
    SUCCESS,
    ERROR,
    WARNING,
    PROMPT,
    HEADER
}

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL MESSAGING COMMAND HANDLER
 * Processes messaging commands from the shell and manages the
 * conversational flow for sending messages
 * ═══════════════════════════════════════════════════════════════════
 *
 * Command Examples:
 * - "send my wife a message"
 * - "text mom hello how are you"
 * - "sms +1234567890 meeting at 5pm"
 * - "message dad I'll be late"
 * - "send a message" (will prompt for recipient)
 * - "text" (will prompt for recipient and message)
 *
 * Alias Setup:
 * - "set wife as [contact name]"
 * - "alias wife = [contact name]"
 * - System will prompt when alias is not found
 */
@Singleton
class ShellMessagingCommandHandler @Inject constructor(
    private val messagingService: ShellMessagingService,
    private val aliasManager: ContactAliasManager
) {
    // Current conversation state
    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.None)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    // Pending alias to set up
    private var pendingAliasSetup: String? = null

    // Pending message details
    private var pendingRecipient: String? = null
    private var pendingRecipientName: String? = null
    private var pendingMessageContent: String? = null

    // Contact selection callback
    private val _showContactPicker = MutableStateFlow<ContactPickerRequest?>(null)
    val showContactPicker: StateFlow<ContactPickerRequest?> = _showContactPicker.asStateFlow()

    /**
     * Check if the input matches messaging commands
     */
    fun isMessagingCommand(input: String): Boolean {
        val lowercaseInput = input.lowercase().trim()
        val messagingKeywords = listOf(
            "send", "text", "sms", "message", "msg", "compose",
            "alias", "set alias", "setup"
        )
        return messagingKeywords.any { lowercaseInput.startsWith(it) || lowercaseInput.contains("message") }
    }

    /**
     * Handle a shell input command
     * Returns list of shell outputs to display
     */
    suspend fun handleCommand(input: String): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()
        val lowercaseInput = input.lowercase().trim()

        // Check for alias setup commands first
        if (lowercaseInput.startsWith("alias ") || lowercaseInput.startsWith("set alias") ||
            lowercaseInput.startsWith("set ") && lowercaseInput.contains(" as ")) {
            return handleAliasSetupCommand(input)
        }

        // Check if we're in a conversation flow
        when (_conversationState.value) {
            is ConversationState.WaitingForRecipientChoice -> {
                return handleRecipientChoiceInput(input)
            }
            is ConversationState.WaitingForPhoneNumber -> {
                return handlePhoneNumberInput(input)
            }
            is ConversationState.WaitingForMessage -> {
                return handleMessageInput(input)
            }
            is ConversationState.WaitingForConfirmation -> {
                return handleConfirmationInput(input)
            }
            is ConversationState.WaitingForAliasSelection -> {
                return handleAliasSelectionInput(input)
            }
            else -> {}
        }

        // Parse the messaging intent
        val intent = messagingService.parseCommand(input)

        return when (intent) {
            is MessagingIntent.NotAMessageCommand -> {
                listOf(ShellOutput(
                    text = "Not a messaging command",
                    type = ShellOutputType.ERROR
                ))
            }

            is MessagingIntent.SendToAlias -> {
                handleSendToAlias(intent)
            }

            is MessagingIntent.SendToNumber -> {
                handleSendToNumber(intent)
            }

            is MessagingIntent.AliasNotFound -> {
                handleAliasNotFound(intent.alias)
            }

            is MessagingIntent.NeedRecipient -> {
                handleNeedRecipient(intent.message)
            }

            is MessagingIntent.InvalidNumber -> {
                listOf(
                    ShellOutput(
                        text = "❌ Invalid phone number: ${intent.number}",
                        type = ShellOutputType.ERROR
                    ),
                    ShellOutput(
                        text = "Please enter a valid phone number (7-15 digits)",
                        type = ShellOutputType.INFO
                    )
                )
            }
        }
    }

    /**
     * Handle sending to a known alias
     */
    private suspend fun handleSendToAlias(intent: MessagingIntent.SendToAlias): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        pendingRecipient = intent.contact.phoneNumbers.firstOrNull()
        pendingRecipientName = intent.contact.name

        outputs.add(ShellOutput(
            text = "📱 Sending to ${intent.alias} (${intent.contact.name})",
            type = ShellOutputType.INFO,
            color = "#00F5D4"
        ))

        if (intent.message != null) {
            pendingMessageContent = intent.message
            // We have both recipient and message - send directly (minimal flow)
            return sendMessage()
        } else {
            // Need message content
            outputs.add(ShellOutput(
                text = "Message:",
                type = ShellOutputType.PROMPT,
                color = "#7B61FF"
            ))
            _conversationState.value = ConversationState.WaitingForMessage
        }

        return outputs
    }

    /**
     * Handle sending to a phone number
     */
    private suspend fun handleSendToNumber(intent: MessagingIntent.SendToNumber): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        pendingRecipient = intent.phoneNumber
        pendingRecipientName = null

        outputs.add(ShellOutput(
            text = "📱 To: ${intent.phoneNumber}",
            type = ShellOutputType.INFO,
            color = "#00F5D4"
        ))

        if (intent.message != null) {
            pendingMessageContent = intent.message
            // Send directly (minimal flow)
            return sendMessage()
        } else {
            outputs.add(ShellOutput(
                text = "Message:",
                type = ShellOutputType.PROMPT,
                color = "#7B61FF"
            ))
            _conversationState.value = ConversationState.WaitingForMessage
        }

        return outputs
    }

    /**
     * Handle when an alias is not found - prompt to set it up
     */
    private fun handleAliasNotFound(alias: String): List<ShellOutput> {
        pendingAliasSetup = alias

        val aliasInfo = aliasManager.getSuggestedAliasInfo(alias)
        val emoji = aliasInfo?.emoji ?: "👤"

        val outputs = mutableListOf(
            ShellOutput(
                text = "$emoji \"$alias\" not set. Setup? (1=contacts, 2=number, 3=cancel)",
                type = ShellOutputType.PROMPT,
                color = "#FFE66D"
            )
        )

        _conversationState.value = ConversationState.WaitingForAliasSelection
        return outputs
    }

    /**
     * Handle when no recipient is specified
     */
    private fun handleNeedRecipient(message: String?): List<ShellOutput> {
        pendingMessageContent = message

        return listOf(
            ShellOutput(
                text = "📨 To? (1=number, 2=contacts, 3=alias, 4=cancel)",
                type = ShellOutputType.PROMPT,
                color = "#00F5D4"
            )
        ).also {
            _conversationState.value = ConversationState.WaitingForRecipientChoice
        }
    }

    /**
     * Handle recipient choice input
     */
    private suspend fun handleRecipientChoiceInput(input: String): List<ShellOutput> {
        val trimmed = input.trim()

        return when {
            trimmed == "1" || trimmed.lowercase() == "number" -> {
                _conversationState.value = ConversationState.WaitingForPhoneNumber
                listOf(ShellOutput(
                    text = "Number:",
                    type = ShellOutputType.PROMPT,
                    color = "#00F5D4"
                ))
            }
            trimmed == "2" || trimmed.lowercase() == "contacts" || trimmed.lowercase() == "search" -> {
                // Trigger contact picker popup
                _showContactPicker.value = ContactPickerRequest(
                    title = "Select Contact",
                    onSelected = { contact ->
                        pendingRecipient = contact.phoneNumbers.firstOrNull()
                        pendingRecipientName = contact.name
                    }
                )
                _conversationState.value = ConversationState.WaitingForContactSelection
                listOf(ShellOutput(
                    text = "📋 Opening contacts...",
                    type = ShellOutputType.INFO,
                    color = "#7B61FF"
                ))
            }
            trimmed == "3" || trimmed.lowercase() == "alias" -> {
                listOf(
                    ShellOutput(
                        text = "Alias:",
                        type = ShellOutputType.PROMPT,
                        color = "#FFE66D"
                    )
                )
            }
            trimmed == "4" || trimmed.lowercase() == "cancel" -> {
                reset()
                listOf(ShellOutput(
                    text = "Cancelled.",
                    type = ShellOutputType.INFO,
                    color = "#FF6B6B"
                ))
            }
            // Check if input looks like a phone number
            trimmed.matches(Regex("^\\+?[0-9\\s-]{7,}$")) -> {
                handlePhoneNumberInput(trimmed)
            }
            // Try as contact search or alias
            else -> {
                // First check aliases
                val contact = aliasManager.getContactByAlias(trimmed)
                if (contact != null) {
                    pendingRecipient = contact.phoneNumbers.firstOrNull()
                    pendingRecipientName = contact.name

                    val outputs = mutableListOf(
                        ShellOutput(
                            text = "✓ Found: ${contact.name} (${pendingRecipient})",
                            type = ShellOutputType.SUCCESS,
                            color = "#00F5D4"
                        )
                    )

                    if (pendingMessageContent != null) {
                        outputs.add(ShellOutput(
                            text = "📝 Message: \"$pendingMessageContent\"",
                            type = ShellOutputType.INFO
                        ))
                        outputs.add(ShellOutput(
                            text = "\nSend this message? (yes/no)",
                            type = ShellOutputType.PROMPT,
                            color = "#FFE66D"
                        ))
                        _conversationState.value = ConversationState.WaitingForConfirmation
                    } else {
                        outputs.add(ShellOutput(
                            text = "Enter your message:",
                            type = ShellOutputType.PROMPT,
                            color = "#7B61FF"
                        ))
                        _conversationState.value = ConversationState.WaitingForMessage
                    }

                    outputs
                } else {
                    // Search contacts
                    val contacts = messagingService.searchContacts(trimmed)
                    if (contacts.isNotEmpty()) {
                        if (contacts.size == 1) {
                            val contact = contacts.first()
                            pendingRecipient = contact.phoneNumbers.firstOrNull()
                            pendingRecipientName = contact.name

                            val outputs = mutableListOf(
                                ShellOutput(
                                    text = "✓ Found: ${contact.name} (${pendingRecipient})",
                                    type = ShellOutputType.SUCCESS,
                                    color = "#00F5D4"
                                )
                            )

                            if (pendingMessageContent != null) {
                                outputs.add(ShellOutput(
                                    text = "📝 Message: \"$pendingMessageContent\"",
                                    type = ShellOutputType.INFO
                                ))
                                outputs.add(ShellOutput(
                                    text = "\nSend this message? (yes/no)",
                                    type = ShellOutputType.PROMPT,
                                    color = "#FFE66D"
                                ))
                                _conversationState.value = ConversationState.WaitingForConfirmation
                            } else {
                                outputs.add(ShellOutput(
                                    text = "Enter your message:",
                                    type = ShellOutputType.PROMPT,
                                    color = "#7B61FF"
                                ))
                                _conversationState.value = ConversationState.WaitingForMessage
                            }

                            outputs
                        } else {
                            // Multiple contacts found - show list
                            val outputs = mutableListOf(
                                ShellOutput(
                                    text = "Found ${contacts.size} contacts:",
                                    type = ShellOutputType.INFO
                                )
                            )

                            contacts.take(10).forEachIndexed { index, contact ->
                                outputs.add(ShellOutput(
                                    text = "  ${index + 1}. ${contact.name} (${contact.phoneNumbers.firstOrNull()})",
                                    type = ShellOutputType.INFO,
                                    color = if (index % 2 == 0) "#00F5D4" else "#7B61FF"
                                ))
                            }

                            outputs.add(ShellOutput(
                                text = "\nEnter number to select (1-${minOf(contacts.size, 10)}):",
                                type = ShellOutputType.PROMPT
                            ))

                            // Store contacts for selection
                            _conversationState.value = ConversationState.WaitingForContactListSelection(contacts.take(10))
                            outputs
                        }
                    } else {
                        listOf(
                            ShellOutput(
                                text = "❌ No contact found for \"$trimmed\"",
                                type = ShellOutputType.ERROR
                            ),
                            ShellOutput(
                                text = "Try entering a phone number or search again:",
                                type = ShellOutputType.PROMPT
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Handle phone number input
     */
    private suspend fun handlePhoneNumberInput(input: String): List<ShellOutput> {
        val cleaned = input.replace(Regex("[\\s-]"), "")

        if (!messagingService.isValidPhoneNumber(cleaned)) {
            return listOf(
                ShellOutput(
                    text = "❌ Invalid phone number format",
                    type = ShellOutputType.ERROR
                ),
                ShellOutput(
                    text = "Please enter a valid number (7-15 digits, can start with +):",
                    type = ShellOutputType.PROMPT
                )
            )
        }

        pendingRecipient = cleaned
        pendingRecipientName = null

        if (pendingMessageContent != null) {
            // Send directly (minimal flow)
            return sendMessage()
        } else {
            return listOf(ShellOutput(
                text = "Message:",
                type = ShellOutputType.PROMPT,
                color = "#7B61FF"
            )).also {
                _conversationState.value = ConversationState.WaitingForMessage
            }
        }
    }

    /**
     * Handle message content input
     */
    private suspend fun handleMessageInput(input: String): List<ShellOutput> {
        if (input.isBlank()) {
            return listOf(
                ShellOutput(
                    text = "❌ Empty message",
                    type = ShellOutputType.ERROR
                ),
                ShellOutput(
                    text = "Message:",
                    type = ShellOutputType.PROMPT
                )
            )
        }

        pendingMessageContent = input

        // Send directly without confirmation (minimal flow)
        return sendMessage()
    }

    /**
     * Handle confirmation input (only used when explicitly requested)
     */
    private suspend fun handleConfirmationInput(input: String): List<ShellOutput> {
        val lowercaseInput = input.lowercase().trim()

        return when {
            lowercaseInput in listOf("yes", "y", "send", "ok", "confirm", "") -> {
                sendMessage()
            }
            lowercaseInput in listOf("no", "n", "cancel") -> {
                reset()
                listOf(ShellOutput(
                    text = "Cancelled.",
                    type = ShellOutputType.INFO,
                    color = "#FF6B6B"
                ))
            }
            lowercaseInput in listOf("edit", "e", "change") -> {
                listOf(
                    ShellOutput(
                        text = "Enter new message:",
                        type = ShellOutputType.PROMPT,
                        color = "#7B61FF"
                    )
                ).also {
                    _conversationState.value = ConversationState.WaitingForMessage
                }
            }
            else -> {
                listOf(
                    ShellOutput(
                        text = "Please enter 'yes' to send, 'no' to cancel, or 'edit' to change message:",
                        type = ShellOutputType.PROMPT
                    )
                )
            }
        }
    }

    /**
     * Handle alias selection input
     */
    private fun handleAliasSelectionInput(input: String): List<ShellOutput> {
        val trimmed = input.trim()

        return when (trimmed) {
            "1" -> {
                // Search contacts - trigger popup
                _showContactPicker.value = ContactPickerRequest(
                    title = "Select contact for \"$pendingAliasSetup\"",
                    forAlias = pendingAliasSetup,
                    onSelected = { contact ->
                        // This will be handled by the UI
                    }
                )
                _conversationState.value = ConversationState.WaitingForContactSelection
                listOf(ShellOutput(
                    text = "📋 Opening contact picker...",
                    type = ShellOutputType.INFO,
                    color = "#7B61FF"
                ))
            }
            "2" -> {
                _conversationState.value = ConversationState.WaitingForAliasPhoneNumber
                listOf(ShellOutput(
                    text = "Enter phone number for \"$pendingAliasSetup\":",
                    type = ShellOutputType.PROMPT,
                    color = "#00F5D4"
                ))
            }
            "3", "cancel" -> {
                reset()
                listOf(ShellOutput(
                    text = "Alias setup cancelled.",
                    type = ShellOutputType.INFO,
                    color = "#FF6B6B"
                ))
            }
            else -> {
                listOf(ShellOutput(
                    text = "Please enter 1, 2, or 3:",
                    type = ShellOutputType.PROMPT
                ))
            }
        }
    }

    /**
     * Handle alias setup command
     */
    private suspend fun handleAliasSetupCommand(input: String): List<ShellOutput> {
        // Parse "alias wife = John Doe" or "set wife as John Doe"
        val aliasPattern = Regex("(?:alias|set)\\s+(\\w+)\\s*(?:=|as)\\s*(.+)", RegexOption.IGNORE_CASE)
        val match = aliasPattern.find(input)

        if (match == null) {
            return listOf(
                ShellOutput(
                    text = "Usage: alias [name] = [contact name or number]",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Example: alias wife = Jane Doe",
                    type = ShellOutputType.INFO,
                    color = "#00F5D4"
                )
            )
        }

        val aliasName = match.groupValues[1].lowercase()
        val contactQuery = match.groupValues[2].trim()

        // Check if it's a phone number
        if (messagingService.isValidPhoneNumber(contactQuery)) {
            aliasManager.setAlias(
                aliasName,
                Contact(id = "", name = contactQuery, phoneNumbers = listOf(contactQuery), photoUri = null),
                contactQuery
            )
            return listOf(
                ShellOutput(
                    text = "✓ Alias \"$aliasName\" set to $contactQuery",
                    type = ShellOutputType.SUCCESS,
                    color = "#00F5D4"
                )
            )
        }

        // Search for contact
        val contacts = messagingService.searchContacts(contactQuery)

        return when {
            contacts.isEmpty() -> {
                listOf(
                    ShellOutput(
                        text = "❌ No contact found for \"$contactQuery\"",
                        type = ShellOutputType.ERROR
                    )
                )
            }
            contacts.size == 1 -> {
                val contact = contacts.first()
                val phoneNumber = contact.phoneNumbers.firstOrNull() ?: return listOf(
                    ShellOutput(
                        text = "❌ Contact has no phone number",
                        type = ShellOutputType.ERROR
                    )
                )

                aliasManager.setAlias(aliasName, contact, phoneNumber)

                listOf(
                    ShellOutput(
                        text = "✓ Alias \"$aliasName\" set to ${contact.name} ($phoneNumber)",
                        type = ShellOutputType.SUCCESS,
                        color = "#00F5D4"
                    )
                )
            }
            else -> {
                // Multiple contacts - need to select
                val outputs = mutableListOf(
                    ShellOutput(
                        text = "Found ${contacts.size} contacts matching \"$contactQuery\":",
                        type = ShellOutputType.INFO
                    )
                )

                contacts.take(10).forEachIndexed { index, contact ->
                    outputs.add(ShellOutput(
                        text = "  ${index + 1}. ${contact.name} (${contact.phoneNumbers.firstOrNull()})",
                        type = ShellOutputType.INFO
                    ))
                }

                pendingAliasSetup = aliasName
                _conversationState.value = ConversationState.WaitingForContactListSelection(contacts.take(10))

                outputs.add(ShellOutput(
                    text = "\nEnter number to select (1-${minOf(contacts.size, 10)}):",
                    type = ShellOutputType.PROMPT
                ))

                outputs
            }
        }
    }

    /**
     * Send the pending message
     */
    private suspend fun sendMessage(): List<ShellOutput> {
        val recipient = pendingRecipient ?: return listOf(
            ShellOutput(
                text = "❌ No recipient specified",
                type = ShellOutputType.ERROR
            )
        )

        val message = pendingMessageContent ?: return listOf(
            ShellOutput(
                text = "❌ No message content",
                type = ShellOutputType.ERROR
            )
        )

        _conversationState.value = ConversationState.Sending

        val recipientDisplay = pendingRecipientName ?: recipient
        val result = messagingService.sendMessage(recipient, message)

        reset()

        return when (result) {
            is SendResult.Success -> {
                listOf(
                    ShellOutput(
                        text = "✅ Sent to $recipientDisplay",
                        type = ShellOutputType.SUCCESS,
                        color = "#00F5D4"
                    )
                )
            }
            is SendResult.InvalidNumber -> {
                listOf(
                    ShellOutput(
                        text = "❌ Invalid number",
                        type = ShellOutputType.ERROR
                    )
                )
            }
            is SendResult.EmptyMessage -> {
                listOf(
                    ShellOutput(
                        text = "❌ Empty message",
                        type = ShellOutputType.ERROR
                    )
                )
            }
            is SendResult.Failed -> {
                listOf(
                    ShellOutput(
                        text = "❌ Failed: ${result.error}",
                        type = ShellOutputType.ERROR
                    )
                )
            }
        }
    }

    /**
     * Handle contact selected from picker
     */
    suspend fun onContactSelected(contact: Contact, phoneNumber: String) {
        _showContactPicker.value = null

        val alias = pendingAliasSetup
        if (alias != null) {
            // Setting up alias
            aliasManager.setAlias(alias, contact, phoneNumber)
            pendingAliasSetup = null
            _conversationState.value = ConversationState.None
        } else {
            // Selecting recipient for message
            pendingRecipient = phoneNumber
            pendingRecipientName = contact.name

            if (pendingMessageContent != null) {
                _conversationState.value = ConversationState.WaitingForConfirmation
            } else {
                _conversationState.value = ConversationState.WaitingForMessage
            }
        }
    }

    /**
     * Handle contact picker cancelled
     */
    fun onContactPickerCancelled() {
        _showContactPicker.value = null
        if (pendingAliasSetup != null) {
            pendingAliasSetup = null
        }
        _conversationState.value = ConversationState.None
    }

    /**
     * Reset the handler state
     */
    fun reset() {
        _conversationState.value = ConversationState.None
        pendingAliasSetup = null
        pendingRecipient = null
        pendingRecipientName = null
        pendingMessageContent = null
        _showContactPicker.value = null
    }

    /**
     * Check if we're in an active conversation
     */
    fun isInConversation(): Boolean {
        return _conversationState.value != ConversationState.None
    }
}

/**
 * Conversation state machine
 */
sealed class ConversationState {
    object None : ConversationState()
    object WaitingForRecipientChoice : ConversationState()
    object WaitingForPhoneNumber : ConversationState()
    object WaitingForMessage : ConversationState()
    object WaitingForConfirmation : ConversationState()
    object WaitingForAliasSelection : ConversationState()
    object WaitingForAliasPhoneNumber : ConversationState()
    object WaitingForContactSelection : ConversationState()
    data class WaitingForContactListSelection(val contacts: List<Contact>) : ConversationState()
    object Sending : ConversationState()
}

/**
 * Contact picker request
 */
data class ContactPickerRequest(
    val title: String,
    val forAlias: String? = null,
    val onSelected: (Contact) -> Unit
)

