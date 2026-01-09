package com.example.mentra.infrastructure.privileged

import android.content.Context
import com.example.mentra.infrastructure.adb.ADBExecutor
import com.example.mentra.infrastructure.apis.AndroidAPIExecutor
import com.example.mentra.infrastructure.root.RootExecutor
import com.example.mentra.infrastructure.shizuku.ShizukuBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Privileged Command Executor
 * Tries multiple methods in order of preference:
 * 1. Shizuku (if available and authorized)
 * 2. Root (if device is rooted)
 * 3. ADB (if accessible)
 * 4. Android APIs (fallback - always works, limited functionality)
 */
@Singleton
class PrivilegedExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuBridge: ShizukuBridge,
    private val rootExecutor: RootExecutor,
    private val adbExecutor: ADBExecutor,
    private val androidAPIExecutor: AndroidAPIExecutor
) {

    private val _executionMethod = MutableStateFlow<ExecutionMethod>(ExecutionMethod.NONE)
    val executionMethod: StateFlow<ExecutionMethod> = _executionMethod.asStateFlow()

    init {
        detectAvailableMethod()
    }

    /**
     * Detect which execution method is available
     */
    private fun detectAvailableMethod() {
        _executionMethod.value = when {
            shizukuBridge.isRunning() && shizukuBridge.checkPermission() -> ExecutionMethod.SHIZUKU
            rootExecutor.hasRootAccess() -> ExecutionMethod.ROOT
            else -> ExecutionMethod.LIMITED
        }
    }

    /**
     * Execute privileged command using best available method
     */
    suspend fun execute(command: String, params: Map<String, String> = emptyMap()): Result<String> {
        // Try Shizuku first (most reliable for system commands)
        if (shizukuBridge.isRunning() && shizukuBridge.checkPermission()) {
            _executionMethod.value = ExecutionMethod.SHIZUKU
            val result = shizukuBridge.executeCommand(command)
            if (result.isSuccess) return result
        }

        // Try Root second (most powerful)
        if (rootExecutor.hasRootAccess()) {
            _executionMethod.value = ExecutionMethod.ROOT
            val result = rootExecutor.executeRootCommand(command)
            if (result.isSuccess) return result
        }

        // Try ADB third (for development)
        val adbResult = adbExecutor.executeADBCommand(command)
        if (adbResult.isSuccess) {
            _executionMethod.value = ExecutionMethod.ADB
            return adbResult
        }

        // Fallback to Android APIs (always works, but limited)
        _executionMethod.value = ExecutionMethod.LIMITED
        val apiResult = androidAPIExecutor.execute(command, params)

        if (apiResult.isSuccess) {
            return apiResult
        }

        // Nothing worked - provide helpful error
        return Result.failure(
            SecurityException(buildErrorMessage(command))
        )
    }

    /**
     * Build helpful error message based on command
     */
    private fun buildErrorMessage(command: String): String {
        val cmd = command.lowercase()

        return when {
            cmd.contains("reboot") || cmd.contains("shutdown") -> """
                Power commands require elevated privileges.
                
                Options:
                1. Install Shizuku (recommended): Play Store → Shizuku
                2. Root your device (advanced users only)
                3. Use 'settings power' to access power menu
            """.trimIndent()

            cmd.contains("wifi") || cmd.contains("bluetooth") || cmd.contains("airplane") -> """
                Network commands need privileges to change state.
                
                Try: 'open ${cmd}' to access ${cmd} settings instead.
                Or install Shizuku for full control.
            """.trimIndent()

            cmd.contains("brightness") -> """
                Direct brightness control requires privileges.
                
                Try: 'open brightness' to access display settings.
                Or install Shizuku for direct brightness control.
            """.trimIndent()

            else -> """
                Command '$command' requires elevated privileges.
                
                Solutions:
                1. Install Shizuku from Play Store (recommended)
                2. Root your device (advanced)
                3. Use 'open settings' for manual control
                
                Type 'help' to see commands that work without privileges.
            """.trimIndent()
        }
    }

    /**
     * Get status message for current method
     */
    fun getStatusMessage(): String {
        return when (_executionMethod.value) {
            ExecutionMethod.SHIZUKU -> "✓ Connected via Shizuku (Full access)"
            ExecutionMethod.ROOT -> "✓ Connected via Root (Full access)"
            ExecutionMethod.ADB -> "✓ Connected via ADB (Full access)"
            ExecutionMethod.LIMITED -> "⚠ Limited access - Using Android APIs only"
            ExecutionMethod.NONE -> "❌ No privileged access available"
        }
    }

    /**
     * Check if we have any privileged access
     */
    fun hasPrivilegedAccess(): Boolean {
        return _executionMethod.value != ExecutionMethod.NONE &&
               _executionMethod.value != ExecutionMethod.LIMITED
    }
}

/**
 * Available execution methods
 */
enum class ExecutionMethod {
    SHIZUKU,    // Via Shizuku (recommended)
    ROOT,       // Via root access
    ADB,        // Via ADB
    LIMITED,    // Limited Android APIs only
    NONE        // No access
}

