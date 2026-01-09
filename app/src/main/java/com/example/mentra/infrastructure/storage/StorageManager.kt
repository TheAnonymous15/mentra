package com.example.mentra.infrastructure.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage management and file system operations
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get internal storage info
     */
    fun getInternalStorageInfo(): StorageInfo {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val total = statFs.totalBytes
        val available = statFs.availableBytes
        val used = total - available

        return StorageInfo(
            type = StorageType.INTERNAL,
            totalBytes = total,
            usedBytes = used,
            availableBytes = available,
            percentUsed = (used.toFloat() / total.toFloat() * 100).toInt()
        )
    }

    /**
     * Get external storage info
     */
    fun getExternalStorageInfo(): StorageInfo? {
        if (!isExternalStorageAvailable()) return null

        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val total = statFs.totalBytes
        val available = statFs.availableBytes
        val used = total - available

        return StorageInfo(
            type = StorageType.EXTERNAL,
            totalBytes = total,
            usedBytes = used,
            availableBytes = available,
            percentUsed = (used.toFloat() / total.toFloat() * 100).toInt()
        )
    }

    /**
     * Check if external storage is available
     */
    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Get app-specific internal directory
     */
    fun getAppInternalDir(): File {
        return context.filesDir
    }

    /**
     * Get app-specific external directory
     */
    fun getAppExternalDir(): File? {
        return context.getExternalFilesDir(null)
    }

    /**
     * Get cache directory
     */
    fun getCacheDir(): File {
        return context.cacheDir
    }

    /**
     * Get external cache directory
     */
    fun getExternalCacheDir(): File? {
        return context.externalCacheDir
    }

    /**
     * Create directory
     */
    suspend fun createDirectory(path: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists()) {
                Result.success(dir)
            } else {
                val created = dir.mkdirs()
                if (created) {
                    Result.success(dir)
                } else {
                    Result.failure(Exception("Failed to create directory"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete file or directory
     */
    suspend fun delete(path: String, recursive: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val deleted = if (recursive && file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copy file
     */
    suspend fun copyFile(source: String, destination: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source)
            val destFile = File(destination)

            sourceFile.copyTo(destFile, overwrite = true)
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Move file
     */
    suspend fun moveFile(source: String, destination: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(source)
            val destFile = File(destination)

            val moved = sourceFile.renameTo(destFile)
            if (moved) {
                Result.success(destFile)
            } else {
                Result.failure(Exception("Failed to move file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List files in directory
     */
    suspend fun listFiles(path: String): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()?.toList() ?: emptyList()
                Result.success(files)
            } else {
                Result.failure(Exception("Not a directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get file info
     */
    fun getFileInfo(path: String): FileInfo? {
        val file = File(path)
        if (!file.exists()) return null

        return FileInfo(
            path = file.absolutePath,
            name = file.name,
            size = file.length(),
            isDirectory = file.isDirectory,
            isFile = file.isFile,
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            lastModified = file.lastModified()
        )
    }

    /**
     * Read file as string
     */
    suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = File(path).readText()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Write string to file
     */
    suspend fun writeFile(path: String, content: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.writeText(content)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Append to file
     */
    suspend fun appendToFile(path: String, content: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.appendText(content)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Storage information
 */
data class StorageInfo(
    val type: StorageType,
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val percentUsed: Int
) {
    val totalGB: Float get() = totalBytes / 1_000_000_000f
    val usedGB: Float get() = usedBytes / 1_000_000_000f
    val availableGB: Float get() = availableBytes / 1_000_000_000f
}

/**
 * Storage type
 */
enum class StorageType {
    INTERNAL,
    EXTERNAL
}

/**
 * File information
 */
data class FileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val isFile: Boolean,
    val canRead: Boolean,
    val canWrite: Boolean,
    val lastModified: Long
) {
    val sizeKB: Float get() = size / 1024f
    val sizeMB: Float get() = size / 1_048_576f
}

/**
 * Cache manager
 */
@Singleton
class CacheManager @Inject constructor(
    private val storageManager: StorageManager
) {

    /**
     * Get cache size
     */
    fun getCacheSize(): Long {
        return calculateDirectorySize(storageManager.getCacheDir())
    }

    /**
     * Clear cache
     */
    suspend fun clearCache(): Result<Boolean> {
        return storageManager.delete(storageManager.getCacheDir().path, recursive = true)
    }

    /**
     * Calculate directory size
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}

