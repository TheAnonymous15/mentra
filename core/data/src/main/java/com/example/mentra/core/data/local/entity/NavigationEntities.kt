package com.example.mentra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Saved route for navigation
 */
@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val distance: Double, // in meters
    val estimatedTime: Long, // in milliseconds
    val createdAt: Long,
    val lastUsed: Long
)

/**
 * Individual route point/waypoint
 */
@Entity(tableName = "route_points")
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int,
    val instruction: String?
)

/**
 * Point of Interest
 */
@Entity(tableName = "poi")
data class POIEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val rating: Float?,
    val isFavorite: Boolean = false
)

