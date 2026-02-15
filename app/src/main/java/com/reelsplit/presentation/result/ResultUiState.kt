package com.reelsplit.presentation.result

import androidx.compose.runtime.Stable
import com.reelsplit.core.base.UiEvent
import com.reelsplit.domain.model.VideoSegment
import java.util.Locale

// =============================================================================
// Result UI State
// =============================================================================

/**
 * UI state for the ResultScreen.
 *
 * Models the screen's lifecycle from loading segments through displaying them
 * with sharing capabilities. Uses [@Stable] because [Loaded.segments] holds
 * a Kotlin `List` which is read-only but not deeply immutable — we promise
 * Compose that changes are always signalled via new state emissions from
 * `StateFlow`.
 */
@Stable
sealed interface ResultUiState {

    /**
     * Loading segments from the repository.
     */
    data object Loading : ResultUiState

    /**
     * Segments loaded and ready for display.
     *
     * @property segments The list of video segments to display.
     * @property videoId The ID of the parent video.
     * @property isSharingAll Whether "Share All" is currently in progress.
     * @property isSaving Whether "Save to Gallery" is currently in progress.
     */
    data class Loaded(
        val segments: List<VideoSegment>,
        val videoId: String,
        val isSharingAll: Boolean = false,
        val isSaving: Boolean = false
    ) : ResultUiState {

        /** Total number of segments. */
        val totalSegments: Int get() = segments.size

        /** Number of segments that have been shared. */
        val sharedCount: Int get() = segments.count { it.isShared }

        /** Total duration of all segments in seconds. */
        val totalDurationSeconds: Long get() = segments.sumOf { it.durationSeconds }

        /** Formatted total duration as MM:SS. */
        val formattedTotalDuration: String
            get() {
                val total = totalDurationSeconds
                val minutes = total / 60
                val seconds = total % 60
                return String.format(Locale.US, "%d:%02d", minutes, seconds)
            }

        /** Whether all segments have been shared. */
        val allShared: Boolean get() = segments.all { it.isShared }

        /** Number of segments valid for WhatsApp Status. */
        val validSegmentCount: Int get() = segments.count { it.isValidForWhatsAppStatus }
    }

    /**
     * Error loading segments.
     *
     * @property message Human-readable error description.
     */
    data class Error(
        val message: String
    ) : ResultUiState
}

// =============================================================================
// Result Navigation Args
// =============================================================================

/**
 * Navigation arguments for ResultScreen.
 *
 * @property videoId The ID of the processed video whose segments to display.
 */
data class ResultNavArgs(
    val videoId: String
)

// =============================================================================
// Result Events (one-time actions)
// =============================================================================

/**
 * One-time UI events emitted by [ResultViewModel].
 */
sealed class ResultEvent : UiEvent {

    /**
     * Navigate back to the home screen.
     */
    data object NavigateHome : ResultEvent()

    /**
     * Show an error message for a failed share operation.
     *
     * @property message The error message to display.
     */
    data class ShowShareError(val message: String) : ResultEvent()

    /**
     * Show confirmation that segments were saved to gallery.
     *
     * @property savedCount The number of segments successfully saved.
     * @property failedCount The number of segments that failed to save.
     */
    data class ShowSavedToGallery(
        val savedCount: Int,
        val failedCount: Int = 0
    ) : ResultEvent()

    /**
     * Trigger the in-app review flow.
     */
    data object RequestInAppReview : ResultEvent()
}
