package com.example.mentra.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Call Broadcast Receiver
 *
 * Receives system broadcasts for phone state changes.
 * NOTE: This is a FALLBACK mechanism only for logging/tracking.
 *
 * IMPORTANT: Do NOT show notifications or play ringtones here!
 * The MentraInCallService handles all incoming call UI via CallForegroundService
 * when it detects Call.STATE_RINGING from the Telecom framework.
 *
 * Using PHONE_STATE broadcasts is deprecated and unreliable for call handling.
 */
class CallBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneStateChange(context, intent)
            }
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                handleOutgoingCall(context, intent)
            }
        }
    }

    private fun handlePhoneStateChange(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        // Just log for debugging - DO NOT show notifications here!
        // MentraInCallService handles all UI via CallForegroundService
        Log.d(TAG, "Phone state changed: $state, number: $number")
    }

    private fun handleOutgoingCall(context: Context, intent: Intent) {
        val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Outgoing call: $number")
    }
}

/**
 * Incoming Call Notification Manager
 *
 * Handles showing fullscreen incoming call UI directly (no notification).
 * Works when screen is off, on lock screen, and during Doze mode.
 * Plays ringtone and vibrates for incoming calls.
 */
object IncomingCallNotificationManager {
    private const val TAG = "IncomingCallNotif"

    // Vibration pattern: wait, vibrate, pause, vibrate...
    private val VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500)

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var vibrator: android.os.Vibrator? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    /**
     * Show incoming call UI directly - no notification, just launch the activity
     */
    fun showIncomingCallNotification(context: Context, phoneNumber: String) {
        // Acquire wake lock to turn on screen
        acquireWakeLock(context)

        // Start ringtone and vibration
        startRingtoneAndVibration(context)

        // Launch IncomingCallActivity directly - bypasses notification completely
        try {
            val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, phoneNumber)
            }
            context.startActivity(fullScreenIntent)
            Log.d(TAG, "Launched IncomingCallActivity for: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch IncomingCallActivity", e)
        }
    }

    fun cancelIncomingCallNotification(context: Context) {
        // Stop ringtone and vibration
        stopRingtoneAndVibration()

        // Release wake lock
        releaseWakeLock()

        Log.d(TAG, "Cancelled incoming call UI")
    }

    private fun acquireWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.FULL_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                android.os.PowerManager.ON_AFTER_RELEASE,
                "mentra:incoming_call"
            )
            wakeLock?.acquire(60000L) // 60 second timeout
        } catch (e: Exception) {
            Log.e("IncomingCallNotif", "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("IncomingCallNotif", "Failed to release wake lock", e)
        }
    }

    private fun startRingtoneAndVibration(context: Context) {
        try {
            // Check ringer mode
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val ringerMode = audioManager.ringerMode

            // Start vibration (unless silent mode)
            if (ringerMode != android.media.AudioManager.RINGER_MODE_SILENT) {
                vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(VIBRATION_PATTERN, 0)
                }
            }

            // Start ringtone (unless silent/vibrate mode)
            if (ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) {
                val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(context, ringtoneUri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("IncomingCallNotif", "Failed to start ringtone/vibration", e)
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            // Stop ringtone
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            // Stop vibration
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("IncomingCallNotif", "Failed to stop ringtone/vibration", e)
        }
    }
}

/**
 * Call Action Receiver
 *
 * Handles notification actions for answering/declining calls.
 */
class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER = "com.example.mentra.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.example.mentra.ACTION_DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val dialerManager = DialerManagerProvider.getDialerManager()

        when (intent.action) {
            ACTION_ANSWER -> {
                dialerManager?.answerCall()
                // Launch in-call UI
                context.startActivity(Intent(context, InCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            ACTION_DECLINE -> {
                dialerManager?.rejectCall()
            }
        }

        // Cancel notification
        IncomingCallNotificationManager.cancelIncomingCallNotification(context)
    }
}

