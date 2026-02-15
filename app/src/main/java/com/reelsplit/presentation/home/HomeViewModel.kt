package com.reelsplit.presentation.home

import com.reelsplit.BuildConfig
import com.reelsplit.core.base.BaseViewModel
import com.reelsplit.core.base.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// =============================================================================
// Home UI State
// =============================================================================

/**
 * UI state for the HomeScreen.
 * Minimal since the screen is primarily static instructional content.
 *
 * @param appVersion The app version string displayed in the footer.
 */
data class HomeUiState(
    val appVersion: String = BuildConfig.VERSION_NAME
)

// =============================================================================
// Home Events (one-time actions)
// =============================================================================

/**
 * One-time UI events emitted by [HomeViewModel].
 */
sealed class HomeEvent : UiEvent {

    /**
     * Navigate to the history screen.
     */
    data object NavigateToHistory : HomeEvent()
}

// =============================================================================
// HomeViewModel
// =============================================================================

/**
 * ViewModel for the HomeScreen.
 *
 * Manages navigation events for the landing page. The screen content is static
 * (usage instructions), so the ViewModel is intentionally thin — its primary
 * responsibility is emitting navigation events in response to user actions.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel<HomeUiState>(HomeUiState()) {

    // =========================================================================
    // User Actions
    // =========================================================================

    /**
     * Called when the user taps the "View History" button.
     * Emits a [HomeEvent.NavigateToHistory] event for the screen to handle.
     */
    fun onViewHistoryClick() {
        emitEvent(HomeEvent.NavigateToHistory)
    }
}
