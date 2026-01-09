package com.example.mentra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mentra.core.common.permissions.PermissionManager
import com.example.mentra.dialer.ui.DialerScreen
import com.example.mentra.dialer.ui.InCallScreen
import com.example.mentra.health.ui.HealthScreen
import com.example.mentra.messaging.MessagePreloader
import com.example.mentra.messaging.ui.ConversationScreen
import com.example.mentra.messaging.ui.MessagingScreen
import com.example.mentra.navigation.ui.NavigationScreen
import com.example.mentra.ui.home.HomeScreen
import com.example.mentra.ui.permissions.PermissionSetupScreen
import com.example.mentra.shell.ui.ShellScreen
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var messagePreloader: MessagePreloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start preloading messages in background
        messagePreloader.startPreloading()

        setContent {
            MentraTheme {
                MentraApp(
                    permissionManager = permissionManager,
                    messagePreloader = messagePreloader
                )
            }
        }
    }
}

@Composable
fun MentraApp(
    permissionManager: PermissionManager,
    messagePreloader: MessagePreloader
) {
    val setupComplete by permissionManager.setupComplete.collectAsState()
    var showPermissionScreen by remember { mutableStateOf(!setupComplete) }
    var currentScreen by remember { mutableStateOf("home") }
    var conversationPhoneNumber by remember { mutableStateOf<String?>(null) }

    // Update when permissions change
    LaunchedEffect(setupComplete) {
        showPermissionScreen = !setupComplete
    }

    // Handle back button press - return to home from any screen
    BackHandler(enabled = currentScreen != "home" && !showPermissionScreen) {
        when (currentScreen) {
            "conversation" -> {
                conversationPhoneNumber = null
                currentScreen = "messaging"
            }
            "incall" -> {
                // Don't allow back from in-call screen
            }
            else -> {
                currentScreen = "home"
            }
        }
    }

    when {
        showPermissionScreen -> {
            PermissionSetupScreen(
                onSetupComplete = {
                    showPermissionScreen = false
                }
            )
        }
        currentScreen == "shell" -> {
            ShellScreen()
        }
        currentScreen == "health" -> {
            HealthScreen()
        }
        currentScreen == "navigation" -> {
            NavigationScreen()
        }
        currentScreen == "messaging" -> {
            MessagingScreen(
                onOpenConversation = { phoneNumber ->
                    conversationPhoneNumber = phoneNumber
                    currentScreen = "conversation"
                }
            )
        }
        currentScreen == "conversation" && conversationPhoneNumber != null -> {
            ConversationScreen(
                phoneNumber = conversationPhoneNumber!!,
                onBack = {
                    conversationPhoneNumber = null
                    currentScreen = "messaging"
                }
            )
        }
        currentScreen == "dialer" -> {
            DialerScreen(
                onNavigateToInCall = {
                    currentScreen = "incall"
                }
            )
        }
        currentScreen == "incall" -> {
            InCallScreen(
                onCallEnded = {
                    currentScreen = "dialer"
                }
            )
        }
        else -> {
            // Main home screen after permission setup
            HomeScreen(
                onNavigateToFeature = { featureId ->
                    when (featureId) {
                        "ai_shell" -> currentScreen = "shell"
                        "health" -> currentScreen = "health"
                        "navigation" -> currentScreen = "navigation"
                        "messaging" -> currentScreen = "messaging"
                        "dialer", "phone" -> currentScreen = "dialer"
                        else -> {
                            // Other features not implemented yet
                        }
                    }
                }
            )
        }
    }
}
