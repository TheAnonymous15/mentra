package com.example.mentra.core.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Extension functions for Context
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasPermissions(vararg permissions: String): Boolean {
    return permissions.all { hasPermission(it) }
}

/**
 * Extension functions for numbers
 */
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun Float.roundTo(decimals: Int): Float {
    return this.toDouble().roundTo(decimals).toFloat()
}

/**
 * Extension functions for collections
 */
fun <T> List<T>.secondOrNull(): T? {
    return if (this.size >= 2) this[1] else null
}

fun <T> List<T>.thirdOrNull(): T? {
    return if (this.size >= 3) this[2] else null
}

/**
 * Time utilities
 */
object TimeUtils {
    const val SECOND_MILLIS = 1000L
    const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    const val DAY_MILLIS = 24 * HOUR_MILLIS

    fun formatDuration(millis: Long): String {
        val hours = millis / HOUR_MILLIS
        val minutes = (millis % HOUR_MILLIS) / MINUTE_MILLIS
        val seconds = (millis % MINUTE_MILLIS) / SECOND_MILLIS

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds)
            else -> String.format("0:%02d", seconds)
        }
    }

    fun formatETA(millis: Long): String {
        val hours = millis / HOUR_MILLIS
        val minutes = (millis % HOUR_MILLIS) / MINUTE_MILLIS

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}

/**
 * Distance utilities
 */
object DistanceUtils {
    const val METERS_PER_KM = 1000.0
    const val METERS_PER_MILE = 1609.34

    fun formatDistance(meters: Double, useMetric: Boolean = true): String {
        return if (useMetric) {
            when {
                meters < 1000 -> "${meters.toInt()} m"
                else -> "${(meters / METERS_PER_KM).roundTo(1)} km"
            }
        } else {
            val miles = meters / METERS_PER_MILE
            when {
                miles < 0.1 -> "${(meters * 3.281).toInt()} ft"
                else -> "${miles.roundTo(1)} mi"
            }
        }
    }
}

/**
 * File size utilities
 */
object FileSizeUtils {
    fun formatFileSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> "${(bytes / kb).roundTo(1)} KB"
            bytes < gb -> "${(bytes / mb).roundTo(1)} MB"
            else -> "${(bytes / gb).roundTo(1)} GB"
        }
    }
}

