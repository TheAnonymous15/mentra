package com.example.mentra.dialer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.mentra.dialer.ui.InCallScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * In-Call Activity
 *
 * Activity that displays during an active call.
 * Manages proximity sensor and screen behavior.
 */
@AndroidEntryPoint
class InCallActivity : ComponentActivity() {

    @Inject
    lateinit var dialerManager: DialerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during call
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val callState by dialerManager.callState.collectAsState()

            // Close activity when call ends
            LaunchedEffect(callState) {
                if (callState == CallState.IDLE) {
                    finish()
                }
            }

            InCallScreen(
                onCallEnded = {
                    finish()
                }
            )
        }
    }

    override fun onBackPressed() {
        // Don't allow back button during call
        // User must use end call button
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

