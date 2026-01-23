package com.example.mentra.dialer.proximity

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * NEXUS PROXIMITY SENSOR HANDLER
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Manages proximity sensor during active calls to:
 * - Turn off screen when phone is near ear (prevents accidental touch)
 * - Turn screen back on when phone is moved away from ear
 *
 * Uses Android's PROXIMITY_SCREEN_OFF_WAKE_LOCK for proper screen control
 */

@Singleton
class NexusProximitySensorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "NexusProximity"
        private const val PROXIMITY_THRESHOLD = 5.0f // cm - typical max range is ~5cm
    }

    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    private val proximitySensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    // Proximity wake lock for screen control
    private var proximityWakeLock: PowerManager.WakeLock? = null

    // State tracking
    private val _isNear = MutableStateFlow(false)
    val isNear: StateFlow<Boolean> = _isNear.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _lastProximityValue = MutableStateFlow(0f)
    val lastProximityValue: StateFlow<Float> = _lastProximityValue.asStateFlow()

    /**
     * Check if proximity sensor is available
     */
    fun isSensorAvailable(): Boolean {
        return proximitySensor != null
    }

    /**
     * Enable proximity sensor during call
     * This will turn off the screen when phone is near the ear
     */
    @Suppress("DEPRECATION")
    fun enable() {
        if (_isEnabled.value) {
            Log.d(TAG, "Proximity sensor already enabled")
            return
        }

        if (!isSensorAvailable()) {
            Log.w(TAG, "Proximity sensor not available on this device")
            return
        }

        try {
            // Create proximity wake lock
            if (proximityWakeLock == null) {
                proximityWakeLock = powerManager?.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "Mentra:ProximityWakeLock"
                )
            }

            // Acquire wake lock
            proximityWakeLock?.let { wakeLock ->
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(10 * 60 * 1000L) // Max 10 minutes per call segment
                    Log.d(TAG, "Proximity wake lock acquired")
                }
            }

            // Register sensor listener
            sensorManager?.registerListener(
                this,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            _isEnabled.value = true
            Log.d(TAG, "Proximity sensor enabled for call")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable proximity sensor", e)
        }
    }

    /**
     * Disable proximity sensor after call ends
     */
    fun disable() {
        if (!_isEnabled.value) {
            return
        }

        try {
            // Unregister sensor listener
            sensorManager?.unregisterListener(this)

            // Release wake lock
            proximityWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "Proximity wake lock released")
                }
            }

            _isEnabled.value = false
            _isNear.value = false
            Log.d(TAG, "Proximity sensor disabled")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable proximity sensor", e)
        }
    }

    /**
     * Handle sensor value changes
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return

        val distance = event.values[0]
        val maxRange = event.sensor.maximumRange
        _lastProximityValue.value = distance

        // Check if object is near (typically within 5cm)
        val isObjectNear = distance < maxRange.coerceAtMost(PROXIMITY_THRESHOLD)

        if (_isNear.value != isObjectNear) {
            _isNear.value = isObjectNear
            Log.d(TAG, "Proximity changed: near=$isObjectNear, distance=$distance, max=$maxRange")

            // The PROXIMITY_SCREEN_OFF_WAKE_LOCK handles screen on/off automatically
            // based on proximity values, so we don't need to manually control the screen
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for proximity sensor
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disable()
        proximityWakeLock = null
    }
}

/**
 * Extension function for Activity to enable keep-screen-on during calls
 * Call this in addition to proximity sensor for devices where proximity sensor may not work well
 */
fun Activity.setKeepScreenOnForCall(keepOn: Boolean) {
    try {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    } catch (e: Exception) {
        Log.e("NexusProximity", "Failed to set keep screen on flag", e)
    }
}

