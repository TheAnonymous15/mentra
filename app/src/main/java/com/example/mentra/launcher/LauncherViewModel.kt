package com.example.mentra.launcher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.infrastructure.apis.AndroidAPIExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Launcher
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val androidAPIExecutor: AndroidAPIExecutor
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val packageManager = context.packageManager
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        loadInstalledApps()
        startTimeUpdates()
        updateBatteryLevel()
    }

    /**
     * Load all installed apps
     */
    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        // Filter to show only launchable apps
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
                    }
                    .map { app ->
                        AppInfo(
                            name = app.loadLabel(packageManager).toString(),
                            packageName = app.packageName,
                            icon = app.loadIcon(packageManager),
                            isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }
                    .sortedBy { it.name }

                _installedApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Launch an app
     */
    fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Show app options (long press)
     */
    fun showAppOptions(app: AppInfo) {
        // TODO: Show app options dialog (uninstall, app info, etc.)
    }

    /**
     * Handle quick action clicks
     */
    fun handleQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action) {
                QuickAction.WIFI -> {
                    androidAPIExecutor.execute("wifi")
                }
                QuickAction.BLUETOOTH -> {
                    androidAPIExecutor.execute("bluetooth")
                }
                QuickAction.BRIGHTNESS -> {
                    androidAPIExecutor.execute("brightness")
                }
                QuickAction.VOLUME -> {
                    androidAPIExecutor.execute("volume")
                }
                QuickAction.AIRPLANE -> {
                    androidAPIExecutor.execute("airplane")
                }
            }
        }
    }

    /**
     * Handle quick settings click
     */
    fun handleQuickSetting(setting: String) {
        viewModelScope.launch {
            androidAPIExecutor.execute("settings", mapOf("type" to setting.lowercase()))
        }
    }

    /**
     * Start time updates (every minute)
     */
    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = timeFormat.format(Date())
                delay(60_000) // Update every minute
            }
        }
    }

    /**
     * Update battery level
     */
    private fun updateBatteryLevel() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    _batteryLevel.value = level
                } catch (e: Exception) {
                    // Fallback using BroadcastReceiver
                    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    val batteryStatus = context.registerReceiver(null, intentFilter)
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
                    _batteryLevel.value = (level * 100 / scale)
                }

                delay(60_000) // Update every minute
            }
        }
    }
}

