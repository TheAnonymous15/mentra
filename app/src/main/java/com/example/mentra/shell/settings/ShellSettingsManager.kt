package com.example.mentra.shell.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * SHELL SETTINGS MANAGER
 * Manages persistent shell settings and preferences
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class ShellSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "mentra_shell_settings"

        // Setting keys
        const val KEY_IN_SHELL_INCOMING_CALL = "in_shell_incoming_call"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_THEME = "theme"
        const val KEY_HISTORY_SIZE = "history_size"
        const val KEY_AUTO_SCROLL = "auto_scroll"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    }

    // ═══════════════════════════════════════════════════════════════════
    // IN-SHELL INCOMING CALL SETTING
    // When enabled, incoming calls are handled entirely in the shell
    // User can Answer (A), Reject (R), or Quick Reply (Q)
    // ═══════════════════════════════════════════════════════════════════
    private val _inShellIncomingCall = MutableStateFlow(
        prefs.getBoolean(KEY_IN_SHELL_INCOMING_CALL, false)
    )
    val inShellIncomingCall: StateFlow<Boolean> = _inShellIncomingCall.asStateFlow()

    fun setInShellIncomingCall(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IN_SHELL_INCOMING_CALL, enabled).apply()
        _inShellIncomingCall.value = enabled
    }

    // ═══════════════════════════════════════════════════════════════════
    // FONT SIZE SETTING
    // ═══════════════════════════════════════════════════════════════════
    private val _fontSize = MutableStateFlow(
        prefs.getInt(KEY_FONT_SIZE, 13)
    )
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    fun setFontSize(size: Int) {
        val clamped = size.coerceIn(10, 24)
        prefs.edit().putInt(KEY_FONT_SIZE, clamped).apply()
        _fontSize.value = clamped
    }

    // ═══════════════════════════════════════════════════════════════════
    // THEME SETTING
    // ═══════════════════════════════════════════════════════════════════
    private val _theme = MutableStateFlow(
        prefs.getString(KEY_THEME, "kali") ?: "kali"
    )
    val theme: StateFlow<String> = _theme.asStateFlow()

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
        _theme.value = theme
    }

    // ═══════════════════════════════════════════════════════════════════
    // HISTORY SIZE SETTING
    // ═══════════════════════════════════════════════════════════════════
    private val _historySize = MutableStateFlow(
        prefs.getInt(KEY_HISTORY_SIZE, 1000)
    )
    val historySize: StateFlow<Int> = _historySize.asStateFlow()

    fun setHistorySize(size: Int) {
        val clamped = size.coerceIn(100, 10000)
        prefs.edit().putInt(KEY_HISTORY_SIZE, clamped).apply()
        _historySize.value = clamped
    }

    // ═══════════════════════════════════════════════════════════════════
    // AUTO SCROLL SETTING
    // ═══════════════════════════════════════════════════════════════════
    private val _autoScroll = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SCROLL, true)
    )
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    fun setAutoScroll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()
        _autoScroll.value = enabled
    }

    // ═══════════════════════════════════════════════════════════════════
    // HAPTIC FEEDBACK SETTING
    // ═══════════════════════════════════════════════════════════════════
    private val _hapticFeedback = MutableStateFlow(
        prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    )
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
        _hapticFeedback.value = enabled
    }

    /**
     * Get all settings as a map (for display/export)
     */
    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            KEY_IN_SHELL_INCOMING_CALL to _inShellIncomingCall.value,
            KEY_FONT_SIZE to _fontSize.value,
            KEY_THEME to _theme.value,
            KEY_HISTORY_SIZE to _historySize.value,
            KEY_AUTO_SCROLL to _autoScroll.value,
            KEY_HAPTIC_FEEDBACK to _hapticFeedback.value
        )
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _inShellIncomingCall.value = false
        _fontSize.value = 13
        _theme.value = "kali"
        _historySize.value = 1000
        _autoScroll.value = true
        _hapticFeedback.value = true
    }
}

