package com.example.mentra.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System-wide event bus for cross-module communication.
 * Uses SharedFlow for efficient multi-subscriber event distribution.
 */
@Singleton
class EventBus @Inject constructor() {

    private val _events = MutableSharedFlow<SystemEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    val events: SharedFlow<SystemEvent> = _events.asSharedFlow()

    suspend fun emit(event: SystemEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: SystemEvent): Boolean {
        return _events.tryEmit(event)
    }
}

/**
 * Base class for all system events
 */
sealed class SystemEvent {

    // Activity & Health Events
    sealed class Activity : SystemEvent() {
        data class StepCountUpdated(val steps: Int, val distance: Double) : Activity()
        data class ActivityDetected(val type: ActivityType, val confidence: Float) : Activity()
        data class CaloriesBurned(val calories: Double) : Activity()
        object SleepStarted : Activity()
        object SleepEnded : Activity()
    }

    // Navigation Events
    sealed class Navigation : SystemEvent() {
        data class LocationUpdated(val latitude: Double, val longitude: Double) : Navigation()
        data class NavigationStarted(val destination: String) : Navigation()
        data class NavigationEnded(val reason: String) : Navigation()
        data class RouteRecalculated(val newDistance: Double, val newEta: Long) : Navigation()
        data class ArrivalImminent(val distanceRemaining: Double) : Navigation()
    }

    // Media Events
    sealed class Media : SystemEvent() {
        data class PlaybackStarted(val mediaId: String, val title: String) : Media()
        data class PlaybackPaused(val mediaId: String) : Media()
        data class PlaybackStopped(val mediaId: String) : Media()
        data class TrackChanged(val mediaId: String, val title: String) : Media()
        data class PlaylistChanged(val playlistId: String) : Media()
    }

    // Shell Events
    sealed class Shell : SystemEvent() {
        data class CommandExecuted(val command: String, val success: Boolean) : Shell()
        data class ScriptExecuted(val scriptPath: String, val result: String) : Shell()
        data class AutomationTriggered(val triggerName: String) : Shell()
    }

    // System Events
    sealed class System : SystemEvent() {
        data class PermissionGranted(val permission: String) : System()
        data class PermissionDenied(val permission: String) : System()
        data class BatteryLow(val level: Int) : System()
        data class StorageLow(val availableMb: Long) : System()
        data class NetworkConnected(val type: String) : System()
        object NetworkDisconnected : System()
    }

    // Launcher Events
    sealed class Launcher : SystemEvent() {
        data class AppLaunched(val packageName: String) : Launcher()
        data class WidgetAdded(val widgetId: String) : Launcher()
        data class WidgetRemoved(val widgetId: String) : Launcher()
    }
}

/**
 * Activity types for tracking
 */
enum class ActivityType {
    STILL,
    WALKING,
    RUNNING,
    CYCLING,
    DRIVING,
    UNKNOWN
}

