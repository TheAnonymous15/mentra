package com.example.mentra.core.data.local.dao

import androidx.room.*
import com.example.mentra.core.data.local.entity.ActivityRecordEntity
import com.example.mentra.core.data.local.entity.HealthStatsEntity
import com.example.mentra.core.data.local.entity.SleepDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for activity tracking
 */
@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityRecordEntity>)

    @Query("SELECT * FROM activity_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getActivitiesInRange(startTime: Long, endTime: Long): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getActivitiesSince(startTime: Long): Flow<List<ActivityRecordEntity>>

    @Query("DELETE FROM activity_records WHERE timestamp < :beforeTime")
    suspend fun deleteOldActivities(beforeTime: Long)
}

/**
 * DAO for daily health stats
 */
@Dao
interface HealthStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: HealthStatsEntity)

    @Query("SELECT * FROM health_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): HealthStatsEntity?

    @Query("SELECT * FROM health_stats WHERE date = :date")
    fun getStatsForDateFlow(date: String): Flow<HealthStatsEntity?>

    @Query("SELECT * FROM health_stats ORDER BY date DESC LIMIT :limit")
    fun getRecentStats(limit: Int = 30): Flow<List<HealthStatsEntity>>

    @Query("SELECT * FROM health_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStatsInRange(startDate: String, endDate: String): Flow<List<HealthStatsEntity>>

    @Delete
    suspend fun deleteStats(stats: HealthStatsEntity)
}

/**
 * DAO for sleep tracking
 */
@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(sleep: SleepDataEntity): Long

    @Query("SELECT * FROM sleep_data WHERE date = :date")
    suspend fun getSleepForDate(date: String): SleepDataEntity?

    @Query("SELECT * FROM sleep_data ORDER BY date DESC LIMIT :limit")
    fun getRecentSleep(limit: Int = 30): Flow<List<SleepDataEntity>>

    @Delete
    suspend fun deleteSleep(sleep: SleepDataEntity)
}

