package com.reelsplit.domain.usecase

import android.content.ActivityNotFoundException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.sharing.WhatsAppSharerContract
import java.io.File

/**
 * Use case for sharing a video segment to WhatsApp Status.
 *
 * This use case encapsulates the business logic for validating WhatsApp availability
 * and delegating the share operation to the WhatsApp sharer. It ensures that WhatsApp
 * is installed and the video file exists before attempting to share.
 *
 * ## Features
 * - Validates that WhatsApp is installed on the device
 * - Validates that the video file exists and is readable
 * - Launches WhatsApp Status intent with the video
 * - Returns appropriate [AppError] on failure
 *
 * ## Threading
 * This method is synchronous as it only validates inputs and delegates to
 * [WhatsAppSharerContract.shareToWhatsAppStatus]. The underlying implementation
 * may launch an Activity, which is safe from any thread when using
 * `FLAG_ACTIVITY_NEW_TASK`.
 *
 * ## File Validation
 * The file validation in this use case is defensive. While [WhatsAppSharerContract]
 * implementations will also fail if the file doesn't exist (via FileProvider),
 * validating here provides:
 * 1. Better, domain-specific error messages for the UI
 * 2. Early failure before platform code is invoked
 * 3. Consistent error handling through [AppError]
 *
 * Usage:
 * ```
 * val shareToWhatsApp = ShareToWhatsAppUseCase(whatsAppSharer)
 * val result = shareToWhatsApp(videoPath = "/storage/.../segment_001.mp4")
 * result.fold(
 *     success = { /* sharing launched successfully */ },
 *     failure = { error -> /* handle error */ }
 * )
 * ```
 *
 * @property whatsAppSharer The contract implementation for WhatsApp operations
 */
class ShareToWhatsAppUseCase(
    private val whatsAppSharer: WhatsAppSharerContract
) {
    /**
     * Shares a video file to WhatsApp Status.
     *
     * The method first validates that WhatsApp is installed and the video file
     * exists, then launches the WhatsApp Status intent.
     *
     * @param videoPath The local file path of the video segment to share
     * @return [Result] containing [Unit] on success (intent launched),
     *         or [AppError] on failure. Possible errors include:
     *         - [AppError.ProcessingError] if WhatsApp is not installed or sharing fails
     *         - [AppError.StorageError] if the video file doesn't exist or isn't readable
     */
    operator fun invoke(videoPath: String): Result<Unit, AppError> {
        // Validate WhatsApp is installed first (most common failure case)
        if (!whatsAppSharer.isWhatsAppInstalled()) {
            return Err(
                AppError.ProcessingError(
                    message = "WhatsApp is not installed on this device. " +
                        "Please install WhatsApp to share videos to Status."
                )
            )
        }

        // Validate input path and then execute share
        return validateVideoPath(videoPath)
            .andThen { executeShare(videoPath) }
    }

    /**
     * Validates the video path for sharing.
     *
     * @param videoPath The path to validate
     * @return [Result] with Unit on success, or [AppError.StorageError] on failure
     */
    private fun validateVideoPath(videoPath: String): Result<Unit, AppError> {
        // Validate path is not empty
        if (videoPath.isBlank()) {
            return Err(
                AppError.StorageError(
                    message = "Video path cannot be empty"
                )
            )
        }

        val videoFile = File(videoPath)

        // Validate file exists
        if (!videoFile.exists()) {
            return Err(
                AppError.StorageError(
                    message = "Video file does not exist",
                    path = videoPath
                )
            )
        }

        // Validate it's a file, not a directory
        if (!videoFile.isFile) {
            return Err(
                AppError.StorageError(
                    message = "Path is not a file",
                    path = videoPath
                )
            )
        }

        // Validate file is readable
        if (!videoFile.canRead()) {
            return Err(
                AppError.StorageError(
                    message = "Video file is not readable",
                    path = videoPath
                )
            )
        }

        // Validate file is not empty
        if (videoFile.length() == 0L) {
            return Err(
                AppError.StorageError(
                    message = "Video file is empty",
                    path = videoPath
                )
            )
        }

        return Ok(Unit)
    }

    /**
     * Executes the share operation.
     *
     * @param videoPath The validated video path to share
     * @return [Result] with Unit on success, or [AppError] on failure:
     *         - [AppError.StorageError] if FileProvider can't access the file
     *         - [AppError.ProcessingError] if WhatsApp was uninstalled or other failure
     */
    private fun executeShare(videoPath: String): Result<Unit, AppError> {
        return try {
            whatsAppSharer.shareToWhatsAppStatus(videoPath)
            Ok(Unit)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp was uninstalled between our check and the share attempt
            Err(
                AppError.ProcessingError(
                    message = "WhatsApp is not available. It may have been uninstalled."
                )
            )
        } catch (e: IllegalArgumentException) {
            // FileProvider couldn't access the file (e.g., not in configured paths)
            Err(
                AppError.StorageError(
                    message = "Cannot share file: ${e.message ?: "File not accessible"}",
                    path = videoPath
                )
            )
        } catch (e: SecurityException) {
            // Permission denied when accessing the file
            Err(
                AppError.PermissionError(
                    message = "Permission denied when sharing file: ${e.message ?: "Access denied"}"
                )
            )
        } catch (e: Exception) {
            Err(
                AppError.ProcessingError(
                    message = "Failed to share to WhatsApp: ${e.message ?: "Unknown error"}"
                )
            )
        }
    }
}
