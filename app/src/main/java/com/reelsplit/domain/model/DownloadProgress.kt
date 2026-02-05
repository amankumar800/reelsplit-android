package com.reelsplit.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Represents the current state of a video download operation.
 *
 * This sealed class provides a type-safe way to represent all possible
 * states during the download lifecycle.
 */
@Serializable
sealed class DownloadProgress {
    /**
     * The download is queued and waiting to start.
     */
    @Serializable
    data object Queued : DownloadProgress()

    /**
     * The download is in progress.
     *
     * @property percent The current download progress (0-100)
     * @property downloadedBytes The number of bytes downloaded so far
     * @property totalBytes The total size of the file being downloaded (0 if unknown)
     * @property speedBytesPerSecond Current download speed in bytes per second
     */
    @Serializable
    data class Downloading(
        val percent: Int,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSecond: Long = 0L
    ) : DownloadProgress() {
        init {
            require(percent in 0..100) { "Percent must be between 0 and 100, was $percent" }
            require(downloadedBytes >= 0) { "Downloaded bytes must be non-negative" }
            require(totalBytes >= 0) { "Total bytes must be non-negative" }
            require(speedBytesPerSecond >= 0) { "Speed must be non-negative" }
        }

        /**
         * Estimated time remaining in seconds, or null if unknown.
         */
        val estimatedTimeRemaining: Long?
            get() = if (speedBytesPerSecond > 0 && totalBytes > downloadedBytes) {
                (totalBytes - downloadedBytes) / speedBytesPerSecond
            } else null

        /**
         * Returns a formatted progress string (e.g., "5.2 MB / 10.0 MB").
         */
        val formattedProgress: String
            get() = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"

        /**
         * Returns a formatted speed string (e.g., "1.5 MB/s").
         */
        val formattedSpeed: String
            get() = "${formatBytes(speedBytesPerSecond)}/s"
    }

    /**
     * The download has been paused.
     *
     * @property percent The progress when paused (0-100)
     * @property downloadedBytes The number of bytes downloaded before pausing
     * @property totalBytes The total size of the file being downloaded
     */
    @Serializable
    data class Paused(
        val percent: Int,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L
    ) : DownloadProgress() {
        init {
            require(percent in 0..100) { "Percent must be between 0 and 100, was $percent" }
            require(downloadedBytes >= 0) { "Downloaded bytes must be non-negative" }
            require(totalBytes >= 0) { "Total bytes must be non-negative" }
        }
    }

    /**
     * The download has completed successfully.
     *
     * @property filePath The local file path where the downloaded file is stored
     * @property fileSizeBytes The final size of the downloaded file
     */
    @Serializable
    data class Completed(
        val filePath: String,
        val fileSizeBytes: Long
    ) : DownloadProgress() {
        init {
            require(filePath.isNotBlank()) { "File path must not be blank" }
            require(fileSizeBytes > 0) { "File size must be positive" }
        }
    }

    /**
     * The download has failed.
     *
     * @property message A human-readable error message describing the failure
     * @property error Optional underlying error that caused the failure
     * @property isRetryable Whether this error can be retried
     */
    @Serializable
    data class Failed(
        val message: String,
        val error: AppError? = null,
        val isRetryable: Boolean = true
    ) : DownloadProgress() {
        init {
            require(message.isNotBlank()) { "Error message must not be blank" }
        }
    }

    /**
     * The download was cancelled by the user.
     */
    @Serializable
    data object Cancelled : DownloadProgress()

    companion object {
        /**
         * Formats bytes into a human-readable string.
         */
        internal fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
