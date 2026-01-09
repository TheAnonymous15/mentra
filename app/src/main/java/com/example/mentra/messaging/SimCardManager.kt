package com.example.mentra.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SIM Card Manager
 * Handles dual/multi SIM functionality
 *
 * Features:
 * - Detect available SIM cards
 * - Get SIM info (carrier, number, slot)
 * - Send SMS via specific SIM
 * - Default SIM selection
 */
@Singleton
class SimCardManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val subscriptionManager: SubscriptionManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    }

    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow<Int?>(null)
    val selectedSimSlot: StateFlow<Int?> = _selectedSimSlot.asStateFlow()

    init {
        loadAvailableSims()
    }

    /**
     * Load all available SIM cards
     */
    fun loadAvailableSims() {
        if (!hasPhonePermission()) {
            _availableSims.value = emptyList()
            return
        }

        try {
            val sims = mutableListOf<SimInfo>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                @Suppress("MissingPermission")
                val subscriptions = subscriptionManager?.activeSubscriptionInfoList

                subscriptions?.forEachIndexed { index, info ->
                    sims.add(
                        SimInfo(
                            subscriptionId = info.subscriptionId,
                            simSlotIndex = info.simSlotIndex,
                            carrierName = info.carrierName?.toString() ?: "SIM ${index + 1}",
                            displayName = info.displayName?.toString() ?: "SIM ${index + 1}",
                            phoneNumber = info.number ?: "",
                            countryIso = info.countryIso ?: "",
                            iconTint = info.iconTint,
                            isDefault = index == 0
                        )
                    )
                }
            }

            // If no SIMs found, add a default entry
            if (sims.isEmpty()) {
                sims.add(
                    SimInfo(
                        subscriptionId = -1,
                        simSlotIndex = 0,
                        carrierName = "Default SIM",
                        displayName = "Default SIM",
                        phoneNumber = "",
                        countryIso = "",
                        iconTint = 0,
                        isDefault = true
                    )
                )
            }

            _availableSims.value = sims

            // Set default selection
            if (_selectedSimSlot.value == null && sims.isNotEmpty()) {
                _selectedSimSlot.value = sims.first().simSlotIndex
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _availableSims.value = listOf(
                SimInfo(
                    subscriptionId = -1,
                    simSlotIndex = 0,
                    carrierName = "Default SIM",
                    displayName = "Default SIM",
                    phoneNumber = "",
                    countryIso = "",
                    iconTint = 0,
                    isDefault = true
                )
            )
        }
    }

    /**
     * Select SIM for sending
     */
    fun selectSim(slotIndex: Int) {
        _selectedSimSlot.value = slotIndex
    }

    /**
     * Get SmsManager for specific SIM
     */
    fun getSmsManagerForSim(subscriptionId: Int): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
                .createForSubscriptionId(subscriptionId)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    /**
     * Get currently selected SIM's SmsManager
     */
    fun getSelectedSmsManager(): SmsManager {
        val selectedSlot = _selectedSimSlot.value ?: 0
        val sim = _availableSims.value.find { it.simSlotIndex == selectedSlot }

        return if (sim != null && sim.subscriptionId > 0) {
            getSmsManagerForSim(sim.subscriptionId)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        }
    }

    /**
     * Send SMS via specific SIM
     */
    fun sendSmsViaSim(
        phoneNumber: String,
        message: String,
        subscriptionId: Int = -1
    ): Result<Boolean> {
        return try {
            val smsManager = if (subscriptionId > 0) {
                getSmsManagerForSim(subscriptionId)
            } else {
                getSelectedSmsManager()
            }

            val parts = smsManager.divideMessage(message)

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Get SIM count
     */
    fun getSimCount(): Int = _availableSims.value.size

    /**
     * Check if device has multiple SIMs
     */
    fun hasMultipleSims(): Boolean = _availableSims.value.size > 1

    /**
     * Get default SIM info
     */
    fun getDefaultSim(): SimInfo? = _availableSims.value.find { it.isDefault }

    /**
     * Check phone permission
     */
    private fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * SIM card information
 */
data class SimInfo(
    val subscriptionId: Int,
    val simSlotIndex: Int,
    val carrierName: String,
    val displayName: String,
    val phoneNumber: String,
    val countryIso: String,
    val iconTint: Int,
    val isDefault: Boolean
) {
    /**
     * Get display label for UI
     */
    fun getLabel(): String {
        return if (phoneNumber.isNotBlank()) {
            "$carrierName (${phoneNumber.takeLast(4)})"
        } else {
            "$carrierName - SIM ${simSlotIndex + 1}"
        }
    }

    /**
     * Get short label
     */
    fun getShortLabel(): String = "SIM ${simSlotIndex + 1}"
}
