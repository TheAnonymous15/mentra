package com.example.mentra.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mentra.core.data.local.dao.*
import com.example.mentra.core.data.local.entity.*

/**
 * Main Room database for Mentra app
 */
@Database(
    entities = [
        // Health & Activity
        ActivityRecordEntity::class,
        HealthStatsEntity::class,
        SleepDataEntity::class,

        // Navigation
        SavedRouteEntity::class,
        RoutePointEntity::class,
        POIEntity::class,

        // Media
        MediaItemEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,

        // Shell & Automation
        ShellHistoryEntity::class,
        ShellAliasEntity::class,
        ShellScriptEntity::class,
        ShellTriggerEntity::class,

        // User
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MentraDatabase : RoomDatabase() {

    // Health DAOs
    abstract fun activityDao(): ActivityDao
    abstract fun healthStatsDao(): HealthStatsDao
    abstract fun sleepDao(): SleepDao

    // Navigation DAOs
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun routePointDao(): RoutePointDao
    abstract fun poiDao(): POIDao

    // Media DAOs
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao

    // Shell DAOs
    abstract fun shellHistoryDao(): ShellHistoryDao
    abstract fun shellAliasDao(): ShellAliasDao
    abstract fun shellScriptDao(): ShellScriptDao
    abstract fun shellTriggerDao(): ShellTriggerDao

    // User DAO
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val DATABASE_NAME = "mentra_database"
    }
}

