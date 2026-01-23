package com.example.mentra.shell.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════
 * APP CACHE SERVICE
 * Background service that maintains a cache of all installed apps
 * for fast shell access and app launching
 * ═══════════════════════════════════════════════════════════════════
 */
@Singleton
class AppCacheService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Cached apps list
    private val _cachedApps = MutableStateFlow<List<CachedApp>>(emptyList())
    val cachedApps: StateFlow<List<CachedApp>> = _cachedApps.asStateFlow()

    // Cache state
    private val _cacheState = MutableStateFlow<CacheState>(CacheState.NotInitialized)
    val cacheState: StateFlow<CacheState> = _cacheState.asStateFlow()

    // Quick lookup maps
    private val appsByName = mutableMapOf<String, CachedApp>()
    private val appsByPackage = mutableMapOf<String, CachedApp>()
    private val appNameVariants = mutableMapOf<String, CachedApp>() // lowercase, no spaces, etc.

    // Icon cache (LRU cache to prevent memory issues)
    private val iconCache = LruCache<String, Bitmap>(100)

    /**
     * Initialize the cache - should be called when app starts
     */
    fun initialize() {
        if (_cacheState.value == CacheState.Loading) return

        scope.launch {
            refreshCache()
        }
    }

    /**
     * Refresh the app cache
     */
    suspend fun refreshCache() {
        _cacheState.value = CacheState.Loading

        try {
            val pm = context.packageManager
            val apps = mutableListOf<CachedApp>()

            // Get all installed applications
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            // Get launchable apps (apps with launcher intent)
            val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = pm.queryIntentActivities(launchIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()

            for (appInfo in installedApps) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                val packageName = appInfo.packageName
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isLaunchable = launchableApps.contains(packageName)

                val version = try {
                    pm.getPackageInfo(packageName, 0).versionName ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }

                val cachedApp = CachedApp(
                    name = appName,
                    packageName = packageName,
                    version = version,
                    isSystemApp = isSystemApp,
                    isLaunchable = isLaunchable
                )

                apps.add(cachedApp)

                // Build lookup maps
                appsByPackage[packageName.lowercase()] = cachedApp
                appsByName[appName.lowercase()] = cachedApp

                // Add name variants for flexible matching
                addNameVariants(appName, cachedApp)
            }

            // Sort by name
            val sortedApps = apps.sortedBy { it.name.lowercase() }
            _cachedApps.value = sortedApps
            _cacheState.value = CacheState.Ready(sortedApps.size)

        } catch (e: Exception) {
            _cacheState.value = CacheState.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Add name variants for flexible matching
     */
    private fun addNameVariants(name: String, app: CachedApp) {
        val lower = name.lowercase()

        // Original lowercase
        appNameVariants[lower] = app

        // Without spaces
        appNameVariants[lower.replace(" ", "")] = app

        // Without special characters
        appNameVariants[lower.replace(Regex("[^a-z0-9]"), "")] = app

        // First word only (for "Google Chrome" -> "chrome")
        val words = lower.split(" ")
        if (words.size > 1) {
            words.forEach { word ->
                if (word.length > 2 && !appNameVariants.containsKey(word)) {
                    appNameVariants[word] = app
                }
            }
        }

        // Common abbreviations
        when {
            lower.contains("youtube") -> appNameVariants["yt"] = app
            lower.contains("whatsapp") -> {
                appNameVariants["wa"] = app
                appNameVariants["whats"] = app
            }
            lower.contains("instagram") -> {
                appNameVariants["ig"] = app
                appNameVariants["insta"] = app
            }
            lower.contains("facebook") -> {
                appNameVariants["fb"] = app
            }
            lower.contains("twitter") || lower.contains("x ") -> {
                appNameVariants["twitter"] = app
                appNameVariants["x"] = app
            }
            lower.contains("telegram") -> {
                appNameVariants["tg"] = app
            }
            lower.contains("chrome") -> {
                appNameVariants["browser"] = app
            }
            lower.contains("calculator") -> {
                appNameVariants["calc"] = app
            }
            lower.contains("calendar") -> {
                appNameVariants["cal"] = app
            }
            lower.contains("camera") -> {
                appNameVariants["cam"] = app
            }
            lower.contains("settings") -> {
                appNameVariants["setting"] = app
            }
            lower.contains("messages") || lower.contains("messaging") -> {
                appNameVariants["msg"] = app
                appNameVariants["sms"] = app
            }
            lower.contains("phone") || lower.contains("dialer") -> {
                appNameVariants["phone"] = app
                appNameVariants["dialer"] = app
                appNameVariants["call"] = app
            }
            lower.contains("gallery") || lower.contains("photos") -> {
                appNameVariants["gallery"] = app
                appNameVariants["photos"] = app
                appNameVariants["photo"] = app
            }
            lower.contains("music") || lower.contains("player") -> {
                appNameVariants["music"] = app
            }
            lower.contains("clock") || lower.contains("alarm") -> {
                appNameVariants["clock"] = app
                appNameVariants["alarm"] = app
            }
            lower.contains("file") || lower.contains("manager") -> {
                appNameVariants["files"] = app
                appNameVariants["filemanager"] = app
            }
        }
    }

    /**
     * Find app by name (flexible matching)
     */
    fun findApp(query: String): CachedApp? {
        val queryLower = query.lowercase().trim()

        // Exact match by package
        appsByPackage[queryLower]?.let { return it }

        // Exact match by name
        appsByName[queryLower]?.let { return it }

        // Match by variant
        appNameVariants[queryLower]?.let { return it }

        // Fuzzy match - starts with
        appsByName.entries.find { it.key.startsWith(queryLower) }?.let { return it.value }
        appNameVariants.entries.find { it.key.startsWith(queryLower) }?.let { return it.value }

        // Fuzzy match - contains
        appsByName.entries.find { it.key.contains(queryLower) }?.let { return it.value }

        return null
    }

    /**
     * Search apps by query
     */
    fun searchApps(query: String): List<CachedApp> {
        val queryLower = query.lowercase().trim()

        return _cachedApps.value.filter { app ->
            app.name.lowercase().contains(queryLower) ||
            app.packageName.lowercase().contains(queryLower)
        }
    }

    /**
     * Get all launchable apps
     */
    fun getLaunchableApps(): List<CachedApp> {
        return _cachedApps.value.filter { it.isLaunchable }
    }

    /**
     * Get user apps (non-system)
     */
    fun getUserApps(): List<CachedApp> {
        return _cachedApps.value.filter { !it.isSystemApp }
    }

    /**
     * Get system apps
     */
    fun getSystemApps(): List<CachedApp> {
        return _cachedApps.value.filter { it.isSystemApp }
    }

    /**
     * Get all apps
     */
    fun getAllApps(): List<CachedApp> {
        return _cachedApps.value
    }

    /**
     * Launch app by name or package
     */
    fun launchApp(query: String): LaunchResult {
        val app = findApp(query)

        if (app == null) {
            return LaunchResult.NotFound(query)
        }

        if (!app.isLaunchable) {
            return LaunchResult.NotLaunchable(app.name)
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                LaunchResult.Success(app)
            } else {
                LaunchResult.NoLaunchIntent(app.name)
            }
        } catch (e: Exception) {
            LaunchResult.Error(app.name, e.message ?: "Unknown error")
        }
    }

    /**
     * Get app icon
     */
    fun getAppIcon(packageName: String): Bitmap? {
        // Check cache first
        iconCache.get(packageName)?.let { return it }

        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            iconCache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        _cachedApps.value = emptyList()
        appsByName.clear()
        appsByPackage.clear()
        appNameVariants.clear()
        iconCache.evictAll()
        _cacheState.value = CacheState.NotInitialized
    }

    /**
     * Cleanup
     */
    fun destroy() {
        scope.cancel()
        clearCache()
    }
}

/**
 * Cached app data
 */
data class CachedApp(
    val name: String,
    val packageName: String,
    val version: String,
    val isSystemApp: Boolean,
    val isLaunchable: Boolean
)

/**
 * Cache state
 */
sealed class CacheState {
    object NotInitialized : CacheState()
    object Loading : CacheState()
    data class Ready(val appCount: Int) : CacheState()
    data class Error(val message: String) : CacheState()
}

/**
 * Launch result
 */
sealed class LaunchResult {
    data class Success(val app: CachedApp) : LaunchResult()
    data class NotFound(val query: String) : LaunchResult()
    data class NotLaunchable(val appName: String) : LaunchResult()
    data class NoLaunchIntent(val appName: String) : LaunchResult()
    data class Error(val appName: String, val message: String) : LaunchResult()
}

