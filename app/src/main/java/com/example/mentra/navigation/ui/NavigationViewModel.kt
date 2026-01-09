package com.example.mentra.navigation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.navigation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation ViewModel
 * Manages navigation state and operations
 */
@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val navigationEngine: NavigationEngine
) : ViewModel() {

    val currentLocation: StateFlow<NavigationLocation?> = navigationEngine.currentLocation

    private val _mapType = MutableStateFlow(MapType.HYBRID)
    val mapType: StateFlow<MapType> = _mapType.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _selectedDestination = MutableStateFlow<NavigationLocation?>(null)
    val selectedDestination: StateFlow<NavigationLocation?> = _selectedDestination.asStateFlow()

    private val _routeInfo = MutableStateFlow<RouteStatistics?>(null)
    val routeInfo: StateFlow<RouteStatistics?> = _routeInfo.asStateFlow()

    private val _recordedWaypoints = MutableStateFlow<List<NavigationLocation>>(emptyList())

    /**
     * Start/stop location tracking
     */
    fun toggleTracking() {
        viewModelScope.launch {
            if (_isTracking.value) {
                // Stop tracking
                _isTracking.value = false
                navigationEngine.cleanup()
            } else {
                // Start tracking
                _isTracking.value = true
                navigationEngine.startTracking()
            }
        }
    }

    /**
     * Set map type
     */
    fun setMapType(type: MapType) {
        _mapType.value = type
    }

    /**
     * Navigate to a POI
     */
    fun navigateTo(poi: PointOfInterest) {
        _selectedDestination.value = NavigationLocation(
            latitude = poi.latitude,
            longitude = poi.longitude
        )
        calculateRoute()
    }

    /**
     * Calculate route statistics
     */
    private fun calculateRoute() {
        viewModelScope.launch {
            val current = currentLocation.value
            val destination = _selectedDestination.value

            if (current != null && destination != null) {
                val waypoints = listOf(current, destination)
                _routeInfo.value = navigationEngine.calculateRouteStats(waypoints)
            }
        }
    }

    /**
     * Center map on current location
     */
    fun centerOnCurrentLocation() {
        // Handled by map view
    }

    /**
     * Start recording route
     */
    fun startRouteRecording() {
        viewModelScope.launch {
            // Start collecting waypoints
            currentLocation.collect { location ->
                location?.let {
                    val waypoints = _recordedWaypoints.value.toMutableList()
                    waypoints.add(it)
                    _recordedWaypoints.value = waypoints

                    // Update route stats
                    if (waypoints.size >= 2) {
                        _routeInfo.value = navigationEngine.calculateRouteStats(waypoints)
                    }
                }
            }
        }
    }

    /**
     * Toggle compass
     */
    fun toggleCompass() {
        // Handled by map view
    }

    /**
     * Map ready callback
     */
    fun onMapReady() {
        // Map is ready
    }

    override fun onCleared() {
        super.onCleared()
        navigationEngine.cleanup()
    }
}

