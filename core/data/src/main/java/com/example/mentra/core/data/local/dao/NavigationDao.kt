package com.example.mentra.core.data.local.dao

import androidx.room.*
import com.example.mentra.core.data.local.entity.POIEntity
import com.example.mentra.core.data.local.entity.RoutePointEntity
import com.example.mentra.core.data.local.entity.SavedRouteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for saved routes
 */
@Dao
interface SavedRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity): Long

    @Query("SELECT * FROM saved_routes ORDER BY lastUsed DESC")
    fun getAllRoutes(): Flow<List<SavedRouteEntity>>

    @Query("SELECT * FROM saved_routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): SavedRouteEntity?

    @Query("UPDATE saved_routes SET lastUsed = :timestamp WHERE id = :routeId")
    suspend fun updateLastUsed(routeId: Long, timestamp: Long)

    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)
}

/**
 * DAO for route points
 */
@Dao
interface RoutePointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: RoutePointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY sequence ASC")
    suspend fun getPointsForRoute(routeId: Long): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePointsForRoute(routeId: Long)
}

/**
 * DAO for Points of Interest
 */
@Dao
interface POIDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOI(poi: POIEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOIs(pois: List<POIEntity>)

    @Query("SELECT * FROM poi WHERE isFavorite = 1")
    fun getFavoritePOIs(): Flow<List<POIEntity>>

    @Query("SELECT * FROM poi WHERE category = :category")
    fun getPOIsByCategory(category: String): Flow<List<POIEntity>>

    @Query("SELECT * FROM poi")
    fun getAllPOIs(): Flow<List<POIEntity>>

    @Query("UPDATE poi SET isFavorite = :favorite WHERE id = :poiId")
    suspend fun updateFavorite(poiId: Long, favorite: Boolean)

    @Delete
    suspend fun deletePOI(poi: POIEntity)
}

