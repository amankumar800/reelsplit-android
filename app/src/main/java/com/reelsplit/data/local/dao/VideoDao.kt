package com.reelsplit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.reelsplit.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [VideoEntity] providing CRUD operations and reactive queries.
 * 
 * This DAO uses Kotlin Flow for reactive data streams, enabling automatic UI updates
 * when the underlying data changes. All suspend functions are safe for use with
 * Kotlin coroutines.
 */
@Dao
interface VideoDao {
    
    // ==================== Reactive Queries (Flow) ====================
    
    /**
     * Observes all videos ordered by creation date (newest first).
     * 
     * @return A Flow emitting the list of all videos whenever the data changes
     */
    @Query("SELECT * FROM videos ORDER BY created_at DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>
    
    /**
     * Observes a single video by its ID.
     * 
     * @param id The unique identifier of the video
     * @return A Flow emitting the video or null if not found
     */
    @Query("SELECT * FROM videos WHERE id = :id")
    fun observeVideoById(id: String): Flow<VideoEntity?>
    
    /**
     * Observes videos filtered by status.
     * 
     * @param status The status to filter by (as string representation of [VideoStatus])
     * @return A Flow emitting videos with the specified status
     */
    @Query("SELECT * FROM videos WHERE status = :status ORDER BY created_at DESC")
    fun getVideosByStatus(status: String): Flow<List<VideoEntity>>
    
    /**
     * Observes the count of videos with a specific status.
     * 
     * @param status The status to count
     * @return A Flow emitting the count of videos with that status
     */
    @Query("SELECT COUNT(*) FROM videos WHERE status = :status")
    fun getVideoCountByStatus(status: String): Flow<Int>
    
    // ==================== One-shot Queries ====================
    
    /**
     * Retrieves a single video by its ID.
     * 
     * @param id The unique identifier of the video
     * @return The video entity or null if not found
     */
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?
    
    /**
     * Retrieves a page of videos for Paging 3 integration.
     * Videos are ordered by creation date (newest first).
     * 
     * @param limit The maximum number of videos to return
     * @param offset The number of videos to skip
     * @return A list of videos for the specified page
     */
    @Query("SELECT * FROM videos ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getVideosPage(limit: Int, offset: Int): List<VideoEntity>
    
    /**
     * Gets the total count of all videos in the database.
     * 
     * @return The total number of videos
     */
    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
    
    /**
     * Checks if a video with the given source URL already exists.
     * 
     * @param sourceUrl The source URL to check
     * @return True if a video with this URL exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM videos WHERE source_url = :sourceUrl)")
    suspend fun existsBySourceUrl(sourceUrl: String): Boolean
    
    /**
     * Gets a video by its source URL.
     * 
     * @param sourceUrl The source URL to search for
     * @return The video entity or null if not found
     */
    @Query("SELECT * FROM videos WHERE source_url = :sourceUrl LIMIT 1")
    suspend fun getVideoBySourceUrl(sourceUrl: String): VideoEntity?
    
    // ==================== Insert Operations ====================
    
    /**
     * Inserts a new video into the database.
     * If a video with the same ID already exists, it will be replaced.
     * 
     * @param video The video entity to insert
     * @return The row ID of the inserted video
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long
    
    /**
     * Inserts multiple videos into the database.
     * Existing videos with the same IDs will be replaced.
     * 
     * @param videos The list of video entities to insert
     * @return The list of row IDs for the inserted videos
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>): List<Long>
    
    /**
     * Inserts or updates a video (upsert operation).
     * 
     * @param video The video entity to upsert
     */
    @Upsert
    suspend fun upsertVideo(video: VideoEntity)
    
    // ==================== Update Operations ====================
    
    /**
     * Updates an existing video in the database.
     * 
     * @param video The video entity with updated values
     * @return The number of rows updated (should be 1 if successful)
     */
    @Update
    suspend fun updateVideo(video: VideoEntity): Int
    
    /**
     * Updates the status of a video.
     * 
     * @param id The ID of the video to update
     * @param status The new status value (as string representation of [VideoStatus])
     * @return The number of rows updated
     */
    @Query("UPDATE videos SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String): Int
    
    /**
     * Updates the status and error message of a video.
     * Useful for setting FAILED status with an error description.
     * 
     * @param id The ID of the video to update
     * @param status The new status value
     * @param errorMessage The error message (can be null)
     * @return The number of rows updated
     */
    @Query("UPDATE videos SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateStatusWithError(id: String, status: String, errorMessage: String?): Int
    
    /**
     * Updates the local path of a video after download completes.
     * 
     * @param id The ID of the video to update
     * @param localPath The new local file path
     * @return The number of rows updated
     */
    @Query("UPDATE videos SET local_path = :localPath WHERE id = :id")
    suspend fun updateLocalPath(id: String, localPath: String): Int
    
    /**
     * Updates video metadata after processing.
     * 
     * @param id The ID of the video to update
     * @param durationSeconds The video duration in seconds
     * @param fileSizeBytes The file size in bytes
     * @param thumbnailPath Optional path to the thumbnail
     * @return The number of rows updated
     */
    @Query("""
        UPDATE videos 
        SET duration_seconds = :durationSeconds, 
            file_size_bytes = :fileSizeBytes, 
            thumbnail_path = :thumbnailPath 
        WHERE id = :id
    """)
    suspend fun updateMetadata(
        id: String,
        durationSeconds: Long,
        fileSizeBytes: Long,
        thumbnailPath: String?
    ): Int
    
    // ==================== Delete Operations ====================
    
    /**
     * Deletes a video by its ID.
     * 
     * @param id The ID of the video to delete
     * @return The number of rows deleted
     */
    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String): Int
    
    /**
     * Deletes a video entity.
     * 
     * @param video The video entity to delete
     * @return The number of rows deleted
     */
    @Delete
    suspend fun deleteVideoEntity(video: VideoEntity): Int
    
    /**
     * Deletes all videos from the database.
     * Use with caution - this removes all video history.
     * 
     * @return The number of rows deleted
     */
    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos(): Int
    
    /**
     * Deletes videos older than the specified timestamp.
     * Useful for cleanup of old entries.
     * 
     * @param timestamp The cutoff timestamp (epoch milliseconds)
     * @return The number of rows deleted
     */
    @Query("DELETE FROM videos WHERE created_at < :timestamp")
    suspend fun deleteVideosOlderThan(timestamp: Long): Int
    
    /**
     * Deletes videos with a specific status.
     * Useful for cleaning up failed or completed videos.
     * 
     * @param status The status of videos to delete
     * @return The number of rows deleted
     */
    @Query("DELETE FROM videos WHERE status = :status")
    suspend fun deleteVideosByStatus(status: String): Int
}
