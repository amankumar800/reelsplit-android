package com.reelsplit.domain.usecase

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.reelsplit.data.worker.VideoDownloadWorker
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Use case for downloading videos using WorkManager.
 *
 * This use case encapsulates the business logic for initiating a video download
 * via WorkManager and observing its progress. It ensures downloads continue
 * even when the app is in the background or killed.
 *
 * Features:
 * - NetworkType.CONNECTED constraint - requires network connectivity
 * - RequiresBatteryNotLow constraint - pauses when battery is low
 * - Exponential backoff for retries (30s initial, WorkManager min is 10s)
 * - Maps WorkInfo states to domain [DownloadProgress] model
 *
 * ## Architecture Note
 * This use case imports from the data layer ([VideoDownloadWorker]) which is typically
 * avoided in Clean Architecture. This is an intentional trade-off for this app since:
 * 1. The worker keys are stable constants unlikely to change
 * 2. Creating an abstraction layer would add complexity without benefit
 * 3. The use case still encapsulates the WorkManager orchestration logic
 *
 * For larger apps, consider creating a WorkerRepository interface.
 *
 * Usage:
 * ```
 * val downloadVideo = DownloadVideoUseCase(workManager)
 * downloadVideo(videoUrl, fileName)
 *     .collect { progress ->
 *         when (progress) {
 *             is DownloadProgress.Queued -> { /* show queued state */ }
 *             is DownloadProgress.Downloading -> { /* show progress */ }
 *             is DownloadProgress.Completed -> { /* navigate to result */ }
 *             is DownloadProgress.Failed -> { /* show error */ }
 *             else -> { /* handle other states */ }
 *         }
 *     }
 * ```
 */
class DownloadVideoUseCase(
    private val workManager: WorkManager
) {
    /**
     * Initiates a video download and returns a Flow of progress updates.
     *
     * The download is performed by [VideoDownloadWorker] which runs as a
     * foreground service with a progress notification.
     *
     * @param videoUrl The direct video URL to download
     * @param fileName The desired filename for the downloaded video
     * @param downloadId Optional unique identifier for this download.
     *                   If null, uses fileName as the unique work name.
     * @return [Flow] emitting [DownloadProgress] states during the download.
     *         Returns [DownloadProgress.Failed] immediately if inputs are invalid.
     */
    operator fun invoke(
        videoUrl: String,
        fileName: String,
        downloadId: String? = null
    ): Flow<DownloadProgress> {
        // Input validation - return early with Failed state if invalid
        if (videoUrl.isBlank()) {
            return flowOf(
                DownloadProgress.Failed(
                    message = "Video URL cannot be empty",
                    error = AppError.InvalidUrlError(
                        message = "Video URL cannot be empty",
                        url = videoUrl
                    ),
                    isRetryable = false
                )
            )
        }
        
        if (fileName.isBlank()) {
            return flowOf(
                DownloadProgress.Failed(
                    message = "File name cannot be empty",
                    error = AppError.ProcessingError(
                        message = "File name cannot be empty"
                    ),
                    isRetryable = false
                )
            )
        }

        // Define constraints for the download
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Build input data for the worker
        val inputData = workDataOf(
            VideoDownloadWorker.KEY_VIDEO_URL to videoUrl,
            VideoDownloadWorker.KEY_FILE_NAME to fileName
        )

        // Create the work request with constraints and backoff policy
        val downloadRequest = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_VIDEO_DOWNLOAD)
            .build()

        // Use fileName as unique work name if downloadId is not provided
        val uniqueWorkName = downloadId ?: "${UNIQUE_WORK_PREFIX}_$fileName"

        // Enqueue the work with REPLACE policy to cancel any existing work with same name
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        // Observe work progress and map to domain model
        return workManager
            .getWorkInfoByIdFlow(downloadRequest.id)
            .map { workInfo -> mapWorkInfoToProgress(workInfo) }
    }

    /**
     * Cancels an ongoing download by its unique identifier.
     *
     * Note: The downloadId must match exactly what was passed to [invoke].
     * If no downloadId was provided when starting the download, you must construct
     * the unique work name manually using [getUniqueWorkName].
     *
     * @param downloadId The unique identifier of the download to cancel
     */
    fun cancel(downloadId: String) {
        workManager.cancelUniqueWork(downloadId)
    }

    /**
     * Cancels a download by fileName when no downloadId was provided.
     *
     * Use this when you started a download without providing a custom downloadId.
     *
     * @param fileName The fileName that was used when starting the download
     */
    fun cancelByFileName(fileName: String) {
        workManager.cancelUniqueWork(getUniqueWorkName(fileName))
    }

    /**
     * Cancels all video downloads.
     */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_VIDEO_DOWNLOAD)
    }

    /**
     * Generates the unique work name for a download.
     *
     * Useful when you need to cancel a download that was started without
     * an explicit downloadId.
     *
     * @param fileName The fileName used when starting the download
     * @return The unique work name that WorkManager uses for this download
     */
    fun getUniqueWorkName(fileName: String): String {
        return "${UNIQUE_WORK_PREFIX}_$fileName"
    }

    /**
     * Maps WorkInfo to the domain [DownloadProgress] model.
     *
     * This method translates WorkManager states to our domain-specific
     * progress states, extracting progress percentage and output data.
     */
    private fun mapWorkInfoToProgress(workInfo: WorkInfo?): DownloadProgress {
        if (workInfo == null) {
            return DownloadProgress.Queued
        }

        return when (workInfo.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> DownloadProgress.Queued

            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt(
                    VideoDownloadWorker.KEY_PROGRESS,
                    0
                )
                DownloadProgress.Downloading(
                    percent = progress,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    speedBytesPerSecond = 0L
                )
            }

            WorkInfo.State.SUCCEEDED -> {
                val filePath = workInfo.outputData.getString(
                    VideoDownloadWorker.KEY_FILE_PATH
                )
                
                val fileSize = workInfo.outputData.getLong(
                    VideoDownloadWorker.KEY_FILE_SIZE,
                    0L
                )
                
                // Handle case where filePath is missing (shouldn't happen but defensive)
                if (filePath.isNullOrBlank()) {
                    return DownloadProgress.Failed(
                        message = "Download completed but file path is missing",
                        error = AppError.StorageError(
                            message = "Download completed but file path is missing"
                        ),
                        isRetryable = false
                    )
                }
                
                DownloadProgress.Completed(
                    filePath = filePath,
                    // Use reported size, or default to 1 if not provided
                    // (Completed requires positive size)
                    fileSizeBytes = if (fileSize > 0) fileSize else 1L
                )
            }

            WorkInfo.State.FAILED -> {
                val errorMessage = workInfo.outputData.getString(
                    VideoDownloadWorker.KEY_ERROR_MESSAGE
                ) ?: "Download failed"
                
                val isRetryable = workInfo.outputData.getBoolean(
                    VideoDownloadWorker.KEY_ERROR_RETRYABLE,
                    true
                )
                
                DownloadProgress.Failed(
                    message = errorMessage,
                    error = AppError.NetworkError(
                        message = errorMessage,
                        isRetryable = isRetryable
                    ),
                    isRetryable = isRetryable
                )
            }

            WorkInfo.State.CANCELLED -> DownloadProgress.Cancelled
        }
    }

    companion object {
        /** Tag for all video download work requests */
        private const val TAG_VIDEO_DOWNLOAD = "video_download"
        
        /** Prefix for unique work names */
        private const val UNIQUE_WORK_PREFIX = "download"
        
        /**
         * Initial backoff delay in seconds for exponential backoff.
         * WorkManager minimum is 10 seconds (BackoffPolicy.MIN_BACKOFF_MILLIS).
         */
        private const val INITIAL_BACKOFF_DELAY_SECONDS = 30L
    }
}
