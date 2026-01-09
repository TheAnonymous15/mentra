package com.example.mentra.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Media item (audio/video)
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String, // MediaStore ID or file path hash
    val title: String,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val duration: Long, // in milliseconds
    val filePath: String,
    val mimeType: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val albumArtPath: String?,
    val playCount: Int = 0,
    val lastPlayed: Long? = null,
    val isFavorite: Boolean = false
)

/**
 * Playlist
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int = 0,
    val coverArtPath: String?
)

/**
 * Playlist items (many-to-many relationship)
 */
@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val mediaId: String,
    val position: Int,
    val addedAt: Long
)

