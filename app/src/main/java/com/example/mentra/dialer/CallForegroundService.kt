package com.example.mentra.dialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mentra.R

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * CALL FOREGROUND SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Purpose:
 * - Survive background restrictions
 * - Gain audio priority for ringtone
 * - Show lock-screen notification with fullScreenIntent
 *
 * Must be started within seconds of Call.STATE_RINGING
 * Uses foregroundServiceType="phoneCall|mediaPlayback"
 */
class CallForegroundService : Service() {

    companion object {
        private const val TAG = "CallForegroundService"

        // Notification
        const val CHANNEL_ID = "mentra_call_channel"
        const val CHANNEL_NAME = "Active Calls"
        const val NOTIFICATION_ID = 2001

        // Actions
        const val ACTION_START_INCOMING = "com.example.mentra.START_INCOMING_CALL"
        const val ACTION_START_OUTGOING = "com.example.mentra.START_OUTGOING_CALL"
        const val ACTION_ANSWER = "com.example.mentra.ANSWER_CALL"
        const val ACTION_REJECT = "com.example.mentra.REJECT_CALL"
        const val ACTION_END = "com.example.mentra.END_CALL"
        const val ACTION_STOP = "com.example.mentra.STOP_CALL_SERVICE"

        // Extras
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"

        // Vibration pattern
        private val VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000, 500)

        /**
         * Start service for incoming call
         */
        fun startIncomingCall(context: Context, phoneNumber: String, contactName: String? = null) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_INCOMING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CONTACT_NAME, contactName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Start service for outgoing call
         */
        fun startOutgoingCall(context: Context, phoneNumber: String, contactName: String? = null) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START_OUTGOING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CONTACT_NAME, contactName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the service
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, CallForegroundService::class.java))
        }
    }

    // Audio
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Vibration
    private var vibrator: Vibrator? = null

    // State
    private var isRinging = false
    private var currentPhoneNumber: String? = null
    private var currentContactName: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallForegroundService created")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START_INCOMING -> {
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                currentContactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                handleIncomingCall()
            }
            ACTION_START_OUTGOING -> {
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                currentContactName = intent.getStringExtra(EXTRA_CONTACT_NAME)
                handleOutgoingCall()
            }
            ACTION_ANSWER -> {
                handleAnswerCall()
            }
            ACTION_REJECT, ACTION_END -> {
                handleEndCall()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "CallForegroundService destroyed")
        stopRingtone()
        releaseAudioFocus()
        super.onDestroy()
    }

    // ═══════════════════════════════════════════════════════════════════
    // NOTIFICATION CHANNEL
    // ═══════════════════════════════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active call notifications"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableVibration(false) // We handle vibration manually
                setSound(null, null) // We handle ringtone manually via audio focus
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INCOMING CALL HANDLING
    // ═══════════════════════════════════════════════════════════════════

    private fun handleIncomingCall() {
        Log.d(TAG, "Handling incoming call from: $currentPhoneNumber")
        isRinging = true

        // 1. FIRST - Launch the IncomingCallActivity to show our slide-to-answer modal
        // This must happen before notification to ensure our UI shows on top
        launchIncomingCallActivity()

        // 2. Start foreground with CALL-CATEGORY notification (required for background survival)
        val notification = buildIncomingCallNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 3. Request audio focus
        requestAudioFocus()

        // 4. Start ringtone and vibration
        startRingtone()
        startVibration()
    }

    /**
     * Launch the IncomingCallActivity to show our slide-to-answer modal
     */
    private fun launchIncomingCallActivity() {
        try {
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, currentPhoneNumber)
                putExtra(IncomingCallActivity.EXTRA_CONTACT_NAME, currentContactName)
            }
            startActivity(intent)
            Log.d(TAG, "Launched IncomingCallActivity for: $currentPhoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch IncomingCallActivity", e)
        }
    }

    private fun handleOutgoingCall() {
        Log.d(TAG, "Handling outgoing call to: $currentPhoneNumber")
        isRinging = false

        // Start foreground with active call notification
        val notification = buildActiveCallNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Request audio focus for call audio
        requestAudioFocus()
    }

    private fun handleAnswerCall() {
        Log.d(TAG, "Call answered")
        isRinging = false

        // Stop ringtone and vibration
        stopRingtone()

        // Update notification to active call
        val notification = buildActiveCallNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Answer via MentraInCallService (preferred) or DialerManager
        MentraInCallService.getInstance()?.answerCurrentCall()
            ?: DialerManagerProvider.getDialerManager()?.answerCall()

        // Note: IncomingCallActivity handles both ringing and active call states now
        // No need to launch a separate InCallActivity
    }

    private fun handleEndCall() {
        Log.d(TAG, "Call ended/rejected")
        isRinging = false

        // Stop ringtone and vibration
        stopRingtone()

        // End/Reject call via MentraInCallService (preferred) or DialerManager
        val inCallService = MentraInCallService.getInstance()
        val dialerManager = DialerManagerProvider.getDialerManager()

        if (inCallService != null) {
            if (inCallService.hasActiveCalls()) {
                inCallService.disconnectCurrentCall()
            } else {
                inCallService.rejectCurrentCall()
            }
        } else if (dialerManager != null) {
            if (dialerManager.hasActiveCall()) {
                dialerManager.endCall()
            } else {
                dialerManager.rejectCall()
            }
        }

        // Stop service
        stopSelf()
    }

    // ═══════════════════════════════════════════════════════════════════
    // NOTIFICATION BUILDERS
    // ═══════════════════════════════════════════════════════════════════

    private fun buildIncomingCallNotification(): Notification {
        val displayName = currentContactName ?: currentPhoneNumber ?: "Unknown"

        // Full-screen intent for lock screen
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, currentPhoneNumber)
            putExtra(IncomingCallActivity.EXTRA_CONTACT_NAME, currentContactName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action
        val answerIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_ANSWER
        }
        val answerPendingIntent = PendingIntent.getService(
            this, 1, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject action
        val rejectIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_REJECT
        }
        val rejectPendingIntent = PendingIntent.getService(
            this, 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Call")
            .setContentText(displayName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL) // IMPORTANT: CATEGORY_CALL
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true) // IMPORTANT: fullScreenIntent
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                answerPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                rejectPendingIntent
            )
            .build()
    }

    private fun buildActiveCallNotification(): Notification {
        val displayName = currentContactName ?: currentPhoneNumber ?: "Unknown"

        // Open in-call UI
        val contentIntent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End call action
        val endIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_END
        }
        val endPendingIntent = PendingIntent.getService(
            this, 3, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Call in progress")
            .setContentText(displayName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endPendingIntent
            )
            .build()
    }

    // ═══════════════════════════════════════════════════════════════════
    // AUDIO FOCUS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    private fun requestAudioFocus() {
        if (hasAudioFocus) return

        Log.d(TAG, "Requesting audio focus")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Audio focus changed: $focusChange")
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            // Pause ringtone if playing
                            mediaPlayer?.pause()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            // Resume ringtone if was playing
                            if (isRinging) {
                                mediaPlayer?.start()
                            }
                        }
                    }
                }
                .build()

            val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d(TAG, "Audio focus request result: $result")
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                { focusChange -> Log.d(TAG, "Audio focus changed: $focusChange") },
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releaseAudioFocus() {
        if (!hasAudioFocus) return

        Log.d(TAG, "Releasing audio focus")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        hasAudioFocus = false
    }

    // ═══════════════════════════════════════════════════════════════════
    // RINGTONE & VIBRATION
    // ═══════════════════════════════════════════════════════════════════

    private fun startRingtone() {
        // Check ringer mode
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            Log.d(TAG, "Phone is in silent mode, skipping ringtone")
            return
        }

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@CallForegroundService, ringtoneUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
                Log.d(TAG, "Ringtone started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ringtone", e)
            }
        }
    }

    private fun startVibration() {
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            Log.d(TAG, "Phone is in silent mode, skipping vibration")
            return
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, 0)
            }
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    private fun stopRingtone() {
        Log.d(TAG, "Stopping ringtone and vibration")

        // Stop ringtone
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone", e)
        }

        // Stop vibration
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }
}

