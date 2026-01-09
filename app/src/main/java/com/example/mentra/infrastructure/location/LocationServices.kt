package com.example.mentra.infrastructure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.mentra.core.common.EventBus
import com.example.mentra.core.common.SystemEvent
import com.example.mentra.infrastructure.location.models.*
import com.google.android.gms.location.*
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
import kotlin.math.*

/**
 * Unified location provider
 * Abstracts GPS and Network location
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventBus: EventBus
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var locationCallback: LocationCallback? = null

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get last known location
     */
    suspend fun getLastLocation(): Result<Location> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val mentraLocation = Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    accuracy = location.accuracy,
                    speed = if (location.hasSpeed()) location.speed else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    timestamp = location.time,
                    provider = location.provider ?: "unknown"
                )
                Result.success(mentraLocation)
            } else {
                Result.failure(Exception("No last known location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start location tracking
     */
    fun startTracking(request: com.example.mentra.infrastructure.location.models.LocationRequest = com.example.mentra.infrastructure.location.models.LocationRequest()) {
        if (!hasLocationPermission()) {
            return
        }

        if (_isTracking.value) {
            stopTracking()
        }

        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            interval = request.interval
            fastestInterval = request.fastestInterval
            smallestDisplacement = request.smallestDisplacement
            priority = mapPriority(request.priority)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val mentraLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = location.accuracy,
                        speed = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null,
                        timestamp = location.time,
                        provider = location.provider ?: "fused"
                    )

                    _currentLocation.value = mentraLocation

                    // Emit event
                    scope.launch {
                        eventBus.emit(SystemEvent.Navigation.LocationUpdated(
                            latitude = mentraLocation.latitude,
                            longitude = mentraLocation.longitude
                        ))
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (e: SecurityException) {
            // Permission revoked
            _isTracking.value = false
        }
    }

    /**
     * Stop location tracking
     */
    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        _isTracking.value = false
    }

    /**
     * Map priority to Google Play Services constant
     */
    private fun mapPriority(priority: LocationPriority): Int {
        return when (priority) {
            LocationPriority.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY
            LocationPriority.BALANCED_POWER -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            LocationPriority.LOW_POWER -> Priority.PRIORITY_LOW_POWER
            LocationPriority.NO_POWER -> Priority.PRIORITY_PASSIVE
        }
    }
}

/**
 * Extension to await Google Play Services Task
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resumeWith(Result.success(result))
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(Result.failure(exception))
        }
    }
}

/**
 * Distance calculator using Haversine formula
 */
@Singleton
class DistanceCalculator @Inject constructor() {

    private val EARTH_RADIUS = 6371000.0 // meters

    /**
     * Calculate distance between two locations
     */
    fun calculateDistance(from: Location, to: Location): DistanceResult {
        val meters = haversine(
            from.latitude, from.longitude,
            to.latitude, to.longitude
        )
        return DistanceResult(meters = meters.toFloat())
    }

    /**
     * Haversine formula for distance calculation
     */
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }

    /**
     * Calculate total distance of a route
     */
    fun calculateRouteDistance(points: List<Location>): Float {
        if (points.size < 2) return 0f

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += haversine(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }

        return totalDistance.toFloat()
    }
}

/**
 * Route tracker - records location points over time
 */
@Singleton
class RouteTracker @Inject constructor(
    private val locationProvider: LocationProvider,
    private val distanceCalculator: DistanceCalculator
) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val recordedPoints = mutableListOf<Location>()
    private var startTime: Long = 0

    /**
     * Start recording route
     */
    fun startRecording() {
        if (_isRecording.value) return

        recordedPoints.clear()
        startTime = System.currentTimeMillis()
        _isRecording.value = true

        // Start location tracking
        locationProvider.startTracking(
            com.example.mentra.infrastructure.location.models.LocationRequest(
                interval = 5000, // 5 seconds
                fastestInterval = 2000,
                priority = com.example.mentra.infrastructure.location.models.LocationPriority.HIGH_ACCURACY,
                smallestDisplacement = 5f // 5 meters
            )
        )
    }

    /**
     * Stop recording and return route
     */
    fun stopRecording(name: String = "Recorded Route"): Route? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        locationProvider.stopTracking()

        if (recordedPoints.isEmpty()) return null

        val distance = distanceCalculator.calculateRouteDistance(recordedPoints)
        val endTime = System.currentTimeMillis()

        return Route(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            points = recordedPoints.toList(),
            distance = distance,
            duration = endTime - startTime,
            startTime = startTime,
            endTime = endTime
        )
    }

    /**
     * Add point to current route
     */
    fun addPoint(location: Location) {
        if (_isRecording.value) {
            recordedPoints.add(location)
        }
    }

    /**
     * Get current route points
     */
    fun getCurrentPoints(): List<Location> {
        return recordedPoints.toList()
    }

    /**
     * Get current distance
     */
    fun getCurrentDistance(): Float {
        return distanceCalculator.calculateRouteDistance(recordedPoints)
    }
}

