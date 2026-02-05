package com.reelsplit.domain.repository

import com.github.michaelbull.result.Result
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user preferences and app settings.
 *
 * This interface defines the contract for managing user preferences,
 * settings, and analytics-related data. Implementations typically use
 * Jetpack DataStore for persistence.
 *
 * Preferences include:
 * - Share tracking for in-app review prompts
 * - User settings and app configuration
 * - First launch and onboarding state
 *
 * ## Thread Safety
 * Implementations MUST be thread-safe. DataStore provides built-in thread
 * safety through its coroutine-based API. All methods can be called from
 * any coroutine context safely.
 */
interface PreferencesRepository {

    // ============================================
    // Share Tracking (for In-App Review)
    // ============================================

    /**
     * Gets the total number of successful shares made by the user.
     *
     * Used to determine when to prompt for an in-app review.
     * The app typically prompts after a certain number of shares.
     *
     * @return The current share count
     */
    suspend fun getShareCount(): Int

    /**
     * Increments the share count after a successful share.
     *
     * Should be called each time the user successfully shares
     * a video segment to WhatsApp.
     */
    suspend fun incrementShareCount()

    // ============================================
    // Review Prompt Tracking
    // ============================================

    /**
     * Gets the timestamp of the last review prompt shown to the user.
     *
     * Used to implement rate limiting for review prompts to avoid
     * annoying users with frequent requests.
     *
     * @return Epoch milliseconds of the last prompt, or 0 if never prompted
     */
    suspend fun getLastReviewPromptTime(): Long

    /**
     * Sets the timestamp of the last review prompt.
     *
     * @param time Epoch milliseconds when the review prompt was shown
     */
    suspend fun setLastReviewPromptTime(time: Long)

    /**
     * Checks if user has already reviewed the app.
     *
     * Once true, review prompts should not be shown again.
     *
     * @return true if user has completed a review
     */
    suspend fun hasUserReviewed(): Boolean

    /**
     * Marks that the user has reviewed the app.
     */
    suspend fun setUserReviewed()

    // ============================================
    // Settings Operations
    // ============================================

    /**
     * Loads and returns the current user settings as a reactive stream.
     *
     * This provides a reactive stream of settings that will emit
     * updates whenever any setting changes.
     *
     * @return A [Flow] emitting the current [UserSettings]
     */
    fun observeSettings(): Flow<UserSettings>

    /**
     * Gets the current user settings synchronously.
     *
     * For one-time reads, prefer this over [observeSettings] to avoid
     * unnecessary flow collection.
     *
     * @return The current [UserSettings]
     */
    suspend fun getSettings(): UserSettings

    /**
     * Updates user settings.
     *
     * @param settings The updated [UserSettings] to persist
     * @return [Result] with Unit on success, or [AppError] on failure
     */
    suspend fun saveSettings(settings: UserSettings): Result<Unit, AppError>

    // ============================================
    // First Launch & Onboarding
    // ============================================

    /**
     * Checks if this is the user's first launch of the app.
     *
     * @return true if the app has never been launched before
     */
    suspend fun isFirstLaunch(): Boolean

    /**
     * Marks that the first launch onboarding has been completed.
     */
    suspend fun setFirstLaunchCompleted()

    // ============================================
    // Download Settings
    // ============================================

    /**
     * Gets the preferred download directory path.
     *
     * @return The custom download path, or null to use the default
     */
    suspend fun getDownloadPath(): String?

    /**
     * Sets the preferred download directory path.
     *
     * @param path The download directory path, or null to reset to default
     */
    suspend fun setDownloadPath(path: String?)

    // ============================================
    // Clear All Data
    // ============================================

    /**
     * Clears all stored preferences.
     *
     * Use with caution - this resets all user data including
     * share counts, review prompts, and settings.
     *
     * @return [Result] with Unit on success, or [AppError] on failure
     */
    suspend fun clearAllPreferences(): Result<Unit, AppError>
}
