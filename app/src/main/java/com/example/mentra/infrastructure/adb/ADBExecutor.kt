package com.example.mentra.infrastructure.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ADB WiFi Executor
 * Execute commands via ADB over WiFi
 * Requires: USB debugging enabled and one-time pairing
 */
@Singleton
class ADBExecutor @Inject constructor() {

    /**
     * Execute command via local ADB
     * Note: This only works if ADB server is running on the device itself
     * (e.g., via Termux or similar)
     */
    suspend fun executeADBCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(RuntimeException("Command failed: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

