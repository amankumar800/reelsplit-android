package com.reelsplit.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Represents a segment of a split video, designed to fit within WhatsApp Status limits.
 *
 * Each segment is a portion of the original video, split to be at most 90 seconds
 * and within the 16MB file size limit for WhatsApp Status.
 *
 * @property id Unique identifier for this segment
 * @property videoId Reference to the parent video's ID
 * @property partNumber The sequential part number (1-indexed)
 * @property totalParts Total number of parts in the parent video
 * @property filePath The local file path where this segment is stored
 * @property durationSeconds Duration of this segment in seconds
 * @property fileSizeBytes Size of this segment file in bytes
 * @property startTimeSeconds The start time of this segment in the original video
 * @property endTimeSeconds The end time of this segment in the original video
 * @property isShared Whether this segment has been shared to WhatsApp
 */
@Serializable
data class VideoSegment(
    val id: String,
    val videoId: String,
    val partNumber: Int,
    val totalParts: Int,
    val filePath: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val startTimeSeconds: Long,
    val endTimeSeconds: Long,
    val isShared: Boolean = false
) {
    init {
        require(id.isNotBlank()) { "Segment id must not be blank" }
        require(videoId.isNotBlank()) { "Video id must not be blank" }
        require(filePath.isNotBlank()) { "File path must not be blank" }
        require(partNumber >= 1) { "Part number must be at least 1" }
        require(totalParts >= 1) { "Total parts must be at least 1" }
        require(partNumber <= totalParts) { "Part number ($partNumber) cannot exceed total parts ($totalParts)" }
        require(durationSeconds >= 0) { "Duration must be non-negative" }
        require(fileSizeBytes >= 0) { "File size must be non-negative" }
        require(startTimeSeconds >= 0) { "Start time must be non-negative" }
        require(endTimeSeconds >= startTimeSeconds) { "End time must be >= start time" }
    }

    companion object {
        /** Maximum duration for WhatsApp Status in seconds */
        const val WHATSAPP_MAX_DURATION_SECONDS = 90L

        /** Maximum file size for WhatsApp Status in bytes (16MB) */
        const val WHATSAPP_MAX_SIZE_BYTES = 16L * 1024 * 1024 // 16,777,216 bytes
    }

    /**
     * Checks if this segment meets WhatsApp Status requirements.
     * - Duration must be ≤ 90 seconds
     * - File size must be ≤ 16MB (16,777,216 bytes)
     */
    val isValidForWhatsAppStatus: Boolean
        get() = durationSeconds <= WHATSAPP_MAX_DURATION_SECONDS &&
                fileSizeBytes <= WHATSAPP_MAX_SIZE_BYTES

    /**
     * Returns a formatted display name for this segment (e.g., "Part 1 of 3").
     */
    val displayName: String
        get() = "Part $partNumber of $totalParts"

    /**
     * Returns the file size formatted as a human-readable string.
     */
    val formattedSize: String
        get() = when {
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", fileSizeBytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
        }

    /**
     * Returns the duration formatted as MM:SS.
     */
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
}
