package com.reelsplit.presentation.processing

import androidx.compose.runtime.Stable
import com.reelsplit.core.base.UiEvent
import com.reelsplit.domain.model.DownloadProgress.Companion.formatBytes
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.domain.model.VideoSegment

// =============================================================================
// Processing UI State
// =============================================================================

/**
 * UI state for the ProcessingScreen.
 *
 * Models every stage of the extract → download → split pipeline with
 * stage-specific data for accurate progress display.
 *
 * Uses [@Stable] instead of [@Immutable] because [Complete.segments] holds
 * a Kotlin `List` which is read-only but not deeply immutable.
 */
@Stable
sealed interface ProcessingUiState {

    /**
     * Initial state — orchestration has not started yet.
     */
    data object Queued : ProcessingUiState

    /**
     * Extracting the direct video URL from the Instagram link.
     * Progress is indeterminate because the extraction duration is unknown.
     *
     * @property sourceUrl The original Instagram URL being processed.
     */
    data class Extracting(
        val sourceUrl: String
    ) : ProcessingUiState

    /**
     * Downloading the video file.
     *
     * Computed properties ([formattedProgress], [formattedSpeed], [estimatedTimeRemaining])
     * delegate to [formatBytes] from the domain layer to avoid duplication.
     *
     * @property percent Download progress percentage (0–100).
     * @property downloadedBytes Bytes downloaded so far.
     * @property totalBytes Total file size in bytes (0 if unknown).
     * @property speedBytesPerSecond Current download speed.
     */
    data class Downloading(
        val percent: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSecond: Long = 0L
    ) : ProcessingUiState {

        /** Human-readable progress, e.g. "5.2 MB / 10.0 MB". */
        val formattedProgress: String
            get() = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"

        /** Human-readable speed, e.g. "1.5 MB/s". */
        val formattedSpeed: String
            get() = "${formatBytes(speedBytesPerSecond)}/s"

        /** Estimated time remaining in seconds, or null if unknown. */
        val estimatedTimeRemaining: Long?
            get() = if (speedBytesPerSecond > 0 && totalBytes > downloadedBytes) {
                (totalBytes - downloadedBytes) / speedBytesPerSecond
            } else null
    }

    /**
     * Splitting the downloaded video into WhatsApp Status–compatible segments.
     *
     * @property currentPart The 1-indexed part currently being processed.
     * @property totalParts Total number of parts expected.
     * @property progressPercent Overall splitting progress (0–100).
     */
    data class Splitting(
        val currentPart: Int = 1,
        val totalParts: Int = 1,
        val progressPercent: Int = 0
    ) : ProcessingUiState

    /**
     * Processing completed successfully.
     *
     * @property segments The resulting video segments.
     * @property videoId The ID of the processed video.
     */
    data class Complete(
        val segments: List<VideoSegment>,
        val videoId: String
    ) : ProcessingUiState

    /**
     * An error occurred during processing.
     *
     * @property message Human-readable error description.
     * @property isRetryable Whether the user can retry the operation.
     * @property failedAt The pipeline stage where the failure occurred.
     */
    data class Error(
        val message: String,
        val isRetryable: Boolean = true,
        val failedAt: ProcessingStage? = null
    ) : ProcessingUiState
}

// =============================================================================
// Processing Events (one-time actions)
// =============================================================================

/**
 * One-time UI events emitted by [ProcessingViewModel].
 */
sealed class ProcessingEvent : UiEvent {

    /**
     * Navigate to the result screen after successful processing.
     *
     * @property videoId The ID of the processed video.
     */
    data class NavigateToResult(val videoId: String) : ProcessingEvent()

    /**
     * Navigate back to the previous screen (e.g. after cancel or dismissing error).
     */
    data object NavigateBack : ProcessingEvent()
}
