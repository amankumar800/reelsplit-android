package com.reelsplit.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a video that has been shared to the app for processing.
 *
 * @property id Unique identifier for the video (UUID format recommended)
 * @property sourceUrl The original URL from which the video was shared (e.g., Instagram reel URL)
 * @property localPath The local file path where the video is stored after download
 * @property durationSeconds Duration of the video in seconds
 * @property fileSizeBytes Size of the video file in bytes
 * @property createdAt Timestamp when the video was added to the app (epoch milliseconds).
 *                     This should be set explicitly when creating the Video instance.
 * @property status Current processing status of the video
 * @property errorMessage Optional error message if status is FAILED
 * @property thumbnailPath Optional path to the video thumbnail
 */
@Serializable
data class Video(
    val id: String,
    val sourceUrl: String,
    val localPath: String? = null,
    val durationSeconds: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val createdAt: Long,
    val status: VideoStatus = VideoStatus.PENDING,
    val errorMessage: String? = null,
    val thumbnailPath: String? = null
) {
    init {
        require(id.isNotBlank()) { "Video id must not be blank" }
        require(sourceUrl.isNotBlank()) { "Source URL must not be blank" }
        require(durationSeconds >= 0) { "Duration must be non-negative" }
        require(fileSizeBytes >= 0) { "File size must be non-negative" }
        require(createdAt > 0) { "Created timestamp must be positive" }
    }

    companion object {
        /** Maximum duration for WhatsApp Status in seconds */
        const val WHATSAPP_MAX_DURATION_SECONDS = 90L

        /**
         * Creates a new Video with the current timestamp.
         * Use this factory method instead of the constructor to ensure proper timestamp.
         */
        fun create(
            id: String,
            sourceUrl: String,
            localPath: String? = null,
            durationSeconds: Long = 0L,
            fileSizeBytes: Long = 0L,
            status: VideoStatus = VideoStatus.PENDING,
            errorMessage: String? = null,
            thumbnailPath: String? = null
        ): Video = Video(
            id = id,
            sourceUrl = sourceUrl,
            localPath = localPath,
            durationSeconds = durationSeconds,
            fileSizeBytes = fileSizeBytes,
            createdAt = System.currentTimeMillis(),
            status = status,
            errorMessage = errorMessage,
            thumbnailPath = thumbnailPath
        )
    }

    /**
     * Returns true if the video needs to be split (duration exceeds WhatsApp limit).
     * WhatsApp Status limit is 90 seconds.
     */
    val needsSplitting: Boolean
        get() = durationSeconds > WHATSAPP_MAX_DURATION_SECONDS

    /**
     * Calculates the estimated number of parts this video will be split into.
     */
    val estimatedParts: Int
        get() = if (durationSeconds <= 0) {
            0
        } else {
            ((durationSeconds + WHATSAPP_MAX_DURATION_SECONDS - 1) / WHATSAPP_MAX_DURATION_SECONDS).toInt()
        }

    /**
     * Returns the duration formatted as MM:SS.
     */
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }
}

/**
 * Represents the current status of a video in the processing pipeline.
 */
@Serializable
enum class VideoStatus {
    /** Video has been added but processing hasn't started */
    PENDING,
    /** Video URL is being extracted/resolved */
    EXTRACTING,
    /** Video is being downloaded */
    DOWNLOADING,
    /** Video is being split into segments */
    SPLITTING,
    /** Video processing is complete */
    COMPLETED,
    /** An error occurred during processing */
    FAILED
}
