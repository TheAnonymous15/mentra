package com.example.mentra.dialer.ussd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.UssdResponseCallback
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ═══════════════════════════════════════════════════════════════════
 * USSD SERVICE - Handles USSD code execution and response capture
 * Supports both legacy dial and modern callback-based USSD handling
 * ═══════════════════════════════════════════════════════════════════
 */

@Singleton
class UssdService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    // State flows for UI observation
    private val _ussdState = MutableStateFlow<UssdState>(UssdState.Idle)
    val ussdState: StateFlow<UssdState> = _ussdState.asStateFlow()

    private val _lastResponse = MutableStateFlow<UssdResponse?>(null)
    val lastResponse: StateFlow<UssdResponse?> = _lastResponse.asStateFlow()

    private val _ussdHistory = MutableStateFlow<List<UssdHistoryEntry>>(emptyList())
    val ussdHistory: StateFlow<List<UssdHistoryEntry>> = _ussdHistory.asStateFlow()

    // Track if we're in an interactive session
    private var isInteractiveSession = false
    private var currentSessionCode: String? = null

    // Store the active TelephonyManager and callback for session continuity
    private var sessionTelephonyManager: TelephonyManager? = null

    // Timeout handling
    private val ussdTimeoutSeconds = 30L
    private var timeoutJob: Job? = null

    /**
     * Check if the code is a valid USSD code
     */
    fun isValidUssdCode(code: String): Boolean {
        val trimmed = code.trim()
        // USSD codes start with * or # and end with #
        // They contain only digits, *, and #
        return trimmed.matches(Regex("^[*#][0-9*#]+#$")) ||
               trimmed.matches(Regex("^[*][0-9*#]+$")) // Some codes don't end with #
    }

    /**
     * Normalize USSD code - ensure proper format
     */
    fun normalizeUssdCode(code: String): String {
        var normalized = code.trim()

        // Ensure it starts with * if it doesn't start with * or #
        if (!normalized.startsWith("*") && !normalized.startsWith("#")) {
            normalized = "*$normalized"
        }

        // Ensure it ends with # if it doesn't
        if (!normalized.endsWith("#")) {
            normalized = "$normalized#"
        }

        return normalized
    }

    /**
     * Execute USSD code using the modern callback API (Android 8.0+)
     * Returns the response or error
     */
    @SuppressLint("MissingPermission")
    suspend fun executeUssd(
        ussdCode: String,
        simSlotIndex: Int = 0,
        isReply: Boolean = false
    ): UssdResult {
        // Check permissions
        if (!hasCallPermission()) {
            return UssdResult.Error(UssdError.PERMISSION_DENIED, "CALL_PHONE permission required")
        }

        // For replies in interactive sessions, don't normalize (just send the number)
        val codeToSend = if (isReply && isInteractiveSession) {
            ussdCode // Send as-is for menu selections (1, 2, 3, etc.)
        } else {
            normalizeUssdCode(ussdCode)
        }

        // Validate only for new USSD codes, not replies
        if (!isReply && !isValidUssdCode(codeToSend)) {
            return UssdResult.Error(UssdError.INVALID_CODE, "Invalid USSD code format")
        }

        _ussdState.value = UssdState.Executing(codeToSend)

        return try {
            // Always use system dialer for USSD codes (most reliable method)
            // The callback API is unreliable across different devices and carriers
            android.util.Log.d("UssdService", "Using system dialer for USSD: $codeToSend (SIM slot: $simSlotIndex)")
            executeUssdLegacy(codeToSend, simSlotIndex)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            android.util.Log.e("UssdService", "USSD execution failed: $errorMessage", e)
            val error = UssdResult.Error(UssdError.EXECUTION_FAILED, errorMessage)
            _ussdState.value = UssdState.Error(errorMessage)
            endSession()
            error
        }
    }

    /**
     * Send a reply to an interactive USSD session
     */
    @SuppressLint("MissingPermission")
    suspend fun sendUssdReply(reply: String, simSlotIndex: Int = 0): UssdResult {
        if (!isInteractiveSession) {
            return UssdResult.Error(UssdError.SESSION_ENDED, "No active USSD session")
        }

        val tm = sessionTelephonyManager ?: return UssdResult.Error(
            UssdError.SERVICE_UNAVAILABLE,
            "Session manager not available"
        )

        _ussdState.value = UssdState.Executing(reply)

        return try {
            // For interactive sessions, we send the reply as a new USSD request
            // but this should continue the existing session
            executeUssd(reply, simSlotIndex, isReply = true)
        } catch (e: Exception) {
            endSession()
            UssdResult.Error(UssdError.EXECUTION_FAILED, e.message)
        }
    }

    /**
     * Modern USSD execution with callback (Android 8.0+)
     */
    @SuppressLint("MissingPermission")
    private suspend fun executeUssdWithCallback(
        ussdCode: String,
        simSlotIndex: Int,
        isReply: Boolean = false
    ): UssdResult = suspendCancellableCoroutine { continuation ->

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            continuation.resume(UssdResult.Error(UssdError.NOT_SUPPORTED, "API level too low"))
            return@suspendCancellableCoroutine
        }

        val tm = telephonyManager ?: run {
            continuation.resume(UssdResult.Error(UssdError.SERVICE_UNAVAILABLE, "TelephonyManager not available"))
            return@suspendCancellableCoroutine
        }

        // Start timeout timer
        timeoutJob?.cancel()
        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(ussdTimeoutSeconds * 1000)
            if (continuation.isActive) {
                android.util.Log.w("UssdService", "USSD request timed out after ${ussdTimeoutSeconds}s")
                endSession()
                _ussdState.value = UssdState.Error("Request timed out")
                continuation.resume(UssdResult.Error(UssdError.TIMEOUT, "USSD request timed out"))
            }
        }

        val callback = object : UssdResponseCallback() {
            override fun onReceiveUssdResponse(
                telephonyManager: TelephonyManager,
                request: String,
                response: CharSequence
            ) {
                // Cancel timeout
                timeoutJob?.cancel()

                val responseText = response.toString()
                android.util.Log.d("UssdService", "USSD response received: $responseText")

                // Detect if this is an interactive session
                val isInteractive = detectInteractiveSession(responseText)

                val ussdResponse = UssdResponse(
                    code = if (isReply) currentSessionCode ?: ussdCode else ussdCode,
                    response = responseText,
                    timestamp = System.currentTimeMillis(),
                    isSuccess = true,
                    isInteractive = isInteractive,
                    sessionActive = isInteractive
                )

                _lastResponse.value = ussdResponse

                if (isInteractive) {
                    // Keep session active for user input
                    isInteractiveSession = true
                    if (!isReply) {
                        currentSessionCode = ussdCode
                    }
                    sessionTelephonyManager = telephonyManager
                    _ussdState.value = UssdState.Interactive(ussdResponse)
                    android.util.Log.d("UssdService", "Interactive USSD session started")
                } else {
                    // Final response, end session
                    endSession()
                    _ussdState.value = UssdState.Success(ussdResponse)
                    android.util.Log.d("UssdService", "USSD session completed")
                }

                addToHistory(ussdCode, responseText, true)

                if (continuation.isActive) {
                    continuation.resume(UssdResult.Success(ussdResponse))
                }
            }

            override fun onReceiveUssdResponseFailed(
                telephonyManager: TelephonyManager,
                request: String,
                failureCode: Int
            ) {
                // Cancel timeout
                timeoutJob?.cancel()

                val errorMessage = when (failureCode) {
                    TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL -> "USSD service unavailable"
                    TelephonyManager.USSD_RETURN_FAILURE -> "USSD request failed - network or carrier issue"
                    else -> "Unknown USSD error (code: $failureCode)"
                }

                android.util.Log.e("UssdService", "USSD failed: $errorMessage (code: $failureCode)")
                endSession()
                _ussdState.value = UssdState.Error(errorMessage)
                addToHistory(currentSessionCode ?: ussdCode, errorMessage, false)

                if (continuation.isActive) {
                    continuation.resume(UssdResult.Error(UssdError.USSD_FAILED, errorMessage))
                }
            }
        }

        try {
            // Get the appropriate TelephonyManager for the SIM slot
            val simTm = if (isReply && sessionTelephonyManager != null) {
                // Reuse the session TelephonyManager for replies
                sessionTelephonyManager!!
            } else {
                // Create new for initial request
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && simSlotIndex > 0) {
                    tm.createForSubscriptionId(getSubscriptionIdForSlot(simSlotIndex))
                } else {
                    tm
                }
            }

            // Store for session continuity
            if (!isReply) {
                sessionTelephonyManager = simTm
                currentSessionCode = ussdCode
            }

            android.util.Log.d("UssdService", "Sending USSD request: $ussdCode (isReply: $isReply)")

            // Send the USSD request
            simTm.sendUssdRequest(
                ussdCode,
                callback,
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
            timeoutJob?.cancel()
            android.util.Log.e("UssdService", "Security exception", e)
            endSession()
            _ussdState.value = UssdState.Error("Permission denied")
            if (continuation.isActive) {
                continuation.resume(UssdResult.Error(UssdError.PERMISSION_DENIED, e.message))
            }
        } catch (e: Exception) {
            timeoutJob?.cancel()
            android.util.Log.e("UssdService", "Exception during USSD execution", e)
            endSession()
            _ussdState.value = UssdState.Error(e.message ?: "Unknown error")
            if (continuation.isActive) {
                continuation.resume(UssdResult.Error(UssdError.EXECUTION_FAILED, e.message))
            }
        }

        continuation.invokeOnCancellation {
            timeoutJob?.cancel()
            endSession()
            _ussdState.value = UssdState.Idle
            android.util.Log.d("UssdService", "USSD request cancelled")
        }
    }

    /**
     * Detect if the USSD response indicates an interactive session
     * Interactive sessions typically have numbered menu options
     */
    private fun detectInteractiveSession(response: String): Boolean {
        // Common patterns for interactive USSD menus
        val interactivePatterns = listOf(
            Regex("\\d+\\.\\s*\\w+"),           // "1. Option" pattern
            Regex("\\d+\\)\\s*\\w+"),           // "1) Option" pattern
            Regex("Reply\\s+with", RegexOption.IGNORE_CASE),
            Regex("Enter\\s+\\d+", RegexOption.IGNORE_CASE),
            Regex("Press\\s+\\d+", RegexOption.IGNORE_CASE),
            Regex("Select\\s+option", RegexOption.IGNORE_CASE),
            Regex("Choose", RegexOption.IGNORE_CASE),
            Regex("0\\.\\s*Exit", RegexOption.IGNORE_CASE),
            Regex("0\\.\\s*Cancel", RegexOption.IGNORE_CASE),
            Regex("\\*\\s*Back", RegexOption.IGNORE_CASE),
            Regex("#\\s*Next", RegexOption.IGNORE_CASE)
        )

        return interactivePatterns.any { it.containsMatchIn(response) }
    }

    /**
     * End the current USSD session
     */
    private fun endSession() {
        timeoutJob?.cancel()
        timeoutJob = null
        isInteractiveSession = false
        currentSessionCode = null
        sessionTelephonyManager = null
        android.util.Log.d("UssdService", "USSD session ended")
    }

    /**
     * Check if there's an active interactive session
     */
    fun hasActiveSession(): Boolean = isInteractiveSession

    /**
     * Cancel the current USSD session
     */
    fun cancelSession() {
        timeoutJob?.cancel()
        endSession()
        _ussdState.value = UssdState.Idle
        _lastResponse.value = null
        android.util.Log.d("UssdService", "USSD session cancelled by user")
    }

    /**
     * Legacy USSD execution via dial intent (System Dialer)
     * This is the most reliable method across all devices and carriers
     * Simply delegates to the system dialer - no encoding needed
     * @param ussdCode The USSD code to dial
     * @param simSlotIndex The SIM slot to use (0 or 1)
     */
    @SuppressLint("MissingPermission")
    private fun executeUssdLegacy(ussdCode: String, simSlotIndex: Int = 0): UssdResult {
        return try {
            // Simply pass the USSD code to the system dialer as-is
            // The system dialer handles all encoding internally
            android.util.Log.d("UssdService", "Delegating USSD to system dialer: $ussdCode")

            val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                // Use Uri.fromParts to properly handle special characters like # and *
                data = android.net.Uri.fromParts("tel", ussdCode, null)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK

                // Add SIM slot selection for dual SIM devices (Android 5.1+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subscriptionId = getSubscriptionIdForSlot(simSlotIndex)
                    android.util.Log.d("UssdService", "Using subscription ID: $subscriptionId for SIM slot: $simSlotIndex")

                    // Different manufacturers use different extras for SIM selection
                    putExtra("com.android.phone.extra.slot", simSlotIndex)
                    putExtra("slot", simSlotIndex)
                    putExtra("simSlot", simSlotIndex)
                    putExtra("sim_slot", simSlotIndex)
                    putExtra("subscription", subscriptionId)
                    putExtra("phone", simSlotIndex)
                    putExtra("com.samsung.phone.extra.SLOT_ID", simSlotIndex)
                    putExtra("com.android.phone.extra.SUBSCRIPTION_ID", subscriptionId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE",
                            getPhoneAccountHandleForSlot(simSlotIndex))
                    }
                }
            }

            context.startActivity(intent)

            _ussdState.value = UssdState.LegacyDialing(ussdCode)
            addToHistory(ussdCode, "Opened in system dialer (SIM ${simSlotIndex + 1})", true)

            CoroutineScope(Dispatchers.Main).launch {
                delay(2000) // Wait 2 seconds
                if (_ussdState.value is UssdState.LegacyDialing) {
                    _ussdState.value = UssdState.Idle
                }
            }

            UssdResult.LegacyDial(ussdCode)
        } catch (e: Exception) {
            android.util.Log.e("UssdService", "Failed to dial USSD via system dialer", e)
            _ussdState.value = UssdState.Error(e.message ?: "Failed to dial")
            UssdResult.Error(UssdError.EXECUTION_FAILED, e.message)
        }
    }

    /**
     * Get PhoneAccountHandle for a given SIM slot (Android 6.0+)
     */


    @SuppressLint("MissingPermission")
    private fun getPhoneAccountHandleForSlot(slotIndex: Int): android.telecom.PhoneAccountHandle? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telecomManager?.callCapablePhoneAccounts?.getOrNull(slotIndex)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UssdService", "Failed to get PhoneAccountHandle", e)
            null
        }
    }

    /**
     * Dial USSD code without waiting for response
     * Simply delegates to the system dialer
     */
    fun dialUssd(ussdCode: String): Boolean {
        return try {
            val normalizedCode = normalizeUssdCode(ussdCode)

            android.util.Log.d("UssdService", "dialUssd: Delegating to system dialer: $normalizedCode")

            // Use Uri.fromParts to properly handle special characters like # and *
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.fromParts("tel", normalizedCode, null)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (hasCallPermission()) {
                context.startActivity(intent)
                addToHistory(normalizedCode, "Dialed", true)
                true
            } else {
                // Use ACTION_DIAL instead which doesn't require permission
                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.fromParts("tel", normalizedCode, null)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("UssdService", "Failed to dial USSD", e)
            false
        }
    }

    /**
     * Get subscription ID for a given SIM slot
     */
    @SuppressLint("MissingPermission")
    private fun getSubscriptionIdForSlot(slotIndex: Int): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as? android.telephony.SubscriptionManager

                subscriptionManager?.activeSubscriptionInfoList?.find {
                    it.simSlotIndex == slotIndex
                }?.subscriptionId ?: android.telephony.SubscriptionManager.getDefaultSubscriptionId()
            } else {
                android.telephony.SubscriptionManager.getDefaultSubscriptionId()
            }
        } catch (e: Exception) {
            android.telephony.SubscriptionManager.getDefaultSubscriptionId()
        }
    }

    /**
     * Check if CALL_PHONE permission is granted
     */
    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Add entry to USSD history
     */
    private fun addToHistory(code: String, response: String, success: Boolean) {
        val entry = UssdHistoryEntry(
            code = code,
            response = response,
            timestamp = System.currentTimeMillis(),
            isSuccess = success
        )

        _ussdHistory.value = listOf(entry) + _ussdHistory.value.take(49) // Keep last 50
    }

    /**
     * Clear USSD history
     */
    fun clearHistory() {
        _ussdHistory.value = emptyList()
    }

    /**
     * Reset state to idle and clear any pending USSD session
     */
    fun resetState() {
        _ussdState.value = UssdState.Idle
        _lastResponse.value = null
    }

    /**
     * Cancel any ongoing USSD request
     */
    fun cancelUssd() {
        _ussdState.value = UssdState.Idle
        _lastResponse.value = null
    }

    /**
     * Check if USSD callback API is available
     */
    fun isCallbackApiAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * Common USSD codes helper
     */
    companion object {
        // Common USSD codes
        val COMMON_CODES = mapOf(
            "Check Balance" to "*123#",
            "Check Data Balance" to "*544#",
            "Check Minutes" to "*122#",
            "My Number" to "*135#",
            "Airtime Balance" to "*100#",
            "Customer Care" to "*100#"
        )

        // Carrier-specific codes (examples)
        val CARRIER_CODES = mapOf(
            "Safaricom" to mapOf(
                "M-Pesa Balance" to "*334#",
                "Data Balance" to "*544#",
                "Airtime Balance" to "*100#"
            ),
            "Airtel" to mapOf(
                "Balance" to "*123#",
                "Data" to "*141#"
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// USSD DATA CLASSES
// ═══════════════════════════════════════════════════════════════════

/**
 * Represents the current state of USSD execution
 */
sealed class UssdState {
    object Idle : UssdState()
    data class Executing(val code: String) : UssdState()
    data class Success(val response: UssdResponse) : UssdState()
    data class Interactive(val response: UssdResponse) : UssdState() // Waiting for user input
    data class Error(val message: String) : UssdState()
    data class LegacyDialing(val code: String) : UssdState()
}

/**
 * USSD execution result
 */
sealed class UssdResult {
    data class Success(val response: UssdResponse) : UssdResult()
    data class Error(val error: UssdError, val message: String?) : UssdResult()
    data class LegacyDial(val code: String) : UssdResult()
}

/**
 * USSD error types
 */
enum class UssdError {
    PERMISSION_DENIED,
    INVALID_CODE,
    SERVICE_UNAVAILABLE,
    NOT_SUPPORTED,
    USSD_FAILED,
    EXECUTION_FAILED,
    TIMEOUT,
    SESSION_ENDED
}

/**
 * USSD response data
 */
data class UssdResponse(
    val code: String,
    val response: String,
    val timestamp: Long,
    val isSuccess: Boolean,
    val isInteractive: Boolean = false,
    val sessionActive: Boolean = false
)

/**
 * USSD history entry
 */
data class UssdHistoryEntry(
    val code: String,
    val response: String,
    val timestamp: Long,
    val isSuccess: Boolean
) {
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

