package com.reelsplit.presentation.history

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.reelsplit.core.base.BaseViewModel
import com.reelsplit.core.base.UiEvent
import com.reelsplit.data.local.dao.VideoDao
import com.reelsplit.data.paging.VideoHistoryPagingSource
import com.reelsplit.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// History UI State
// =============================================================================

/**
 * UI state for the HistoryScreen.
 *
 * Minimal since Paging 3's [LazyPagingItems] manages its own loading / error /
 * empty states internally. This state exists for any auxiliary UI flags
 * (e.g., deletion confirmation) that may be added in the future.
 */
data class HistoryUiState(
    /** Whether a delete operation is currently in progress. */
    val isDeleting: Boolean = false
)

// =============================================================================
// History Events (one-time actions)
// =============================================================================

/**
 * One-time UI events emitted by [HistoryViewModel].
 */
sealed class HistoryEvent : UiEvent {

    /**
     * Navigate to the result screen for a specific video.
     *
     * @property videoId The ID of the video to view results for.
     */
    data class NavigateToResult(val videoId: String) : HistoryEvent()

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : HistoryEvent()
}

// =============================================================================
// HistoryViewModel
// =============================================================================

/**
 * ViewModel for the HistoryScreen.
 *
 * Provides paginated video history via [pagingFlow] and handles user
 * interactions such as tapping a video or navigating back.
 *
 * ### Paging Strategy
 * Uses [Pager] with [VideoHistoryPagingSource] for offset-based pagination.
 * The paging flow is cached in [viewModelScope] so it survives configuration
 * changes without re-fetching data.
 *
 * @param videoDao DAO for creating the [VideoHistoryPagingSource].
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val videoDao: VideoDao
) : BaseViewModel<HistoryUiState>(HistoryUiState()) {

    // =========================================================================
    // Paging
    // =========================================================================

    /**
     * Paginated stream of video history items.
     *
     * Collected in the screen via `collectAsLazyPagingItems()`. The [Pager]
     * creates a new [VideoHistoryPagingSource] each time the data is
     * invalidated (e.g., after a database write).
     */
    val pagingFlow: Flow<PagingData<Video>> = Pager(
        config = PagingConfig(
            pageSize = VideoHistoryPagingSource.PAGE_SIZE,
            enablePlaceholders = false,
            initialLoadSize = VideoHistoryPagingSource.PAGE_SIZE
        ),
        pagingSourceFactory = { VideoHistoryPagingSource(videoDao) }
    ).flow.cachedIn(viewModelScope)

    // =========================================================================
    // User Actions
    // =========================================================================

    /**
     * Called when the user taps a video item in the history list.
     * Emits a [HistoryEvent.NavigateToResult] event for the screen to handle.
     *
     * @param videoId The ID of the tapped video.
     */
    fun onVideoClick(videoId: String) {
        Timber.d("History item tapped: $videoId")
        emitEvent(HistoryEvent.NavigateToResult(videoId))
    }

    /**
     * Called when the user taps the back button.
     * Emits a [HistoryEvent.NavigateBack] event for the screen to handle.
     */
    fun onNavigateBack() {
        emitEvent(HistoryEvent.NavigateBack)
    }
}
