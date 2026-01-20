package com.example.mentra.shell.calling

import android.content.Context
import android.provider.ContactsContract
import com.example.mentra.dialer.DialerManager
import com.example.mentra.dialer.ussd.UssdService
import com.example.mentra.shell.messaging.ContactAliasManager
import com.example.mentra.shell.models.ShellOutput
import com.example.mentra.shell.models.ShellOutputType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL CALLING COMMAND HANDLER
 * Handles voice call commands via the shell
 * Uses shared ContactAliasManager for alias lookup (same as SMS)
 * ═══════════════════════════════════════════════════════════════════
 */

@Singleton
class ShellCallingCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dialerManager: DialerManager,
    private val ussdService: UssdService,
    private val aliasManager: ContactAliasManager
) {
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    // Active call session for terminal display
    private val _activeCallSession = MutableStateFlow<ActiveCallSession?>(null)
    val activeCallSession: StateFlow<ActiveCallSession?> = _activeCallSession

    // Call control states
    private var isMuted = false
    private var isSpeaker = false

    /**
     * Check if there's an active call session (for terminal listener)
     */
    fun isInActiveCall(): Boolean = _activeCallSession.value != null

    /**
     * Handle call control input during active call
     * Controls: X=End, S=Speaker, M=Mute, H=Hold, ?=Help
     * Digits 0-9 are sent as DTMF tones for IVR/extension input
     */
    suspend fun handleCallControlInput(input: String): List<ShellOutput> {
        val session = _activeCallSession.value ?: return listOf(
            ShellOutput("No active call session", ShellOutputType.ERROR)
        )

        // Check if this is actually a new calling command - if so, end the current session and handle as new command
        if (isCallingCommand(input)) {
            // End current session and process as new command
            endActiveCall()
            return handleCommand(input)
        }

        val trimmedInput = input.trim().lowercase()

        // First check for single digit DTMF (0-9, *, #)
        if (input.trim().length == 1 && input.trim()[0] in "0123456789*#") {
            val tone = input.trim()[0]
            sendDtmf(tone)
            return buildActiveCallDisplay(session, "📞 Sent: $tone")
        }

        return when (trimmedInput) {
            "x", "end", "hangup", "cut" -> {
                endActiveCall()
                listOf(
                    ShellOutput("", ShellOutputType.INFO),
                    ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.ERROR),
                    ShellOutput("📵 CALL ENDED", ShellOutputType.ERROR),
                    ShellOutput("  Duration: ${formatDuration(session.getDuration())}", ShellOutputType.INFO),
                    ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.ERROR),
                    ShellOutput("", ShellOutputType.INFO)
                )
            }
            "s", "speaker" -> {
                isSpeaker = !isSpeaker
                toggleSpeaker(isSpeaker)
                buildActiveCallDisplay(session, "🔊 Speaker ${if (isSpeaker) "ON" else "OFF"}")
            }
            "m", "mute" -> {
                isMuted = !isMuted
                toggleMute(isMuted)
                buildActiveCallDisplay(session, "🎤 Mic ${if (isMuted) "MUTED" else "ON"}")
            }
            "h", "hold" -> {
                // Toggle hold (if supported)
                buildActiveCallDisplay(session, "⏸️ Hold toggled")
            }
            "?", "help" -> {
                buildActiveCallDisplay(session, "X=End S=Speaker M=Mute 0-9=DTMF")
            }
            else -> {
                // Check if it's multiple digits (for quick DTMF sequence)
                if (input.trim().all { it in "0123456789*#" }) {
                    input.trim().forEach { tone -> sendDtmf(tone) }
                    buildActiveCallDisplay(session, "📞 Sent: ${input.trim()}")
                } else {
                    buildActiveCallDisplay(session, "Unknown. Press ? for help")
                }
            }
        }
    }

    /**
     * Build the ANSI-style active call display
     */
    private fun buildActiveCallDisplay(session: ActiveCallSession, statusMessage: String = ""): List<ShellOutput> {
        val duration = formatDuration(session.getDuration())
        val name = session.contactName ?: session.phoneNumber
        val speakerStatus = if (isSpeaker) "ON" else "OFF"
        val muteStatus = if (isMuted) "MUTED" else "ON"

        return buildList {
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.SUCCESS))
            add(ShellOutput("📞 ACTIVE CALL", ShellOutputType.SUCCESS))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  Contact: $name", ShellOutputType.INFO))
            if (session.contactName != null) {
                add(ShellOutput("  Number:  ${session.phoneNumber}", ShellOutputType.INFO))
            }
            add(ShellOutput("  SIM:     SIM ${session.simSlot + 1}", ShellOutputType.INFO))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  ⏱️ $duration", ShellOutputType.WARNING))
            add(ShellOutput("  🔊 Speaker: $speakerStatus   🎤 Mic: $muteStatus", ShellOutputType.INFO))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  [X] End  [S] Speaker  [M] Mute  [H] Hold", ShellOutputType.PROMPT))
            add(ShellOutput("  [0-9] DTMF for IVR/Extensions  [?] Help", ShellOutputType.PROMPT))
            add(ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.SUCCESS))
            if (statusMessage.isNotEmpty()) {
                add(ShellOutput("→ $statusMessage", ShellOutputType.WARNING))
            }
        }
    }

    /**
     * Start an active call session and return initial display
     */
    fun startActiveCallSession(phoneNumber: String, contactName: String?, simSlot: Int): List<ShellOutput> {
        val session = ActiveCallSession(
            phoneNumber = phoneNumber,
            contactName = contactName,
            simSlot = simSlot,
            startTime = System.currentTimeMillis()
        )
        _activeCallSession.value = session
        isMuted = false
        isSpeaker = false

        return buildList {
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.SUCCESS))
            add(ShellOutput("📞 INITIATING CALL...", ShellOutputType.SUCCESS))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  To:  ${contactName ?: phoneNumber}", ShellOutputType.INFO))
            add(ShellOutput("  Via: SIM ${simSlot + 1}", ShellOutputType.INFO))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  🔔 Ringing...", ShellOutputType.WARNING))
            add(ShellOutput("", ShellOutputType.INFO))
            add(ShellOutput("  [X] End  [S] Speaker  [M] Mute  [0-9] DTMF", ShellOutputType.PROMPT))
            add(ShellOutput("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", ShellOutputType.SUCCESS))
            add(ShellOutput("Type X to end, or 0-9 for IVR:", ShellOutputType.PROMPT))
        }
    }

    /**
     * End the active call session
     */
    fun endActiveCall() {
        dialerManager.endCall()
        _activeCallSession.value = null
        _callState.value = CallState.Idle
        isMuted = false
        isSpeaker = false
    }

    private fun toggleSpeaker(enabled: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.isSpeakerphoneOn = enabled
        } catch (e: Exception) {
            android.util.Log.e("CallHandler", "Failed to toggle speaker", e)
        }
    }

    private fun toggleMute(muted: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.isMicrophoneMute = muted
        } catch (e: Exception) {
            android.util.Log.e("CallHandler", "Failed to toggle mute", e)
        }
    }

    private fun sendDtmf(tone: Char): Boolean {
        // Send DTMF tone through the dialer manager
        // DialerManager will handle sending to active call or playing through voice stream
        return dialerManager.sendDtmf(tone)
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private fun padEnd(str: String, length: Int): String {
        return if (str.length >= length) str.take(length) else str + " ".repeat(length - str.length)
    }

    private fun padCenter(str: String, length: Int): String {
        if (str.length >= length) return str.take(length)
        val padding = length - str.length
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        return " ".repeat(leftPad) + str + " ".repeat(rightPad)
    }

    // Common USSD shortcuts
    private val ussdShortcuts = mapOf(
        "check balance" to "*144#",
        "balance" to "*144#",
        "my balance" to "*144#",
        "check data" to "*544#",
        "data balance" to "*544*44#",
        "my number" to "*135#",
        "airtime balance" to "*144#",
        "check minutes" to "*122#"
        ,"dial bank" to "*247#"
        ,"bank ussd" to "*247#"
        ,"equity" to "*247#"
        ,"kcb" to "*522#"
        ,"coop" to "*667#"
        ,"family bank" to "*642#"
        ,"stanchart" to "*722#"
        ,"ollin" to "*645#"

    )

    /**
     * Check if command is a calling command
     */
    fun isCallingCommand(input: String): Boolean {
        val lowerInput = input.lowercase().trim()
        return lowerInput.startsWith("call ") ||
               lowerInput.startsWith("dial ") ||
               lowerInput.startsWith("phone ") ||
               lowerInput == "call" ||
               lowerInput == "make a call" ||
               lowerInput == "make call" ||
               lowerInput == "place a call" ||
               lowerInput == "place call" ||
               lowerInput.startsWith("check balance") ||
               lowerInput.startsWith("balance") ||
               lowerInput.startsWith("check data") ||
               lowerInput.startsWith("data balance") ||
               lowerInput.startsWith("my number") ||
               lowerInput.startsWith("airtime balance") ||
               lowerInput.startsWith("dial bank") ||
               lowerInput.startsWith("check minutes") ||
               lowerInput.startsWith("bank ussd") ||
               lowerInput.startsWith("equity") ||
               lowerInput.startsWith("kcb") ||
               lowerInput.startsWith("coop") ||
               lowerInput.startsWith("family bank") ||
               lowerInput.startsWith("ollin") ||
               lowerInput.startsWith("stanchart")
    }

    /**
     * Handle calling commands
     */
    suspend fun handleCommand(input: String): List<ShellOutput> {
        val lowerInput = input.lowercase().trim()

        return when {
            // USSD shortcuts
            lowerInput.startsWith("check balance") ||
            lowerInput.startsWith("balance") ||
            lowerInput.startsWith("check data") ||
            lowerInput.startsWith("data balance") ||
            lowerInput.startsWith("my number") ||
            lowerInput.startsWith("airtime balance") ||
                    lowerInput.startsWith(("dial bank")) ||
            lowerInput.startsWith("check minutes") ||
            lowerInput.startsWith("bank ussd") ||
            lowerInput.startsWith("equity") ||
            lowerInput.startsWith("kcb") ||
            lowerInput.startsWith("coop") ||
            lowerInput.startsWith("family bank") ||
                    lowerInput.startsWith("ollin") ||
            lowerInput.startsWith("stanchart")

                -> handleUssdShortcut(lowerInput)

            // Direct call commands
            lowerInput.startsWith("call ") -> handleCallCommand(input.substring(5).trim())
            lowerInput.startsWith("dial ") -> handleCallCommand(input.substring(5).trim())
            lowerInput.startsWith("phone ") -> handleCallCommand(input.substring(6).trim())

            // Generic call/make call
            lowerInput == "call" ||
            lowerInput == "make a call" ||
                    lowerInput == "dial" ||
                    lowerInput == "make dial" ||
            lowerInput == "make call" ||
            lowerInput == "place a call" ||
            lowerInput == "place call" -> handleGenericCall()

            else -> listOf(
                ShellOutput(
                    text = "Unknown calling command. Try 'call <name/number>' or 'check balance'",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    /**
     * Handle USSD shortcut commands
     */
    private suspend fun handleUssdShortcut(command: String): List<ShellOutput> {
        // Extract SIM preference if specified
        val simSlot = when {
            command.contains("sim 1") || command.contains("sim1") -> 0
            command.contains("sim 2") || command.contains("sim2") -> 1
            else -> null
        }

        // Find matching USSD code
        val ussdCode = ussdShortcuts.entries.find {
            command.startsWith(it.key)
        }?.value

        return if (ussdCode != null) {
            if (simSlot != null) {
                // Execute USSD with specific SIM - ensure state is Idle (no call listener)
                _callState.value = CallState.Idle
                ussdService.executeUssd(ussdCode, simSlot)
                listOf(

                )
            } else {
                // Show SIM selection
                _callState.value = CallState.AwaitingSimSelection(
                    action = CallAction.Ussd(ussdCode)
                )
                listOf(
                    ShellOutput(
                        text = "Select SIM:",
                        type = ShellOutputType.PROMPT
                    ),
                    ShellOutput(
                        text = "1. SIM 1",
                        type = ShellOutputType.INFO
                    ),
                    ShellOutput(
                        text = "2. SIM 2",
                        type = ShellOutputType.INFO
                    )
                )
            }
        } else {
            listOf(
                ShellOutput(
                    text = "Undefined shotcut",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    /**
     * Handle direct call command with name/number
     * Checks aliases first (shared with SMS), then contacts
     * Supports direct SIM specification: "call my wife sim 2"
     */
    private suspend fun handleCallCommand(target: String): List<ShellOutput> {
        // Extract SIM preference if specified in the command
        val simFromCommand = when {
            target.lowercase().contains(" sim 1") || target.lowercase().contains(" sim1") -> 0
            target.lowercase().contains(" sim 2") || target.lowercase().contains(" sim2") -> 1
            else -> null
        }

        // Remove SIM specification from target
        val cleanTarget = target.lowercase()
            .replace(" sim 1", "")
            .replace(" sim1", "")
            .replace(" sim 2", "")
            .replace(" sim2", "")
            .trim()

        // Check if it's a phone number first
        val phoneNumber = extractPhoneNumber(cleanTarget)

        if (phoneNumber != null) {
            // Direct phone number
            if (simFromCommand != null) {
                // SIM already specified - make the call directly
                dialerManager.placeCall(phoneNumber, simFromCommand)
                _callState.value = CallState.InCall
                return startActiveCallSession(phoneNumber, null, simFromCommand)
            }

            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(phoneNumber, null)
            )
            return listOf(
                ShellOutput(
                    text = "📞 Calling: $phoneNumber",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        }

        // Check if target is an alias (e.g., "my wife", "wife", "mom")
        val aliasTarget = cleanTarget.lowercase()
            .removePrefix("my ")
            .removePrefix("to ")
            .trim()

        // Try to find alias first
        val aliasContact = aliasManager.getContactByAlias(aliasTarget)
        if (aliasContact != null && aliasContact.phoneNumbers.isNotEmpty()) {
            val number = aliasContact.phoneNumbers.first()

            if (simFromCommand != null) {
                // SIM already specified - make the call directly
                dialerManager.placeCall(number, simFromCommand)
                _callState.value = CallState.InCall
                return startActiveCallSession(number, aliasContact.name, simFromCommand)
            }

            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(number, aliasContact.name)
            )
            return listOf(
                ShellOutput(
                    text = "📞 Calling ${aliasContact.name} (alias: $aliasTarget)",
                    type = ShellOutputType.SUCCESS
                ),
                ShellOutput(
                    text = "Number: $number",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        }

        // Not a phone number or alias - search contacts by name
        val contacts = searchContactsByName(cleanTarget)

        return when {
            contacts.isEmpty() -> {
                // Check if this looks like an alias that's not set up
                val suggestedAlias = com.example.mentra.shell.messaging.ContactAliasManager.SUGGESTED_ALIASES
                    .find { it.alias.equals(aliasTarget, ignoreCase = true) }

                if (suggestedAlias != null) {
                    listOf(
                        ShellOutput(
                            text = "❌ Alias '$aliasTarget' is not set up yet",
                            type = ShellOutputType.WARNING
                        ),
                        ShellOutput(
                            text = "💡 Set it up with: alias $aliasTarget = <contact name>",
                            type = ShellOutputType.INFO
                        ),
                        ShellOutput(
                            text = "Or use 'call <phone number>' to dial directly",
                            type = ShellOutputType.INFO
                        )
                    )
                } else {
                    listOf(
                        ShellOutput(
                            text = "❌ No contact found for '$cleanTarget'",
                            type = ShellOutputType.ERROR
                        ),
                        ShellOutput(
                            text = "💡 Tip: Use 'call <phone number>' to dial directly",
                            type = ShellOutputType.INFO
                        ),
                        ShellOutput(
                            text = "Or set up an alias: alias $aliasTarget = <contact name>",
                            type = ShellOutputType.INFO
                        )
                    )
                }
            }
            contacts.size == 1 -> {
                val contact = contacts.first()

                if (simFromCommand != null) {
                    // SIM already specified - make the call directly
                    dialerManager.placeCall(contact.number, simFromCommand)
                    _callState.value = CallState.InCall
                    return startActiveCallSession(contact.number, contact.name, simFromCommand)
                }

                _callState.value = CallState.AwaitingSimSelection(
                    action = CallAction.Call(contact.number, contact.name)
                )
                listOf(
                    ShellOutput(
                        text = "📞 Calling ${contact.name}",
                        type = ShellOutputType.INFO
                    ),
                    ShellOutput(
                        text = "Number: ${contact.number}",
                        type = ShellOutputType.INFO
                    ),
                    ShellOutput(
                        text = "Select SIM:",
                        type = ShellOutputType.PROMPT
                    ),
                    ShellOutput(
                        text = "1. SIM 1",
                        type = ShellOutputType.INFO
                    ),
                    ShellOutput(
                        text = "2. SIM 2",
                        type = ShellOutputType.INFO
                    )
                )
            }
            else -> {
                // Multiple matches - show contact selection
                _callState.value = CallState.AwaitingContactSelection(contacts)
                buildList {
                    add(ShellOutput(
                        text = "Multiple contacts found for '$cleanTarget':",
                        type = ShellOutputType.INFO
                    ))
                    contacts.forEachIndexed { index, contact ->
                        add(ShellOutput(
                            text = "${index + 1}. ${contact.name} - ${contact.number}",
                            type = ShellOutputType.INFO
                        ))
                    }
                    add(ShellOutput(
                        text = "Enter number to select contact:",
                        type = ShellOutputType.PROMPT
                    ))
                }
            }
        }
    }

    /**
     * Handle generic call command - show options
     */
    private fun handleGenericCall(): List<ShellOutput> {
        _callState.value = CallState.AwaitingCallMethod
        return listOf(
            ShellOutput(
                text = "How would you like to make a call?",
                type = ShellOutputType.PROMPT
            ),
            ShellOutput(
                text = "1. Enter phone number",
                type = ShellOutputType.INFO
            ),
            ShellOutput(
                text = "2. Use alias",
                type = ShellOutputType.INFO
            ),
            ShellOutput(
                text = "3. Choose from contacts",
                type = ShellOutputType.INFO
            ),
            ShellOutput(
                text = "Enter your choice (1-3):",
                type = ShellOutputType.PROMPT
            )
        )
    }

    /**
     * Handle user response based on current state
     */
    suspend fun handleResponse(input: String): List<ShellOutput> {
        val trimmed = input.trim()

        return when (val state = _callState.value) {
            is CallState.AwaitingCallMethod -> handleCallMethodSelection(trimmed)
            is CallState.AwaitingNumberInput -> handleNumberInput(trimmed)
            is CallState.AwaitingAliasInput -> handleAliasInput(trimmed)
            is CallState.AwaitingContactSelection -> handleContactSelection(trimmed, state.contacts)
            is CallState.AwaitingSimSelection -> handleSimSelection(trimmed, state.action)
            else -> listOf(
                ShellOutput(
                    text = "No active call session",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    private fun handleCallMethodSelection(choice: String): List<ShellOutput> {
        return when (choice) {
            "1" -> {
                _callState.value = CallState.AwaitingNumberInput
                listOf(
                    ShellOutput(
                        text = "Enter phone number:",
                        type = ShellOutputType.PROMPT
                    )
                )
            }
            "2" -> {
                _callState.value = CallState.AwaitingAliasInput
                listOf(
                    ShellOutput(
                        text = "Enter alias name:",
                        type = ShellOutputType.PROMPT
                    )
                )
            }
            "3" -> {
                _callState.value = CallState.ShowContactModal
                listOf(
                    ShellOutput(
                        text = "Opening contact selection modal...",
                        type = ShellOutputType.SUCCESS
                    )
                )
            }
            else -> listOf(
                ShellOutput(
                    text = "Invalid choice. Please enter 1, 2, or 3",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    private suspend fun handleNumberInput(number: String): List<ShellOutput> {
        val phoneNumber = extractPhoneNumber(number)

        return if (phoneNumber != null && isValidPhoneNumber(phoneNumber)) {
            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(phoneNumber, null)
            )
            listOf(
                ShellOutput(
                    text = "Calling: $phoneNumber",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        } else {
            listOf(
                ShellOutput(
                    text = "Invalid phone number. Please enter a valid number:",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    private suspend fun handleAliasInput(alias: String): List<ShellOutput> {
        val normalizedAlias = alias.lowercase()
            .removePrefix("my ")
            .trim()

        // First check the shared alias database
        val aliasContact = aliasManager.getContactByAlias(normalizedAlias)

        if (aliasContact != null && aliasContact.phoneNumbers.isNotEmpty()) {
            val number = aliasContact.phoneNumbers.first()
            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(number, aliasContact.name)
            )
            return listOf(
                ShellOutput(
                    text = "📞 Calling ${aliasContact.name} (alias: $normalizedAlias)",
                    type = ShellOutputType.SUCCESS
                ),
                ShellOutput(
                    text = "Number: $number",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        }

        // Fallback: search contacts by name
        val contacts = searchContactsByName(alias)

        return if (contacts.isNotEmpty()) {
            val contact = contacts.first()
            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(contact.number, contact.name)
            )
            listOf(
                ShellOutput(
                    text = "📞 Calling ${contact.name}",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Number: ${contact.number}",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        } else {
            listOf(
                ShellOutput(
                    text = "❌ No alias or contact found for '$alias'",
                    type = ShellOutputType.ERROR
                ),
                ShellOutput(
                    text = "💡 Set up an alias with: alias $normalizedAlias = <contact name>",
                    type = ShellOutputType.INFO
                )
            )
        }
    }

    private suspend fun handleContactSelection(
        choice: String,
        contacts: List<SimpleContact>
    ): List<ShellOutput> {
        val index = choice.toIntOrNull()?.minus(1)

        return if (index != null && index in contacts.indices) {
            val contact = contacts[index]
            _callState.value = CallState.AwaitingSimSelection(
                action = CallAction.Call(contact.number, contact.name)
            )
            listOf(
                ShellOutput(
                    text = "Calling ${contact.name}",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Number: ${contact.number}",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "Select SIM:",
                    type = ShellOutputType.PROMPT
                ),
                ShellOutput(
                    text = "1. SIM 1",
                    type = ShellOutputType.INFO
                ),
                ShellOutput(
                    text = "2. SIM 2",
                    type = ShellOutputType.INFO
                )
            )
        } else {
            listOf(
                ShellOutput(
                    text = "Invalid selection. Please enter a number between 1 and ${contacts.size}",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    private suspend fun handleSimSelection(
        choice: String,
        action: CallAction
    ): List<ShellOutput> {
        val simSlot = when (choice) {
            "1" -> 0
            "2" -> 1
            else -> null
        }

        return if (simSlot != null) {
            when (action) {
                is CallAction.Call -> {
                    // Place the call
                    dialerManager.placeCall(action.phoneNumber, simSlot)
                    _callState.value = CallState.InCall

                    // Start terminal-based call session and return ANSI display
                    startActiveCallSession(action.phoneNumber, action.contactName, simSlot)
                }
                is CallAction.Ussd -> {
                    // Execute USSD
                    ussdService.executeUssd(action.code, simSlot)
                    _callState.value = CallState.Idle

                    listOf(
                        ShellOutput(
                            text = "Executing USSD: ${action.code} on SIM ${simSlot + 1}",
                            type = ShellOutputType.SUCCESS
                        ),
                        ShellOutput(
                            text = "Check your system dialer for the response",
                            type = ShellOutputType.INFO
                        )
                    )
                }
            }
        } else {
            listOf(
                ShellOutput(
                    text = "Invalid SIM selection. Please enter 1 or 2",
                    type = ShellOutputType.ERROR
                )
            )
        }
    }

    /**
     * Extract phone number from input
     */
    private fun extractPhoneNumber(input: String): String? {
        // Remove common prefixes and clean input
        val cleaned = input.trim()
            .removePrefix("+")
            .replace(Regex("[^0-9+]"), "")

        return if (cleaned.isNotEmpty()) cleaned else null
    }

    /**
     * Validate phone number
     */
    private fun isValidPhoneNumber(number: String): Boolean {
        // Basic validation: starts with + or digit, length 7-15
        val cleaned = number.replace(Regex("[^0-9]"), "")
        return cleaned.length in 7..15
    }

    /**
     * Reset call state
     */
    fun resetState() {
        _callState.value = CallState.Idle
    }

    /**
     * Set call target from contact picker - transitions to SIM selection
     */
    fun setCallTarget(phoneNumber: String, contactName: String?) {
        _callState.value = CallState.AwaitingSimSelection(
            action = CallAction.Call(phoneNumber, contactName)
        )
    }

    /**
     * Place a call directly with SIM already selected (from contact picker with integrated SIM buttons)
     */
    fun placeCallDirectly(phoneNumber: String, contactName: String?, simSlot: Int): List<ShellOutput> {
        // Place the call
        dialerManager.placeCall(phoneNumber, simSlot)
        _callState.value = CallState.InCall

        // Start terminal-based call session and return ANSI display
        return startActiveCallSession(phoneNumber, contactName, simSlot)
    }

    /**
     * Get available USSD shortcuts
     */
    fun getUssdShortcuts(): Map<String, String> = ussdShortcuts

    /**
     * Search contacts by name using ContentResolver
     */
    private fun searchContactsByName(query: String): List<SimpleContact> {
        val contacts = mutableListOf<SimpleContact>()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext() && contacts.size < 10) { // Limit to 10 results
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    contacts.add(SimpleContact(name, number))
                }
            }
        } catch (e: Exception) {
            // Handle permission or other errors
            android.util.Log.e("CallingHandler", "Error searching contacts", e)
        }

        return contacts
    }
}

/**
 * Simple contact data class
 */
data class SimpleContact(
    val name: String,
    val number: String
)

/**
 * Call state machine
 */
sealed class CallState {
    object Idle : CallState()
    object InCall : CallState()  // Active call in progress
    object AwaitingCallMethod : CallState()
    object AwaitingNumberInput : CallState()
    object AwaitingAliasInput : CallState()
    data class AwaitingContactSelection(val contacts: List<SimpleContact>) : CallState()
    data class AwaitingSimSelection(val action: CallAction) : CallState()
    object ShowContactModal : CallState()

}

/**
 * Call action types
 */
sealed class CallAction {
    data class Call(val phoneNumber: String, val contactName: String?) : CallAction()
    data class Ussd(val code: String) : CallAction()
}

/**
 * Call modal information for displaying the NexusCallModal
 */
data class CallModalInfo(
    val phoneNumber: String,
    val contactName: String?,
    val simSlot: Int
)

/**
 * Active call session for terminal-based call control
 */
data class ActiveCallSession(
    val phoneNumber: String,
    val contactName: String?,
    val simSlot: Int,
    val startTime: Long
) {
    fun getDuration(): Long = System.currentTimeMillis() - startTime
}
