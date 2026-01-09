package com.example.mentra.dialer

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Incoming Call Activity
 *
 * Fullscreen activity that shows on lock screen and during Doze.
 * Must work reliably in all scenarios.
 */
@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    @Inject
    lateinit var dialerManager: DialerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity show over lock screen
        setupLockScreenBehavior()

        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"

        setContent {
            IncomingCallScreen(
                phoneNumber = phoneNumber,
                onAnswer = {
                    dialerManager.answerCall()
                    // Launch InCallActivity
                    startActivity(android.content.Intent(this, InCallActivity::class.java))
                    finish()
                },
                onDecline = {
                    dialerManager.rejectCall()
                    finish()
                }
            )
        }
    }

    private fun setupLockScreenBehavior() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onBackPressed() {
        // Don't allow back button to dismiss incoming call
    }
}

@Composable
fun IncomingCallScreen(
    phoneNumber: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E27),
                        Color(0xFF1A1F3A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Incoming call text
            Text(
                text = "Incoming Call",
                color = Color(0xFF4EC9B0),
                fontSize = 18.sp,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Caller avatar
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(20.dp, CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4EC9B0), Color(0xFF2A8A7A))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phoneNumber.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phone number
            Text(
                text = phoneNumber,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Answer/Decline buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decline button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = onDecline,
                        containerColor = Color.Transparent,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFE57373), Color(0xFFB71C1C))
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Decline",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }

                // Answer button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = onAnswer,
                        containerColor = Color.Transparent,
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF4EC9B0), Color(0xFF2A8A7A))
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Answer",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Answer",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

