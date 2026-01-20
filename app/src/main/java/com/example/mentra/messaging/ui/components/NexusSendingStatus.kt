package com.example.mentra.messaging.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mentra.messaging.ui.theme.NexusColors

/**
 * ═══════════════════════════════════════════════════════════════════
 * SENDING STATUS COMPONENTS
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Message sending status enum
 */
enum class SendingStatus {
    IDLE,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    SCHEDULED
}

/**
 * Circular status indicator with optional spinning animation
 */
@Composable
fun SendingStatusIndicator(
    icon: ImageVector?,
    isSpinning: Boolean,
    spinnerRotation: Float,
    backgroundColor: Color,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(backgroundColor, CircleShape)
                .shadow(8.dp, CircleShape, spotColor = backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (isSpinning) {
                // Spinning loader
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = spinnerRotation },
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = text,
            color = backgroundColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Full-width status bar showing sending progress
 */
@Composable
fun SendingStatusBar(
    status: SendingStatus,
    message: String,
    spinnerRotation: Float = 0f
) {
    val backgroundColor = when (status) {
        SendingStatus.SENDING -> NexusColors.accent.copy(alpha = 0.15f)
        SendingStatus.SENT -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        SendingStatus.DELIVERED -> Color(0xFF2196F3).copy(alpha = 0.15f)
        SendingStatus.FAILED -> NexusColors.tertiary.copy(alpha = 0.15f)
        SendingStatus.SCHEDULED -> NexusColors.secondary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val iconColor = when (status) {
        SendingStatus.SENDING -> NexusColors.accent
        SendingStatus.SENT -> Color(0xFF4CAF50)
        SendingStatus.DELIVERED -> Color(0xFF2196F3)
        SendingStatus.FAILED -> NexusColors.tertiary
        SendingStatus.SCHEDULED -> NexusColors.secondary
        else -> NexusColors.textMuted
    }

    val icon = when (status) {
        SendingStatus.SENDING -> null
        SendingStatus.SENT -> Icons.Default.Check
        SendingStatus.DELIVERED -> Icons.Default.DoneAll
        SendingStatus.FAILED -> Icons.Default.ErrorOutline
        SendingStatus.SCHEDULED -> Icons.Default.Schedule
        else -> null
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == SendingStatus.SENDING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = iconColor,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = message,
                color = iconColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

