package com.example.mentra.infrastructure.location.models

/**
 * Location data
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "unknown"
)

/**
 * Route/path data
 */
data class Route(
    val id: String,
    val name: String,
    val points: List<Location>,
    val distance: Float, // meters
    val duration: Long? = null, // milliseconds
    val startTime: Long,
    val endTime: Long? = null,
    val elevationGain: Float? = null,
    val elevationLoss: Float? = null
)

/**
 * Distance result
 */
data class DistanceResult(
    val meters: Float,
    val kilometers: Float = meters / 1000f,
    val miles: Float = meters * 0.000621371f
)

/**
 * Location provider type
 */
enum class LocationProviderType {
    GPS,
    NETWORK,
    FUSED,
    PASSIVE
}

/**
 * Location update request
 */
data class LocationRequest(
    val interval: Long = 10000, // ms
    val fastestInterval: Long = 5000, // ms
    val priority: LocationPriority = LocationPriority.HIGH_ACCURACY,
    val smallestDisplacement: Float = 0f // meters
)

/**
 * Location priority
 */
enum class LocationPriority {
    HIGH_ACCURACY,      // GPS + Network
    BALANCED_POWER,     // Network primarily
    LOW_POWER,          // Passive
    NO_POWER            // Only when other apps request location
}

