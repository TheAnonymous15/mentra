package com.example.mentra.infrastructure.sensors.models

import com.example.mentra.core.common.ActivityType
import kotlin.math.pow

/**
 * Sensor data snapshot
 */
data class SensorData(
    val sensorType: SensorType,
    val values: FloatArray,
    val accuracy: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (sensorType != other.sensorType) return false
        if (!values.contentEquals(other.values)) return false
        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensorType.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Sensor types supported
 */
enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    STEP_COUNTER,
    STEP_DETECTOR,
    BAROMETER,
    PROXIMITY,
    LIGHT,
    TEMPERATURE,
    HUMIDITY,
    ACTIVITY_RECOGNITION
}

/**
 * Step data
 */
data class StepData(
    val steps: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dayOfYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
)

/**
 * Activity data
 */
data class ActivityData(
    val activityType: ActivityType,
    val confidence: Int, // 0-100
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Motion data from accelerometer
 */
data class MotionData(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromValues(values: FloatArray): MotionData {
            val x = values.getOrElse(0) { 0f }
            val y = values.getOrElse(1) { 0f }
            val z = values.getOrElse(2) { 0f }
            val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
            return MotionData(x, y, z, magnitude)
        }
    }
}

/**
 * Orientation data from gyroscope
 */
data class OrientationData(
    val azimuth: Float,  // Rotation around Z axis
    val pitch: Float,    // Rotation around X axis
    val roll: Float,     // Rotation around Y axis
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Barometer/pressure data
 */
data class PressureData(
    val pressure: Float,      // hPa
    val altitude: Float? = null,  // Calculated altitude in meters
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // Calculate altitude from pressure (standard atmosphere)
        fun calculateAltitude(pressure: Float, seaLevelPressure: Float = 1013.25f): Float {
            val ratio = pressure / seaLevelPressure
            val exponent = 1f / 5.255f
            return 44330f * (1f - ratio.pow(exponent))
        }
    }
}

/**
 * Sensor event with metadata
 */
data class SensorEvent(
    val sensorType: SensorType,
    val data: Any,  // StepData, ActivityData, MotionData, etc.
    val accuracy: SensorAccuracy,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sensor accuracy levels
 */
enum class SensorAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromInt(accuracy: Int): SensorAccuracy {
            return when (accuracy) {
                android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE -> UNRELIABLE
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
                else -> UNRELIABLE
            }
        }
    }
}

/**
 * Sensor configuration
 */
data class SensorConfig(
    val sensorType: SensorType,
    val samplingPeriodUs: Int = android.hardware.SensorManager.SENSOR_DELAY_NORMAL,
    val maxReportLatencyUs: Int = 0,
    val enabled: Boolean = true
)

/**
 * Sensor statistics
 */
data class SensorStats(
    val sensorType: SensorType,
    val eventsReceived: Long = 0,
    val lastEventTime: Long? = null,
    val averageFrequency: Float = 0f,  // Hz
    val isActive: Boolean = false
)

