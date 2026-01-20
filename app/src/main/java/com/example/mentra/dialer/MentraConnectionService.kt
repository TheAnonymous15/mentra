package com.example.mentra.dialer

import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * ═══════════════════════════════════════════════════════════════════
 * MENTRA CONNECTION SERVICE
 * Handles outgoing call connections for the Mentra Dialer
 * ═══════════════════════════════════════════════════════════════════
 */
class MentraConnectionService : ConnectionService() {

    companion object {
        private const val TAG = "MentraConnectionService"
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "Creating outgoing connection to ${request?.address}")

        return MentraConnection().apply {
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            setDialing()
            setConnectionCapabilities(
                Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_SUPPORT_HOLD or
                Connection.CAPABILITY_HOLD
            )
        }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(TAG, "Failed to create outgoing connection")
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "Creating incoming connection from ${request?.address}")

        return MentraConnection().apply {
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            setRinging()
            setConnectionCapabilities(
                Connection.CAPABILITY_MUTE or
                Connection.CAPABILITY_SUPPORT_HOLD or
                Connection.CAPABILITY_HOLD
            )
        }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(TAG, "Failed to create incoming connection")
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }
}

/**
 * Mentra Connection - Represents a single call connection
 */
class MentraConnection : Connection() {

    companion object {
        private const val TAG = "MentraConnection"
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        Log.d(TAG, "Connection state changed to $state")
    }

    override fun onAnswer() {
        Log.d(TAG, "Call answered")
        setActive()
    }

    override fun onAnswer(videoState: Int) {
        Log.d(TAG, "Call answered with video state: $videoState")
        setActive()
    }

    override fun onReject() {
        Log.d(TAG, "Call rejected")
        setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
        destroy()
    }

    override fun onReject(rejectReason: Int) {
        Log.d(TAG, "Call rejected with reason: $rejectReason")
        setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        Log.d(TAG, "Call disconnected")
        setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        Log.d(TAG, "Call aborted")
        setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.CANCELED))
        destroy()
    }

    override fun onHold() {
        Log.d(TAG, "Call put on hold")
        setOnHold()
    }

    override fun onUnhold() {
        Log.d(TAG, "Call resumed from hold")
        setActive()
    }

    override fun onPlayDtmfTone(digit: Char) {
        Log.d(TAG, "DTMF tone: $digit")
        // Handle DTMF tone
    }

    override fun onStopDtmfTone() {
        Log.d(TAG, "Stop DTMF tone")
    }
}

