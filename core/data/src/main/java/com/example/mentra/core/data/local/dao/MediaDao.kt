package com.example.mentra.core.data.local.dao

import androidx.room.*
import com.example.mentra.core.data.local.entity.MediaItemEntity
import com.example.mentra.core.data.local.entity.PlaylistEntity
import com.example.mentra.core.data.local.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for media items
 */
@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items ORDER BY title ASC")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mimeType LIKE 'audio/%' ORDER BY title ASC")
    fun getAudioItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mimeType LIKE 'video/%' ORDER BY title ASC")
    fun getVideoItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE artist = :artist ORDER BY title ASC")
    fun getItemsByArtist(artist: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE album = :album ORDER BY title ASC")
    fun getItemsByAlbum(album: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaItemEntity>>

    @Query("UPDATE media_items SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :mediaId")
    suspend fun incrementPlayCount(mediaId: String, timestamp: Long)

    @Query("UPDATE media_items SET isFavorite = :favorite WHERE id = :mediaId")
    suspend fun updateFavorite(mediaId: String, favorite: Boolean)

    @Delete
    suspend fun deleteMediaItem(item: MediaItemEntity)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()
}

/**
 * DAO for playlists
 */
@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
}

/**
 * DAO for playlist items
 */
@Dao
interface PlaylistItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getItemsForPlaylist(playlistId: Long): List<PlaylistItemEntity>

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeItemFromPlaylist(playlistId: Long, mediaId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
}

