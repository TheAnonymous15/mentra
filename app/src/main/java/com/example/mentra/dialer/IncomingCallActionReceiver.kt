package com.example.mentra.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast Receiver for incoming call notification actions (Answer/Reject)
 */
@AndroidEntryPoint
class IncomingCallActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallAction"

        const val ACTION_ANSWER = "com.example.mentra.action.ANSWER_CALL"
        const val ACTION_REJECT = "com.example.mentra.action.REJECT_CALL"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }

    @Inject
    lateinit var incomingCallHandler: IncomingCallHandler

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        Log.d(TAG, "Received action: ${intent.action} for number: $phoneNumber")

        when (intent.action) {
            ACTION_ANSWER -> {
                Log.d(TAG, "Answering call")
                // Cancel notification first
                IncomingCallNotificationManager.cancelIncomingCallNotification(context)
                // Answer the call
                incomingCallHandler.answerCall()
            }

            ACTION_REJECT -> {
                Log.d(TAG, "Rejecting call")
                // Cancel notification first
                IncomingCallNotificationManager.cancelIncomingCallNotification(context)
                // Reject the call
                incomingCallHandler.rejectCall()
            }
        }
    }
}

