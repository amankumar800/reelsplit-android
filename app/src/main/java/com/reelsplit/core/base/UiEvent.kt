package com.reelsplit.core.base

import androidx.annotation.StringRes

/**
 * Base interface for UI events (one-time actions like navigation, snackbars).
 * Implement this interface for feature-specific events.
 */
interface UiEvent

/**
 * Common UI events that can be reused across ViewModels.
 * These represent typical one-time UI actions.
 */
sealed class CommonUiEvent : UiEvent {
    
    /**
     * Navigate back event.
     */
    data object NavigateBack : CommonUiEvent()
    
    /**
     * Show a snackbar with a string message.
     *
     * @param message The message to display.
     * @param actionLabel Optional action button label.
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null
    ) : CommonUiEvent()
    
    /**
     * Show a snackbar with a string resource.
     *
     * @param messageResId The string resource ID for the message.
     * @param actionLabelResId Optional string resource ID for action button.
     */
    data class ShowSnackbarRes(
        @StringRes val messageResId: Int,
        @StringRes val actionLabelResId: Int? = null
    ) : CommonUiEvent()
    
    /**
     * Show a toast with a message.
     *
     * @param message The message to display.
     */
    data class ShowToast(val message: String) : CommonUiEvent()
    
    /**
     * Show a toast with a string resource.
     *
     * @param messageResId The string resource ID for the message.
     */
    data class ShowToastRes(@StringRes val messageResId: Int) : CommonUiEvent()
}
