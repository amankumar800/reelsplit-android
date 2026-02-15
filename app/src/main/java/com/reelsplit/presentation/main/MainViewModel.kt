package com.reelsplit.presentation.main

import com.reelsplit.core.base.BaseViewModel
import com.reelsplit.core.base.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

// =============================================================================
// Main UI State
// =============================================================================

/**
 * UI state for [MainActivity][com.reelsplit.presentation.MainActivity].
 *
 * @property isReady When `false` the splash screen stays visible via
 *   `setKeepOnScreenCondition`. Set to `true` once one-time startup work
 *   (e.g. future in-app update check) completes.
 * @property pendingReelUrl A reel URL received from a share intent that
 *   has not yet been consumed by the UI for navigation. `null` means no
 *   pending navigation. This uses **state** rather than a one-time event
 *   so that the value survives the gap between `Activity.onCreate()` (which
 *   calls [MainViewModel.onReelUrlReceived]) and `LaunchedEffect` subscribing
 *   inside `setContent`. A `SharedFlow` with `replay = 0` would silently
 *   drop the URL on cold-start because there is no active collector yet.
 */
data class MainUiState(
    val isReady: Boolean = false,
    val pendingReelUrl: String? = null
)

// =============================================================================
// Main Events (one-time actions)
// =============================================================================

/**
 * One-time UI events emitted by [MainViewModel].
 *
 * Currently empty — share-intent navigation is handled via [MainUiState.pendingReelUrl]
 * to avoid the cold-start race condition. Retain this sealed class for future
 * one-time events (e.g. showing an in-app update dialog).
 */
sealed class MainEvent : UiEvent

// =============================================================================
// MainViewModel
// =============================================================================

/**
 * ViewModel for [MainActivity][com.reelsplit.presentation.MainActivity].
 *
 * Responsibilities:
 * - Controls splash screen hold via [MainUiState.isReady].
 * - Receives reel URLs forwarded from the share receiver and stores them
 *   in [MainUiState.pendingReelUrl] for the Compose layer to consume.
 * - Provides a stub for future in-app update checking.
 */
@HiltViewModel
class MainViewModel @Inject constructor() : BaseViewModel<MainUiState>(MainUiState()) {

    init {
        launchSafe {
            // Perform any one-time startup work here (e.g. warm caches,
            // check feature flags). Currently a no-op — the splash screen
            // dismisses almost immediately.
            updateState { copy(isReady = true) }
        }
    }

    // =========================================================================
    // Share Intent Handling
    // =========================================================================

    /**
     * Called when a reel URL is received from
     * [ShareReceiverActivity][com.reelsplit.presentation.share.ShareReceiverActivity].
     *
     * Stores the URL in [MainUiState.pendingReelUrl]. The Compose layer
     * observes this state, navigates to ProcessingScreen, and then calls
     * [onPendingReelUrlConsumed] to clear it.
     *
     * Uses state instead of a one-time event so the URL survives the gap
     * between `Activity.onCreate()` and `LaunchedEffect` subscription.
     *
     * @param url The Instagram reel URL or video URI string.
     */
    fun onReelUrlReceived(url: String) {
        Timber.d("Reel URL received: $url")
        updateState { copy(pendingReelUrl = url) }
    }

    /**
     * Called after the Compose layer has consumed the pending reel URL
     * and initiated navigation. Clears [MainUiState.pendingReelUrl] so
     * the same URL is not navigated to again on recomposition.
     */
    fun onPendingReelUrlConsumed() {
        updateState { copy(pendingReelUrl = null) }
    }

    // =========================================================================
    // In-App Update (stub)
    // =========================================================================

    /**
     * Checks for available app updates.
     *
     * Currently a no-op. Wire up the Google Play In-App Updates API
     * (`com.google.android.play:app-update-ktx`) when the dependency is
     * added to the project.
     *
     * TODO: Implement with AppUpdateManager from Play Core library.
     */
    fun checkForUpdates() {
        Timber.d("In-app update check: not yet implemented")
        // Future implementation:
        // 1. Create AppUpdateManager
        // 2. Request AppUpdateInfo
        // 3. If update available, emit event to start update flow
    }
}
