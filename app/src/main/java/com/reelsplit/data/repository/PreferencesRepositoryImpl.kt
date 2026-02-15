package com.reelsplit.data.repository

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.reelsplit.data.local.datastore.PreferencesDataStore
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.UserSettings
import com.reelsplit.domain.repository.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of [PreferencesRepository] backed by [PreferencesDataStore].
 *
 * This class delegates all preference operations to the DataStore layer,
 * adding Result-based error handling where required by the interface contract.
 *
 * ## Error Handling
 * Methods returning [Result] catch exceptions from DataStore operations and
 * wrap them in [AppError] using [AppError.fromThrowable]. Direct-return methods
 * propagate exceptions to the caller (typically a use case or ViewModel that
 * handles them via its own error boundary).
 *
 * @param preferencesDataStore The DataStore wrapper for preference persistence
 */
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : PreferencesRepository {

    // ============================================
    // Share Tracking (for In-App Review)
    // ============================================

    override suspend fun getShareCount(): Int {
        return preferencesDataStore.getShareCount()
    }

    override suspend fun incrementShareCount() {
        preferencesDataStore.incrementShareCount()
    }

    // ============================================
    // Review Prompt Tracking
    // ============================================

    override suspend fun getLastReviewPromptTime(): Long {
        return preferencesDataStore.getLastReviewPromptTime()
    }

    override suspend fun setLastReviewPromptTime(time: Long) {
        preferencesDataStore.setLastReviewPromptTime(time)
    }

    override suspend fun hasUserReviewed(): Boolean {
        return preferencesDataStore.hasUserReviewed()
    }

    override suspend fun setUserReviewed() {
        preferencesDataStore.setUserReviewed()
    }

    // ============================================
    // Settings Operations
    // ============================================

    override fun observeSettings(): Flow<UserSettings> {
        return preferencesDataStore.observeSettings()
    }

    override suspend fun getSettings(): UserSettings {
        return preferencesDataStore.getSettings()
    }

    override suspend fun saveSettings(settings: UserSettings): Result<Unit, AppError> {
        return try {
            preferencesDataStore.saveSettings(settings)
            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to save settings")
            Err(AppError.fromThrowable(e))
        }
    }

    // ============================================
    // First Launch & Onboarding
    // ============================================

    override suspend fun isFirstLaunch(): Boolean {
        return preferencesDataStore.isFirstLaunch()
    }

    override suspend fun setFirstLaunchCompleted() {
        preferencesDataStore.setFirstLaunchCompleted()
    }

    // ============================================
    // Download Settings
    // ============================================

    override suspend fun getDownloadPath(): String? {
        return preferencesDataStore.getDownloadPath()
    }

    override suspend fun setDownloadPath(path: String?) {
        preferencesDataStore.setDownloadPath(path)
    }

    // ============================================
    // Clear All Data
    // ============================================

    override suspend fun clearAllPreferences(): Result<Unit, AppError> {
        return try {
            preferencesDataStore.clearAllPreferences()
            Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear preferences")
            Err(AppError.fromThrowable(e))
        }
    }
}
