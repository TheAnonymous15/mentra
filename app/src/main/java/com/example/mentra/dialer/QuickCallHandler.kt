package com.example.mentra.dialer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * QUICK CALL HANDLER
 * Manages quick calls from anywhere in the app (e.g., from messaging)
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class QuickCallHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telecomManager: TelecomManager? by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    }

    // Call state
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _currentCallInfo = MutableStateFlow<QuickCallInfo?>(null)
    val currentCallInfo: StateFlow<QuickCallInfo?> = _currentCallInfo.asStateFlow()

    private val _callState = MutableStateFlow<QuickCallState>(QuickCallState.IDLE)
    val callState: StateFlow<QuickCallState> = _callState.asStateFlow()

    /**
     * Initiate a quick call
     */
    fun initiateCall(phoneNumber: String, contactName: String? = null, simSlot: Int = -1): QuickCallResult {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            return QuickCallResult.PermissionDenied
        }

        // Validate number
        if (!isValidPhoneNumber(phoneNumber)) {
            return QuickCallResult.InvalidNumber
        }

        // Update state
        _currentCallInfo.value = QuickCallInfo(
            phoneNumber = phoneNumber,
            contactName = contactName,
            simSlot = simSlot
        )
        _callState.value = QuickCallState.INITIATING
        _isCallActive.value = true

        // Place the call using system dialer
        try {
            val uri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // If a specific SIM is selected, add the SIM info
                if (simSlot >= 0) {
                    putExtra("com.android.phone.extra.slot", simSlot)
                    putExtra("simSlot", simSlot)
                }
            }

            context.startActivity(callIntent)
            _callState.value = QuickCallState.CALLING

            return QuickCallResult.Success
        } catch (e: Exception) {
            _callState.value = QuickCallState.FAILED
            _isCallActive.value = false
            return QuickCallResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * End the current call (if possible)
     */
    fun endCall() {
        _callState.value = QuickCallState.ENDED
        _isCallActive.value = false
        _currentCallInfo.value = null
    }

    /**
     * Reset state
     */
    fun reset() {
        _callState.value = QuickCallState.IDLE
        _isCallActive.value = false
        _currentCallInfo.value = null
    }

    /**
     * Check if we can make calls
     */
    fun canMakeCalls(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Validate phone number
     */
    private fun isValidPhoneNumber(number: String): Boolean {
        val cleaned = number.replace(Regex("[\\s\\-().]"), "")
        return cleaned.matches(Regex("^\\+?[0-9]{7,15}$"))
    }

    /**
     * Get available SIMs
     */
    fun getAvailableSims(): List<QuickCallSimInfo> {
        // This would integrate with the DialerManager to get real SIM info
        // For now, return a placeholder
        return listOf(
            QuickCallSimInfo(0, "SIM 1", "Carrier 1"),
            QuickCallSimInfo(1, "SIM 2", "Carrier 2")
        )
    }
}

data class QuickCallInfo(
    val phoneNumber: String,
    val contactName: String?,
    val simSlot: Int,
    val startTime: Long = System.currentTimeMillis()
)

data class QuickCallSimInfo(
    val slotIndex: Int,
    val label: String,
    val carrierName: String
)

enum class QuickCallState {
    IDLE,
    INITIATING,
    CALLING,
    CONNECTED,
    ENDED,
    FAILED
}

sealed class QuickCallResult {
    object Success : QuickCallResult()
    object PermissionDenied : QuickCallResult()
    object InvalidNumber : QuickCallResult()
    data class Failed(val message: String) : QuickCallResult()
}

