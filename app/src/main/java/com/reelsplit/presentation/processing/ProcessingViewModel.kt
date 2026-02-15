package com.reelsplit.presentation.processing

import androidx.lifecycle.SavedStateHandle
import com.reelsplit.core.base.BaseViewModel
import com.reelsplit.domain.model.DownloadProgress
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.domain.usecase.DownloadVideoUseCase
import com.reelsplit.domain.usecase.ExtractVideoUrlUseCase
import com.reelsplit.domain.usecase.SplitVideoUseCase
import com.github.michaelbull.result.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.transformWhile
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// ProcessingViewModel
// =============================================================================

/**
 * ViewModel that orchestrates the full video processing pipeline:
 *
 *  1. **Extract** — resolve the Instagram URL to a direct video URL
 *  2. **Download** — download the video file with progress tracking
 *  3. **Split** — split the video into WhatsApp Status–compatible segments
 *
 * The pipeline runs inside a single [launchNamed] coroutine ("processing")
 * so that [onCancel] can kill the entire chain with one call.
 *
 * @param extractVideoUrlUseCase Extracts direct video URL from Instagram link.
 * @param downloadVideoUseCase Downloads video with WorkManager progress tracking.
 * @param splitVideoUseCase Splits downloaded video into ≤90-second segments.
 * @param savedStateHandle Contains the `url` navigation argument.
 */
@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val extractVideoUrlUseCase: ExtractVideoUrlUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val splitVideoUseCase: SplitVideoUseCase,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<ProcessingUiState>(ProcessingUiState.Queued) {

    /** The Instagram URL passed as a navigation argument. */
    private val url: String = checkNotNull(savedStateHandle.get<String>("url")) {
        "ProcessingScreen requires a 'url' navigation argument"
    }

    /** Stable ID for this processing session. Persisted across process death. */
    private val videoId: String = savedStateHandle.get<String>(KEY_VIDEO_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_VIDEO_ID] = it }

    /** File name derived from the video ID. Persisted across process death. */
    private val fileName: String = savedStateHandle.get<String>(KEY_FILE_NAME)
        ?: "reelsplit_$videoId.mp4".also { savedStateHandle[KEY_FILE_NAME] = it }

    init {
        startProcessing()
    }

    // =========================================================================
    // Pipeline Orchestration
    // =========================================================================

    /**
     * Launches the extract → download → split pipeline.
     *
     * Runs inside a named job so that [onCancel] or [onRetry] can
     * cancel/restart it cleanly.
     */
    private fun startProcessing() {
        launchNamed(JOB_NAME) {
            // ──── Stage 1: Extract ──────────────────────────────────
            setState(ProcessingUiState.Extracting(sourceUrl = url))
            Timber.d("Extracting video URL from: $url")

            val directUrl = extractVideoUrlUseCase(url).fold(
                success = { it },
                failure = { error ->
                    Timber.e("Extraction failed: ${error.message}")
                    setState(
                        ProcessingUiState.Error(
                            message = error.message,
                            isRetryable = error.isRetryable,
                            failedAt = ProcessingStage.EXTRACTION
                        )
                    )
                    return@launchNamed
                }
            )

            // ──── Stage 2: Download ─────────────────────────────────
            Timber.d("Downloading video from: $directUrl")
            var downloadedFilePath: String? = null

            // transformWhile terminates collection after a terminal state.
            // Without it, WorkManager's getWorkInfoByIdFlow (a long-lived Flow)
            // would suspend collect forever and the split stage would never start.
            downloadVideoUseCase(
                videoUrl = directUrl,
                fileName = fileName,
                downloadId = videoId
            ).transformWhile { progress ->
                emit(progress)
                // Continue collecting while in a non-terminal state
                progress !is DownloadProgress.Completed &&
                        progress !is DownloadProgress.Failed &&
                        progress !is DownloadProgress.Cancelled
            }.collect { progress ->
                when (progress) {
                    is DownloadProgress.Queued -> {
                        setState(ProcessingUiState.Downloading())
                    }

                    is DownloadProgress.Downloading -> {
                        setState(
                            ProcessingUiState.Downloading(
                                percent = progress.percent,
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                                speedBytesPerSecond = progress.speedBytesPerSecond
                            )
                        )
                    }

                    is DownloadProgress.Paused -> {
                        // Keep showing last known progress while paused
                        setState(
                            ProcessingUiState.Downloading(
                                percent = progress.percent,
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes
                            )
                        )
                    }

                    is DownloadProgress.Completed -> {
                        downloadedFilePath = progress.filePath
                        Timber.d("Download completed: ${progress.filePath}")
                    }

                    is DownloadProgress.Failed -> {
                        Timber.e("Download failed: ${progress.message}")
                        setState(
                            ProcessingUiState.Error(
                                message = progress.message,
                                isRetryable = progress.isRetryable,
                                failedAt = ProcessingStage.DOWNLOAD
                            )
                        )
                    }

                    is DownloadProgress.Cancelled -> {
                        Timber.d("Download cancelled by user")
                        setState(
                            ProcessingUiState.Error(
                                message = "Download cancelled",
                                isRetryable = true,
                                failedAt = ProcessingStage.DOWNLOAD
                            )
                        )
                    }
                }
            }

            // If we didn't get a file path, an error/cancellation state was already set
            val filePath = downloadedFilePath ?: return@launchNamed

            // ──── Stage 3: Split ────────────────────────────────────
            setState(
                ProcessingUiState.Splitting(
                    currentPart = 1,
                    totalParts = 1,
                    progressPercent = 0
                )
            )
            Timber.d("Splitting video: $filePath")

            splitVideoUseCase(videoId = videoId, inputPath = filePath).fold(
                success = { segments ->
                    Timber.d("Splitting complete: ${segments.size} segments")
                    setState(
                        ProcessingUiState.Complete(
                            segments = segments,
                            videoId = videoId
                        )
                    )
                    emitEvent(ProcessingEvent.NavigateToResult(videoId = videoId))
                },
                failure = { error ->
                    Timber.e("Splitting failed: ${error.message}")
                    setState(
                        ProcessingUiState.Error(
                            message = error.message,
                            isRetryable = error.isRetryable,
                            failedAt = ProcessingStage.SPLITTING
                        )
                    )
                }
            )
        }
    }

    // =========================================================================
    // User Actions
    // =========================================================================

    /**
     * Cancels the current processing pipeline.
     * Cancels the named coroutine job and any in-flight WorkManager download,
     * then emits [ProcessingEvent.NavigateBack] to leave the screen.
     */
    fun onCancel() {
        Timber.d("User cancelled processing")
        cancelJob(JOB_NAME)
        downloadVideoUseCase.cancel(videoId)
        emitEvent(ProcessingEvent.NavigateBack)
    }

    /**
     * Dismisses the error state and navigates back.
     * Unlike [onRetry], this does not restart processing.
     */
    fun onDismissError() {
        Timber.d("User dismissed error")
        emitEvent(ProcessingEvent.NavigateBack)
    }

    /**
     * Retries the entire processing pipeline from the beginning.
     */
    fun onRetry() {
        Timber.d("User retrying processing")
        cancelJob(JOB_NAME)
        downloadVideoUseCase.cancel(videoId)
        startProcessing()
    }

    // =========================================================================
    // Exception Handling
    // =========================================================================

    /**
     * Catches uncaught exceptions from [launchNamed] and transitions to the
     * [ProcessingUiState.Error] state so the user always sees an actionable
     * error screen rather than a frozen UI.
     *
     * [BaseViewModel.handleException] only sets [_errorMessage], which
     * [ProcessingScreen] does not observe.
     */
    override fun handleException(throwable: Throwable) {
        Timber.e(throwable, "Uncaught exception in ProcessingViewModel")
        setState(
            ProcessingUiState.Error(
                message = throwable.message ?: "An unexpected error occurred",
                isRetryable = true,
                failedAt = null
            )
        )
    }

    companion object {
        /** Named job identifier for the processing pipeline coroutine. */
        private const val JOB_NAME = "processing"

        /** SavedStateHandle keys for process-death survival. */
        private const val KEY_VIDEO_ID = "video_id"
        private const val KEY_FILE_NAME = "file_name"
    }
}
