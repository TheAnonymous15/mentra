package com.example.mentra.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mentra.shell.ui.ShellScreen
import com.example.mentra.shell.apps.AppCacheService
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Launcher Activity - SHELL AS HOME
 * The Shell Terminal IS the home screen - command center for everything
 * No-UI control philosophy: Control everything via shell commands
 *
 * When Mentra is set as default launcher:
 * - Home button brings you to Shell
 * - Back button does nothing (you're home)
 * - All control is via terminal commands
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var appCacheService: AppCacheService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app cache
        appCacheService.initialize()

        // Disable back press - Shell IS home, nowhere to go back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Shell is home - back press does nothing
                // This prevents accidentally closing the launcher
            }
        })

        setContent {
            MentraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Shell IS the home screen - direct access to command center
                    ShellScreen(
                        appCacheService = appCacheService,
                        onNavigateToMessages = {
                            // Launch messaging activity
                            startActivity(Intent(this@LauncherActivity, com.example.mentra.MainActivity::class.java).apply {
                                putExtra("navigate_to", "messaging")
                            })
                        },
                        onNavigateToDialer = {
                            // Launch dialer activity
                            startActivity(Intent(this@LauncherActivity, com.example.mentra.MainActivity::class.java).apply {
                                putExtra("navigate_to", "dialer")
                            })
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle HOME button press (when already running)
        // Just brings shell to foreground - already at home
        if (intent.action == Intent.ACTION_MAIN) {
            // Already on shell/home - stays as is
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Called when user leaves via HOME or opens another app
        // Shell stays ready in background
    }
}

