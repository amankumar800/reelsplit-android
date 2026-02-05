package com.reelsplit.domain.usecase

import com.reelsplit.domain.model.Video
import com.reelsplit.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Use case for retrieving video information.
 *
 * This use case provides access to video metadata from the repository,
 * supporting both ID-based lookup and reactive updates via Flow.
 *
 * Usage:
 * ```
 * val getVideoInfo = GetVideoInfoUseCase(videoRepository)
 * getVideoInfo("video-id-123")
 *     .collect { video ->
 *         video?.let { /* display video info */ }
 *             ?: /* handle missing video */
 *     }
 * ```
 */
class GetVideoInfoUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    /**
     * Retrieves video information by its unique identifier.
     *
     * Returns a Flow that emits the video if found, or null if not found.
     * The Flow allows observing changes to the video (e.g., status updates,
     * segment additions) reactively.
     *
     * @param videoId The unique identifier of the video to retrieve
     * @return [Flow] emitting the [Video] if found, or null if not found.
     *         The Flow will emit updates whenever the video data changes.
     *         Returns null immediately if videoId is blank.
     */
    operator fun invoke(videoId: String): Flow<Video?> {
        // Return null immediately for blank IDs to avoid unnecessary database queries
        if (videoId.isBlank()) {
            return flowOf(null)
        }
        return videoRepository.getVideoById(videoId)
    }

    /**
     * Retrieves all videos stored in the repository.
     *
     * Returns a Flow that emits all videos, sorted by creation date
     * (most recent first). Useful for displaying video history.
     *
     * @return [Flow] emitting a list of all [Video]s, or empty list if none exist.
     */
    fun getAllVideos(): Flow<List<Video>> {
        return videoRepository.getAllVideos()
    }
}
