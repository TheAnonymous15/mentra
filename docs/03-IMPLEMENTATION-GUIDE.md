# Implementation Guide

This document provides detailed step-by-step implementation instructions for building the Mentra super app.

---

## Table of Contents

1. [Project Setup](#1-project-setup)
2. [Core Infrastructure](#2-core-infrastructure)
3. [Custom Launcher](#3-custom-launcher)
4. [Health & Activity Tracking](#4-health--activity-tracking)
5. [Navigation & Maps](#5-navigation--maps)
6. [Media Player](#6-media-player)
7. [AI Shell](#7-ai-shell)
8. [Additional Features](#8-additional-features)
9. [Testing & Optimization](#9-testing--optimization)

---

## 1. Project Setup

### 1.1 Create Multi-Module Project Structure

```bash
mentra/
├── app/                          # Main application module
├── core/
│   ├── common/                   # Shared utilities
│   ├── data/                     # Data layer abstractions
│   ├── domain/                   # Business logic
│   └── ui/                       # UI components
├── features/
│   ├── launcher/                 # Custom launcher
│   ├── health/                   # Health & activity
│   ├── navigation/               # Maps & navigation
│   ├── media/                    # Media player
│   ├── aishell/                  # AI shell
│   ├── messaging/                # Messaging
│   ├── camera/                   # Camera
│   └── utilities/                # System utilities
└── infrastructure/
    ├── shizuku/                  # Shizuku integration
    ├── sensors/                  # Sensor abstraction
    ├── location/                 # Location services
    └── storage/                  # Storage management
```

### 1.2 Update build.gradle.kts (Project Level)

```kotlin
// build.gradle.kts (project)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### 1.3 Update gradle/libs.versions.toml

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
compose-bom = "2024.12.01"
compose-compiler = "1.5.8"
ksp = "2.1.0-1.0.29"
hilt = "2.52"
room = "2.6.1"
lifecycle = "2.8.7"
navigation = "2.8.5"
coroutines = "1.9.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
exoplayer = "1.5.0"
shizuku = "13.1.5"
tensorflow = "2.14.0"
accompanist = "0.36.0"

[libraries]
# AndroidX Core
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.15.0" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }

# Compose BOM
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# Coroutines
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# WorkManager
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version = "2.10.0" }

# DataStore
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version = "1.1.1" }

# ExoPlayer
exoplayer-core = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "exoplayer" }
exoplayer-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "exoplayer" }
exoplayer-session = { group = "androidx.media3", name = "media3-session", version.ref = "exoplayer" }

# Shizuku
shizuku-api = { group = "dev.rikka.shizuku", name = "api", version.ref = "shizuku" }
shizuku-provider = { group = "dev.rikka.shizuku", name = "provider", version.ref = "shizuku" }

# TensorFlow Lite
tensorflow-lite = { group = "org.tensorflow", name = "tensorflow-lite", version.ref = "tensorflow" }
tensorflow-lite-support = { group = "org.tensorflow", name = "tensorflow-lite-support", version = "0.4.4" }

# Location
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version = "21.3.0" }

# OSM Droid (for maps)
osmdroid-android = { group = "org.osmdroid", name = "osmdroid-android", version = "6.1.18" }

# Accompanist
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanist" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.6.1" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 1.4 Update app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.mentra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mentra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // ExoPlayer
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    implementation(libs.exoplayer.session)
    
    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    
    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    
    // Location
    implementation(libs.play.services.location)
    
    // Maps
    implementation(libs.osmdroid.android)
    
    // Accompanist
    implementation(libs.accompanist.permissions)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

### 1.5 Update AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Location -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- Activity Recognition -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    
    <!-- Sensors -->
    <uses-permission android:name="android.permission.BODY_SENSORS" />
    
    <!-- Phone & SMS -->
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    
    <!-- Storage -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    
    <!-- Camera -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- System -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Shizuku -->
    <uses-permission android:name="moe.shizuku.manager.permission.API_V23" />

    <application
        android:name=".MentraApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Mentra"
        tools:targetApi="31">
        
        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Mentra">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- Set as default launcher -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
        
        <!-- Services will be added here -->
        
    </application>

</manifest>
```

---

## 2. Core Infrastructure

### 2.1 Create Base Application Class

```kotlin
// app/src/main/java/com/example/mentra/MentraApplication.kt
package com.example.mentra

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MentraApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize analytics (optional)
        // Initialize crash reporting (optional)
        // Initialize Shizuku
        initializeShizuku()
    }
    
    private fun initializeShizuku() {
        // Shizuku initialization will be added later
    }
}
```

### 2.2 Create Database

```kotlin
// core/data/src/main/java/com/example/mentra/core/data/local/MentraDatabase.kt
package com.example.mentra.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mentra.core.data.local.dao.*
import com.example.mentra.core.data.local.entity.*

@Database(
    entities = [
        ActivityRecord::class,
        HealthStats::class,
        SleepData::class,
        SavedRoute::class,
        RoutePoint::class,
        POI::class,
        MediaItem::class,
        Playlist::class,
        PlaylistItem::class,
        ShellHistory::class,
        ShellAlias::class,
        ShellScript::class,
        UserProfile::class,
        Preference::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MentraDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun healthDao(): HealthDao
    abstract fun navigationDao(): NavigationDao
    abstract fun mediaDao(): MediaDao
    abstract fun shellDao(): ShellDao
    abstract fun userDao(): UserDao
}
```

### 2.3 Create Type Converters

```kotlin
// core/data/src/main/java/com/example/mentra/core/data/local/Converters.kt
package com.example.mentra.core.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
}
```

### 2.4 Create Dependency Injection Modules

```kotlin
// app/src/main/java/com/example/mentra/di/DatabaseModule.kt
package com.example.mentra.di

import android.content.Context
import androidx.room.Room
import com.example.mentra.core.data.local.MentraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MentraDatabase {
        return Room.databaseBuilder(
            context,
            MentraDatabase::class.java,
            "mentra.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideActivityDao(database: MentraDatabase) = database.activityDao()
    
    @Provides
    fun provideHealthDao(database: MentraDatabase) = database.healthDao()
    
    @Provides
    fun provideNavigationDao(database: MentraDatabase) = database.navigationDao()
    
    @Provides
    fun provideMediaDao(database: MentraDatabase) = database.mediaDao()
    
    @Provides
    fun provideShellDao(database: MentraDatabase) = database.shellDao()
    
    @Provides
    fun provideUserDao(database: MentraDatabase) = database.userDao()
}
```

### 2.5 Create Event Bus

```kotlin
// core/common/src/main/java/com/example/mentra/core/common/EventBus.kt
package com.example.mentra.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SystemEvent {
    // Activity Events
    data class ActivityDetected(val type: ActivityType, val confidence: Float) : SystemEvent()
    data class StepCountUpdated(val steps: Int, val timestamp: Long) : SystemEvent()
    
    // Navigation Events
    data class LocationUpdated(val latitude: Double, val longitude: Double) : SystemEvent()
    data class NavigationStarted(val destination: String) : SystemEvent()
    data object NavigationCompleted : SystemEvent()
    
    // Media Events
    data class MediaStarted(val mediaId: String) : SystemEvent()
    data object MediaStopped : SystemEvent()
    data class PlaylistChanged(val playlistId: String) : SystemEvent()
    
    // Shell Events
    data class CommandExecuted(val command: String, val result: String) : SystemEvent()
    data class AutomationTriggered(val trigger: String) : SystemEvent()
    
    // System Events
    data class BatteryLow(val level: Int) : SystemEvent()
    data class ConnectivityChanged(val isOnline: Boolean) : SystemEvent()
    data class HeadphonesPlugged(val plugged: Boolean) : SystemEvent()
}

enum class ActivityType {
    STILL, WALKING, RUNNING, CYCLING, 
    IN_VEHICLE, ON_BICYCLE, UNKNOWN
}

@Singleton
class EventBus @Inject constructor() {
    
    private val _events = MutableSharedFlow<SystemEvent>(replay = 0)
    val events: SharedFlow<SystemEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: SystemEvent) {
        _events.emit(event)
    }
}
```

---

## 3. Custom Launcher

### 3.1 Create Launcher Home Screen

```kotlin
// features/launcher/src/main/java/com/example/mentra/features/launcher/LauncherScreen.kt
package com.example.mentra.features.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            LauncherTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Widgets section
            WidgetsSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            )
            
            // App grid
            AppGrid(
                apps = uiState.filteredApps,
                onAppClick = viewModel::launchApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
            )
        }
    }
}

@Composable
fun AppGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(apps) { app ->
            AppIcon(
                app = app,
                onClick = { onAppClick(app) }
            )
        }
    }
}
```

### 3.2 Create Launcher ViewModel

```kotlin
// features/launcher/src/main/java/com/example/mentra/features/launcher/LauncherViewModel.kt
package com.example.mentra.features.launcher

import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LauncherUiState(
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val packageManager: PackageManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()
    
    init {
        loadInstalledApps()
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val apps = packageManager.queryIntentActivities(mainIntent, 0)
                .map { resolveInfo ->
                    AppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(packageManager).toString(),
                        icon = resolveInfo.loadIcon(packageManager),
                        isSystemApp = (resolveInfo.activityInfo.applicationInfo.flags and
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .sortedBy { it.label }
            
            _uiState.update {
                it.copy(
                    apps = apps,
                    filteredApps = apps,
                    isLoading = false
                )
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.apps
            } else {
                state.apps.filter { app ->
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                }
            }
            
            state.copy(
                searchQuery = query,
                filteredApps = filtered
            )
        }
    }
    
    fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        launchIntent?.let {
            // Context needed here - will be passed from UI
        }
    }
}
```

---

## 4. Health & Activity Tracking

### 4.1 Create Activity Tracking Service

```kotlin
// features/health/src/main/java/com/example/mentra/features/health/ActivityTrackingService.kt
package com.example.mentra.features.health

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTrackingService : Service(), SensorEventListener {
    
    @Inject
    lateinit var sensorManager: SensorManager
    
    @Inject
    lateinit var activityEngine: ActivityEngine
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var stepCounter: Sensor? = null
    private var accelerometer: Sensor? = null
    
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        initializeSensors()
    }
    
    private fun initializeSensors() {
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    handleStepCount(it.values[0].toInt())
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    handleAccelerometer(it.values)
                }
            }
        }
    }
    
    private fun handleStepCount(steps: Int) {
        serviceScope.launch {
            activityEngine.updateStepCount(steps)
        }
    }
    
    private fun handleAccelerometer(values: FloatArray) {
        serviceScope.launch {
            activityEngine.processAccelerometerData(values)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Activity Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your daily activity and steps"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mentra Activity Tracking")
            .setContentText("Tracking your daily activity")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    
    companion object {
        private const val CHANNEL_ID = "activity_tracking"
        private const val NOTIFICATION_ID = 1001
    }
}
```

### 4.2 Create Activity Engine

```kotlin
// features/health/src/main/java/com/example/mentra/features/health/ActivityEngine.kt
package com.example.mentra.features.health

import com.example.mentra.core.common.ActivityType
import com.example.mentra.core.common.EventBus
import com.example.mentra.core.common.SystemEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ActivityEngine @Inject constructor(
    private val eventBus: EventBus
) {
    
    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps
    
    private val _currentActivity = MutableStateFlow(ActivityType.STILL)
    val currentActivity: StateFlow<ActivityType> = _currentActivity
    
    private var lastStepCount = 0
    private var stepsSinceLastUpdate = 0
    
    private val accelerometerBuffer = mutableListOf<FloatArray>()
    private val BUFFER_SIZE = 50 // 1 second at 50Hz
    
    suspend fun updateStepCount(totalSteps: Int) {
        if (lastStepCount == 0) {
            lastStepCount = totalSteps
            return
        }
        
        val newSteps = totalSteps - lastStepCount
        if (newSteps > 0) {
            stepsSinceLastUpdate += newSteps
            _dailySteps.value += newSteps
            lastStepCount = totalSteps
            
            eventBus.emit(
                SystemEvent.StepCountUpdated(
                    steps = _dailySteps.value,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    suspend fun processAccelerometerData(values: FloatArray) {
        accelerometerBuffer.add(values.clone())
        
        if (accelerometerBuffer.size >= BUFFER_SIZE) {
            val activity = classifyActivity(accelerometerBuffer)
            _currentActivity.value = activity
            
            eventBus.emit(
                SystemEvent.ActivityDetected(
                    type = activity,
                    confidence = 0.85f // Placeholder
                )
            )
            
            accelerometerBuffer.clear()
        }
    }
    
    private fun classifyActivity(buffer: List<FloatArray>): ActivityType {
        // Calculate magnitude variance
        val magnitudes = buffer.map { values ->
            sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        }
        
        val mean = magnitudes.average()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average()
        
        return when {
            variance < 0.5 -> ActivityType.STILL
            variance < 2.0 -> ActivityType.WALKING
            variance < 4.0 -> ActivityType.RUNNING
            else -> ActivityType.UNKNOWN
        }
    }
    
    fun calculateDistance(steps: Int, userHeightCm: Float): Float {
        // Stride length = height × 0.413 for walking
        val strideLengthMeters = (userHeightCm / 100f) * 0.413f
        return steps * strideLengthMeters
    }
    
    fun calculateCalories(
        steps: Int,
        weightKg: Float,
        activityType: ActivityType
    ): Float {
        val met = when (activityType) {
            ActivityType.STILL -> 1.0f
            ActivityType.WALKING -> 3.3f
            ActivityType.RUNNING -> 9.8f
            ActivityType.CYCLING -> 8.0f
            else -> 2.0f
        }
        
        // Approximate duration in hours (assuming 100 steps/minute walking)
        val durationHours = steps / 6000f
        
        return met * weightKg * durationHours
    }
}
```

---

## 5. Navigation & Maps

### 5.1 Create Navigation Engine

```kotlin
// features/navigation/src/main/java/com/example/mentra/features/navigation/NavigationEngine.kt
package com.example.mentra.features.navigation

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class NavigationEngine @Inject constructor() {
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    private val _activeRoute = MutableStateFlow<Route?>(null)
    val activeRoute: StateFlow<Route?> = _activeRoute
    
    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }
    
    fun calculateRoute(
        start: LatLng,
        end: LatLng,
        mode: TravelMode
    ): Route {
        // Simple straight-line route for now
        // In production, use routing algorithm or API
        
        val distance = calculateDistance(start, end)
        val duration = calculateETA(distance, mode)
        
        return Route(
            id = generateRouteId(),
            startLocation = start,
            endLocation = end,
            waypoints = listOf(start, end),
            distanceMeters = distance,
            durationSeconds = duration,
            travelMode = mode,
            instructions = generateInstructions(start, end)
        )
    }
    
    fun calculateDistance(loc1: LatLng, loc2: LatLng): Float {
        val earthRadius = 6371000.0 // meters
        
        val lat1 = Math.toRadians(loc1.latitude)
        val lat2 = Math.toRadians(loc2.latitude)
        val deltaLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val deltaLon = Math.toRadians(loc2.longitude - loc1.longitude)
        
        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return (earthRadius * c).toFloat()
    }
    
    fun calculateETA(distanceMeters: Float, mode: TravelMode): Int {
        val averageSpeed = when (mode) {
            TravelMode.WALKING -> 1.4f // m/s (5 km/h)
            TravelMode.CYCLING -> 4.2f // m/s (15 km/h)
            TravelMode.DRIVING -> 13.9f // m/s (50 km/h)
            TravelMode.TRANSIT -> 8.3f // m/s (30 km/h)
        }
        
        return (distanceMeters / averageSpeed).toInt()
    }
    
    private fun generateRouteId() = "route_${System.currentTimeMillis()}"
    
    private fun generateInstructions(start: LatLng, end: LatLng): List<NavigationInstruction> {
        // Placeholder - would generate turn-by-turn instructions
        return listOf(
            NavigationInstruction(
                type = InstructionType.START,
                distance = 0f,
                duration = 0,
                description = "Start at current location",
                location = start
            ),
            NavigationInstruction(
                type = InstructionType.ARRIVE,
                distance = calculateDistance(start, end),
                duration = 0,
                description = "Arrive at destination",
                location = end
            )
        )
    }
}

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class Route(
    val id: String,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val waypoints: List<LatLng>,
    val distanceMeters: Float,
    val durationSeconds: Int,
    val travelMode: TravelMode,
    val instructions: List<NavigationInstruction>
)

enum class TravelMode {
    WALKING, DRIVING, CYCLING, TRANSIT
}

data class NavigationInstruction(
    val type: InstructionType,
    val distance: Float,
    val duration: Int,
    val description: String,
    val location: LatLng
)

enum class InstructionType {
    START, TURN_LEFT, TURN_RIGHT,
    CONTINUE, ARRIVE, ROUNDABOUT
}
```

---

## 6. Media Player

### 6.2 Create Media Playback Engine

```kotlin
// features/media/src/main/java/com/example/mentra/features/media/MediaPlaybackEngine.kt
package com.example.mentra.features.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlaybackEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState
    
    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updatePlaybackState()
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }
        })
    }
    
    fun play(mediaItem: com.example.mentra.features.media.MediaItem) {
        val exoMediaItem = MediaItem.fromUri(mediaItem.filePath)
        player.setMediaItem(exoMediaItem)
        player.prepare()
        player.play()
        
        updatePlaybackState(currentItem = mediaItem)
    }
    
    fun pause() {
        player.pause()
        updatePlaybackState()
    }
    
    fun resume() {
        player.play()
        updatePlaybackState()
    }
    
    fun stop() {
        player.stop()
        updatePlaybackState(currentItem = null)
    }
    
    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }
    
    fun setQueue(items: List<com.example.mentra.features.media.MediaItem>) {
        val mediaItems = items.map { MediaItem.fromUri(it.filePath) }
        player.setMediaItems(mediaItems)
        player.prepare()
    }
    
    private fun updatePlaybackState(
        currentItem: com.example.mentra.features.media.MediaItem? = _playbackState.value.currentItem
    ) {
        _playbackState.value = PlaybackState(
            currentItem = currentItem,
            position = player.currentPosition,
            isPlaying = player.isPlaying,
            duration = player.duration
        )
    }
    
    fun release() {
        player.release()
    }
}

data class PlaybackState(
    val currentItem: com.example.mentra.features.media.MediaItem? = null,
    val position: Long = 0,
    val isPlaying: Boolean = false,
    val duration: Long = 0
)

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val filePath: String,
    val duration: Long
)
```

---

## 7. AI Shell

### 7.1 Create Shell Engine

```kotlin
// features/aishell/src/main/java/com/example/mentra/features/aishell/ShellEngine.kt
package com.example.mentra.features.aishell

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellEngine @Inject constructor(
    private val intentRecognizer: IntentRecognizer,
    private val actionRouter: ActionRouter,
    private val permissionValidator: PermissionValidator
) {
    
    suspend fun executeCommand(input: String): CommandResult {
        try {
            // 1. Recognize intent
            val intent = intentRecognizer.recognize(input)
            
            // 2. Check confidence
            if (intent.confidence < 0.7f) {
                return CommandResult(
                    success = false,
                    message = "I'm not sure what you mean. Could you rephrase?"
                )
            }
            
            // 3. Validate permissions
            val hasPermission = permissionValidator.validate(intent.action)
            if (!hasPermission) {
                return CommandResult(
                    success = false,
                    message = "Permission denied for this action"
                )
            }
            
            // 4. Route to appropriate handler
            return actionRouter.route(intent)
            
        } catch (e: Exception) {
            return CommandResult(
                success = false,
                message = "Error: ${e.message}"
            )
        }
    }
}

data class Intent(
    val action: Action,
    val entity: String?,
    val parameters: Map<String, Any>,
    val confidence: Float
)

enum class Action {
    OPEN_APP, CALL, MESSAGE, PLAY_MEDIA,
    NAVIGATE, QUERY_SYSTEM, CHANGE_SETTING
}

data class CommandResult(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
```

This implementation guide provides a solid foundation. Due to length constraints, I'll create additional documentation files for:
- Development setup
- Testing strategies  
- Deployment guide

Let me continue with those files...

