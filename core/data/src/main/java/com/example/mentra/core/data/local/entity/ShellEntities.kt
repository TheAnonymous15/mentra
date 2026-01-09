package com.example.mentra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Shell command history
 */
@Entity(tableName = "shell_history")
data class ShellHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val originalLanguage: String?,
    val translatedCommand: String?,
    val result: String,
    val success: Boolean,
    val timestamp: Long
)

/**
 * Shell aliases (shortcuts)
 */
@Entity(tableName = "shell_aliases")
data class ShellAliasEntity(
    @PrimaryKey
    val alias: String,
    val target: String,
    val description: String?,
    val createdAt: Long
)

/**
 * Shell scripts
 */
@Entity(tableName = "shell_scripts")
data class ShellScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val executionCount: Int = 0,
    val lastExecuted: Long? = null
)

/**
 * Shell automation triggers
 */
@Entity(tableName = "shell_triggers")
data class ShellTriggerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val triggerType: String, // ON_BOOT, ON_HEADPHONES_PLUGGED, etc.
    val scriptId: Long,
    val enabled: Boolean = true,
    val conditions: String? // JSON conditions
)

/**
 * User profile
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1, // Single row
    val name: String?,
    val height: Double?, // in cm
    val weight: Double?, // in kg
    val age: Int?,
    val gender: String?,
    val dailyStepGoal: Int = 10000,
    val useMetricSystem: Boolean = true,
    val theme: String = "SYSTEM",
    val language: String = "en"
)

