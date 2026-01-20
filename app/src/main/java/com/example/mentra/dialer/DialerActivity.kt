package com.example.mentra.dialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.mentra.dialer.ui.DialerScreen
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * ═══════════════════════════════════════════════════════════════════
 * DIALER ACTIVITY
 * Main entry point for the Mentra Phone Dialer
 * Handles tel: intents and acts as the default dialer
 * ═══════════════════════════════════════════════════════════════════
 */
@AndroidEntryPoint
class DialerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Make the activity draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Extract phone number from intent if present
        val phoneNumber = extractPhoneNumber(intent)

        setContent {
            MentraTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DialerScreen(
                        initialNumber = phoneNumber,
                        onNavigateToInCall = {
                            startActivity(Intent(this, InCallActivity::class.java))
                        },
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle new tel: intent
        val phoneNumber = extractPhoneNumber(intent)
        if (phoneNumber != null) {
            // Update the dialer with new number
            // This will be handled by the ViewModel
        }
    }

    private fun extractPhoneNumber(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW, Intent.ACTION_CALL -> {
                intent.data?.schemeSpecificPart?.replace("-", "")?.replace(" ", "")
            }
            else -> null
        }
    }

    /**
     * Request to become the default dialer
     */
    fun requestDefaultDialer() {
        val telecomManager = getSystemService(TelecomManager::class.java)
        if (telecomManager != null && packageName != telecomManager.defaultDialerPackage) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    companion object {
        fun createIntent(context: android.content.Context, phoneNumber: String? = null): Intent {
            return Intent(context, DialerActivity::class.java).apply {
                if (phoneNumber != null) {
                    data = Uri.parse("tel:$phoneNumber")
                    action = Intent.ACTION_DIAL
                }
            }
        }
    }
}

