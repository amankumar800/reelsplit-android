package com.reelsplit.domain.repository

import com.github.michaelbull.result.Result
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.DownloadProgress
import com.reelsplit.domain.model.Video
import com.reelsplit.domain.model.VideoSegment
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for video operations.
 *
 * This interface defines the contract for all video-related data operations,
 * enabling separation between the domain and data layers. Implementations
 * can use different data sources (local database, network, cache, etc.).
 *
 * All methods that can fail return [Result] from kotlin-result for type-safe
 * error handling. Streaming operations use [Flow] for reactive data access.
 *
 * ## Thread Safety
 * Implementations MUST be thread-safe. All methods can be called from any
 * coroutine context. Database operations should use Room's built-in thread
 * safety, and network operations should use appropriate dispatchers.
 *
 * ## Note on Interface Size
 * This interface combines URL extraction, downloading, splitting, and CRUD
 * operations intentionally for app simplicity. For larger apps, consider splitting
 * into separate interfaces (e.g., VideoExtractionRepository, VideoStorageRepository).
 */
interface VideoRepository {

    // ============================================
    // URL Extraction Operations
    // ============================================

    /**
     * Extracts the direct video URL from an Instagram reel URL.
     *
     * This operation resolves the shareable Instagram URL to a downloadable
     * video URL using youtubedl-android or similar extraction mechanism.
     *
     * @param instagramUrl The Instagram reel URL (e.g., https://www.instagram.com/reel/xxx)
     * @return [Result] with the direct video URL on success,
     *         or [AppError] on failure (e.g., network error, invalid URL, rate limited)
     */
    suspend fun extractVideoUrl(instagramUrl: String): Result<String, AppError>

    // ============================================
    // Download Operations
    // ============================================

    /**
     * Downloads a video from the given URL.
     *
     * Emits [DownloadProgress] states as the download progresses:
     * - [DownloadProgress.Queued] - Download is queued
     * - [DownloadProgress.Downloading] - Download in progress with percentage
     * - [DownloadProgress.Completed] - Download finished successfully
     * - [DownloadProgress.Failed] - Download failed with error (includes [AppError])
     * - [DownloadProgress.Paused] - Download was paused
     * - [DownloadProgress.Cancelled] - Download was cancelled
     *
     * Note: This returns Flow instead of Result because downloads emit multiple
     * progress states. Error states are represented by [DownloadProgress.Failed]
     * which includes the underlying [AppError].
     *
     * @param url The direct video URL to download
     * @param fileName The desired file name for the downloaded video
     * @param downloadId Unique identifier for this download (for cancellation support)
     * @return A [Flow] emitting [DownloadProgress] states during the download
     */
    fun downloadVideo(url: String, fileName: String, downloadId: String): Flow<DownloadProgress>

    /**
     * Cancels an ongoing download.
     *
     * @param downloadId The unique identifier of the download to cancel
     */
    suspend fun cancelDownload(downloadId: String)

    // ============================================
    // Video Split Operations
    // ============================================

    /**
     * Splits a video into segments suitable for WhatsApp Status.
     *
     * Each segment will be at most 90 seconds and under 16MB to meet
     * WhatsApp Status requirements. Uses Media3 Transformer for processing.
     *
     * @param videoId The unique identifier of the parent video (used to link segments)
     * @param inputPath The local file path of the video to split
     * @return [Result] containing a list of [VideoSegment]s on success,
     *         or [AppError] on failure (e.g., codec error, storage full)
     */
    suspend fun splitVideo(videoId: String, inputPath: String): Result<List<VideoSegment>, AppError>

    // ============================================
    // Video CRUD Operations
    // ============================================

    /**
     * Retrieves a video by its unique identifier.
     *
     * @param id The unique identifier of the video
     * @return A [Flow] emitting the [Video] if found, or null if not found.
     *         The Flow allows observing changes to the video.
     */
    fun getVideoById(id: String): Flow<Video?>

    /**
     * Retrieves all videos stored in the repository.
     *
     * @return A [Flow] emitting a list of all [Video]s, sorted by creation date
     *         (most recent first). Empty list if no videos exist.
     */
    fun getAllVideos(): Flow<List<Video>>

    /**
     * Saves or updates a video in the repository.
     *
     * If a video with the same ID already exists, it will be updated.
     * Otherwise, a new video entry will be created.
     *
     * @param video The [Video] to save or update
     * @return [Result] with Unit on success, or [AppError] on failure
     *         (e.g., database error, storage full)
     */
    suspend fun saveVideo(video: Video): Result<Unit, AppError>

    /**
     * Deletes a video and its associated files from the repository.
     *
     * This will also delete any associated video segments and their files.
     *
     * @param id The unique identifier of the video to delete
     * @return [Result] with Unit on success, or [AppError] on failure
     *         (e.g., file not found, permission denied)
     */
    suspend fun deleteVideo(id: String): Result<Unit, AppError>

    // ============================================
    // Segment Operations
    // ============================================

    /**
     * Retrieves all segments for a specific video.
     *
     * @param videoId The unique identifier of the parent video
     * @return A [Flow] emitting a list of [VideoSegment]s for the video,
     *         ordered by part number
     */
    fun getSegmentsByVideoId(videoId: String): Flow<List<VideoSegment>>

    /**
     * Retrieves a specific segment by its unique identifier.
     *
     * @param segmentId The unique identifier of the segment
     * @return A [Flow] emitting the [VideoSegment] if found, or null if not found
     */
    fun getSegmentById(segmentId: String): Flow<VideoSegment?>

    /**
     * Saves a video segment to the repository.
     *
     * @param segment The [VideoSegment] to save
     * @return [Result] with Unit on success, or [AppError] on failure
     */
    suspend fun saveSegment(segment: VideoSegment): Result<Unit, AppError>

    /**
     * Marks a segment as shared.
     *
     * @param segmentId The unique identifier of the segment
     * @return [Result] with Unit on success, or [AppError] on failure
     */
    suspend fun markSegmentAsShared(segmentId: String): Result<Unit, AppError>

    /**
     * Deletes a specific segment and its associated file.
     *
     * @param segmentId The unique identifier of the segment to delete
     * @return [Result] with Unit on success, or [AppError] on failure
     */
    suspend fun deleteSegment(segmentId: String): Result<Unit, AppError>
}
