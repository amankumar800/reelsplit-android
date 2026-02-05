package com.reelsplit.domain.model

import kotlinx.serialization.Serializable

/**
 * Data class representing user preferences and settings.
 *
 * @property autoDeleteAfterShare Whether to automatically delete source video after all parts are shared
 * @property showNotifications Whether to show progress notifications during download/split
 * @property defaultVideoQuality Preferred video quality for downloads
 * @property keepScreenOnDuringProcessing Whether to keep screen on during video processing
 */
@Serializable
data class UserSettings(
    val autoDeleteAfterShare: Boolean = false,
    val showNotifications: Boolean = true,
    val defaultVideoQuality: VideoQuality = VideoQuality.HIGH,
    val keepScreenOnDuringProcessing: Boolean = true
)

/**
 * Available video quality options for downloads.
 */
@Serializable
enum class VideoQuality {
    /** Lowest quality, smallest file size */
    LOW,

    /** Balanced quality and file size */
    MEDIUM,

    /** Best quality, largest file size */
    HIGH
}
