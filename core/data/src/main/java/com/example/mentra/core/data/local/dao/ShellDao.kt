package com.example.mentra.core.data.local.dao

import androidx.room.*
import com.example.mentra.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for shell command history
 */
@Dao
interface ShellHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ShellHistoryEntity): Long

    @Query("SELECT * FROM shell_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<ShellHistoryEntity>>

    @Query("SELECT * FROM shell_history WHERE command LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<ShellHistoryEntity>>

    @Query("DELETE FROM shell_history WHERE timestamp < :beforeTime")
    suspend fun deleteOldHistory(beforeTime: Long)

    @Query("DELETE FROM shell_history")
    suspend fun clearHistory()
}

/**
 * DAO for shell aliases
 */
@Dao
interface ShellAliasDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: ShellAliasEntity)

    @Query("SELECT * FROM shell_aliases ORDER BY alias ASC")
    fun getAllAliases(): Flow<List<ShellAliasEntity>>

    @Query("SELECT * FROM shell_aliases WHERE alias = :aliasName")
    suspend fun getAlias(aliasName: String): ShellAliasEntity?

    @Delete
    suspend fun deleteAlias(alias: ShellAliasEntity)
}

/**
 * DAO for shell scripts
 */
@Dao
interface ShellScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ShellScriptEntity): Long

    @Query("SELECT * FROM shell_scripts ORDER BY name ASC")
    fun getAllScripts(): Flow<List<ShellScriptEntity>>

    @Query("SELECT * FROM shell_scripts WHERE id = :scriptId")
    suspend fun getScriptById(scriptId: Long): ShellScriptEntity?

    @Query("UPDATE shell_scripts SET executionCount = executionCount + 1, lastExecuted = :timestamp WHERE id = :scriptId")
    suspend fun incrementExecutionCount(scriptId: Long, timestamp: Long)

    @Update
    suspend fun updateScript(script: ShellScriptEntity)

    @Delete
    suspend fun deleteScript(script: ShellScriptEntity)
}

/**
 * DAO for automation triggers
 */
@Dao
interface ShellTriggerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: ShellTriggerEntity): Long

    @Query("SELECT * FROM shell_triggers WHERE enabled = 1")
    fun getEnabledTriggers(): Flow<List<ShellTriggerEntity>>

    @Query("SELECT * FROM shell_triggers WHERE triggerType = :type AND enabled = 1")
    fun getTriggersByType(type: String): Flow<List<ShellTriggerEntity>>

    @Query("UPDATE shell_triggers SET enabled = :enabled WHERE id = :triggerId")
    suspend fun updateTriggerEnabled(triggerId: Long, enabled: Boolean)

    @Delete
    suspend fun deleteTrigger(trigger: ShellTriggerEntity)
}

/**
 * DAO for user profile
 */
@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
}

