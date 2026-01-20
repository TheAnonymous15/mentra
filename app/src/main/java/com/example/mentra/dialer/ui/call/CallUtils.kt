package com.example.mentra.dialer.ui.call

/**
 * Utility functions for call UI
 */
object CallUtils {

    fun formatRingTime(seconds: Long): String {
        return if (seconds < 60) {
            "Ringing ${seconds}s"
        } else {
            val min = seconds / 60
            val sec = seconds % 60
            "Ringing ${min}m ${sec}s"
        }
    }

    fun formatCallDuration(seconds: Long): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "%02d:%02d".format(min, sec)
    }
}

