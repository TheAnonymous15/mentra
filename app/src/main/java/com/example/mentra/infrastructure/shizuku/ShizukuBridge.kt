package com.example.mentra.infrastructure.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main bridge to Shizuku for system-level operations
 * Provides safe access to privileged Android APIs without root
 */
@Singleton
class ShizukuBridge @Inject constructor() {

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkAvailability()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isAvailable.value = false
        _isPermissionGranted.value = false
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _isPermissionGranted.value = grantResult == PackageManager.PERMISSION_GRANTED
    }

    init {
        registerListeners()
        checkAvailability()
    }

    /**
     * Register Shizuku listeners
     */
    private fun registerListeners() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
    }

    /**
     * Unregister listeners (call in onDestroy)
     */
    fun unregisterListeners() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    /**
     * Check if Shizuku is available and we have permission
     */
    private fun checkAvailability() {
        _isAvailable.value = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

        if (_isAvailable.value) {
            _isPermissionGranted.value = checkPermission()
        }
    }

    /**
     * Check if we have Shizuku permission
     */
    fun checkPermission(): Boolean {
        return if (_isAvailable.value) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Request Shizuku permission
     */
    fun requestPermission() {
        if (!_isAvailable.value) {
            return
        }

        try {
            if (Shizuku.isPreV11()) {
                // For Shizuku pre-v11
                Shizuku.requestPermission(0)
            } else {
                // For Shizuku v11+
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(0)
                }
            }
        } catch (e: Exception) {
            // Permission request failed
        }
    }

    /**
     * Check if Shizuku is running
     */
    fun isRunning(): Boolean {
        return _isAvailable.value
    }

    /**
     * Execute command with Shizuku
     * Returns output or null if failed
     */
    suspend fun executeCommand(command: String): Result<String> {
        if (!_isAvailable.value) {
            return Result.failure(UnsupportedOperationException("Shizuku not running. Please start Shizuku app."))
        }

        if (!_isPermissionGranted.value) {
            return Result.failure(SecurityException("Shizuku permission not granted. Please authorize Mentra in Shizuku app."))
        }

        return try {
            // Use Shizuku to execute shell command
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(RuntimeException("Command failed (exit $exitCode): ${error.ifEmpty { "Unknown error" }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

