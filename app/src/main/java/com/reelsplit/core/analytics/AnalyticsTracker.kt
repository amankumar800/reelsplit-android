package com.reelsplit.core.analytics

/**
 * Contract for analytics event tracking.
 *
 * Abstracting analytics behind an interface enables:
 * - **Testability**: Swap with a no-op or recording fake in unit tests.
 * - **Flexibility**: Replace the backing implementation (e.g., Firebase → Mixpanel)
 *   without touching call-sites.
 *
 * The production implementation is [AnalyticsManager].
 */
interface AnalyticsTracker {

    /** Logs a successful video processing event. */
    fun logVideoProcessed(durationSeconds: Long, partsCreated: Int)

    /** Logs a share-to-WhatsApp event for a specific video part. */
    fun logShareToWhatsApp(partNumber: Int)

    /** Logs an application error event. */
    fun logError(errorType: String, errorMessage: String)

    /** Sets a user property for audience segmentation. */
    fun setUserProperty(key: String, value: String)

    /** Enables or disables analytics data collection (e.g., for GDPR opt-out). */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}
