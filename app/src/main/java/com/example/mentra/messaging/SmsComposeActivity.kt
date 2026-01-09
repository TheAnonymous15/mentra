package com.example.mentra.messaging

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.ui.theme.MentraTheme

/**
 * SMS Compose Activity
 * Handles sms:, smsto:, mms:, mmsto: intents
 * Required for default SMS app functionality
 */
class SmsComposeActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipient = extractRecipient(intent)
        val body = intent.getStringExtra(Intent.EXTRA_TEXT) ?:
                   intent.getStringExtra("sms_body") ?: ""

        setContent {
            MentraTheme {
                ComposeScreen(
                    initialRecipient = recipient,
                    initialBody = body,
                    onSend = { number, message ->
                        sendMessage(number, message)
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun extractRecipient(intent: Intent): String {
        val data = intent.data
        return when {
            data != null -> {
                val scheme = data.scheme
                if (scheme in listOf("sms", "smsto", "mms", "mmsto")) {
                    data.schemeSpecificPart?.split("?")?.firstOrNull() ?: ""
                } else ""
            }
            else -> intent.getStringExtra("address") ?: ""
        }
    }

    private fun sendMessage(recipient: String, body: String) {
        try {
            val androidSmsManager = android.telephony.SmsManager.getDefault()
            androidSmsManager.sendTextMessage(recipient, null, body, null, null)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeScreen(
    initialRecipient: String,
    initialBody: String,
    onSend: (String, String) -> Unit,
    onClose: () -> Unit
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var body by remember { mutableStateOf(initialBody) }

    val canSend = recipient.isNotBlank() && body.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050810),
                        Color(0xFF0A0F1C)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Message",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recipient field
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("To", color = Color(0xFF6B7280)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00F5D4),
                    focusedBorderColor = Color(0xFF00F5D4),
                    unfocusedBorderColor = Color(0xFF6B7280).copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF1A1F35),
                    unfocusedContainerColor = Color(0xFF1A1F35)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message field
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Message", color = Color(0xFF6B7280)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00F5D4),
                    focusedBorderColor = Color(0xFF00F5D4),
                    unfocusedBorderColor = Color(0xFF6B7280).copy(alpha = 0.3f),
                    focusedContainerColor = Color(0xFF1A1F35),
                    unfocusedContainerColor = Color(0xFF1A1F35)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Send button
            Button(
                onClick = { onSend(recipient, body) },
                enabled = canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00F5D4),
                    disabledContainerColor = Color(0xFF1A1F35)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (canSend) Color(0xFF050810) else Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send",
                    color = if (canSend) Color(0xFF050810) else Color(0xFF6B7280),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

