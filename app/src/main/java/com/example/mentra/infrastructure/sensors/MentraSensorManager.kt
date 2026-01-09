package com.example.mentra.infrastructure.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import com.example.mentra.core.common.EventBus
import com.example.mentra.core.common.SystemEvent
import com.example.mentra.infrastructure.sensors.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified sensor management system
 * Provides abstraction over Android sensor APIs
 */
@Singleton
class MentraSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: EventBus
) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager

    private val _sensorStates = MutableStateFlow<Map<SensorType, Boolean>>(emptyMap())
    val sensorStates: StateFlow<Map<SensorType, Boolean>> = _sensorStates.asStateFlow()

    private val sensorListeners = mutableMapOf<SensorType, SensorEventListener>()
    private val sensorStats = mutableMapOf<SensorType, SensorStats>()

    /**
     * Check if sensor is available
     */
    fun isSensorAvailable(sensorType: SensorType): Boolean {
        val androidSensorType = mapSensorType(sensorType)
        return sensorManager.getDefaultSensor(androidSensorType) != null
    }

    /**
     * Get available sensors
     */
    fun getAvailableSensors(): List<SensorType> {
        return SensorType.values().filter { isSensorAvailable(it) }
    }

    /**
     * Start sensor
     */
    fun startSensor(
        sensorType: SensorType,
        onData: (SensorData) -> Unit,
        samplingPeriod: Int = AndroidSensorManager.SENSOR_DELAY_NORMAL
    ): Boolean {
        if (!isSensorAvailable(sensorType)) {
            return false
        }

        // Stop if already running
        stopSensor(sensorType)

        val androidSensorType = mapSensorType(sensorType)
        val sensor = sensorManager.getDefaultSensor(androidSensorType) ?: return false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sensorData = SensorData(
                    sensorType = sensorType,
                    values = event.values.clone(),
                    accuracy = event.accuracy,
                    timestamp = event.timestamp
                )

                onData(sensorData)
                updateStats(sensorType, sensorData)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        val registered = sensorManager.registerListener(listener, sensor, samplingPeriod)

        if (registered) {
            sensorListeners[sensorType] = listener
            updateSensorState(sensorType, true)

            sensorStats[sensorType] = SensorStats(
                sensorType = sensorType,
                isActive = true
            )
        }

        return registered
    }

    /**
     * Stop sensor
     */
    fun stopSensor(sensorType: SensorType) {
        sensorListeners[sensorType]?.let { listener ->
            sensorManager.unregisterListener(listener)
            sensorListeners.remove(sensorType)
            updateSensorState(sensorType, false)

            sensorStats[sensorType] = sensorStats[sensorType]?.copy(isActive = false)
                ?: SensorStats(sensorType, isActive = false)
        }
    }

    /**
     * Stop all sensors
     */
    fun stopAllSensors() {
        sensorListeners.keys.toList().forEach { stopSensor(it) }
    }

    /**
     * Get sensor statistics
     */
    fun getSensorStats(sensorType: SensorType): SensorStats? {
        return sensorStats[sensorType]
    }

    /**
     * Get all sensor statistics
     */
    fun getAllSensorStats(): Map<SensorType, SensorStats> {
        return sensorStats.toMap()
    }

    /**
     * Update sensor state
     */
    private fun updateSensorState(sensorType: SensorType, isActive: Boolean) {
        val currentStates = _sensorStates.value.toMutableMap()
        currentStates[sensorType] = isActive
        _sensorStates.value = currentStates
    }

    /**
     * Update sensor statistics
     */
    private fun updateStats(sensorType: SensorType, data: SensorData) {
        val current = sensorStats[sensorType] ?: return

        val eventsReceived = current.eventsReceived + 1
        val lastEventTime = data.timestamp

        // Calculate frequency (simple moving average)
        val frequency = if (current.lastEventTime != null) {
            val timeDiff = (lastEventTime - current.lastEventTime) / 1_000_000f // ns to ms
            if (timeDiff > 0) {
                1000f / timeDiff // Hz
            } else {
                current.averageFrequency
            }
        } else {
            0f
        }

        sensorStats[sensorType] = current.copy(
            eventsReceived = eventsReceived,
            lastEventTime = lastEventTime,
            averageFrequency = frequency
        )
    }

    /**
     * Map our SensorType to Android sensor type
     */
    private fun mapSensorType(sensorType: SensorType): Int {
        return when (sensorType) {
            SensorType.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
            SensorType.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
            SensorType.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
            SensorType.STEP_COUNTER -> Sensor.TYPE_STEP_COUNTER
            SensorType.STEP_DETECTOR -> Sensor.TYPE_STEP_DETECTOR
            SensorType.BAROMETER -> Sensor.TYPE_PRESSURE
            SensorType.PROXIMITY -> Sensor.TYPE_PROXIMITY
            SensorType.LIGHT -> Sensor.TYPE_LIGHT
            SensorType.TEMPERATURE -> Sensor.TYPE_AMBIENT_TEMPERATURE
            SensorType.HUMIDITY -> Sensor.TYPE_RELATIVE_HUMIDITY
            SensorType.ACTIVITY_RECOGNITION -> Sensor.TYPE_STEP_DETECTOR // Fallback
        }
    }

    /**
     * Get sensor info
     */
    fun getSensorInfo(sensorType: SensorType): SensorInfo? {
        val androidSensorType = mapSensorType(sensorType)
        val sensor = sensorManager.getDefaultSensor(androidSensorType) ?: return null

        return SensorInfo(
            type = sensorType,
            name = sensor.name,
            vendor = sensor.vendor,
            version = sensor.version,
            power = sensor.power,
            resolution = sensor.resolution,
            maxRange = sensor.maximumRange,
            minDelay = sensor.minDelay
        )
    }
}

/**
 * Sensor information
 */
data class SensorInfo(
    val type: SensorType,
    val name: String,
    val vendor: String,
    val version: Int,
    val power: Float,        // mA
    val resolution: Float,
    val maxRange: Float,
    val minDelay: Int        // microseconds
)

