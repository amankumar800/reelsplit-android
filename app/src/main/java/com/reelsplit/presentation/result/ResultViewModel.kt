package com.reelsplit.presentation.result

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import com.github.michaelbull.result.fold
import com.reelsplit.core.base.BaseViewModel
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.repository.VideoRepository
import com.reelsplit.domain.sharing.WhatsAppSharerContract
import com.reelsplit.domain.usecase.ShareToWhatsAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

// =============================================================================
// ResultViewModel
// =============================================================================

/**
 * ViewModel for the ResultScreen.
 *
 * Loads video segments for a completed processing job and provides:
 * - Individual segment sharing to WhatsApp Status or Chat
 * - Bulk "Share All to Status" action
 * - Save to Gallery via MediaStore
 * - In-app review prompt after the first successful share
 *
 * Segments are loaded reactively from [VideoRepository], so any updates
 * (e.g., marking a segment as shared) are automatically reflected in the UI.
 *
 * @param videoRepository Repository for loading segments and marking them shared.
 * @param shareToWhatsAppUseCase Use case for sharing individual segments to Status.
 * @param whatsAppSharer Contract for WhatsApp chat sharing (Status is handled by use case).
 * @param applicationContext Application context for MediaStore operations.
 * @param savedStateHandle Contains the `videoId` navigation argument.
 */
@HiltViewModel
class ResultViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val shareToWhatsAppUseCase: ShareToWhatsAppUseCase,
    private val whatsAppSharer: WhatsAppSharerContract,
    @ApplicationContext private val applicationContext: Context,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<ResultUiState>(ResultUiState.Loading) {

    /** The video ID passed as a navigation argument. */
    private val videoId: String = checkNotNull(savedStateHandle.get<String>("videoId")) {
        "ResultScreen requires a 'videoId' navigation argument"
    }

    /** Tracks whether we've already requested an in-app review this session. */
    private var hasRequestedReview: Boolean = false

    init {
        loadSegments()
    }

    // =========================================================================
    // Data Loading
    // =========================================================================

    /**
     * Loads segments reactively from the repository.
     *
     * Collects [VideoRepository.getSegmentsByVideoId] as a Flow, so the UI
     * automatically updates when segments are marked as shared.
     *
     * **Important:** When updating segments from the Flow, transient UI flags
     * (`isSharingAll`, `isSaving`) are preserved from the current state to
     * prevent clobbering during in-progress operations.
     */
    private fun loadSegments() {
        launchNamed(JOB_LOAD_SEGMENTS) {
            videoRepository.getSegmentsByVideoId(videoId).collect { segments ->
                if (segments.isEmpty()) {
                    setState(
                        ResultUiState.Error(
                            message = "No segments found for this video. " +
                                "The processing may not have completed successfully."
                        )
                    )
                } else {
                    // Preserve transient flags from the current Loaded state
                    // to avoid clobbering isSharingAll / isSaving mid-operation.
                    val current = currentState as? ResultUiState.Loaded
                    setState(
                        ResultUiState.Loaded(
                            segments = segments,
                            videoId = videoId,
                            isSharingAll = current?.isSharingAll ?: false,
                            isSaving = current?.isSaving ?: false
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // User Actions — Sharing
    // =========================================================================

    /**
     * Shares a single segment to WhatsApp Status.
     *
     * Validates the segment, invokes the share use case, marks it as shared
     * on success, and triggers an in-app review on the first successful share.
     *
     * @param segmentId The ID of the segment to share.
     */
    fun onShareToStatus(segmentId: String) {
        val segment = findSegment(segmentId) ?: return
        Timber.d("Sharing segment ${segment.displayName} to Status")

        launchSafe {
            shareToWhatsAppUseCase(videoPath = segment.filePath).fold(
                success = {
                    Timber.d("Share to Status succeeded for ${segment.displayName}")
                    markSegmentShared(segmentId)
                    maybeRequestInAppReview()
                },
                failure = { error ->
                    Timber.e("Share to Status failed: ${error.message}")
                    emitEvent(ResultEvent.ShowShareError(error.message))
                }
            )
        }
    }

    /**
     * Shares a single segment to WhatsApp Chat.
     *
     * Uses [WhatsAppSharerContract.shareToWhatsAppChat] directly since
     * [ShareToWhatsAppUseCase] only handles the Status path. Wrapped in
     * [launchSafe] for consistent error handling via the coroutine exception
     * handler.
     *
     * @param segmentId The ID of the segment to share.
     */
    fun onShareToChat(segmentId: String) {
        val segment = findSegment(segmentId) ?: return
        Timber.d("Sharing segment ${segment.displayName} to Chat")

        launchSafe {
            try {
                whatsAppSharer.shareToWhatsAppChat(segment.filePath)
                markSegmentShared(segmentId)
            } catch (e: Exception) {
                Timber.e(e, "Share to Chat failed")
                emitEvent(
                    ResultEvent.ShowShareError(
                        e.message ?: "Failed to share to WhatsApp Chat"
                    )
                )
            }
        }
    }

    /**
     * Shares all valid segments to WhatsApp Status sequentially.
     *
     * Marks each segment as shared upon successful sharing. Emits an error
     * event if any segment fails but continues with the remaining segments.
     * The `isSharingAll` flag is set for the duration, disabling individual
     * share buttons in the UI.
     */
    fun onShareAllToStatus() {
        val state = currentState as? ResultUiState.Loaded ?: return
        if (state.isSharingAll) return // Guard against double-tap

        val validSegments = state.segments.filter {
            it.isValidForWhatsAppStatus && !it.isShared
        }
        if (validSegments.isEmpty()) return

        Timber.d("Sharing all ${validSegments.size} segments to Status")

        launchNamed(JOB_SHARE_ALL) {
            updateState { if (this is ResultUiState.Loaded) copy(isSharingAll = true) else this }

            try {
                var shareCount = 0
                for (segment in validSegments) {
                    shareToWhatsAppUseCase(videoPath = segment.filePath).fold(
                        success = {
                            shareCount++
                            markSegmentShared(segment.id)
                            Timber.d("Shared ${segment.displayName} ($shareCount/${validSegments.size})")
                        },
                        failure = { error ->
                            Timber.e("Failed to share ${segment.displayName}: ${error.message}")
                            emitEvent(ResultEvent.ShowShareError(error.message))
                        }
                    )
                }

                if (shareCount > 0) {
                    maybeRequestInAppReview()
                }
            } finally {
                updateState { if (this is ResultUiState.Loaded) copy(isSharingAll = false) else this }
            }
        }
    }

    // =========================================================================
    // User Actions — Save to Gallery
    // =========================================================================

    /**
     * Saves all video segments to the device's gallery (Movies directory)
     * using [MediaStore] for scoped storage compatibility.
     *
     * Tracks per-segment success/failure and reports the outcome via
     * [ResultEvent.ShowSavedToGallery]. The `isSaving` flag prevents
     * duplicate taps while the operation is in progress.
     */
    fun onSaveToGallery() {
        val state = currentState as? ResultUiState.Loaded ?: return
        if (state.isSaving) return // Guard against double-tap

        Timber.d("Saving ${state.segments.size} segments to gallery")

        launchNamed(JOB_SAVE_GALLERY) {
            updateState { if (this is ResultUiState.Loaded) copy(isSaving = true) else this }

            try {
                var savedCount = 0
                var failedCount = 0

                withContext(Dispatchers.IO) {
                    for (segment in state.segments) {
                        val success = saveSegmentToGallery(segment)
                        if (success) savedCount++ else failedCount++
                    }
                }

                Timber.d("Gallery save complete: $savedCount saved, $failedCount failed")
                emitEvent(ResultEvent.ShowSavedToGallery(savedCount = savedCount, failedCount = failedCount))
            } finally {
                updateState { if (this is ResultUiState.Loaded) copy(isSaving = false) else this }
            }
        }
    }

    /**
     * Saves a single segment file to the Movies directory via MediaStore.
     *
     * On API 29+, uses scoped storage (no permissions needed).
     * On older APIs, writes directly to the external Movies directory.
     *
     * @return `true` if the file was saved successfully, `false` otherwise.
     */
    private fun saveSegmentToGallery(segment: VideoSegment): Boolean {
        val sourceFile = File(segment.filePath)
        if (!sourceFile.exists()) {
            Timber.w("Segment file not found: ${segment.filePath}")
            return false
        }

        val fileName = "ReelSplit_${segment.videoId}_part${segment.partNumber}.mp4"
        val resolver = applicationContext.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/ReelSplit"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: run {
            Timber.e("Failed to create MediaStore entry for $fileName")
            return false
        }

        return try {
            val outputStream = resolver.openOutputStream(uri)
                ?: run {
                    Timber.e("Failed to open output stream for $fileName")
                    resolver.delete(uri, null, null)
                    return false
                }

            outputStream.use { os ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(os)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Timber.d("Saved segment to gallery: $fileName")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save segment to gallery: $fileName")
            // Clean up the partially written entry
            resolver.delete(uri, null, null)
            false
        }
    }

    // =========================================================================
    // User Actions — Navigation
    // =========================================================================

    /**
     * Navigates back to the home screen.
     */
    fun onNavigateHome() {
        Timber.d("User navigating home from results")
        emitEvent(ResultEvent.NavigateHome)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Finds a segment by ID from the current loaded state.
     *
     * @param segmentId The segment ID to look up.
     * @return The [VideoSegment], or null if not in the Loaded state or not found.
     */
    private fun findSegment(segmentId: String): VideoSegment? {
        val state = currentState as? ResultUiState.Loaded ?: return null
        return state.segments.find { it.id == segmentId }
    }

    /**
     * Marks a segment as shared in the repository.
     * Errors are logged but not surfaced to the user since the share itself succeeded.
     */
    private fun markSegmentShared(segmentId: String) {
        launchSafe {
            videoRepository.markSegmentAsShared(segmentId).fold(
                success = { Timber.d("Marked segment $segmentId as shared") },
                failure = { error ->
                    Timber.w("Failed to mark segment $segmentId as shared: ${error.message}")
                }
            )
        }
    }

    /**
     * Triggers an in-app review prompt once per session, after the first
     * successful share operation.
     */
    private fun maybeRequestInAppReview() {
        if (!hasRequestedReview) {
            hasRequestedReview = true
            emitEvent(ResultEvent.RequestInAppReview)
        }
    }

    // =========================================================================
    // Exception Handling
    // =========================================================================

    /**
     * Catches uncaught exceptions and transitions to the Error state.
     *
     * Cancels the segment-loading Flow to prevent it from overriding the
     * error state with new segment emissions.
     */
    override fun handleException(throwable: Throwable) {
        Timber.e(throwable, "Uncaught exception in ResultViewModel")
        cancelJob(JOB_LOAD_SEGMENTS)
        setState(
            ResultUiState.Error(
                message = throwable.message ?: "An unexpected error occurred"
            )
        )
    }

    companion object {
        /** Named job identifier for segment loading. */
        private const val JOB_LOAD_SEGMENTS = "load_segments"

        /** Named job identifier for share-all operation. */
        private const val JOB_SHARE_ALL = "share_all"

        /** Named job identifier for gallery save operation. */
        private const val JOB_SAVE_GALLERY = "save_gallery"
    }
}
