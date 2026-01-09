package com.example.mentra

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for Mentra ecosystem.
 * Initializes Hilt dependency injection.
 */
@HiltAndroidApp
class MentraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any global components here
    }
}

