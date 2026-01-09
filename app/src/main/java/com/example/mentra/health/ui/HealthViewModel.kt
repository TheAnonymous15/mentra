package com.example.mentra.health.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.health.ActivityState
import com.example.mentra.health.HealthDataManager
import com.example.mentra.health.HealthMetrics
import com.example.mentra.health.HealthSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Health Screen
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthDataManager: HealthDataManager
) : ViewModel() {

    val healthMetrics: StateFlow<HealthMetrics> = healthDataManager.healthMetrics

    private val _healthSummary = MutableStateFlow(HealthSummary(
        steps = 0,
        distance = 0f,
        calories = 0,
        heartRate = 0,
        activityLevel = "Idle",
        healthScore = 0
    ))
    val healthSummary: StateFlow<HealthSummary> = _healthSummary.asStateFlow()

    private val _activityState = MutableStateFlow("IDLE")
    val activityState: StateFlow<String> = _activityState.asStateFlow()

    init {
        collectHealthData()
    }

    /**
     * Collect health data updates
     */
    private fun collectHealthData() {
        viewModelScope.launch {
            healthDataManager.healthMetrics.collect { metrics ->
                updateHealthSummary()
            }
        }

        viewModelScope.launch {
            healthDataManager.activityState.collect { state ->
                _activityState.value = state.name
            }
        }
    }

    /**
     * Update health summary
     */
    private fun updateHealthSummary() {
        _healthSummary.value = healthDataManager.getHealthSummary()
    }

    /**
     * Reset daily stats
     */
    fun resetDailyStats() {
        healthDataManager.resetDailyCounters()
        updateHealthSummary()
    }

    override fun onCleared() {
        super.onCleared()
        healthDataManager.cleanup()
    }
}

