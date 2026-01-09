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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mentra.shell.ui.ShellScreen
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher Activity
 * Main entry point when Mentra is set as the default launcher
 * Handles system navigation: Home, Back, Recents buttons
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MentraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    // Handle back press based on current route
                    LaunchedEffect(currentRoute) {
                        val callback = object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                when (currentRoute) {
                                    "launcher" -> {
                                        // On launcher home screen - do nothing
                                        // Launcher should NOT close on back press
                                    }
                                    "shell" -> {
                                        // In shell - go back to launcher
                                        navController.popBackStack()
                                    }
                                    else -> {
                                        // Any other screen - navigate back
                                        if (!navController.popBackStack()) {
                                            // Nothing to pop - stay on launcher
                                        }
                                    }
                                }
                            }
                        }
                        onBackPressedDispatcher.addCallback(callback)
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "launcher"
                    ) {
                        composable("launcher") {
                            LauncherScreen(
                                onNavigateToShell = {
                                    navController.navigate("shell")
                                }
                            )
                        }

                        composable("shell") {
                            ShellScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle HOME button press (when launcher is already running)
        // This brings the launcher to foreground
        if (intent.action == Intent.ACTION_MAIN) {
            // Already on launcher - do nothing or reset to home screen
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Called when user presses HOME to leave the launcher
        // Or opens another app
        // Save state if needed
    }
}

