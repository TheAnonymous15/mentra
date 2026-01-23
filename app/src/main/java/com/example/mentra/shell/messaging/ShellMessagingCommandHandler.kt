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

    // Current reading context (for quick replies)
    private var currentReadingAddress: String? = null
    private var currentReadingName: String? = null

    // Contact selection callback
    private val _showContactPicker = MutableStateFlow<ContactPickerRequest?>(null)
    val showContactPicker: StateFlow<ContactPickerRequest?> = _showContactPicker.asStateFlow()

    /**
     * Check if the input matches messaging commands
     */
    fun isMessagingCommand(input: String): Boolean {
        val lowercaseInput = input.lowercase().trim()

        // Exclude UI navigation commands - these should be handled by the shell executor
        if (lowercaseInput == "sms --ui" || lowercaseInput == "messages --ui" ||
            lowercaseInput == "open messages" || lowercaseInput == "open sms") {
            return false
        }

        val messagingKeywords = listOf(
            "send", "text", "sms", "message", "msg", "compose",
            "alias", "set alias", "setup",
            // Read commands
            "inbox", "unread", "read", "messages", "chat"
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

        // Check for inbox/read commands FIRST (before alias check)
        // Support: inbox, inbox mpesa, inbox mom, messages, messages mpesa
        if (lowercaseInput == "inbox" || lowercaseInput == "messages" || lowercaseInput == "sms") {
            return handleInboxCommand()
        }

        // Handle "inbox [name/number] [count]" - directly open that contact's inbox
        // Support: inbox mpesa, inbox mpesa 3, messages wife 5
        if (lowercaseInput.startsWith("inbox ") || lowercaseInput.startsWith("messages ")) {
            val parts = lowercaseInput
                .removePrefix("inbox ")
                .removePrefix("messages ")
                .trim()
                .split(Regex("\\s+"))

            if (parts.isNotEmpty()) {
                // Check if last part is a number (message count)
                val lastPart = parts.lastOrNull()
                val messageCount = lastPart?.toIntOrNull()

                val target = if (messageCount != null && parts.size > 1) {
                    // "inbox mpesa 3" -> target="mpesa", count=3
                    parts.dropLast(1).joinToString(" ")
                } else {
                    // "inbox mpesa" -> target="mpesa", count=default
                    parts.joinToString(" ")
                }

                if (target.isNotEmpty()) {
                    return handleReadMessagesCommand(target, messageCount ?: 15)
                }
            }
        }

        if (lowercaseInput == "unread" || lowercaseInput.startsWith("unread messages") ||
            lowercaseInput.startsWith("new messages")) {
            return handleUnreadCommand()
        }

        // "read [contact/number]" or "read messages from [contact]" or "chat [contact]"
        if (lowercaseInput.startsWith("read ") || lowercaseInput.startsWith("chat ")) {
            val target = lowercaseInput
                .removePrefix("read messages from ")
                .removePrefix("read from ")
                .removePrefix("read ")
                .removePrefix("chat with ")
                .removePrefix("chat ")
                .trim()
            if (target.isNotEmpty()) {
                return handleReadMessagesCommand(target)
            }
        }

        // "reply [message]" when in reading mode
        if (lowercaseInput.startsWith("reply ") && currentReadingAddress != null) {
            val message = input.removePrefix("reply ").removePrefix("Reply ").trim()
            if (message.isNotEmpty()) {
                return handleQuickReply(message)
            }
        }

        // Check for alias setup commands first
        if (lowercaseInput.startsWith("alias ") || lowercaseInput.startsWith("set alias") ||
            lowercaseInput.startsWith("set ") && lowercaseInput.contains(" as ")) {
            return handleAliasSetupCommand(input)
        }

        // Check if we're in a conversation flow
        when (val state = _conversationState.value) {
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
            is ConversationState.WaitingForThreadSelection -> {
                return handleThreadSelectionInput(input, state.threads, state.messageCount)
            }
            is ConversationState.WaitingForContactListSelectionForRead -> {
                return handleContactSelectionForReadInput(input, state.contacts, state.messageCount)
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
     * Handle thread selection input when multiple threads match keyword
     */
    private suspend fun handleThreadSelectionInput(
        input: String,
        threads: List<InboxConversation>,
        messageCount: Int
    ): List<ShellOutput> {
        val trimmed = input.trim().lowercase()

        // Check for cancel
        if (trimmed == "cancel" || trimmed == "c" || trimmed == "0") {
            _conversationState.value = ConversationState.None
            return listOf(ShellOutput(
                text = "Selection cancelled.",
                type = ShellOutputType.INFO,
                color = "#888888"
            ))
        }

        val selection = trimmed.toIntOrNull()
        if (selection == null || selection < 1 || selection > threads.size) {
            return listOf(ShellOutput(
                text = "❌ Invalid selection. Enter 1-${threads.size} or 'cancel':",
                type = ShellOutputType.ERROR
            ))
        }

        val thread = threads[selection - 1]
        _conversationState.value = ConversationState.None

        return readAndDisplayMessages(
            thread.address,
            thread.contactName ?: thread.address,
            messageCount
        )
    }

    /**
     * Handle contact selection input for reading messages
     */
    private suspend fun handleContactSelectionForReadInput(
        input: String,
        contacts: List<Contact>,
        messageCount: Int
    ): List<ShellOutput> {
        val trimmed = input.trim().lowercase()

        // Check for cancel
        if (trimmed == "cancel" || trimmed == "c" || trimmed == "0") {
            _conversationState.value = ConversationState.None
            return listOf(ShellOutput(
                text = "Selection cancelled.",
                type = ShellOutputType.INFO,
                color = "#888888"
            ))
        }

        val selection = trimmed.toIntOrNull()
        if (selection == null || selection < 1 || selection > contacts.size) {
            return listOf(ShellOutput(
                text = "❌ Invalid selection. Enter 1-${contacts.size} or 'cancel':",
                type = ShellOutputType.ERROR
            ))
        }

        val contact = contacts[selection - 1]
        _conversationState.value = ConversationState.None

        return readAndDisplayMessages(
            contact.phoneNumbers.firstOrNull() ?: contact.name,
            contact.name,
            messageCount
        )
    }

    /**
     * Handle alias setup command
     * Supports various formats:
     * - alias dk = John Doe
     * - alias dk=John
     * - alias dk John Doe
     * - alias dk 0773123456
     * - set dk as John
     */
    private suspend fun handleAliasSetupCommand(input: String): List<ShellOutput> {
        val trimmedInput = input.trim()

        // Try multiple patterns for flexibility
        // Pattern 1: alias name = value or alias name=value
        val pattern1 = Regex("(?:alias|set)\\s+(\\w+)\\s*=\\s*(.+)", RegexOption.IGNORE_CASE)
        // Pattern 2: set name as value
        val pattern2 = Regex("set\\s+(\\w+)\\s+as\\s+(.+)", RegexOption.IGNORE_CASE)
        // Pattern 3: alias name value (space separated, no = or as)
        val pattern3 = Regex("alias\\s+(\\w+)\\s+(.+)", RegexOption.IGNORE_CASE)

        val match = pattern1.find(trimmedInput)
            ?: pattern2.find(trimmedInput)
            ?: pattern3.find(trimmedInput)

        if (match == null) {
            return listOf(
                ShellOutput(
                    text = "Usage: alias [name] [contact/number]",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Examples:",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "  alias wife Jane Doe",
                    type = ShellOutputType.INFO,
                    color = "#00F5D4"
                ),
                ShellOutput(
                    text = "  alias dk 0773123456",
                    type = ShellOutputType.INFO,
                    color = "#00F5D4"
                ),
                ShellOutput(
                    text = "  alias boss = John Smith",
                    type = ShellOutputType.INFO,
                    color = "#00F5D4"
                )
            )
        }

        val aliasName = match.groupValues[1].lowercase().trim()
        val contactQuery = match.groupValues[2].trim()

        // Don't allow empty alias name or value
        if (aliasName.isBlank() || contactQuery.isBlank()) {
            return listOf(
                ShellOutput(
                    text = "❌ Both alias name and value are required",
                    type = ShellOutputType.ERROR
                )
            )
        }

        // Check if it's a phone number (starts with + or digit, contains mostly digits)
        val cleanedNumber = contactQuery.replace(Regex("[\\s\\-()]"), "")
        val isPhoneNumber = cleanedNumber.matches(Regex("^\\+?[0-9]{7,15}$")) ||
                           messagingService.isValidPhoneNumber(contactQuery)

        if (isPhoneNumber) {
            // Direct phone number aliasing
            val normalizedNumber = if (cleanedNumber.startsWith("+")) cleanedNumber else cleanedNumber
            aliasManager.setAlias(
                aliasName,
                Contact(id = "direct_$aliasName", name = aliasName.replaceFirstChar { it.uppercase() }, phoneNumbers = listOf(normalizedNumber), photoUri = null),
                normalizedNumber
            )
            return listOf(
                ShellOutput(
                    text = "✓ Alias \"$aliasName\" → $normalizedNumber",
                    type = ShellOutputType.SUCCESS,
                    color = "#00F5D4"
                )
            )
        }

        // Search for contact by name
        val contacts = messagingService.searchContacts(contactQuery)

        return when {
            contacts.isEmpty() -> {
                // No contact found - maybe user wants to alias a custom name to a number they'll provide
                listOf(
                    ShellOutput(
                        text = "❌ No contact found for \"$contactQuery\"",
                        type = ShellOutputType.ERROR
                    ),
                    ShellOutput(
                        text = "💡 To alias a number directly: alias $aliasName 0712345678",
                        type = ShellOutputType.INFO
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
                        text = "✓ Alias \"$aliasName\" → ${contact.name} ($phoneNumber)",
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
        // Don't clear reading context - allows quick reply even after other commands
    }

    /**
     * Clear the reading context (after explicitly exiting read mode)
     */
    fun clearReadingContext() {
        currentReadingAddress = null
        currentReadingName = null
    }

    /**
     * Handle inbox command - show recent conversations
     */
    private suspend fun handleInboxCommand(): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        outputs.add(ShellOutput(
            text = "📬 INBOX",
            type = ShellOutputType.HEADER,
            color = "#00F5D4"
        ))

        val conversations = messagingService.getInbox(10)
        val unreadTotal = messagingService.getUnreadCount()

        if (unreadTotal > 0) {
            outputs.add(ShellOutput(
                text = "📩 $unreadTotal unread message${if (unreadTotal > 1) "s" else ""}",
                type = ShellOutputType.INFO,
                color = "#FFE66D"
            ))
        }

        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))

        if (conversations.isEmpty()) {
            outputs.add(ShellOutput(
                text = "No messages",
                type = ShellOutputType.INFO
            ))
        } else {
            conversations.forEachIndexed { index, conv ->
                val nameOrNumber = conv.contactName ?: conv.address
                val unreadBadge = if (conv.unreadCount > 0) " (${conv.unreadCount} new)" else ""
                val directionIcon = if (conv.isOutgoing) "➜" else "←"
                val timeAgo = formatTimeAgo(conv.lastMessageTime)
                val preview = conv.lastMessage.take(35).let { if (conv.lastMessage.length > 35) "$it..." else it }

                // Conversation entry
                outputs.add(ShellOutput(
                    text = "${index + 1}. $nameOrNumber$unreadBadge",
                    type = if (conv.unreadCount > 0) ShellOutputType.WARNING else ShellOutputType.INFO,
                    color = if (conv.unreadCount > 0) "#00F5D4" else "#888888"
                ))
                outputs.add(ShellOutput(
                    text = "   $directionIcon $preview  [$timeAgo]",
                    type = ShellOutputType.INFO,
                    color = "#666666"
                ))
            }
        }

        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))
        outputs.add(ShellOutput(
            text = "Commands: read [name/number] | reply [message]",
            type = ShellOutputType.PROMPT,
            color = "#7B61FF"
        ))

        return outputs
    }

    /**
     * Handle unread command - show unread count
     */
    private suspend fun handleUnreadCommand(): List<ShellOutput> {
        val unreadCount = messagingService.getUnreadCount()

        return if (unreadCount == 0) {
            listOf(ShellOutput(
                text = "✓ No unread messages",
                type = ShellOutputType.SUCCESS,
                color = "#00F5D4"
            ))
        } else {
            val conversations = messagingService.getInbox(20).filter { it.unreadCount > 0 }
            val outputs = mutableListOf<ShellOutput>()

            outputs.add(ShellOutput(
                text = "📩 $unreadCount unread message${if (unreadCount > 1) "s" else ""}",
                type = ShellOutputType.WARNING,
                color = "#FFE66D"
            ))

            conversations.take(5).forEach { conv ->
                val nameOrNumber = conv.contactName ?: conv.address
                outputs.add(ShellOutput(
                    text = "  • $nameOrNumber (${conv.unreadCount})",
                    type = ShellOutputType.INFO,
                    color = "#00F5D4"
                ))
            }

            if (conversations.size > 5) {
                outputs.add(ShellOutput(
                    text = "  ... and ${conversations.size - 5} more",
                    type = ShellOutputType.INFO,
                    color = "#888888"
                ))
            }

            outputs.add(ShellOutput(
                text = "Type: read [name] to view",
                type = ShellOutputType.PROMPT,
                color = "#7B61FF"
            ))

            outputs
        }
    }

    /**
     * Handle read messages command - read conversation from contact
     * Supports: read [contact], inbox [contact] [count]
     */
    private suspend fun handleReadMessagesCommand(target: String, messageCount: Int = 15): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        // First check if target is a direct alias
        val aliasContact = aliasManager.getContactByAlias(target.lowercase())
        if (aliasContact != null) {
            // Direct alias match - open that conversation
            return readAndDisplayMessages(
                aliasContact.phoneNumbers.firstOrNull() ?: target,
                aliasContact.name,
                messageCount
            )
        }

        // Check if target looks like a phone number
        val cleanedTarget = target.replace(Regex("[\\s-]"), "")
        if (cleanedTarget.matches(Regex("^\\+?[0-9]{7,15}$"))) {
            // Direct phone number - open that conversation
            val contactName = getContactNameForNumber(cleanedTarget)
            return readAndDisplayMessages(cleanedTarget, contactName ?: cleanedTarget, messageCount)
        }

        // Search for threads matching the keyword
        val matchingThreads = messagingService.searchThreadsByKeyword(target)

        return when {
            matchingThreads.isEmpty() -> {
                // No matches found - search contacts
                val contacts = messagingService.searchContacts(target)
                if (contacts.isEmpty()) {
                    listOf(
                        ShellOutput(
                            text = "❌ No conversations found for \"$target\"",
                            type = ShellOutputType.ERROR
                        ),
                        ShellOutput(
                            text = "Try: inbox [exact name] or inbox [phone number]",
                            type = ShellOutputType.INFO,
                            color = "#888888"
                        )
                    )
                } else if (contacts.size == 1) {
                    // Single contact match
                    val contact = contacts.first()
                    readAndDisplayMessages(
                        contact.phoneNumbers.firstOrNull() ?: target,
                        contact.name,
                        messageCount
                    )
                } else {
                    // Multiple contacts - let user select
                    showContactSelectionPrompt(contacts.take(10), target, messageCount)
                }
            }
            matchingThreads.size == 1 -> {
                // Single match - open directly
                val thread = matchingThreads.first()
                readAndDisplayMessages(
                    thread.address,
                    thread.contactName ?: thread.address,
                    messageCount
                )
            }
            else -> {
                // Multiple matches - show selection
                showThreadSelectionPrompt(matchingThreads, target, messageCount)
            }
        }
    }

    /**
     * Show thread selection prompt when multiple threads match keyword
     */
    private fun showThreadSelectionPrompt(
        threads: List<InboxConversation>,
        keyword: String,
        messageCount: Int
    ): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        outputs.add(ShellOutput(
            text = "📋 Multiple threads match \"$keyword\":",
            type = ShellOutputType.WARNING,
            color = "#FFE66D"
        ))
        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))

        threads.forEachIndexed { index, thread ->
            val displayName = thread.contactName ?: thread.address
            val preview = thread.lastMessage.take(30).let { if (thread.lastMessage.length > 30) "$it..." else it }
            val timeAgo = formatTimeAgo(thread.lastMessageTime)

            outputs.add(ShellOutput(
                text = "${index + 1}. $displayName",
                type = ShellOutputType.INFO,
                color = if (index % 2 == 0) "#00F5D4" else "#7B61FF"
            ))
            outputs.add(ShellOutput(
                text = "   $preview [$timeAgo]",
                type = ShellOutputType.INFO,
                color = "#666666"
            ))
        }

        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))
        outputs.add(ShellOutput(
            text = "Enter number to select (1-${threads.size}):",
            type = ShellOutputType.PROMPT,
            color = "#FFE66D"
        ))

        // Store threads for selection
        _conversationState.value = ConversationState.WaitingForThreadSelection(threads, messageCount)

        return outputs
    }

    /**
     * Show contact selection prompt
     */
    private fun showContactSelectionPrompt(
        contacts: List<Contact>,
        keyword: String,
        messageCount: Int
    ): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        outputs.add(ShellOutput(
            text = "📋 Multiple contacts match \"$keyword\":",
            type = ShellOutputType.WARNING,
            color = "#FFE66D"
        ))

        contacts.forEachIndexed { index, contact ->
            outputs.add(ShellOutput(
                text = "${index + 1}. ${contact.name} (${contact.phoneNumbers.firstOrNull()})",
                type = ShellOutputType.INFO,
                color = if (index % 2 == 0) "#00F5D4" else "#7B61FF"
            ))
        }

        outputs.add(ShellOutput(
            text = "Enter number to select (1-${contacts.size}):",
            type = ShellOutputType.PROMPT,
            color = "#FFE66D"
        ))

        _conversationState.value = ConversationState.WaitingForContactListSelectionForRead(contacts, messageCount)

        return outputs
    }

    /**
     * Read and display messages with alternating colors for readability
     */
    private suspend fun readAndDisplayMessages(
        address: String,
        displayName: String,
        messageCount: Int
    ): List<ShellOutput> {
        val outputs = mutableListOf<ShellOutput>()

        // Set reading context for quick replies
        currentReadingAddress = address
        currentReadingName = displayName

        outputs.add(ShellOutput(
            text = "💬 Chat with $displayName",
            type = ShellOutputType.HEADER,
            color = "#00F5D4"
        ))
        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))

        val messages = messagingService.readMessagesFrom(address, messageCount)

        if (messages.isEmpty()) {
            outputs.add(ShellOutput(
                text = "No messages found",
                type = ShellOutputType.INFO
            ))
        } else {
            messages.forEachIndexed { index, msg ->
                val direction = if (msg.isOutgoing) "You" else displayName.take(12)
                val timeStr = formatTime(msg.timestamp)
                val isOdd = index % 2 == 0 // 0-indexed, so 0,2,4 are "odd" messages (1st, 3rd, 5th)

                // Alternating colors for better readability
                // Odd messages (1st, 3rd, 5th): White/light on dark
                // Even messages (2nd, 4th, 6th): Cyan/purple tones
                val headerColor = if (msg.isOutgoing) {
                    if (isOdd) "#B8B8FF" else "#7B61FF"
                } else {
                    if (isOdd) "#FFFFFF" else "#00F5D4"
                }

                val bodyColor = if (msg.isOutgoing) {
                    if (isOdd) "#D0D0FF" else "#9B8BFF"
                } else {
                    if (isOdd) "#E0E0E0" else "#66FFE8"
                }

                // Header with time and sender
                outputs.add(ShellOutput(
                    text = "[$timeStr] $direction:",
                    type = ShellOutputType.INFO,
                    color = headerColor
                ))

                // Full message body (no truncation)
                outputs.add(ShellOutput(
                    text = "  ${msg.body}",
                    type = ShellOutputType.INFO,
                    color = bodyColor
                ))

                // Add small spacing between messages for readability
                if (index < messages.size - 1) {
                    outputs.add(ShellOutput(
                        text = "",
                        type = ShellOutputType.INFO
                    ))
                }
            }
        }

        outputs.add(ShellOutput(
            text = "─".repeat(40),
            type = ShellOutputType.INFO,
            color = "#555555"
        ))
        outputs.add(ShellOutput(
            text = "💡 Quick reply: reply [your message]",
            type = ShellOutputType.PROMPT,
            color = "#FFE66D"
        ))

        return outputs
    }

    /**
     * Handle quick reply - send message to current reading contact
     */
    private suspend fun handleQuickReply(message: String): List<ShellOutput> {
        val address = currentReadingAddress ?: return listOf(
            ShellOutput(
                text = "❌ No conversation open. Use: read [contact] first",
                type = ShellOutputType.ERROR
            )
        )

        val displayName = currentReadingName ?: address

        val result = messagingService.sendMessage(address, message)

        return when (result) {
            is SendResult.Success -> {
                listOf(
                    ShellOutput(
                        text = "✅ Sent to $displayName",
                        type = ShellOutputType.SUCCESS,
                        color = "#00F5D4"
                    ),
                    ShellOutput(
                        text = "➜ \"$message\"",
                        type = ShellOutputType.INFO,
                        color = "#7B61FF"
                    )
                )
            }
            is SendResult.Failed -> {
                listOf(ShellOutput(
                    text = "❌ Failed: ${result.error}",
                    type = ShellOutputType.ERROR
                ))
            }
            else -> {
                listOf(ShellOutput(
                    text = "❌ Send failed",
                    type = ShellOutputType.ERROR
                ))
            }
        }
    }

    /**
     * Get contact name for a phone number
     */
    private suspend fun getContactNameForNumber(number: String): String? {
        val contacts = messagingService.searchContacts(number)
        return contacts.firstOrNull()?.name
    }

    /**
     * Format timestamp to relative time (e.g., "2m ago", "1h ago", "Yesterday")
     */
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 2 -> "Yesterday"
            days < 7 -> "${days}d"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }

    /**
     * Format timestamp to time string (e.g., "14:30" or "Yesterday 14:30")
     */
    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = diff / (1000 * 60 * 60 * 24)

        val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeStr = timeSdf.format(java.util.Date(timestamp))

        return when {
            days < 1 -> timeStr
            days < 2 -> "Yesterday $timeStr"
            days < 7 -> {
                val daySdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                "${daySdf.format(java.util.Date(timestamp))} $timeStr"
            }
            else -> {
                val dateSdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                "${dateSdf.format(java.util.Date(timestamp))} $timeStr"
            }
        }
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
    data class WaitingForThreadSelection(val threads: List<InboxConversation>, val messageCount: Int) : ConversationState()
    data class WaitingForContactListSelectionForRead(val contacts: List<Contact>, val messageCount: Int) : ConversationState()
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

