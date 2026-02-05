package com.reelsplit.data.processing

import android.content.Context
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.reelsplit.di.IoDispatcher
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.ProcessingStage
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts direct video URLs from Instagram reel URLs using youtubedl-android.
 *
 * This class wraps the yt-dlp library to extract the actual video URL from an Instagram
 * reel link. The extracted URL can then be used by a downloader to fetch the video content.
 *
 * Important: YoutubeDL must be initialized in the Application class before using this extractor.
 * See [com.reelsplit.ReelSplitApplication] for initialization logic.
 *
 * @property context Application context (reserved for future use, e.g., cache directory access).
 * @property ioDispatcher Dispatcher for IO-bound operations, injected for testability.
 */
@Singleton
class VideoExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Extracts the direct video URL from an Instagram reel URL.
     *
     * The extraction is performed on the IO dispatcher to avoid blocking the main thread.
     * Uses yt-dlp under the hood with options optimized for Instagram reels:
     * - `best[ext=mp4]`: Selects the best quality MP4 format
     * - `--no-playlist`: Prevents downloading multiple videos if the URL points to a playlist
     *
     * @param instagramUrl The Instagram reel URL (e.g., https://www.instagram.com/reel/xxx/)
     * @return [Result] containing either:
     *   - [Ok] with the direct video URL on success
     *   - [Err] with an [AppError] describing what went wrong
     *
     * Possible error scenarios:
     * - [AppError.InvalidUrlError]: URL format is invalid or not supported
     * - [AppError.NetworkError]: Network issues or Instagram API problems
     * - [AppError.ProcessingError]: yt-dlp failed to extract video info
     */
    suspend fun extractVideoUrl(instagramUrl: String): Result<String, AppError> = 
        withContext(ioDispatcher) {
            Timber.d("Extracting video URL from: $instagramUrl")
            
            // Early validation for Instagram URLs
            if (!isValidInstagramUrl(instagramUrl)) {
                Timber.w("Invalid Instagram URL: $instagramUrl")
                return@withContext Err(
                    AppError.InvalidUrlError(
                        message = "The URL is not a valid Instagram reel URL",
                        url = instagramUrl,
                        isRetryable = false
                    )
                )
            }
            
            try {
                val request = YoutubeDLRequest(instagramUrl).apply {
                    // Select best quality MP4 format for WhatsApp compatibility
                    addOption("-f", "best[ext=mp4]")
                    // Don't process playlists, only single videos
                    addOption("--no-playlist")
                }
                
                val videoInfo = YoutubeDL.getInstance().getInfo(request)
                val videoUrl = videoInfo.url
                
                if (videoUrl.isNullOrBlank()) {
                    Timber.w("Extracted video URL is null or blank for: $instagramUrl")
                    Err(
                        AppError.ProcessingError(
                            message = "Failed to extract video URL: No URL found in video info",
                            stage = ProcessingStage.EXTRACTION,
                            isRetryable = true
                        )
                    )
                } else {
                    Timber.d("Successfully extracted video URL")
                    Ok(videoUrl)
                }
            } catch (e: CancellationException) {
                // Re-throw cancellation to maintain structured concurrency
                throw e
            } catch (e: YoutubeDLException) {
                Timber.e(e, "YoutubeDL failed to extract video URL")
                Err(mapYoutubeDLException(e, instagramUrl))
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error extracting video URL")
                Err(
                    AppError.ProcessingError(
                        message = "Unexpected error during URL extraction: ${e.message ?: "Unknown error"}",
                        stage = ProcessingStage.EXTRACTION,
                        isRetryable = false
                    )
                )
            }
        }
    
    /**
     * Validates that the URL is a valid Instagram URL.
     *
     * Accepts both instagram.com and instagr.am domains, as well as various
     * URL patterns (reel, p, tv).
     */
    private fun isValidInstagramUrl(url: String): Boolean {
        val instagramPatterns = listOf(
            "instagram.com/reel/",
            "instagram.com/reels/",  // Alternative plural form
            "instagram.com/p/",
            "instagram.com/tv/",
            "instagr.am/reel/",
            "instagr.am/reels/",
            "instagr.am/p/",
            "instagr.am/tv/"
        )
        val lowerUrl = url.lowercase()
        return instagramPatterns.any { lowerUrl.contains(it) }
    }
    
    /**
     * Maps YoutubeDLException to an appropriate AppError.
     *
     * Analyzes the exception message to determine the most appropriate error type.
     */
    private fun mapYoutubeDLException(e: YoutubeDLException, url: String): AppError {
        val message = e.message ?: "Unknown extraction error"
        val lowerMessage = message.lowercase()
        
        return when {
            // URL-related errors
            lowerMessage.contains("unsupported url") ||
            lowerMessage.contains("url format not recognized") ||
            lowerMessage.contains("invalid url") -> AppError.InvalidUrlError(
                message = "The provided URL is not supported: $message",
                url = url,
                isRetryable = false
            )
            
            // Network-related errors
            lowerMessage.contains("network") ||
            lowerMessage.contains("connection") ||
            lowerMessage.contains("timeout") ||
            lowerMessage.contains("unable to download") ||
            lowerMessage.contains("http error") -> AppError.NetworkError(
                message = "Network error during extraction: $message",
                isRetryable = true
            )
            
            // Private/restricted content
            lowerMessage.contains("private") ||
            lowerMessage.contains("login required") ||
            lowerMessage.contains("restricted") -> AppError.ProcessingError(
                message = "This content is private or requires login",
                stage = ProcessingStage.EXTRACTION,
                isRetryable = false
            )
            
            // Content not found
            lowerMessage.contains("not found") ||
            lowerMessage.contains("does not exist") ||
            lowerMessage.contains("deleted") -> AppError.ProcessingError(
                message = "The video could not be found. It may have been deleted.",
                stage = ProcessingStage.EXTRACTION,
                isRetryable = false
            )
            
            // Default to processing error
            else -> AppError.ProcessingError(
                message = "Failed to extract video URL: $message",
                stage = ProcessingStage.EXTRACTION,
                isRetryable = true
            )
        }
    }
}
