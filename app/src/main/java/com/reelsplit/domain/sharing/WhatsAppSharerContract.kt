package com.reelsplit.domain.sharing

/**
 * Contract interface for WhatsApp sharing operations.
 *
 * This interface defines the contract for WhatsApp-related operations,
 * enabling separation between the domain and implementation layers.
 * The concrete implementation ([com.reelsplit.sharing.WhatsAppSharer])
 * handles the actual Android Intent operations.
 *
 * ## Thread Safety
 * All methods should be callable from any thread. Implementations that
 * require the Main thread (e.g., for Activity launching) should handle
 * the thread switching internally or document the requirement.
 *
 * ## Architecture Note
 * This interface lives in the domain layer to allow use cases to depend
 * on an abstraction rather than the concrete implementation, following
 * the Dependency Inversion Principle of Clean Architecture.
 *
 * @see com.reelsplit.sharing.WhatsAppSharer for the production implementation
 */
interface WhatsAppSharerContract {

    /**
     * Checks if WhatsApp is installed on the device.
     *
     * @return `true` if WhatsApp (com.whatsapp) is installed, `false` otherwise
     */
    fun isWhatsAppInstalled(): Boolean

    /**
     * Shares a video file to WhatsApp Status.
     *
     * Uses Meta's official WhatsApp Status API action
     * (`com.whatsapp.intent.action.SEND_MEDIA_TO_STATUS`) to open the
     * WhatsApp Status composer directly with the video pre-attached.
     *
     * The video file must exist and be accessible via the app's FileProvider.
     *
     * @param videoPath The absolute path to the video file to share
     * @throws IllegalArgumentException if the file is not accessible via FileProvider
     * @throws android.content.ActivityNotFoundException if WhatsApp is not installed
     *         (always check [isWhatsAppInstalled] first to avoid this)
     */
    fun shareToWhatsAppStatus(videoPath: String)

    /**
     * Shares a video file to WhatsApp chat.
     *
     * Opens WhatsApp's standard share flow, allowing the user to select
     * a contact or group to send the video to.
     *
     * @param videoPath The absolute path to the video file to share
     * @throws IllegalArgumentException if the file is not accessible via FileProvider
     * @throws android.content.ActivityNotFoundException if WhatsApp is not installed
     */
    fun shareToWhatsAppChat(videoPath: String)
}
