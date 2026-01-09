package com.example.mentra.infrastructure.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root command executor
 * Alternative to Shizuku - requires rooted device
 */
@Singleton
class RootExecutor @Inject constructor() {

    private var isRootAvailable: Boolean = false

    init {
        checkRootAccess()
    }

    /**
     * Check if device has root access
     */
    private fun checkRootAccess() {
        isRootAvailable = try {
            val process = Runtime.getRuntime().exec("su")
            val output = DataOutputStream(process.outputStream)
            output.writeBytes("id\n")
            output.writeBytes("exit\n")
            output.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute command as root
     */
    suspend fun executeRootCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isRootAvailable) {
            return@withContext Result.failure(SecurityException("Root access not available"))
        }

        try {
            val process = Runtime.getRuntime().exec("su")
            val output = DataOutputStream(process.outputStream)

            output.writeBytes("$command\n")
            output.writeBytes("exit\n")
            output.flush()

            val result = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(result)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(RuntimeException("Command failed: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun hasRootAccess(): Boolean = isRootAvailable
}

