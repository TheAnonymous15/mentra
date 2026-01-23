package com.example.mentra.dialer.billing

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * NEXUS BILL CALCULATOR SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Real-time call billing calculator for Kenyan MNOs
 *
 * Supported Networks:
 * - Safaricom: Peak (8am-10pm): Ksh 4.87/min, Off-peak (10pm-8am): Ksh 2.50/min
 * - Airtel: Peak: Ksh 4.5/min, Off-peak: Ksh 2.0/min
 * - Telkom: Peak: Ksh 4.0/min, Off-peak: Ksh 2.0/min
 *
 * Features:
 * - Real-time cost calculation per second
 * - MNO detection from SIM card
 * - Peak/Off-peak rate detection
 * - Cost tracking for outgoing calls only
 * - Billing starts when call is answered, not when dialing
 */

@Singleton
class NexusBillCalculator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NexusBillCalculator"

        // Peak hours: 8:00 AM to 9:59 PM (10 PM is start of off-peak)
        private const val PEAK_START_HOUR = 8
        private const val PEAK_END_HOUR = 22 // 10 PM

        // Rate constants in KSH per minute
        object SafaricomRates {
            const val PEAK_RATE = 4.87
            const val OFF_PEAK_RATE = 2.50
        }

        object AirtelRates {
            const val PEAK_RATE = 4.50
            const val OFF_PEAK_RATE = 2.00
        }

        object TelkomRates {
            const val PEAK_RATE = 4.00
            const val OFF_PEAK_RATE = 2.00
        }

        // Default rates for unknown networks
        object DefaultRates {
            const val PEAK_RATE = 5.00
            const val OFF_PEAK_RATE = 3.00
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Billing state
    private val _currentBillingInfo = MutableStateFlow<BillingInfo?>(null)
    val currentBillingInfo: StateFlow<BillingInfo?> = _currentBillingInfo.asStateFlow()

    private val _totalCost = MutableStateFlow(0.0)
    val totalCost: StateFlow<Double> = _totalCost.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Tracking job
    private var billingJob: Job? = null
    private var billStartTime: Long = 0
    private var currentSimSlot: Int = -1
    private var currentMno: MobileNetworkOperator = MobileNetworkOperator.UNKNOWN

    /**
     * Start tracking call billing for an outgoing call
     * Call this when the call is ANSWERED (STATE_ACTIVE), not when dialing
     *
     * @param simSlot The SIM slot used for the call (-1 for default)
     */
    fun startTracking(simSlot: Int = -1) {
        if (_isTracking.value) {
            Log.w(TAG, "Already tracking a call, stopping previous")
            stopTracking()
        }

        currentSimSlot = simSlot
        currentMno = detectMno(simSlot)
        billStartTime = System.currentTimeMillis()

        val isPeak = isPeakHours()
        val ratePerMinute = getRatePerMinute(currentMno, isPeak)
        val ratePerSecond = ratePerMinute / 60.0

        Log.d(TAG, "Starting billing: MNO=$currentMno, Peak=$isPeak, Rate=$ratePerMinute/min")

        _currentBillingInfo.value = BillingInfo(
            mno = currentMno,
            simSlot = simSlot,
            isPeakHours = isPeak,
            ratePerMinute = ratePerMinute,
            ratePerSecond = ratePerSecond,
            startTime = billStartTime,
            duration = 0,
            totalCost = 0.0
        )

        _totalCost.value = 0.0
        _isTracking.value = true

        // Start real-time cost tracking
        billingJob = scope.launch {
            while (isActive && _isTracking.value) {
                updateBilling()
                delay(1000) // Update every second
            }
        }
    }

    /**
     * Stop tracking and return final billing info
     */
    fun stopTracking(): BillingInfo? {
        _isTracking.value = false
        billingJob?.cancel()
        billingJob = null

        val finalInfo = _currentBillingInfo.value?.copy(
            duration = if (billStartTime > 0) (System.currentTimeMillis() - billStartTime) / 1000 else 0,
            totalCost = _totalCost.value
        )

        Log.d(TAG, "Billing stopped: Duration=${finalInfo?.duration}s, Cost=${finalInfo?.totalCost} KSH")

        _currentBillingInfo.value = null
        billStartTime = 0
        currentSimSlot = -1
        currentMno = MobileNetworkOperator.UNKNOWN

        return finalInfo
    }

    /**
     * Get formatted current cost string
     */
    fun getFormattedCost(): String {
        val cost = _totalCost.value
        return "KSH %.2f".format(cost)
    }

    /**
     * Get estimated final cost (for display during call)
     */
    fun getEstimatedCostDisplay(): String {
        val info = _currentBillingInfo.value ?: return "KSH 0.00"
        return "KSH %.2f (${info.mno.displayName})".format(_totalCost.value)
    }

    /**
     * Update billing calculation
     */
    private fun updateBilling() {
        if (!_isTracking.value || billStartTime == 0L) return

        val duration = (System.currentTimeMillis() - billStartTime) / 1000
        val currentInfo = _currentBillingInfo.value ?: return

        // Recalculate rate in case peak/off-peak changed during call
        val isPeakNow = isPeakHours()
        val ratePerMinute = getRatePerMinute(currentMno, isPeakNow)
        val ratePerSecond = ratePerMinute / 60.0

        val totalCost = duration * ratePerSecond

        _totalCost.value = totalCost
        _currentBillingInfo.value = currentInfo.copy(
            isPeakHours = isPeakNow,
            ratePerMinute = ratePerMinute,
            ratePerSecond = ratePerSecond,
            duration = duration,
            totalCost = totalCost
        )
    }

    /**
     * Detect MNO from SIM card
     */
    private fun detectMno(simSlot: Int): MobileNetworkOperator {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            val operatorName = if (simSlot >= 0) {
                // Get operator for specific SIM slot
                subscriptionManager?.getActiveSubscriptionInfoForSimSlotIndex(simSlot)?.carrierName?.toString()
                    ?: telephonyManager?.networkOperatorName
            } else {
                telephonyManager?.networkOperatorName
            }

            val operatorCode = telephonyManager?.networkOperator

            Log.d(TAG, "Detected operator: $operatorName, code: $operatorCode")

            when {
                operatorName?.contains("safaricom", ignoreCase = true) == true ||
                operatorCode?.startsWith("639") == true -> MobileNetworkOperator.SAFARICOM

                operatorName?.contains("airtel", ignoreCase = true) == true ||
                operatorCode?.startsWith("639") == true && operatorName?.contains("airtel", ignoreCase = true) == true -> MobileNetworkOperator.AIRTEL

                operatorName?.contains("telkom", ignoreCase = true) == true ||
                operatorName?.contains("orange", ignoreCase = true) == true -> MobileNetworkOperator.TELKOM

                else -> MobileNetworkOperator.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting MNO", e)
            MobileNetworkOperator.UNKNOWN
        }
    }

    /**
     * Check if current time is peak hours
     */
    private fun isPeakHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return currentHour in PEAK_START_HOUR until PEAK_END_HOUR
    }

    /**
     * Get rate per minute based on MNO and time of day
     */
    private fun getRatePerMinute(mno: MobileNetworkOperator, isPeak: Boolean): Double {
        return when (mno) {
            MobileNetworkOperator.SAFARICOM -> if (isPeak) SafaricomRates.PEAK_RATE else SafaricomRates.OFF_PEAK_RATE
            MobileNetworkOperator.AIRTEL -> if (isPeak) AirtelRates.PEAK_RATE else AirtelRates.OFF_PEAK_RATE
            MobileNetworkOperator.TELKOM -> if (isPeak) TelkomRates.PEAK_RATE else TelkomRates.OFF_PEAK_RATE
            MobileNetworkOperator.UNKNOWN -> if (isPeak) DefaultRates.PEAK_RATE else DefaultRates.OFF_PEAK_RATE
        }
    }
}

/**
 * Mobile Network Operators
 */
enum class MobileNetworkOperator(val displayName: String) {
    SAFARICOM("Safaricom"),
    AIRTEL("Airtel"),
    TELKOM("Telkom"),
    UNKNOWN("Unknown")
}

/**
 * Billing information data class
 */
data class BillingInfo(
    val mno: MobileNetworkOperator,
    val simSlot: Int,
    val isPeakHours: Boolean,
    val ratePerMinute: Double,
    val ratePerSecond: Double,
    val startTime: Long,
    val duration: Long, // in seconds
    val totalCost: Double // in KSH
) {
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun getFormattedCost(): String = "KSH %.2f".format(totalCost)

    fun getRateInfo(): String = "KSH %.2f/min (${if (isPeakHours) "Peak" else "Off-peak"})".format(ratePerMinute)
}

