package com.example.mentra.core.common

import android.content.Context
import android.util.Log

/**
 * Logging utilities for the app
 */
object Logger {
    private const val TAG_PREFIX = "Mentra"

    private var isDebugMode = true

    fun setDebugMode(enabled: Boolean) {
        isDebugMode = enabled
    }

    fun d(tag: String, message: String) {
        if (isDebugMode) {
            Log.d("$TAG_PREFIX:$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$TAG_PREFIX:$tag", message, throwable)
        } else {
            Log.w("$TAG_PREFIX:$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX:$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX:$tag", message)
        }
    }
}

/**
 * Extension function for easy logging from any class
 */
inline fun <reified T> T.logD(message: String) {
    Logger.d(T::class.java.simpleName, message)
}

inline fun <reified T> T.logI(message: String) {
    Logger.i(T::class.java.simpleName, message)
}

inline fun <reified T> T.logW(message: String, throwable: Throwable? = null) {
    Logger.w(T::class.java.simpleName, message, throwable)
}

inline fun <reified T> T.logE(message: String, throwable: Throwable? = null) {
    Logger.e(T::class.java.simpleName, message, throwable)
}

