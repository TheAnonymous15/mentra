package com.example.mentra

import android.app.Application
import com.example.mentra.shell.apps.AppCacheService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class for Mentra ecosystem.
 * Initializes Hilt dependency injection.
 */
@HiltAndroidApp
class MentraApplication : Application() {

    @Inject
    lateinit var appCacheService: AppCacheService

    override fun onCreate() {
        super.onCreate()
        // Initialize app cache in background
        initializeAppCache()
    }

    private fun initializeAppCache() {
        // AppCacheService will scan all installed apps and cache them
        // This runs in the background and doesn't block app startup
        appCacheService.initialize()
    }
}

