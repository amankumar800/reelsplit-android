package com.reelsplit.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the overall processing state of a video in the app.
 *
 * This sealed class provides a comprehensive view of where a video
 * is in the processing pipeline, from initial receipt to completion.
 */
@Serializable
sealed class ProcessingState {
    /**
     * No processing is currently happening.
     */
    @Serializable
    data object Idle : ProcessingState()

    /**
     * Extracting the video URL from the shared link.
     * This involves using youtubedl-android to resolve the actual video URL.
     *
     * @property sourceUrl The original URL being extracted
     * @property message Optional status message for UI display
     */
    @Serializable
    data class Extracting(
        val sourceUrl: String,
        val message: String = "Extracting video information..."
    ) : ProcessingState()

    /**
     * Downloading the video file.
     *
     * @property progress The current download progress
     * @property videoUrl The resolved video URL being downloaded
     */
    @Serializable
    data class Downloading(
        val progress: DownloadProgress,
        val videoUrl: String? = null
    ) : ProcessingState()

    /**
     * Splitting the video into segments for WhatsApp Status.
     *
     * @property currentPart The current part being processed (1-indexed)
     * @property totalParts The total number of parts the video will be split into
     * @property progressPercent Overall splitting progress (0-100)
     */
    @Serializable
    data class Splitting(
        val currentPart: Int,
        val totalParts: Int,
        val progressPercent: Int = 0
    ) : ProcessingState() {
        init {
            require(totalParts >= 1) { "Total parts must be at least 1" }
            require(currentPart >= 1) { "Current part must be at least 1" }
            require(currentPart <= totalParts) { "Current part ($currentPart) cannot exceed total parts ($totalParts)" }
            require(progressPercent in 0..100) { "Progress percent must be between 0 and 100" }
        }

        /**
         * Returns a human-readable progress message.
         */
        val progressMessage: String
            get() = "Splitting part $currentPart of $totalParts ($progressPercent%)"
    }

    /**
     * Processing has completed successfully.
     *
     * @property segments The list of video segments created
     */
    @Serializable
    data class Complete(
        val segments: List<VideoSegment>
    ) : ProcessingState() {
        init {
            require(segments.isNotEmpty()) { "Completed state must have at least one segment" }
        }

        /**
         * Returns the total number of segments.
         */
        val segmentCount: Int
            get() = segments.size

        /**
         * Returns the total duration of all segments.
         */
        val totalDurationSeconds: Long
            get() = segments.sumOf { it.durationSeconds }

        /**
         * Returns the total file size of all segments.
         */
        val totalFileSizeBytes: Long
            get() = segments.sumOf { it.fileSizeBytes }
    }

    /**
     * An error occurred during processing.
     *
     * @property error The error that occurred
     * @property failedAt Description of which stage the error occurred at
     * @property isRetryable Whether the operation can be retried
     */
    @Serializable
    data class Error(
        val error: AppError,
        val failedAt: ProcessingStage? = null,
        val isRetryable: Boolean = true
    ) : ProcessingState()
}

/**
 * Represents the stage at which processing failed.
 */
@Serializable
enum class ProcessingStage {
    EXTRACTION,
    DOWNLOAD,
    SPLITTING,
    SAVING
}
