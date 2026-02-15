package com.reelsplit.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Analytics for structured event tracking throughout the app.
 *
 * Provides type-safe methods for logging key user actions such as video processing,
 * WhatsApp sharing, and errors. All events are logged asynchronously by the
 * Firebase SDK—no coroutine context is needed.
 *
 * **Usage**: Inject [AnalyticsTracker] via Hilt and call the appropriate method:
 * ```
 * analyticsTracker.logVideoProcessed(durationSeconds = 180, partsCreated = 2)
 * ```
 *
 * **Testability**: Callers depend on the [AnalyticsTracker] interface so they can be
 * tested with a fake implementation without Firebase on the classpath.
 *
 * @property firebaseAnalytics The Firebase Analytics instance, provided by Hilt.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    // ==================== Constants ====================

    private companion object {
        // Event names
        const val EVENT_VIDEO_PROCESSED = "video_processed"
        const val EVENT_SHARE_TO_WHATSAPP = "share_to_whatsapp"
        const val EVENT_APP_ERROR = "app_error"

        // Parameter keys
        const val PARAM_DURATION_SECONDS = "duration_seconds"
        const val PARAM_PARTS_CREATED = "parts_created"
        const val PARAM_PART_NUMBER = "part_number"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_ERROR_MESSAGE = "error_message"

        /** Firebase limits event parameter string values to 100 characters. */
        const val MAX_EVENT_PARAM_LENGTH = 100

        /** Firebase limits user property values to 36 characters. */
        const val MAX_USER_PROPERTY_VALUE_LENGTH = 36

        /** Firebase limits user property names to 24 characters. */
        const val MAX_USER_PROPERTY_KEY_LENGTH = 24
    }

    // ==================== Event Logging ====================

    /**
     * Logs a successful video processing event.
     *
     * Negative or zero [durationSeconds] values are clamped to 0 to prevent
     * nonsensical data reaching the Firebase dashboard.
     *
     * @param durationSeconds Total duration of the original video in seconds.
     * @param partsCreated Number of split parts generated (must be ≥ 1).
     */
    override fun logVideoProcessed(durationSeconds: Long, partsCreated: Int) {
        val safeDuration = durationSeconds.coerceAtLeast(0)
        val safeParts = partsCreated.coerceAtLeast(1)

        Timber.d("Analytics: video_processed (duration=${safeDuration}s, parts=$safeParts)")
        firebaseAnalytics.logEvent(EVENT_VIDEO_PROCESSED) {
            param(PARAM_DURATION_SECONDS, safeDuration)
            param(PARAM_PARTS_CREATED, safeParts.toLong())
        }
    }

    /**
     * Logs a share-to-WhatsApp event for a specific video part.
     *
     * @param partNumber The 1-based index of the part being shared (clamped to ≥ 1).
     */
    override fun logShareToWhatsApp(partNumber: Int) {
        val safePart = partNumber.coerceAtLeast(1)

        Timber.d("Analytics: share_to_whatsapp (part=$safePart)")
        firebaseAnalytics.logEvent(EVENT_SHARE_TO_WHATSAPP) {
            param(PARAM_PART_NUMBER, safePart.toLong())
        }
    }

    /**
     * Logs an application error event.
     *
     * Blank [errorType] or [errorMessage] values are replaced with "unknown" to
     * ensure every error event is filterable in the Firebase dashboard.
     * Strings are truncated to [MAX_EVENT_PARAM_LENGTH] (100) characters.
     *
     * @param errorType Short identifier for the error category (e.g., "network", "processing").
     * @param errorMessage Human-readable error description.
     */
    override fun logError(errorType: String, errorMessage: String) {
        val safeType = errorType.ifBlank { "unknown" }.take(MAX_EVENT_PARAM_LENGTH)
        val safeMessage = errorMessage.ifBlank { "unknown" }.take(MAX_EVENT_PARAM_LENGTH)

        Timber.d("Analytics: app_error (type=$safeType, message=$safeMessage)")
        firebaseAnalytics.logEvent(EVENT_APP_ERROR) {
            param(PARAM_ERROR_TYPE, safeType)
            param(PARAM_ERROR_MESSAGE, safeMessage)
        }
    }

    // ==================== User Properties ====================

    /**
     * Sets a user property for audience segmentation in Firebase Analytics.
     *
     * User properties persist across sessions and are useful for filtering
     * reports (e.g., by app version or preferred sharing target).
     *
     * Firebase limits:
     * - Property names: max 24 characters
     * - Property values: max 36 characters
     *
     * Both [key] and [value] are truncated to their respective limits.
     * Blank keys are rejected with a warning log.
     *
     * @param key The user property name.
     * @param value The user property value.
     */
    override fun setUserProperty(key: String, value: String) {
        if (key.isBlank()) {
            Timber.w("Analytics: setUserProperty called with blank key, ignoring")
            return
        }

        val safeKey = key.take(MAX_USER_PROPERTY_KEY_LENGTH)
        val safeValue = value.take(MAX_USER_PROPERTY_VALUE_LENGTH)

        Timber.d("Analytics: setUserProperty ($safeKey=$safeValue)")
        firebaseAnalytics.setUserProperty(safeKey, safeValue)
    }

    // ==================== Collection Control ====================

    /**
     * Enables or disables analytics data collection.
     *
     * Call with `false` to respect user privacy preferences or GDPR opt-outs.
     * When disabled, no events are sent to Firebase. Re-enable with `true`.
     *
     * @param enabled Whether to enable analytics collection.
     */
    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        Timber.d("Analytics: collection ${if (enabled) "enabled" else "disabled"}")
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
