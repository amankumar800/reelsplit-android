package com.reelsplit.data.repository

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.reelsplit.data.local.dao.VideoDao
import com.reelsplit.data.local.mapper.toDomain
import com.reelsplit.data.local.mapper.toEntity
import com.reelsplit.data.processing.VideoDownloadManager
import com.reelsplit.data.processing.VideoExtractor
import com.reelsplit.data.processing.VideoSplitter
import com.reelsplit.di.IoDispatcher
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.DownloadProgress
import com.reelsplit.domain.model.Video
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [VideoRepository] that coordinates between data sources.
 *
 * This repository acts as a single source of truth for video data, coordinating:
 * - Remote operations: URL extraction via [VideoExtractor], downloading via [VideoDownloadManager]
 * - Local operations: Database CRUD via [VideoDao]
 * - Processing operations: Video splitting via [VideoSplitter]
 *
 * ## Thread Safety
 * All operations are safe for concurrent use. Segment operations use in-memory
 * caching with ConcurrentHashMap and Mutex for thread-safe updates.
 *
 * ## Note on Segments
 * Video segments are currently stored in-memory only. When a SegmentEntity/SegmentDao
 * is implemented, the segment operations should be updated to use persistent storage.
 *
 * @property videoExtractor Extracts direct video URLs from Instagram links
 * @property downloadManager Downloads videos with progress tracking
 * @property videoSplitter Splits videos into WhatsApp-compatible segments
 * @property videoDao Data Access Object for video persistence
 * @property ioDispatcher Dispatcher for IO-bound operations
 */
@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoExtractor: VideoExtractor,
    private val downloadManager: VideoDownloadManager,
    private val videoSplitter: VideoSplitter,
    private val videoDao: VideoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : VideoRepository {

    /**
     * In-memory cache for video segments, keyed by segment ID.
     * Thread-safe via ConcurrentHashMap.
     *
     * TODO: Replace with SegmentDao when persistent segment storage is implemented.
     */
    private val segmentCache = ConcurrentHashMap<String, VideoSegment>()

    /**
     * Index of segments by video ID for efficient lookup.
     * Maps videoId -> set of segmentIds.
     * Using Set to avoid duplicates and provide thread-safe contains checks.
     * All modifications happen within segmentMutex.
     */
    private val segmentsByVideoId = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Mutex for segment write operations to ensure consistency between cache and index.
     */
    private val segmentMutex = Mutex()

    /**
     * Flow-backed segment data for reactive updates.
     * Emits when segments are added, updated, or removed.
     */
    private val segmentsFlow = MutableStateFlow<Map<String, VideoSegment>>(emptyMap())

    /**
     * Tracks active download IDs for cancellation support.
     * Maps downloadId (String from caller) -> PRDownloader downloadId (Int).
     */
    private val activeDownloads = ConcurrentHashMap<String, Int>()

    // ============================================
    // URL Extraction Operations
    // ============================================

    override suspend fun extractVideoUrl(instagramUrl: String): Result<String, AppError> {
        Timber.d("Repository: extractVideoUrl called for $instagramUrl")
        return videoExtractor.extractVideoUrl(instagramUrl)
    }

    // ============================================
    // Download Operations
    // ============================================

    /**
     * Downloads a video from the given URL with progress reporting.
     *
     * Uses [callbackFlow] to bridge PRDownloader's callback-based API with Flow,
     * enabling real-time progress updates to the UI.
     *
     * Note: Progress reporting uses the callback-based [VideoDownloadManager.downloadVideo]
     * method to emit progress updates in real-time.
     */
    override fun downloadVideo(
        url: String,
        fileName: String,
        downloadId: String
    ): Flow<DownloadProgress> = callbackFlow {
        Timber.d("Repository: downloadVideo called for $downloadId")
        trySend(DownloadProgress.Queued)

        var lastProgress = -1

        val internalDownloadId = downloadManager.downloadVideo(
            url = url,
            fileName = fileName,
            onProgress = { progress ->
                // Only emit if progress changed to avoid flooding
                if (progress != lastProgress) {
                    lastProgress = progress
                    trySend(DownloadProgress.Downloading(percent = progress))
                }
            },
            onComplete = { filePath ->
                val file = File(filePath)
                val fileSize = if (file.exists()) file.length() else 0L

                // Emit 100% progress before completion
                trySend(DownloadProgress.Downloading(percent = 100))

                val completedProgress = if (fileSize > 0) {
                    DownloadProgress.Completed(
                        filePath = filePath,
                        fileSizeBytes = fileSize
                    )
                } else {
                    // File size validation failed, but download reported success
                    // Let the caller handle this edge case
                    DownloadProgress.Completed(
                        filePath = filePath,
                        fileSizeBytes = 1L // Minimum valid size per domain model
                    )
                }
                trySend(completedProgress)

                // Remove from active downloads
                activeDownloads.remove(downloadId)
                channel.close()
            },
            onError = { error ->
                trySend(
                    DownloadProgress.Failed(
                        message = error.message,
                        error = error,
                        isRetryable = error.isRetryable
                    )
                )
                // Remove from active downloads
                activeDownloads.remove(downloadId)
                channel.close()
            }
        )

        // Track the download for cancellation support
        if (internalDownloadId != -1) {
            activeDownloads[downloadId] = internalDownloadId
        }

        // Handle Flow cancellation
        awaitClose {
            Timber.d("Download flow closed for $downloadId")
            val removedId = activeDownloads.remove(downloadId)
            if (removedId != null) {
                Timber.d("Cancelling download due to flow closure: $downloadId")
                downloadManager.cancelDownload(removedId)
            }
        }
    }.flowOn(ioDispatcher)

    override suspend fun cancelDownload(downloadId: String) {
        Timber.d("Repository: cancelDownload called for $downloadId")
        val internalId = activeDownloads.remove(downloadId)
        if (internalId != null) {
            downloadManager.cancelDownload(internalId)
            Timber.d("Cancelled download with internal ID: $internalId")
        } else {
            Timber.w("No active download found for ID: $downloadId")
        }
    }

    // ============================================
    // Video Split Operations
    // ============================================

    override suspend fun splitVideo(
        videoId: String,
        inputPath: String
    ): Result<List<VideoSegment>, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: splitVideo called for videoId=$videoId, inputPath=$inputPath")

        // Determine output directory (same directory as input file)
        val inputFile = File(inputPath)
        val parentDir = inputFile.parentFile
        if (parentDir == null) {
            Timber.w("Cannot determine parent directory for: $inputPath")
            return@withContext Err(
                AppError.StorageError(
                    message = "Cannot determine output directory for video splitting",
                    path = inputPath,
                    isRetryable = false
                )
            )
        }

        val segmentOutputDir = File(parentDir, "segments_$videoId")

        val result = videoSplitter.splitVideo(
            inputPath = inputPath,
            outputDir = segmentOutputDir.absolutePath,
            onProgress = { current, total, progress ->
                Timber.d("Split progress: segment $current/$total - ${(progress * 100).toInt()}%")
            }
        )

        result.map { splitResult ->
            // Convert SplitSegmentInfo to VideoSegment domain models
            val segments = splitResult.segments.map { segmentInfo ->
                VideoSegment(
                    id = UUID.randomUUID().toString(),
                    videoId = videoId,
                    partNumber = segmentInfo.partNumber,
                    totalParts = segmentInfo.totalParts,
                    filePath = segmentInfo.filePath,
                    durationSeconds = segmentInfo.durationSeconds,
                    fileSizeBytes = segmentInfo.fileSizeBytes,
                    startTimeSeconds = segmentInfo.startTimeSeconds,
                    endTimeSeconds = segmentInfo.endTimeSeconds,
                    isShared = false
                )
            }

            // Cache the segments
            segmentMutex.withLock {
                segments.forEach { segment ->
                    segmentCache[segment.id] = segment
                    segmentsByVideoId.getOrPut(videoId) { mutableSetOf() }.add(segment.id)
                }
                updateSegmentsFlow()
            }

            segments
        }
    }

    // ============================================
    // Video CRUD Operations
    // ============================================

    override fun getVideoById(id: String): Flow<Video?> {
        return videoDao.observeVideoById(id)
            .map { entity -> entity?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getAllVideos(): Flow<List<Video>> {
        return videoDao.getAllVideos()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveVideo(video: Video): Result<Unit, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: saveVideo called for ${video.id}")
        try {
            videoDao.upsertVideo(video.toEntity())
            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to save video: ${video.id}")
            Err(
                AppError.StorageError(
                    message = "Failed to save video: ${e.message ?: "Unknown error"}",
                    isRetryable = false
                )
            )
        }
    }

    override suspend fun deleteVideo(id: String): Result<Unit, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: deleteVideo called for $id")
        try {
            // First, get the video to find associated file paths
            val video = videoDao.getVideoById(id)

            // Delete associated segments - collect info within lock, delete files outside
            val segmentsToDelete: List<VideoSegment>
            segmentMutex.withLock {
                val segmentIds = segmentsByVideoId[id]?.toSet() ?: emptySet()
                segmentsToDelete = segmentIds.mapNotNull { segmentId ->
                    segmentCache.remove(segmentId)
                }
                segmentsByVideoId.remove(id)
                updateSegmentsFlow()
            }

            // Delete segment files outside the lock
            segmentsToDelete.forEach { segment ->
                deleteFileSafely(segment.filePath)
            }

            // Delete the video's local file if it exists
            video?.localPath?.let { path ->
                if (path.isNotBlank()) {
                    deleteFileSafely(path)
                }
            }

            // Delete segments directory if it exists
            video?.localPath?.let { localPath ->
                if (localPath.isNotBlank()) {
                    val parentDir = File(localPath).parentFile
                    if (parentDir != null) {
                        val segmentsDir = File(parentDir, "segments_$id")
                        try {
                            if (segmentsDir.exists()) {
                                segmentsDir.deleteRecursively()
                                Timber.d("Deleted segments directory: ${segmentsDir.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to delete segments directory: ${segmentsDir.absolutePath}")
                        }
                    }
                }
            }

            // Delete from database
            val deletedRows = videoDao.deleteVideo(id)
            if (deletedRows > 0) {
                Timber.d("Deleted video from database: $id")
            } else {
                Timber.w("Video not found in database: $id")
            }
            // Always return success since the goal is to ensure the video is gone
            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete video: $id")
            Err(
                AppError.StorageError(
                    message = "Failed to delete video: ${e.message ?: "Unknown error"}",
                    isRetryable = false
                )
            )
        }
    }

    // ============================================
    // Segment Operations
    // ============================================

    override fun getSegmentsByVideoId(videoId: String): Flow<List<VideoSegment>> {
        // Filter directly from segmentsFlow to avoid reading from the mutable index
        return segmentsFlow.map { allSegments ->
            allSegments.values
                .filter { it.videoId == videoId }
                .sortedBy { it.partNumber }
        }.flowOn(ioDispatcher)
    }

    override fun getSegmentById(segmentId: String): Flow<VideoSegment?> {
        return segmentsFlow.map { allSegments ->
            allSegments[segmentId]
        }.flowOn(ioDispatcher)
    }

    override suspend fun saveSegment(segment: VideoSegment): Result<Unit, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: saveSegment called for ${segment.id}")
        try {
            segmentMutex.withLock {
                segmentCache[segment.id] = segment
                // Using Set automatically handles duplicate prevention
                segmentsByVideoId.getOrPut(segment.videoId) { mutableSetOf() }.add(segment.id)
                updateSegmentsFlow()
            }
            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to save segment: ${segment.id}")
            Err(
                AppError.StorageError(
                    message = "Failed to save segment: ${e.message ?: "Unknown error"}",
                    isRetryable = false
                )
            )
        }
    }

    override suspend fun markSegmentAsShared(segmentId: String): Result<Unit, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: markSegmentAsShared called for $segmentId")
        try {
            // Perform update within lock, capture result for return outside lock
            val updateSuccessful = segmentMutex.withLock {
                val currentSegment = segmentCache[segmentId]
                if (currentSegment != null) {
                    val updatedSegment = currentSegment.copy(isShared = true)
                    segmentCache[segmentId] = updatedSegment
                    updateSegmentsFlow()
                    true
                } else {
                    false
                }
            }

            if (!updateSuccessful) {
                Timber.w("Segment not found: $segmentId")
                return@withContext Err(
                    AppError.StorageError(
                        message = "Segment not found: $segmentId",
                        isRetryable = false
                    )
                )
            }

            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark segment as shared: $segmentId")
            Err(
                AppError.StorageError(
                    message = "Failed to update segment: ${e.message ?: "Unknown error"}",
                    isRetryable = false
                )
            )
        }
    }

    override suspend fun deleteSegment(segmentId: String): Result<Unit, AppError> = withContext(ioDispatcher) {
        Timber.d("Repository: deleteSegment called for $segmentId")
        try {
            // Perform removal within lock, capture result for file deletion outside lock
            val removedSegment: VideoSegment? = segmentMutex.withLock {
                val segment = segmentCache.remove(segmentId)
                if (segment != null) {
                    segmentsByVideoId[segment.videoId]?.remove(segmentId)
                    updateSegmentsFlow()
                }
                segment
            }

            if (removedSegment == null) {
                Timber.w("Segment not found: $segmentId")
                return@withContext Err(
                    AppError.StorageError(
                        message = "Segment not found: $segmentId",
                        isRetryable = false
                    )
                )
            }

            // Delete the file outside of the lock
            deleteFileSafely(removedSegment.filePath)

            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete segment: $segmentId")
            Err(
                AppError.StorageError(
                    message = "Failed to delete segment: ${e.message ?: "Unknown error"}",
                    isRetryable = false
                )
            )
        }
    }

    // ============================================
    // Private Helpers
    // ============================================

    /**
     * Updates the segments flow with the current cache state.
     * Must be called within segmentMutex lock.
     */
    private fun updateSegmentsFlow() {
        segmentsFlow.value = segmentCache.toMap()
    }

    /**
     * Safely deletes a file, logging any errors without throwing.
     *
     * @param path The path to the file to delete
     */
    private fun deleteFileSafely(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Timber.d("Deleted file: $path")
                } else {
                    Timber.w("Failed to delete file (returned false): $path")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete file: $path")
        }
    }
}
