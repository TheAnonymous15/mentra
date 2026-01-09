package com.example.mentra.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Data Manager
 * Collects and manages all health-related data
 */
@Singleton
class HealthDataManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _healthMetrics = MutableStateFlow(HealthMetrics())
    val healthMetrics: StateFlow<HealthMetrics> = _healthMetrics.asStateFlow()

    private val _activityState = MutableStateFlow(ActivityState.IDLE)
    val activityState: StateFlow<ActivityState> = _activityState.asStateFlow()

    private var stepCounterOffset = 0
    private var lastStepCount = 0

    init {
        initializeSensors()
    }

    /**
     * Initialize all health sensors
     */
    private fun initializeSensors() {
        // Step Counter
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Heart Rate (if available)
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Activity Detection
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                handleStepCount(event.values[0].toInt())
            }
            Sensor.TYPE_HEART_RATE -> {
                handleHeartRate(event.values[0])
            }
            Sensor.TYPE_SIGNIFICANT_MOTION -> {
                _activityState.value = ActivityState.MOVING
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    /**
     * Handle step count updates
     */
    private fun handleStepCount(totalSteps: Int) {
        if (lastStepCount == 0) {
            // First reading - set offset
            stepCounterOffset = totalSteps
            lastStepCount = totalSteps
            return
        }

        val dailySteps = totalSteps - stepCounterOffset
        val newSteps = totalSteps - lastStepCount

        _healthMetrics.value = _healthMetrics.value.copy(
            steps = dailySteps,
            calories = calculateCalories(dailySteps),
            distance = calculateDistance(dailySteps)
        )

        lastStepCount = totalSteps

        // Detect activity based on step frequency
        if (newSteps > 0) {
            _activityState.value = ActivityState.WALKING
        }
    }

    /**
     * Handle heart rate updates
     */
    private fun handleHeartRate(bpm: Float) {
        _healthMetrics.value = _healthMetrics.value.copy(
            heartRate = bpm.toInt()
        )

        // Update activity state based on heart rate
        _activityState.value = when {
            bpm < 60 -> ActivityState.RESTING
            bpm < 100 -> ActivityState.WALKING
            bpm < 140 -> ActivityState.JOGGING
            else -> ActivityState.RUNNING
        }
    }

    /**
     * Calculate calories burned from steps
     */
    private fun calculateCalories(steps: Int): Int {
        // Average: 0.04 calories per step
        return (steps * 0.04).toInt()
    }

    /**
     * Calculate distance from steps
     */
    private fun calculateDistance(steps: Int): Float {
        // Average stride: 0.762 meters
        return (steps * 0.762f) / 1000f // in kilometers
    }

    /**
     * Reset daily counters (call at midnight)
     */
    fun resetDailyCounters() {
        stepCounterOffset = lastStepCount
        _healthMetrics.value = HealthMetrics()
    }

    /**
     * Get health summary
     */
    fun getHealthSummary(): HealthSummary {
        val metrics = _healthMetrics.value
        return HealthSummary(
            steps = metrics.steps,
            distance = metrics.distance,
            calories = metrics.calories,
            heartRate = metrics.heartRate,
            activityLevel = calculateActivityLevel(),
            healthScore = calculateHealthScore()
        )
    }

    /**
     * Calculate overall activity level
     */
    private fun calculateActivityLevel(): String {
        val steps = _healthMetrics.value.steps
        return when {
            steps < 2000 -> "Sedentary"
            steps < 5000 -> "Lightly Active"
            steps < 8000 -> "Moderately Active"
            steps < 12000 -> "Very Active"
            else -> "Extremely Active"
        }
    }

    /**
     * Calculate health score (0-100)
     */
    private fun calculateHealthScore(): Int {
        val metrics = _healthMetrics.value
        var score = 0

        // Steps contribution (40 points max)
        score += (metrics.steps.coerceAtMost(10000) / 250)

        // Heart rate contribution (30 points max)
        if (metrics.heartRate in 60..100) {
            score += 30
        } else if (metrics.heartRate in 50..120) {
            score += 20
        } else {
            score += 10
        }

        // Activity variety (30 points max)
        score += when (_activityState.value) {
            ActivityState.IDLE, ActivityState.RESTING -> 10
            ActivityState.WALKING -> 20
            ActivityState.JOGGING, ActivityState.RUNNING -> 30
            else -> 15
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Clean up
     */
    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}

/**
 * Current health metrics
 */
data class HealthMetrics(
    val steps: Int = 0,
    val distance: Float = 0f, // in km
    val calories: Int = 0,
    val heartRate: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Health summary for display
 */
data class HealthSummary(
    val steps: Int,
    val distance: Float,
    val calories: Int,
    val heartRate: Int,
    val activityLevel: String,
    val healthScore: Int
)

/**
 * Activity states
 */
enum class ActivityState {
    IDLE,
    RESTING,
    WALKING,
    JOGGING,
    RUNNING,
    MOVING
}

