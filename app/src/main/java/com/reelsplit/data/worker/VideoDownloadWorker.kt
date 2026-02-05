package com.reelsplit.data.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.michaelbull.result.fold
import com.reelsplit.R
import com.reelsplit.core.constants.AppConstants
import com.reelsplit.data.processing.VideoDownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WorkManager worker for downloading videos in the background.
 *
 * This worker ensures downloads complete even if the app is closed or killed.
 * It uses a foreground service with a progress notification to prevent the system
 * from terminating the download.
 *
 * Features:
 * - Progress notification with download percentage
 * - Automatic retry (up to 3 retries / 4 total attempts) for retryable errors
 * - Proper cancellation handling
 * - Result reporting via output data
 *
 * **Note on Retry Backoff**: WorkManager handles backoff automatically.
 * Configure backoff when building the WorkRequest using `setBackoffCriteria()`.
 *
 * Input Data:
 * - [KEY_VIDEO_URL]: The URL of the video to download (required)
 * - [KEY_FILE_NAME]: The filename for the downloaded video (required)
 * - [KEY_DOWNLOAD_DIR]: Optional custom download directory
 *
 * Output Data (on success):
 * - [KEY_FILE_PATH]: The absolute path to the downloaded file
 *
 * @property videoDownloader Manager for performing the actual download
 */
@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val videoDownloader: VideoDownloadManager
) : CoroutineWorker(context, workerParams) {

    /** Flag to ensure notification channel is only created once */
    private val channelCreated = AtomicBoolean(false)

    private val notificationManager = 
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString(KEY_VIDEO_URL)
        val fileName = inputData.getString(KEY_FILE_NAME)
        val downloadDir = inputData.getString(KEY_DOWNLOAD_DIR)
            ?: videoDownloader.defaultDownloadDirectory

        // Validate required inputs
        if (videoUrl.isNullOrBlank()) {
            Timber.e("VideoDownloadWorker: Missing video URL")
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Video URL is required")
            )
        }

        if (fileName.isNullOrBlank()) {
            Timber.e("VideoDownloadWorker: Missing file name")
            return Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "File name is required")
            )
        }

        Timber.d("VideoDownloadWorker: Starting download attempt ${runAttemptCount + 1}/$MAX_TOTAL_ATTEMPTS")
        Timber.d("VideoDownloadWorker: URL=$videoUrl, FileName=$fileName")

        // Ensure notification channel exists (do this once before any notifications)
        ensureNotificationChannelCreated()

        // Start as foreground service with notification
        try {
            setForeground(createForegroundInfo(0))
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on Android 12+ if app is in background
            // This can happen if the user navigates away before WorkManager starts
            Timber.w(e, "Failed to set foreground. Continuing in background.")
            // Continue anyway - download might still work, just without notification
        }

        // Perform the download
        val result = videoDownloader.downloadVideoSuspend(
            url = videoUrl,
            fileName = fileName,
            onProgress = { progress ->
                // Update notification with progress (fire-and-forget, non-blocking)
                updateProgressAsync(progress)
            },
            downloadDir = downloadDir
        )

        return result.fold(
            success = { filePath ->
                Timber.d("VideoDownloadWorker: Download completed successfully: $filePath")
                // Get file size for the output data
                val fileSize = java.io.File(filePath).length()
                Result.success(
                    workDataOf(
                        KEY_FILE_PATH to filePath,
                        KEY_FILE_SIZE to fileSize
                    )
                )
            },
            failure = { error ->
                Timber.e("VideoDownloadWorker: Download failed: ${error.message}")
                
                // Check if we should retry
                // runAttemptCount is 0-indexed, so < MAX_RETRIES allows MAX_RETRIES retry attempts
                if (error.isRetryable && runAttemptCount < MAX_RETRIES) {
                    Timber.d("VideoDownloadWorker: Scheduling retry (attempt ${runAttemptCount + 2}/$MAX_TOTAL_ATTEMPTS)")
                    Result.retry()
                } else {
                    Timber.e("VideoDownloadWorker: Max retries exceeded or non-retryable error")
                    Result.failure(
                        workDataOf(
                            KEY_ERROR_MESSAGE to error.message,
                            KEY_ERROR_RETRYABLE to error.isRetryable
                        )
                    )
                }
            }
        )
    }

    /**
     * Updates the foreground notification with the current download progress.
     *
     * This is called from a non-suspend callback, so we use the async versions
     * of setProgress and setForeground that return ListenableFuture.
     *
     * Note: These are fire-and-forget updates. If they fail, the download continues.
     */
    private fun updateProgressAsync(progress: Int) {
        try {
            setProgressAsync(workDataOf(KEY_PROGRESS to progress))
            setForegroundAsync(createForegroundInfo(progress))
        } catch (e: Exception) {
            // Ignore notification update failures - download should continue
            Timber.v("Failed to update progress notification: ${e.message}")
        }
    }

    /**
     * Creates the foreground info with a progress notification.
     *
     * This notification is required for API 26+ to run as a foreground service.
     * It shows the download progress and can be used to cancel the download.
     */
    private fun createForegroundInfo(progress: Int): ForegroundInfo {

        val notification = createNotification(progress)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                AppConstants.NOTIFICATION_ID_DOWNLOAD,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                AppConstants.NOTIFICATION_ID_DOWNLOAD,
                notification
            )
        }
    }

    /**
     * Creates the notification for the foreground service.
     *
     * Shows:
     * - App icon and name
     * - Download progress percentage
     * - Progress bar (determinate if progress is known, indeterminate otherwise)
     */
    private fun createNotification(progress: Int): Notification {
        val title = applicationContext.getString(R.string.notification_download_title)
        val text = if (progress > 0) {
            applicationContext.getString(R.string.notification_download_progress, progress)
        } else {
            applicationContext.getString(R.string.notification_download_starting)
        }

        return NotificationCompat.Builder(applicationContext, AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(
                PROGRESS_MAX,
                progress,
                progress == 0 // Indeterminate until we have progress
            )
            .build()
    }

    /**
     * Ensures the notification channel is created exactly once.
     *
     * Required for API 26+ (Android O and above).
     * Uses low importance to avoid interrupting the user.
     * Thread-safe via AtomicBoolean check.
     */
    private fun ensureNotificationChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only create once per worker instance
            if (channelCreated.compareAndSet(false, true)) {
                val channel = NotificationChannel(
                    AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD,
                    AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD_DESC
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
                Timber.d("Download notification channel created")
            }
        }
    }

    companion object {
        // ==================== Input Data Keys ====================
        
        /** Key for the video URL in input data (required) */
        const val KEY_VIDEO_URL = "video_url"
        
        /** Key for the output filename in input data (required) */
        const val KEY_FILE_NAME = "file_name"
        
        /** Key for optional custom download directory in input data */
        const val KEY_DOWNLOAD_DIR = "download_dir"

        // ==================== Output Data Keys ====================
        
        /** Key for the downloaded file path in output data */
        const val KEY_FILE_PATH = "file_path"
        
        /** Key for the downloaded file size in bytes in output data */
        const val KEY_FILE_SIZE = "file_size"
        
        /** Key for error message in output data (on failure) */
        const val KEY_ERROR_MESSAGE = "error_message"
        
        /** Key for error retryability in output data (on failure) */
        const val KEY_ERROR_RETRYABLE = "error_retryable"

        // ==================== Progress Keys ====================
        
        /** Key for download progress percentage in progress data */
        const val KEY_PROGRESS = "progress"

        // ==================== Configuration ====================
        
        /** Maximum number of retry attempts (not including the initial attempt) */
        private const val MAX_RETRIES = 3
        
        /** Total number of attempts (initial + retries) */
        private const val MAX_TOTAL_ATTEMPTS = MAX_RETRIES + 1
        
        /** Maximum progress value for the notification progress bar */
        private const val PROGRESS_MAX = 100

        // ==================== Work Request Helpers ====================
        
        /** Unique work name for video downloads (used for work chaining/deduplication) */
        const val UNIQUE_WORK_NAME = "video_download_work"
    }
}
