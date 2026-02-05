package com.reelsplit.domain.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.repository.VideoRepository
import javax.inject.Inject

/**
 * Use case for extracting a direct video URL from an Instagram reel URL.
 *
 * This use case encapsulates the business logic for video URL extraction,
 * including input validation, before delegating to the [VideoRepository].
 *
 * Usage:
 * ```
 * val extractVideoUrl = ExtractVideoUrlUseCase(videoRepository)
 * val result = extractVideoUrl("https://www.instagram.com/reel/xxx")
 * result.fold(
 *     success = { directUrl -> /* use the URL */ },
 *     failure = { error -> /* handle error */ }
 * )
 * ```
 */
class ExtractVideoUrlUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    /**
     * Extracts the direct video URL from an Instagram reel URL.
     *
     * Performs input validation before delegating to the repository.
     *
     * @param instagramUrl The Instagram reel URL (e.g., https://www.instagram.com/reel/xxx)
     * @return [Result] with the direct video URL on success,
     *         or [AppError.InvalidUrlError] if the URL is blank,
     *         or other [AppError] on failure (e.g., network error, rate limited)
     */
    suspend operator fun invoke(instagramUrl: String): Result<String, AppError> {
        // Validate input - this is a domain-level business rule
        if (instagramUrl.isBlank()) {
            return Err(
                AppError.InvalidUrlError(
                    message = "Instagram URL cannot be empty",
                    url = instagramUrl
                )
            )
        }

        return videoRepository.extractVideoUrl(instagramUrl)
    }
}
