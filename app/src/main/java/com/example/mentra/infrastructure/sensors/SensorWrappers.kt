package com.example.mentra.infrastructure.sensors

import android.content.Context
import com.example.mentra.core.common.EventBus
import com.example.mentra.core.common.SystemEvent
import com.example.mentra.infrastructure.sensors.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Step counter sensor wrapper
 * Tracks steps and provides daily step count
 */
@Singleton
class StepCounterSensor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorManager: MentraSensorManager,
    private val eventBus: EventBus
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private var baselineSteps = 0
    private var currentDayOfYear = getCurrentDayOfYear()

    /**
     * Start tracking steps
     */
    fun startTracking() {
        sensorManager.startSensor(SensorType.STEP_COUNTER, ::onStepData)
    }

    /**
     * Stop tracking steps
     */
    fun stopTracking() {
        sensorManager.stopSensor(SensorType.STEP_COUNTER)
    }

    /**
     * Handle step data
     */
    private fun onStepData(data: SensorData) {
        val steps = data.values.firstOrNull()?.toInt() ?: return

        // Check if it's a new day
        val dayOfYear = getCurrentDayOfYear()
        if (dayOfYear != currentDayOfYear) {
            // New day - reset baseline
            baselineSteps = steps
            currentDayOfYear = dayOfYear
        }

        // Calculate daily steps
        val dailySteps = steps - baselineSteps

        _stepCount.value = steps
        _dailySteps.value = dailySteps

        // Emit event
        scope.launch {
            eventBus.emit(
                SystemEvent.Activity.StepCountUpdated(
                    steps = dailySteps,
                    distance = calculateDistance(dailySteps)
                )
            )
        }
    }

    /**
     * Calculate approximate distance from steps
     * Assumes average stride length of 0.78m
     */
    private fun calculateDistance(steps: Int): Double {
        val strideLength = 0.78 // meters
        return steps * strideLength
    }

    /**
     * Reset daily steps
     */
    fun resetDailySteps() {
        baselineSteps = _stepCount.value
        _dailySteps.value = 0
    }

    /**
     * Set baseline (for calibration)
     */
    fun setBaseline(steps: Int) {
        baselineSteps = steps
    }

    /**
     * Get current day of year
     */
    private fun getCurrentDayOfYear(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    }
}

/**
 * Accelerometer sensor wrapper
 * Detects motion and activity
 */
@Singleton
class AccelerometerSensor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorManager: MentraSensorManager,
    private val eventBus: EventBus
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _motionData = MutableStateFlow<MotionData?>(null)
    val motionData: StateFlow<MotionData?> = _motionData.asStateFlow()

    private val movementHistory = mutableListOf<Float>()
    private val maxHistorySize = 100

    /**
     * Start tracking motion
     */
    fun startTracking() {
        sensorManager.startSensor(SensorType.ACCELEROMETER, ::onMotionData)
    }

    /**
     * Stop tracking motion
     */
    fun stopTracking() {
        sensorManager.stopSensor(SensorType.ACCELEROMETER)
    }

    /**
     * Handle motion data
     */
    private fun onMotionData(data: SensorData) {
        val motion = MotionData.fromValues(data.values)
        _motionData.value = motion

        // Add to history
        movementHistory.add(motion.magnitude)
        if (movementHistory.size > maxHistorySize) {
            movementHistory.removeAt(0)
        }

        // Detect activity based on motion pattern
        val activity = detectActivity(motion.magnitude)

        scope.launch {
            eventBus.emit(
                SystemEvent.Activity.ActivityDetected(
                    type = activity,
                    confidence = (motion.magnitude * 20).coerceIn(0f, 100f)
                )
            )
        }
    }

    /**
     * Detect activity type based on motion magnitude
     */
    private fun detectActivity(magnitude: Float): com.example.mentra.core.common.ActivityType {
        return when {
            magnitude < 0.5f -> com.example.mentra.core.common.ActivityType.STILL
            magnitude < 2.0f -> com.example.mentra.core.common.ActivityType.WALKING
            magnitude < 5.0f -> com.example.mentra.core.common.ActivityType.RUNNING
            else -> com.example.mentra.core.common.ActivityType.UNKNOWN
        }
    }

    /**
     * Calculate average motion over recent history
     */
    fun getAverageMotion(): Float {
        if (movementHistory.isEmpty()) return 0f
        return movementHistory.average().toFloat()
    }

    /**
     * Check if device is moving
     */
    fun isMoving(threshold: Float = 1.0f): Boolean {
        return getAverageMotion() > threshold
    }
}

/**
 * Barometer/pressure sensor wrapper
 * Tracks elevation changes
 */
@Singleton
class BarometerSensor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorManager: MentraSensorManager
) {

    private val _pressureData = MutableStateFlow<PressureData?>(null)
    val pressureData: StateFlow<PressureData?> = _pressureData.asStateFlow()

    private var baselineAltitude: Float? = null

    /**
     * Start tracking pressure/altitude
     */
    fun startTracking() {
        sensorManager.startSensor(SensorType.BAROMETER, ::onPressureData)
    }

    /**
     * Stop tracking
     */
    fun stopTracking() {
        sensorManager.stopSensor(SensorType.BAROMETER)
    }

    /**
     * Handle pressure data
     */
    private fun onPressureData(data: SensorData) {
        val pressure = data.values.firstOrNull() ?: return
        val altitude = PressureData.calculateAltitude(pressure)

        val pressureData = PressureData(
            pressure = pressure,
            altitude = altitude
        )

        _pressureData.value = pressureData

        // Set baseline on first reading
        if (baselineAltitude == null) {
            baselineAltitude = altitude
        }
    }

    /**
     * Get elevation change from baseline
     */
    fun getElevationChange(): Float? {
        val current = _pressureData.value?.altitude ?: return null
        val baseline = baselineAltitude ?: return null
        return current - baseline
    }

    /**
     * Reset baseline altitude
     */
    fun resetBaseline() {
        baselineAltitude = _pressureData.value?.altitude
    }
}

/**
 * Sensor fusion - combines multiple sensors for better accuracy
 */
@Singleton
class SensorFusion @Inject constructor(
    private val stepCounter: StepCounterSensor,
    private val accelerometer: AccelerometerSensor,
    private val barometer: BarometerSensor
) {

    /**
     * Get comprehensive activity data
     */
    fun getActivityData(): FusedActivityData {
        return FusedActivityData(
            steps = stepCounter.dailySteps.value,
            isMoving = accelerometer.isMoving(),
            activityIntensity = accelerometer.getAverageMotion(),
            elevationChange = barometer.getElevationChange()
        )
    }

    /**
     * Start all sensors
     */
    fun startAllSensors() {
        stepCounter.startTracking()
        accelerometer.startTracking()
        barometer.startTracking()
    }

    /**
     * Stop all sensors
     */
    fun stopAllSensors() {
        stepCounter.stopTracking()
        accelerometer.stopTracking()
        barometer.stopTracking()
    }
}

/**
 * Fused activity data from multiple sensors
 */
data class FusedActivityData(
    val steps: Int,
    val isMoving: Boolean,
    val activityIntensity: Float,
    val elevationChange: Float?
)

