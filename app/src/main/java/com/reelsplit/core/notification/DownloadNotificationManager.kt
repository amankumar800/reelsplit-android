package com.reelsplit.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.reelsplit.R
import com.reelsplit.core.constants.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages download and processing progress notifications.
 *
 * Responsibilities:
 * - Creates notification channels (required for Android 8.0+)
 * - Shows indeterminate and determinate progress notifications
 * - Shows completion notification with tap-to-open action
 * - Checks POST_NOTIFICATIONS permission (required for Android 13+)
 * - Cancels active notifications
 *
 * This manager centralizes notification logic so that Workers and ViewModels
 * don't need to deal with notification plumbing directly.
 *
 * @property context Application context for creating notifications
 * @property notificationManager System NotificationManager for posting notifications
 */
@Singleton
class DownloadNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    init {
        createNotificationChannels()
    }

    // ==================== Public API ====================

    /**
     * Checks whether the app has permission to post notifications.
     *
     * - On Android 13+ (API 33+), the [Manifest.permission.POST_NOTIFICATIONS] runtime
     *   permission must be granted.
     * - On older versions, notifications are allowed by default.
     *
     * @return `true` if notifications can be posted, `false` otherwise.
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Shows or updates a progress notification.
     *
     * If the app lacks notification permission on Android 13+, this call is silently
     * ignored and a debug log is emitted.
     *
     * @param title       The notification title (e.g., "Downloading Video").
     * @param progress    Current progress value (0–[maxProgress]).
     * @param maxProgress Maximum progress value (default 100).
     * @param message     Optional body text override. When `null`, a default
     *                    percentage string is generated (e.g., "Downloading… 50%").
     *                    Pass a custom message when using this for non-download
     *                    operations (e.g., "Processing… 50%").
     */
    fun showProgress(
        title: String,
        progress: Int,
        maxProgress: Int = PROGRESS_MAX,
        message: String? = null
    ) {
        if (!hasNotificationPermission()) {
            Timber.d("Cannot show notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        // Guard against invalid maxProgress to prevent coerceIn IllegalArgumentException
        val safeMaxProgress = maxProgress.coerceAtLeast(1)
        val clampedProgress = progress.coerceIn(0, safeMaxProgress)
        val isIndeterminate = clampedProgress == 0

        val contentText = message ?: if (isIndeterminate) {
            context.getString(R.string.notification_download_starting)
        } else {
            context.getString(
                R.string.notification_download_progress,
                (clampedProgress * 100) / safeMaxProgress
            )
        }

        val notification = NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(safeMaxProgress, clampedProgress, isIndeterminate)
            .build()

        notifyOrLog(AppConstants.NOTIFICATION_ID_DOWNLOAD, notification)
    }

    /**
     * Shows a "download complete" notification with a tap-to-open action.
     *
     * Tapping the notification opens the downloaded file using the system's default
     * video player via a [FileProvider] URI. If the file does not exist, the
     * notification is shown without a tap action.
     *
     * If the app lacks notification permission on Android 13+, this call is silently
     * ignored.
     *
     * @param title    The notification title (e.g., "Download Complete").
     * @param filePath Absolute path to the downloaded/processed file.
     */
    fun showComplete(title: String, filePath: String) {
        if (!hasNotificationPermission()) {
            Timber.d("Cannot show notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        val file = File(filePath)
        val pendingIntent = if (file.exists()) {
            createOpenFilePendingIntent(file)
        } else {
            Timber.w("Completed file does not exist: $filePath")
            null
        }

        val notification = NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_complete))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setProgress(0, 0, false) // Explicitly remove progress bar
            .apply {
                pendingIntent?.let { setContentIntent(it) }
            }
            .build()

        notifyOrLog(AppConstants.NOTIFICATION_ID_DOWNLOAD, notification)
    }

    /**
     * Cancels the active download notification.
     */
    fun cancel() {
        notificationManager.cancel(AppConstants.NOTIFICATION_ID_DOWNLOAD)
    }

    // ==================== Internal Helpers ====================

    /**
     * Posts a notification, catching [SecurityException] that some OEM ROMs may
     * throw even after a permission check (e.g., race between check and post,
     * or vendor-specific restrictions).
     */
    private fun notifyOrLog(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException when posting notification id=$id")
        }
    }

    /**
     * Creates all notification channels required by the app.
     *
     * Safe to call multiple times — the system ignores channel creation requests
     * if the channel already exists (channel settings are preserved).
     * Required on API 26+ (Android O).
     *
     * Note: [VideoDownloadWorker][com.reelsplit.data.worker.VideoDownloadWorker]
     * also creates the download channel independently as a safety net, since
     * WorkManager may start the worker before this singleton is instantiated.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD,
                AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppConstants.NOTIFICATION_CHANNEL_DOWNLOAD_DESC
                setShowBadge(false)
            }

            val splitChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_SPLIT,
                AppConstants.NOTIFICATION_CHANNEL_SPLIT_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppConstants.NOTIFICATION_CHANNEL_SPLIT_DESC
                setShowBadge(false)
            }

            val generalChannel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_GENERAL,
                AppConstants.NOTIFICATION_CHANNEL_GENERAL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = AppConstants.NOTIFICATION_CHANNEL_GENERAL_DESC
            }

            notificationManager.createNotificationChannels(
                listOf(downloadChannel, splitChannel, generalChannel)
            )

            Timber.d("Notification channels created")
        }
    }

    /**
     * Creates a [PendingIntent] that opens the given file with the system's
     * default handler for the video MIME type.
     *
     * Uses [FileProvider] to generate a content URI so that the receiving app
     * has read permission for the file.
     *
     * @return A [PendingIntent] to open the file, or `null` if the file path
     *         is not covered by the FileProvider configuration.
     */
    private fun createOpenFilePendingIntent(file: File): PendingIntent? {
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "File is outside configured FileProvider paths: ${file.absolutePath}")
            return null
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, AppConstants.VIDEO_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            intent,
            flags
        )
    }

    companion object {
        /** Default maximum progress value. */
        private const val PROGRESS_MAX = 100

        /** Request code for the tap-to-open PendingIntent. */
        private const val PENDING_INTENT_REQUEST_CODE = 2001
    }
}
