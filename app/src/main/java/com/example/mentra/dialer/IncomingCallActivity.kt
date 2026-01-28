package com.example.mentra.dialer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.mentra.dialer.ui.IncomingCallPopup
import com.example.mentra.ui.theme.MentraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager

/**
 * Full-screen incoming / in-call activity
 * Default dialer only.
 */
@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_CONTACT_NAME = "contact_name"
    }

    @Inject
    lateinit var dialerManager: DialerManager

    private var isRinging = true
    private var isRingtoneSilenced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenBypass()

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)

        setContent {
            MentraTheme {

                val ringingState = remember {
                    IncomingCallState.Ringing(
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        startTime = System.currentTimeMillis()
                    )
                }

                var hasMultipleCalls by remember { mutableStateOf(false) }
                var isConference by remember { mutableStateOf(false) }
                var conferenceParticipants by remember { mutableStateOf<List<String>>(emptyList()) }
                var shouldDismiss by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    while (!shouldDismiss) {
                        val calls = MentraInCallService.getInstance()?.getCurrentCalls().orEmpty()

                        val activeCalls = calls.filter {
                            it.state != Call.STATE_DISCONNECTED &&
                                    it.state != Call.STATE_DISCONNECTING
                        }

                        if (activeCalls.isEmpty()) {
                            shouldDismiss = true
                            break
                        }

                        hasMultipleCalls = activeCalls.size > 1

                        isConference = activeCalls.any {
                            it.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true
                        }

                        conferenceParticipants =
                            if (isConference) {
                                activeCalls.flatMap { call ->
                                    buildList {
                                        call.details?.handle?.schemeSpecificPart?.let(::add)
                                        call.children.forEach { child ->
                                            child.details?.handle?.schemeSpecificPart?.let(::add)
                                        }
                                    }
                                }.distinct()
                            } else emptyList()

                        kotlinx.coroutines.delay(500)
                    }
                }

                LaunchedEffect(shouldDismiss) {
                    if (shouldDismiss) {
                        kotlinx.coroutines.delay(1200)
                        finish()
                    }
                }

                IncomingCallPopup(
                    incomingCallState = ringingState,
                    onAnswer = ::answerCall,
                    onReject = ::rejectCall,
                    onDismiss = ::finish,
                    onEndCall = ::endCall,
                    onMuteToggle = ::toggleMute,
                    onSpeakerToggle = ::toggleSpeaker,
                    onHoldToggle = ::toggleHold,
                    onKeypadPress = ::sendDtmf,
                    onAddCall = ::addCall,
                    onMergeCalls = ::mergeCalls,
                    onSwapCalls = ::swapCalls,
                    hasMultipleCalls = hasMultipleCalls,
                    isConference = isConference,
                    conferenceParticipants = conferenceParticipants,
                    isCallEnded = shouldDismiss
                )
            }
        }
    }

    /* ──────────────────────────────
       Call control
       ────────────────────────────── */

    private fun answerCall() {
        isRinging = false
        startService(Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_ANSWER
        })
    }

    private fun rejectCall() {
        startService(Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_REJECT
        })
    }

    private fun endCall() {
        startService(Intent(this, CallForegroundService::class.java).apply {
            action = CallForegroundService.ACTION_END
        })
    }

    private fun toggleMute(muted: Boolean) {
        MentraInCallService.getInstance()?.setMutedState(muted)
    }

    private fun toggleSpeaker(speaker: Boolean) {
        val route =
            if (speaker) CallAudioState.ROUTE_SPEAKER
            else CallAudioState.ROUTE_EARPIECE

        MentraInCallService.getInstance()?.setAudioRouteState(route)
    }

    private fun toggleHold(hold: Boolean) {
        MentraInCallService.getInstance()
            ?.getCurrentCalls()
            ?.firstOrNull()
            ?.let { if (hold) it.hold() else it.unhold() }
    }

    private fun sendDtmf(key: Char) {
        MentraInCallService.getInstance()
            ?.getCurrentCalls()
            ?.firstOrNull()
            ?.apply {
                playDtmfTone(key)
                stopDtmfTone()
            }
    }

    /* ──────────────────────────────
       Multi-call handling
       ────────────────────────────── */

    private fun addCall(number: String) {
        val currentCall =
            MentraInCallService.getInstance()?.getCurrentCalls()?.firstOrNull()

        currentCall?.hold()

        val uri = android.net.Uri.parse("tel:$number")
        val extras = Bundle().apply {
            currentCall?.details?.accountHandle?.let {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
            }
        }

        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecomManager.placeCall(uri, extras)
    }

    private fun mergeCalls() {
        val calls = MentraInCallService.getInstance()?.getCurrentCalls().orEmpty()
        if (calls.size < 2) return

        val active = calls.find { it.state == Call.STATE_ACTIVE } ?: return
        val other = calls.firstOrNull { it != active } ?: return

        active.conference(other)
    }

    private fun swapCalls() {
        val calls = MentraInCallService.getInstance()?.getCurrentCalls().orEmpty()
        val held = calls.find { it.state == Call.STATE_HOLDING }
        held?.unhold()
    }

    /* ──────────────────────────────
       Volume handling (clean + correct)
       ────────────────────────────── */

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isRinging && !isRingtoneSilenced && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    silenceRingtone()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun silenceRingtone() {
        if (isRingtoneSilenced) return
        isRingtoneSilenced = true

        val telecomManager =
            getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        telecomManager.silenceRinger()
    }

    /* ──────────────────────────────
       Lock screen handling
       ────────────────────────────── */

    private fun setupLockScreenBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguard =
                getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguard.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = android.media.AudioManager.STREAM_RING
    }
}
