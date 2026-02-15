package com.reelsplit.domain.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.repository.VideoRepository
import java.io.File

/**
 * Use case for splitting a video into WhatsApp Status-compatible segments.
 *
 * This use case encapsulates the business logic for validating input files
 * and delegating the splitting operation to the repository. Each segment
 * is guaranteed to be at most 90 seconds and under 16MB.
 *
 * Features:
 * - Validates that the input file exists before splitting
 * - Validates supported video file extensions
 * - Returns a list of [VideoSegment]s on success
 * - Returns appropriate [AppError] on failure
 *
 * Usage:
 * ```
 * val splitVideo = SplitVideoUseCase(videoRepository)
 * val result = splitVideo(videoId = "abc123", inputPath = "/storage/.../video.mp4")
 * result.fold(
 *     success = { segments -> /* handle segments */ },
 *     failure = { error -> /* handle error */ }
 * )
 * ```
 */
class SplitVideoUseCase(
    private val videoRepository: VideoRepository
) {
    /**
     * Splits a video file into WhatsApp Status-compatible segments.
     *
     * The method first validates that the input file exists and is readable,
     * then delegates to the repository for actual splitting.
     *
     * @param videoId The unique identifier of the parent video (used to link segments)
     * @param inputPath The local file path of the video to split
     * @return [Result] containing a list of [VideoSegment]s on success,
     *         or [AppError] on failure. Possible errors include:
     *         - [AppError.StorageError] if the input file doesn't exist or isn't readable
     *         - [AppError.ProcessingError] if splitting fails (codec error, etc.)
     */
    suspend operator fun invoke(
        videoId: String,
        inputPath: String
    ): Result<List<VideoSegment>, AppError> {
        // Validate input parameters
        return validateInputs(videoId, inputPath)
            .andThen { videoRepository.splitVideo(videoId, inputPath) }
    }

    /**
     * Validates the input parameters before splitting.
     *
     * @param videoId The unique identifier of the parent video
     * @param inputPath The local file path of the video to split
     * @return [Result] with Unit on success, or [AppError] on validation failure
     */
    private fun validateInputs(videoId: String, inputPath: String): Result<Unit, AppError> {
        // Validate videoId
        if (videoId.isBlank()) {
            return Err(
                AppError.ProcessingError(
                    message = "Video ID cannot be empty"
                )
            )
        }

        // Validate inputPath
        if (inputPath.isBlank()) {
            return Err(
                AppError.StorageError(
                    message = "Input file path cannot be empty"
                )
            )
        }

        // Validate file exists
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return Err(
                AppError.StorageError(
                    message = "Input file does not exist",
                    path = inputPath
                )
            )
        }

        // Validate file is readable
        if (!inputFile.canRead()) {
            return Err(
                AppError.StorageError(
                    message = "Input file is not readable",
                    path = inputPath
                )
            )
        }

        // Validate it's a file, not a directory
        if (!inputFile.isFile) {
            return Err(
                AppError.StorageError(
                    message = "Path is not a file",
                    path = inputPath
                )
            )
        }

        // Validate file is not empty
        if (inputFile.length() == 0L) {
            return Err(
                AppError.StorageError(
                    message = "Input file is empty",
                    path = inputPath
                )
            )
        }

        // Validate video file extension
        val extension = inputFile.extension.lowercase()
        if (extension.isEmpty()) {
            return Err(
                AppError.ProcessingError(
                    message = "File has no extension. Supported video formats: ${SUPPORTED_VIDEO_EXTENSIONS.joinToString { ".$it" }}"
                )
            )
        }
        if (extension !in SUPPORTED_VIDEO_EXTENSIONS) {
            return Err(
                AppError.ProcessingError(
                    message = "Unsupported video format: .$extension. Supported formats: ${SUPPORTED_VIDEO_EXTENSIONS.joinToString { ".$it" }}"
                )
            )
        }

        return Ok(Unit)
    }

    companion object {
        /**
         * Supported video file extensions for splitting.
         * These are common video formats that Media3 Transformer supports.
         */
        private val SUPPORTED_VIDEO_EXTENSIONS = setOf(
            "mp4",
            "mkv",
            "webm",
            "mov",
            "avi",
            "3gp",
            "m4v"
        )
    }
}
