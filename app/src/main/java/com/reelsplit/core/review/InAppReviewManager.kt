package com.reelsplit.core.review

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.review.ReviewManagerFactory
import com.reelsplit.data.local.datastore.PreferencesDataStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages Google Play In-App Review prompts based on user engagement.
 *
 * ## Eligibility Criteria
 * A review prompt is shown only when **all** of the following are true:
 * 1. The user has completed **≥ 3** successful shares
 * 2. At least **30 days** have elapsed since the last review prompt
 * 3. The user has **not** been marked as reviewed (e.g., via manual Play Store review)
 *
 * ## Usage
 * Call [requestReviewIfEligible] from an Activity scope (e.g., after a
 * successful WhatsApp share in `ResultScreen`). The method is safe to call
 * frequently — it no-ops when the user is ineligible, and a [Mutex] ensures
 * concurrent calls are serialised.
 *
 * ## Important Notes
 * - Google's Play Core API controls the actual display of the review dialog.
 *   Even when all eligibility criteria are met, Google may choose **not** to
 *   show the dialog (e.g., quota limits, user already reviewed via Play Store).
 * - The API does not report whether the user actually submitted a review.
 *   We intentionally do **not** permanently mark the user as reviewed after
 *   the flow completes — only the 30-day cooldown timer is updated, so the
 *   user can be re-prompted in a future window.
 *
 * @param preferencesDataStore Persists share counts and review-prompt timestamps
 */
@Singleton
class InAppReviewManager @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
) {
    /** Prevents concurrent review flows from overlapping. */
    private val mutex = Mutex()

    /**
     * Overridable clock source for testing cooldown logic.
     *
     * In production, defaults to [System.currentTimeMillis]. Tests can replace
     * this with a deterministic clock to avoid flaky time-based assertions.
     */
    @VisibleForTesting
    internal var clock: () -> Long = System::currentTimeMillis

    /**
     * Checks eligibility and, if met, launches the In-App Review flow.
     *
     * This is the primary entry point. It is safe to call on every share
     * completion — ineligible users are filtered cheaply via DataStore reads.
     * Concurrent calls are serialised by a [Mutex] to prevent duplicate prompts.
     *
     * @param activity The current foreground [Activity], required by the
     *                 Play Core Review API to attach its UI.
     */
    suspend fun requestReviewIfEligible(activity: Activity) {
        mutex.withLock {
            if (!isEligibleForReview()) {
                Timber.d("User not eligible for in-app review prompt")
                return
            }

            Timber.d("User is eligible for in-app review — launching flow")
            launchReviewFlow(activity)
        }
    }

    /**
     * Launches the Google Play In-App Review flow.
     *
     * Requests a `ReviewInfo` object from Play Core, then starts the review
     * dialog attached to the given [activity]. On completion, only the
     * cooldown timestamp is updated — the user is **not** permanently marked
     * as reviewed because the Play API does not confirm whether the dialog
     * was actually shown or whether the user submitted a review.
     *
     * All exceptions are caught and logged — review prompts are non-critical
     * and must never crash the app.
     *
     * @param activity The current foreground [Activity]
     */
    private suspend fun launchReviewFlow(activity: Activity) {
        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val reviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(activity, reviewInfo).await()

            // Only update the cooldown timer — do NOT call setUserReviewed().
            // The Play API does not reveal whether the dialog was shown or
            // whether the user actually submitted a review. Permanently
            // marking as reviewed would disable future prompts even when
            // Google chose not to display the dialog (e.g., quota limits).
            preferencesDataStore.setLastReviewPromptTime(clock())

            Timber.i("In-app review flow completed successfully")
        } catch (e: CancellationException) {
            // Must re-throw so coroutine cancellation propagates correctly.
            // Without this, scope cancellation (e.g., Activity/ViewModel destruction)
            // would be silently swallowed by the generic Exception catch below.
            throw e
        } catch (e: Exception) {
            // Review prompts are best-effort; never let them crash the app.
            Timber.e(e, "Failed to launch in-app review flow")
        }
    }

    /**
     * Determines whether the user meets all criteria for a review prompt.
     *
     * @return `true` if all eligibility conditions are satisfied
     */
    private suspend fun isEligibleForReview(): Boolean {
        // Already reviewed — never prompt again.
        if (preferencesDataStore.hasUserReviewed()) {
            Timber.d("Review eligibility: user has already reviewed")
            return false
        }

        // Need at least MINIMUM_SHARE_COUNT shares.
        val shareCount = preferencesDataStore.getShareCount()
        if (shareCount < MINIMUM_SHARE_COUNT) {
            Timber.d("Review eligibility: share count $shareCount < $MINIMUM_SHARE_COUNT")
            return false
        }

        // Must wait PROMPT_COOLDOWN_DAYS since the last prompt.
        // A value of 0L means the user has never been prompted, so they are eligible.
        val lastPromptTime = preferencesDataStore.getLastReviewPromptTime()
        if (lastPromptTime != 0L) {
            val daysSinceLastPrompt = daysSince(lastPromptTime)
            if (daysSinceLastPrompt < PROMPT_COOLDOWN_DAYS) {
                Timber.d(
                    "Review eligibility: only $daysSinceLastPrompt days since last prompt" +
                        " (need $PROMPT_COOLDOWN_DAYS)"
                )
                return false
            }
        }

        return true
    }

    /**
     * Calculates the number of whole days between [timestampMillis] and now.
     *
     * @param timestampMillis A positive epoch-millisecond timestamp
     * @return Number of full days elapsed
     */
    private fun daysSince(timestampMillis: Long): Long {
        val elapsedMillis = clock() - timestampMillis
        return TimeUnit.MILLISECONDS.toDays(elapsedMillis)
    }

    companion object {
        /** Minimum number of successful shares before prompting. */
        const val MINIMUM_SHARE_COUNT = 3

        /** Minimum days between consecutive review prompts. */
        const val PROMPT_COOLDOWN_DAYS = 30L
    }
}
