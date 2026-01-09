package com.example.mentra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.mentra.core.common.ActivityType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Activity record entity for health tracking
 */
@Entity(tableName = "activity_records")
data class ActivityRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val activityType: ActivityType,
    val steps: Int,
    val distance: Double, // in meters
    val calories: Double,
    val duration: Long, // in milliseconds
    val confidence: Float
)

/**
 * Daily health stats aggregation
 */
@Entity(tableName = "health_stats")
data class HealthStatsEntity(
    @PrimaryKey
    val date: String, // LocalDate as string (YYYY-MM-DD)
    val totalSteps: Int,
    val totalDistance: Double,
    val totalCalories: Double,
    val activeMinutes: Int,
    val walkingMinutes: Int,
    val runningMinutes: Int,
    val cyclingMinutes: Int
)

/**
 * Sleep tracking data
 */
@Entity(tableName = "sleep_data")
data class SleepDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // LocalDate
    val sleepStart: Long,
    val sleepEnd: Long,
    val duration: Long, // in milliseconds
    val quality: Int // 1-10 scale
)

/**
 * Type converters for Room
 */
class Converters {

    @TypeConverter
    fun fromActivityType(value: ActivityType): String {
        return value.name
    }

    @TypeConverter
    fun toActivityType(value: String): ActivityType {
        return ActivityType.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

/**
 * Extension functions for date conversion
 */
fun LocalDate.toEpochMilli(): Long {
    return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun LocalDateTime.toEpochMilli(): Long {
    return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

