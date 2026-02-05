package com.reelsplit.data.processing

import android.content.Context
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.Status
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.reelsplit.di.IoDispatcher
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.ProcessingStage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps PRDownloader for HTTP video downloads with progress tracking.
 *
 * This manager provides a clean interface for downloading video files from URLs,
 * with support for:
 * - Progress callbacks for UI updates
 * - Download cancellation via download ID
 * - Coroutine-based suspend function with Result type for functional error handling
 * - File validation after download completion
 * - Input validation for URLs and filenames
 *
 * **Thread Safety Note**: Callbacks (`onProgress`, `onComplete`, `onError`) are invoked
 * on a background thread. If updating UI, ensure you switch to the main dispatcher.
 *
 * Important: PRDownloader must be initialized in the Application class before using this manager.
 * See [com.reelsplit.ReelSplitApplication] for initialization logic.
 *
 * @property context Application context for accessing cache directory.
 * @property ioDispatcher Dispatcher for IO-bound operations, injected for testability.
 */
@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Default download directory (app's cache directory).
     * Can be overridden per-download via [downloadVideo] overload.
     */
    val defaultDownloadDirectory: String
        get() = context.cacheDir.absolutePath

    /**
     * Minimum valid video file size in bytes.
     * Files smaller than this are considered corrupted or incomplete.
     */
    private companion object {
        const val MIN_VIDEO_FILE_SIZE_BYTES = 1024L // 1 KB minimum
    }

    /**
     * Downloads a video from the given URL with progress reporting.
     *
     * Uses PRDownloader internally which supports:
     * - HTTPS connections (recommended, HTTP may fail on Android 9+)
     * - Pause/Resume functionality
     * - Parallel downloads
     *
     * **Thread Safety**: Callbacks are invoked on a background thread.
     *
     * @param url The HTTPS URL of the video to download (HTTPS-only recommended).
     * @param fileName The name for the downloaded file (e.g., "reel_123456789.mp4").
     *                 Must be a valid filename without path separators.
     * @param onProgress Callback invoked with download progress percentage (0-100).
     * @param onComplete Callback invoked with the absolute file path on successful completion.
     * @param onError Callback invoked with an [AppError] if download fails.
     * @param downloadDir Optional custom download directory (defaults to cache dir).
     * @return The download ID which can be used to cancel the download via [cancelDownload],
     *         or -1 if validation fails (error will be reported via onError callback).
     */
    fun downloadVideo(
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit,
        onComplete: (String) -> Unit,
        onError: (AppError) -> Unit,
        downloadDir: String = defaultDownloadDirectory
    ): Int {
        // Validate URL
        val urlValidationError = validateUrl(url)
        if (urlValidationError != null) {
            Timber.e("URL validation failed: ${urlValidationError.message}")
            onError(urlValidationError)
            return INVALID_DOWNLOAD_ID
        }
        
        // Validate and sanitize filename
        val sanitizedFileName = sanitizeFileName(fileName)
        if (sanitizedFileName == null) {
            val error = AppError.InvalidUrlError(
                message = "Invalid filename provided",
                url = url,
                isRetryable = false
            )
            Timber.e("Filename validation failed: $fileName")
            onError(error)
            return INVALID_DOWNLOAD_ID
        }
        
        Timber.d("Starting download: $sanitizedFileName from URL")
        
        // Warn if using HTTP instead of HTTPS
        if (!url.startsWith("https://", ignoreCase = true)) {
            Timber.w("Non-HTTPS URL detected. This may fail on Android 9+ due to cleartext traffic restrictions.")
        }
        
        return PRDownloader.download(url, downloadDir, sanitizedFileName)
            .build()
            .setOnStartOrResumeListener {
                Timber.d("Download started/resumed: $sanitizedFileName")
            }
            .setOnPauseListener {
                Timber.d("Download paused: $sanitizedFileName")
            }
            .setOnCancelListener {
                Timber.d("Download cancelled: $sanitizedFileName")
            }
            .setOnProgressListener { progress ->
                if (progress.totalBytes > 0) {
                    val percent = ((progress.currentBytes * 100) / progress.totalBytes).toInt()
                    onProgress(percent.coerceIn(0, 100))
                }
            }
            .start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    val filePath = "$downloadDir${File.separator}$sanitizedFileName"
                    val validationResult = validateDownloadedFile(filePath)
                    
                    when (validationResult) {
                        is FileValidationResult.Valid -> {
                            Timber.d("Download complete: $filePath (${validationResult.fileSize} bytes)")
                            onComplete(filePath)
                        }
                        is FileValidationResult.Invalid -> {
                            Timber.e("File validation failed: ${validationResult.error.message}")
                            onError(validationResult.error)
                        }
                    }
                }

                override fun onError(error: Error) {
                    val appError = mapDownloadError(error)
                    Timber.e("Download failed: ${appError.message}")
                    onError(appError)
                }
            })
    }

    /**
     * Downloads a video using coroutines with Result type error handling.
     *
     * This is the recommended API for downloading videos as it:
     * - Uses kotlin-result for consistent error handling
     * - Properly handles coroutine cancellation
     * - Validates downloaded files
     * - Validates inputs before starting download
     *
     * @param url The HTTPS URL of the video to download.
     * @param fileName The name for the downloaded file.
     * @param onProgress Optional callback for progress updates (0-100). Called on IO dispatcher.
     * @param downloadDir Optional custom download directory (defaults to cache dir).
     * @return [Result] containing either the file path or an [AppError].
     */
    suspend fun downloadVideoSuspend(
        url: String,
        fileName: String,
        onProgress: ((Int) -> Unit)? = null,
        downloadDir: String = defaultDownloadDirectory
    ): Result<String, AppError> = withContext(ioDispatcher) {
        try {
            suspendCancellableCoroutine { continuation ->
                val downloadId = downloadVideo(
                    url = url,
                    fileName = fileName,
                    onProgress = { progress -> onProgress?.invoke(progress) },
                    onComplete = { filePath ->
                        if (continuation.isActive) {
                            continuation.resume(Ok(filePath))
                        }
                    },
                    onError = { error ->
                        if (continuation.isActive) {
                            continuation.resume(Err(error))
                        }
                    },
                    downloadDir = downloadDir
                )
                
                // If validation failed synchronously, downloadId will be INVALID_DOWNLOAD_ID
                // The error callback will have been called already, so no action needed here
                
                // Handle cancellation - cancel the download if coroutine is cancelled
                continuation.invokeOnCancellation {
                    if (downloadId != INVALID_DOWNLOAD_ID) {
                        Timber.d("Download coroutine cancelled, cancelling download ID: $downloadId")
                        cancelDownload(downloadId)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Re-throw cancellation to maintain structured concurrency
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during download")
            Err(
                AppError.ProcessingError(
                    message = "Unexpected download error: ${e.message ?: "Unknown error"}",
                    stage = ProcessingStage.DOWNLOAD,
                    isRetryable = false
                )
            )
        }
    }

    /**
     * Cancels an in-progress download.
     *
     * @param downloadId The download ID returned by [downloadVideo].
     */
    fun cancelDownload(downloadId: Int) {
        if (downloadId == INVALID_DOWNLOAD_ID) {
            Timber.w("Attempted to cancel invalid download ID")
            return
        }
        Timber.d("Cancelling download with ID: $downloadId")
        PRDownloader.cancel(downloadId)
    }

    /**
     * Pauses an in-progress download.
     *
     * The download can be resumed later using [resumeDownload].
     *
     * @param downloadId The download ID returned by [downloadVideo].
     */
    fun pauseDownload(downloadId: Int) {
        if (downloadId == INVALID_DOWNLOAD_ID) {
            Timber.w("Attempted to pause invalid download ID")
            return
        }
        Timber.d("Pausing download with ID: $downloadId")
        PRDownloader.pause(downloadId)
    }

    /**
     * Resumes a paused download.
     *
     * @param downloadId The download ID returned by [downloadVideo].
     */
    fun resumeDownload(downloadId: Int) {
        if (downloadId == INVALID_DOWNLOAD_ID) {
            Timber.w("Attempted to resume invalid download ID")
            return
        }
        Timber.d("Resuming download with ID: $downloadId")
        PRDownloader.resume(downloadId)
    }

    /**
     * Gets the current status of a download.
     *
     * @param downloadId The download ID returned by [downloadVideo].
     * @return The current [DownloadStatus] of the download.
     */
    fun getDownloadStatus(downloadId: Int): DownloadStatus {
        if (downloadId == INVALID_DOWNLOAD_ID) {
            return DownloadStatus.UNKNOWN
        }
        return when (PRDownloader.getStatus(downloadId)) {
            Status.RUNNING -> DownloadStatus.RUNNING
            Status.PAUSED -> DownloadStatus.PAUSED
            Status.COMPLETED -> DownloadStatus.COMPLETED
            Status.CANCELLED -> DownloadStatus.CANCELLED
            Status.FAILED -> DownloadStatus.FAILED
            Status.QUEUED -> DownloadStatus.QUEUED
            Status.UNKNOWN -> DownloadStatus.UNKNOWN
            null -> DownloadStatus.UNKNOWN
        }
    }

    /**
     * Cancels all in-progress downloads.
     *
     * Useful for cleanup when the app is being closed or reset.
     */
    fun cancelAllDownloads() {
        Timber.d("Cancelling all downloads")
        PRDownloader.cancelAll()
    }

    // ==================== Validation Helpers ====================

    /**
     * Validates the download URL.
     *
     * @return null if valid, or an [AppError] describing the issue.
     */
    private fun validateUrl(url: String): AppError? {
        return when {
            url.isBlank() -> AppError.InvalidUrlError(
                message = "URL cannot be empty",
                url = url,
                isRetryable = false
            )
            !url.startsWith("http://", ignoreCase = true) && 
            !url.startsWith("https://", ignoreCase = true) -> AppError.InvalidUrlError(
                message = "URL must start with http:// or https://",
                url = url,
                isRetryable = false
            )
            else -> null
        }
    }

    /**
     * Sanitizes the filename to prevent path traversal attacks and invalid characters.
     *
     * @return The sanitized filename, or null if the filename is invalid.
     */
    private fun sanitizeFileName(fileName: String): String? {
        if (fileName.isBlank()) return null
        
        // Remove any path separators to prevent path traversal
        val sanitized = fileName
            .replace("/", "")
            .replace("\\", "")
            .replace("..", "")
            .trim()
        
        // Check for valid characters (alphanumeric, underscore, hyphen, period)
        if (sanitized.isBlank() || sanitized.startsWith(".")) {
            return null
        }
        
        // Ensure it has a reasonable length
        if (sanitized.length > 255) {
            return sanitized.take(255)
        }
        
        return sanitized
    }

    /**
     * Validates the downloaded file.
     *
     * @return [FileValidationResult] indicating success or failure with error details.
     */
    private fun validateDownloadedFile(filePath: String): FileValidationResult {
        val file = File(filePath)
        
        return when {
            !file.exists() -> {
                FileValidationResult.Invalid(
                    AppError.StorageError(
                        message = "Downloaded file not found",
                        path = filePath,
                        isRetryable = true
                    )
                )
            }
            file.length() == 0L -> {
                // Clean up empty file
                file.delete()
                FileValidationResult.Invalid(
                    AppError.NetworkError(
                        message = "Downloaded file is empty. The video may no longer be available.",
                        isRetryable = true
                    )
                )
            }
            file.length() < MIN_VIDEO_FILE_SIZE_BYTES -> {
                // File too small to be a valid video
                Timber.w("Downloaded file is suspiciously small: ${file.length()} bytes")
                // Still allow it but log warning - some thumbnails might be small
                FileValidationResult.Valid(file.length())
            }
            else -> {
                FileValidationResult.Valid(file.length())
            }
        }
    }

    /**
     * Maps PRDownloader [Error] to the appropriate [AppError] type.
     *
     * Analyzes the error to determine the most appropriate error category
     * for consistent error handling across the application.
     */
    private fun mapDownloadError(error: Error): AppError {
        return when {
            error.isConnectionError -> {
                val message = error.connectionException?.message
                when {
                    message?.contains("timeout", ignoreCase = true) == true ->
                        AppError.NetworkError(
                            message = "Connection timed out. Please try again.",
                            isRetryable = true
                        )
                    message?.contains("host", ignoreCase = true) == true ->
                        AppError.NetworkError(
                            message = "Unable to connect to server. Please check your internet connection.",
                            isRetryable = true
                        )
                    message?.contains("ssl", ignoreCase = true) == true ||
                    message?.contains("certificate", ignoreCase = true) == true ->
                        AppError.NetworkError(
                            message = "Secure connection failed. Please try again.",
                            isRetryable = true
                        )
                    else ->
                        AppError.NetworkError(
                            message = "Connection error. Please check your internet connection.",
                            isRetryable = true
                        )
                }
            }
            error.isServerError -> {
                val serverMessage = error.serverErrorMessage ?: "Unknown server error"
                val responseCode = error.responseCode
                AppError.NetworkError(
                    message = "Server error: $serverMessage",
                    statusCode = responseCode,
                    isRetryable = responseCode in 500..599
                )
            }
            else -> {
                AppError.ProcessingError(
                    message = error.connectionException?.message ?: "Download failed",
                    stage = ProcessingStage.DOWNLOAD,
                    isRetryable = true
                )
            }
        }
    }

    // ==================== Inner Types ====================

    /**
     * Result of file validation after download.
     */
    private sealed class FileValidationResult {
        data class Valid(val fileSize: Long) : FileValidationResult()
        data class Invalid(val error: AppError) : FileValidationResult()
    }

    /**
     * Download status enum that wraps PRDownloader's Status.
     * Provides a clean API without exposing internal library types.
     */
    enum class DownloadStatus {
        QUEUED,
        RUNNING,
        PAUSED,
        COMPLETED,
        CANCELLED,
        FAILED,
        UNKNOWN
    }
}

/**
 * Constant for invalid download ID returned when validation fails.
 */
private const val INVALID_DOWNLOAD_ID = -1
