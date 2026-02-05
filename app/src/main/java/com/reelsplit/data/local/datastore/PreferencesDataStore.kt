package com.reelsplit.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.reelsplit.domain.model.UserSettings
import com.reelsplit.domain.model.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to create a DataStore instance for preferences.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reelsplit_preferences"
)

/**
 * DataStore-based preferences manager for ReelSplit.
 *
 * This class provides type-safe access to user preferences using Jetpack DataStore,
 * replacing SharedPreferences with a modern, coroutine-friendly API.
 *
 * ## Features
 * - Share count tracking for in-app review prompts
 * - Review prompt timing and completion tracking
 * - User settings persistence (auto-delete, notifications, video quality)
 * - First launch detection for onboarding
 * - Custom download path configuration
 *
 * ## Thread Safety
 * All operations are thread-safe. DataStore handles concurrency internally
 * using atomic file operations and coroutines.
 *
 * @param context Application context for DataStore access
 */
@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // ============================================
    // Preference Keys
    // ============================================

    private object PreferenceKeys {
        // Share Tracking
        val SHARE_COUNT = intPreferencesKey("share_count")

        // Review Prompt
        val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
        val HAS_USER_REVIEWED = booleanPreferencesKey("has_user_reviewed")

        // User Settings
        val AUTO_DELETE_AFTER_SHARE = booleanPreferencesKey("auto_delete_after_share")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        // First Launch
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")

        // Download Path
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
    }

    // ============================================
    // Share Tracking
    // ============================================

    /**
     * Observes the total number of successful shares as a reactive stream.
     *
     * @return Flow emitting the current share count
     */
    fun observeShareCount(): Flow<Int> = dataStore.data
        .catchIOException()
        .map { preferences ->
            preferences[PreferenceKeys.SHARE_COUNT] ?: 0
        }
        .distinctUntilChanged()

    /**
     * Gets the current share count.
     *
     * @return The current share count
     */
    suspend fun getShareCount(): Int = observeShareCount().first()

    /**
     * Increments the share count by one.
     *
     * Called each time the user successfully shares a video segment.
     */
    suspend fun incrementShareCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[PreferenceKeys.SHARE_COUNT] ?: 0
            preferences[PreferenceKeys.SHARE_COUNT] = currentCount + 1
        }
    }

    // ============================================
    // Review Prompt Tracking
    // ============================================

    /**
     * Observes the timestamp of the last review prompt as a reactive stream.
     *
     * @return Flow emitting epoch milliseconds, or 0 if never prompted
     */
    fun observeLastReviewPromptTime(): Flow<Long> = dataStore.data
        .catchIOException()
        .map { preferences ->
            preferences[PreferenceKeys.LAST_REVIEW_PROMPT_TIME] ?: 0L
        }
        .distinctUntilChanged()

    /**
     * Gets the last review prompt time.
     *
     * @return Epoch milliseconds of the last prompt, or 0 if never prompted
     */
    suspend fun getLastReviewPromptTime(): Long = observeLastReviewPromptTime().first()

    /**
     * Sets the timestamp of when a review prompt was shown.
     *
     * @param time Epoch milliseconds of the review prompt (must be non-negative)
     * @throws IllegalArgumentException if time is negative
     */
    suspend fun setLastReviewPromptTime(time: Long) {
        require(time >= 0) { "Review prompt time must be non-negative, got: $time" }
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_REVIEW_PROMPT_TIME] = time
        }
    }

    /**
     * Observes whether the user has reviewed the app.
     *
     * @return Flow emitting true if user has reviewed
     */
    fun observeHasUserReviewed(): Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { preferences ->
            preferences[PreferenceKeys.HAS_USER_REVIEWED] ?: false
        }
        .distinctUntilChanged()

    /**
     * Gets whether the user has reviewed.
     *
     * @return true if user has completed a review
     */
    suspend fun hasUserReviewed(): Boolean = observeHasUserReviewed().first()

    /**
     * Marks that the user has reviewed the app.
     *
     * Once set, review prompts should not be shown again.
     */
    suspend fun setUserReviewed() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_USER_REVIEWED] = true
        }
    }

    // ============================================
    // User Settings
    // ============================================

    /**
     * Observes all user settings as a reactive stream.
     *
     * Emits a new [UserSettings] whenever any setting changes.
     *
     * @return Flow of current user settings
     */
    fun observeSettings(): Flow<UserSettings> = dataStore.data
        .catchIOException()
        .map { preferences ->
            UserSettings(
                autoDeleteAfterShare = preferences[PreferenceKeys.AUTO_DELETE_AFTER_SHARE] ?: false,
                showNotifications = preferences[PreferenceKeys.SHOW_NOTIFICATIONS] ?: true,
                defaultVideoQuality = parseVideoQuality(
                    preferences[PreferenceKeys.DEFAULT_VIDEO_QUALITY]
                ),
                keepScreenOnDuringProcessing = preferences[PreferenceKeys.KEEP_SCREEN_ON] ?: true
            )
        }
        .distinctUntilChanged()

    /**
     * Gets the current user settings.
     *
     * @return The current [UserSettings]
     */
    suspend fun getSettings(): UserSettings = observeSettings().first()

    /**
     * Saves all user settings atomically.
     *
     * @param settings The [UserSettings] to persist
     */
    suspend fun saveSettings(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_DELETE_AFTER_SHARE] = settings.autoDeleteAfterShare
            preferences[PreferenceKeys.SHOW_NOTIFICATIONS] = settings.showNotifications
            preferences[PreferenceKeys.DEFAULT_VIDEO_QUALITY] = settings.defaultVideoQuality.name
            preferences[PreferenceKeys.KEEP_SCREEN_ON] = settings.keepScreenOnDuringProcessing
        }
    }

    // ============================================
    // First Launch & Onboarding
    // ============================================

    /**
     * Observes the first launch state as a reactive stream.
     *
     * @return Flow emitting true if this is the first launch
     */
    fun observeFirstLaunch(): Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { preferences ->
            preferences[PreferenceKeys.IS_FIRST_LAUNCH] ?: true
        }
        .distinctUntilChanged()

    /**
     * Checks if this is the user's first launch.
     *
     * The flag defaults to true and is set to false after onboarding completes.
     *
     * @return true if the app has never been launched before
     */
    suspend fun isFirstLaunch(): Boolean = observeFirstLaunch().first()

    /**
     * Marks that first launch onboarding has been completed.
     */
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_FIRST_LAUNCH] = false
        }
    }

    // ============================================
    // Download Settings
    // ============================================

    /**
     * Observes the preferred download directory path as a reactive stream.
     *
     * @return Flow emitting the custom download path, or null for default
     */
    fun observeDownloadPath(): Flow<String?> = dataStore.data
        .catchIOException()
        .map { preferences ->
            preferences[PreferenceKeys.DOWNLOAD_PATH]
        }
        .distinctUntilChanged()

    /**
     * Gets the preferred download directory path.
     *
     * @return The custom download path, or null to use the default
     */
    suspend fun getDownloadPath(): String? = observeDownloadPath().first()

    /**
     * Sets the preferred download directory path.
     *
     * Empty strings are treated as null (reset to default).
     *
     * @param path The download directory path, or null/empty to reset to default
     */
    suspend fun setDownloadPath(path: String?) {
        val normalizedPath = path?.takeIf { it.isNotBlank() }
        dataStore.edit { preferences ->
            if (normalizedPath != null) {
                preferences[PreferenceKeys.DOWNLOAD_PATH] = normalizedPath
            } else {
                preferences.remove(PreferenceKeys.DOWNLOAD_PATH)
            }
        }
    }

    // ============================================
    // Clear All Data
    // ============================================

    /**
     * Clears all stored preferences.
     *
     * Use with caution - this resets all user data including
     * share counts, review prompts, and settings.
     */
    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // ============================================
    // Helper Extensions
    // ============================================

    /**
     * Parses a VideoQuality from its string representation.
     *
     * Logs a warning if an invalid value is encountered, which helps
     * debug preference corruption or migration issues.
     *
     * @param value The stored string value, or null
     * @return The parsed [VideoQuality], or [VideoQuality.HIGH] as default
     */
    private fun parseVideoQuality(value: String?): VideoQuality {
        if (value == null) return VideoQuality.HIGH
        return try {
            VideoQuality.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Timber.w("Invalid VideoQuality stored: '$value', using default HIGH")
            VideoQuality.HIGH
        }
    }

    /**
     * Extension function to handle IOException gracefully.
     *
     * When an IOException occurs during DataStore read, it logs the error
     * and emits empty preferences, allowing the app to continue with defaults.
     */
    private fun Flow<Preferences>.catchIOException(): Flow<Preferences> = this.catch { exception ->
        if (exception is IOException) {
            Timber.e(exception, "Error reading preferences")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
}

