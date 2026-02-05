package com.reelsplit.data.processing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.reelsplit.core.constants.WhatsAppConstants
import com.reelsplit.di.IoDispatcher
import com.reelsplit.di.MainDispatcher
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.ProcessingStage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Information about a single split segment.
 *
 * This is a lightweight data class returned by the VideoSplitter, containing only
 * the information that can be determined during the splitting process. The caller
 * is responsible for mapping this to a full [com.reelsplit.domain.model.VideoSegment]
 * with additional metadata like `id` and `videoId`.
 *
 * @property partNumber The 1-indexed part number of this segment
 * @property totalParts Total number of parts the video was split into
 * @property filePath Absolute path to the segment file
 * @property startTimeMs Start time in the original video (milliseconds)
 * @property endTimeMs End time in the original video (milliseconds)
 * @property durationMs Duration of this segment in milliseconds
 * @property fileSizeBytes Size of the segment file in bytes
 */
data class SplitSegmentInfo(
    val partNumber: Int,
    val totalParts: Int,
    val filePath: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val fileSizeBytes: Long
) {
    /**
     * Duration in seconds (for convenience when mapping to VideoSegment).
     */
    val durationSeconds: Long
        get() = durationMs / 1000
    
    /**
     * Start time in seconds (for convenience when mapping to VideoSegment).
     */
    val startTimeSeconds: Long
        get() = startTimeMs / 1000
    
    /**
     * End time in seconds (for convenience when mapping to VideoSegment).
     */
    val endTimeSeconds: Long
        get() = endTimeMs / 1000
}

/**
 * Result of a video splitting operation.
 *
 * @property segments The list of segment information created
 * @property originalDurationMs The original video duration in milliseconds
 */
data class SplitResult(
    val segments: List<SplitSegmentInfo>,
    val originalDurationMs: Long
) {
    /**
     * Returns true if the video was actually split (more than one segment).
     */
    val wasSplit: Boolean
        get() = segments.size > 1
    
    /**
     * Returns the number of segments created.
     */
    val segmentCount: Int
        get() = segments.size
}

/**
 * Splits videos into segments suitable for WhatsApp Status sharing using Media3 Transformer.
 *
 * This class handles the splitting of videos that exceed the WhatsApp Status duration limit
 * (90 seconds) into multiple segments. Each segment is encoded as an MP4 file with H.264 video
 * and AAC audio for maximum compatibility.
 *
 * Key features:
 * - Automatic duration detection using MediaMetadataRetriever
 * - Hardware-accelerated video processing via Media3 Transformer
 * - Graceful handling of short videos (no splitting needed)
 * - Progress reporting during splitting operations
 *
 * Important: The Transformer must be started on the Main thread, but the actual processing
 * happens on a background thread managed by Media3.
 *
 * Note: Progress callbacks report segment-level progress (start/end of each segment).
 * Media3 Transformer does not provide frame-level progress without polling, which would
 * add significant complexity.
 *
 * @property context Application context for creating Transformer instances
 * @property ioDispatcher Dispatcher for IO-bound operations (metadata retrieval, file operations)
 * @property mainDispatcher Dispatcher for Main thread operations (Transformer.start())
 */
@Singleton
class VideoSplitter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
    companion object {
        /** Output filename format for split segments */
        private const val SEGMENT_FILENAME_FORMAT = "part_%03d.mp4"
        
        /** Minimum segment duration to avoid creating very short trailing segments (5 seconds) */
        private const val MIN_SEGMENT_DURATION_MS = 5_000L
        
        /** Minimum allowed segment duration for validation (1 second) */
        private const val MIN_ALLOWED_SEGMENT_DURATION_MS = 1_000L
    }
    
    /**
     * Splits a video into segments suitable for WhatsApp Status.
     *
     * If the video is shorter than or equal to the WhatsApp Status duration limit,
     * no splitting occurs and a single segment is returned pointing to the original file.
     *
     * For longer videos, segments are created with the following naming convention:
     * `part_001.mp4`, `part_002.mp4`, etc.
     *
     * @param inputPath Absolute path to the input video file
     * @param outputDir Directory where split segments will be saved
     * @param segmentDurationMs Optional custom segment duration in milliseconds
     *        (defaults to WhatsApp's effective max duration including safety margin).
     *        Must be at least 1000ms.
     * @param onProgress Optional callback for progress updates. Called with:
     *        - currentSegment: 1-indexed current segment being processed
     *        - totalSegments: total number of segments
     *        - progress: 0.0 at start of segment, 1.0 at completion
     * @return [Result] containing either:
     *   - [Ok] with a [SplitResult] containing segment information
     *   - [Err] with an [AppError] describing what went wrong
     *
     * Possible error scenarios:
     * - [AppError.StorageError]: Input file doesn't exist, output directory issues, or no write permission
     * - [AppError.ProcessingError]: Failed to read video metadata, invalid parameters, or Transformer error
     */
    suspend fun splitVideo(
        inputPath: String,
        outputDir: String,
        segmentDurationMs: Long = WhatsAppConstants.EFFECTIVE_MAX_DURATION_MS,
        onProgress: ((currentSegment: Int, totalSegments: Int, progress: Float) -> Unit)? = null
    ): Result<SplitResult, AppError> = withContext(ioDispatcher) {
        Timber.d("Starting video split: inputPath=$inputPath, outputDir=$outputDir, segmentDurationMs=$segmentDurationMs")
        
        // Validate input parameters
        if (segmentDurationMs < MIN_ALLOWED_SEGMENT_DURATION_MS) {
            Timber.w("Invalid segment duration: $segmentDurationMs (minimum: $MIN_ALLOWED_SEGMENT_DURATION_MS)")
            return@withContext Err(
                AppError.ProcessingError(
                    message = "Segment duration must be at least ${MIN_ALLOWED_SEGMENT_DURATION_MS}ms",
                    stage = ProcessingStage.SPLITTING,
                    isRetryable = false
                )
            )
        }
        
        // Validate input file exists
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            Timber.w("Input file does not exist: $inputPath")
            return@withContext Err(
                AppError.StorageError(
                    message = "Input video file not found",
                    path = inputPath,
                    isRetryable = false
                )
            )
        }
        
        if (!inputFile.canRead()) {
            Timber.w("Input file is not readable: $inputPath")
            return@withContext Err(
                AppError.StorageError(
                    message = "Cannot read input video file",
                    path = inputPath,
                    isRetryable = false
                )
            )
        }
        
        // Create and validate output directory
        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
            Timber.w("Failed to create output directory: $outputDir")
            return@withContext Err(
                AppError.StorageError(
                    message = "Failed to create output directory",
                    path = outputDir,
                    isRetryable = false
                )
            )
        }
        
        if (!outputDirFile.canWrite()) {
            Timber.w("Output directory is not writable: $outputDir")
            return@withContext Err(
                AppError.StorageError(
                    message = "Cannot write to output directory",
                    path = outputDir,
                    isRetryable = false
                )
            )
        }
        
        // Get video duration
        val videoDurationMs = getVideoDurationMs(inputPath)
        if (videoDurationMs == null || videoDurationMs <= 0) {
            Timber.w("Failed to retrieve video duration: $inputPath")
            return@withContext Err(
                AppError.ProcessingError(
                    message = "Failed to read video duration. The file may be corrupted.",
                    stage = ProcessingStage.SPLITTING,
                    isRetryable = false
                )
            )
        }
        
        Timber.d("Video duration: ${videoDurationMs}ms (${videoDurationMs / 1000.0}s)")
        
        // Check if splitting is needed
        if (videoDurationMs <= segmentDurationMs) {
            Timber.d("Video is short enough, no splitting needed")
            
            // Create a single segment pointing to the original file
            val segment = SplitSegmentInfo(
                partNumber = 1,
                totalParts = 1,
                filePath = inputPath,
                startTimeMs = 0,
                endTimeMs = videoDurationMs,
                durationMs = videoDurationMs,
                fileSizeBytes = inputFile.length()
            )
            
            return@withContext Ok(
                SplitResult(
                    segments = listOf(segment),
                    originalDurationMs = videoDurationMs
                )
            )
        }
        
        // Calculate number of segments needed
        val segmentRanges = calculateSegments(videoDurationMs, segmentDurationMs)
        val totalParts = segmentRanges.size
        Timber.d("Video will be split into $totalParts segments")
        
        // Process each segment
        val processedSegments = mutableListOf<SplitSegmentInfo>()
        
        for ((index, segmentRange) in segmentRanges.withIndex()) {
            val partNumber = index + 1
            val outputFileName = SEGMENT_FILENAME_FORMAT.format(partNumber)
            val outputPath = File(outputDir, outputFileName).absolutePath
            
            Timber.d("Processing segment $partNumber/$totalParts: ${segmentRange.first}ms - ${segmentRange.second}ms")
            onProgress?.invoke(partNumber, totalParts, 0f)
            
            val result = processSegment(
                inputPath = inputPath,
                outputPath = outputPath,
                startMs = segmentRange.first,
                endMs = segmentRange.second,
                partNumber = partNumber,
                totalParts = totalParts
            )
            
            when (result) {
                is Ok -> {
                    processedSegments.add(result.value)
                    onProgress?.invoke(partNumber, totalParts, 1f)
                }
                is Err -> {
                    // Clean up any already-created segments on failure
                    cleanupSegments(processedSegments)
                    return@withContext Err(result.error)
                }
            }
        }
        
        Timber.d("Video splitting complete: ${processedSegments.size} segments created")
        Ok(
            SplitResult(
                segments = processedSegments,
                originalDurationMs = videoDurationMs
            )
        )
    }
    
    /**
     * Gets the duration of a video file in milliseconds using MediaMetadataRetriever.
     *
     * @param filePath Path to the video file
     * @return Duration in milliseconds, or null if retrieval failed
     */
    private fun getVideoDurationMs(filePath: String): Long? {
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                val durationString = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )
                durationString?.toLongOrNull()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve video duration for: $filePath")
            null
        }
    }
    
    /**
     * Calculates segment time ranges for splitting.
     *
     * Handles edge cases like very short trailing segments by adjusting
     * the previous segment's end time.
     *
     * @param totalDurationMs Total video duration in milliseconds
     * @param segmentDurationMs Target duration for each segment
     * @return List of (startMs, endMs) pairs for each segment
     */
    private fun calculateSegments(
        totalDurationMs: Long,
        segmentDurationMs: Long
    ): List<Pair<Long, Long>> {
        val segments = mutableListOf<Pair<Long, Long>>()
        var currentStartMs = 0L
        
        while (currentStartMs < totalDurationMs) {
            val remainingDuration = totalDurationMs - currentStartMs
            
            // Check if the remaining duration is less than segment duration
            if (remainingDuration <= segmentDurationMs) {
                // This is the last segment - use remaining duration
                segments.add(Pair(currentStartMs, totalDurationMs))
                break
            }
            
            // Check if next segment would be too short
            val proposedEndMs = currentStartMs + segmentDurationMs
            val durationAfterThis = totalDurationMs - proposedEndMs
            
            if (durationAfterThis > 0 && durationAfterThis < MIN_SEGMENT_DURATION_MS) {
                // Remaining duration is too short - merge with current segment
                segments.add(Pair(currentStartMs, totalDurationMs))
                break
            }
            
            // Create normal segment
            segments.add(Pair(currentStartMs, proposedEndMs))
            currentStartMs = proposedEndMs
        }
        
        return segments
    }
    
    /**
     * Processes a single video segment using Media3 Transformer.
     *
     * Must call Transformer.start() on the Main thread as required by Media3.
     * The Transformer is properly released after completion or cancellation.
     *
     * @param inputPath Path to the source video
     * @param outputPath Path for the output segment file
     * @param startMs Start time in milliseconds
     * @param endMs End time in milliseconds
     * @param partNumber 1-indexed part number
     * @param totalParts Total number of parts
     * @return Result containing the SplitSegmentInfo on success, or AppError on failure
     */
    private suspend fun processSegment(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        partNumber: Int,
        totalParts: Int
    ): Result<SplitSegmentInfo, AppError> {
        return try {
            // Switch to Main thread to create and start Transformer
            val exportResult = withContext(mainDispatcher) {
                // Build ClippingConfiguration for the segment
                val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
                
                // Create MediaItem with clipping using proper file URI
                val inputUri = File(inputPath).toUri()
                val mediaItem = MediaItem.Builder()
                    .setUri(inputUri)
                    .setClippingConfiguration(clippingConfiguration)
                    .build()
                
                val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
                
                // Use suspendCancellableCoroutine to bridge Transformer's callback-based API
                suspendCancellableCoroutine { continuation ->
                    // Build Transformer with listener
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult
                            ) {
                                Timber.d("Segment $partNumber export completed")
                                if (continuation.isActive) {
                                    continuation.resume(Ok(exportResult))
                                }
                            }
                            
                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                Timber.e(exportException, "Segment $partNumber export failed")
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Err(
                                            AppError.ProcessingError(
                                                message = "Failed to split video segment $partNumber: ${exportException.message ?: "Unknown error"}",
                                                stage = ProcessingStage.SPLITTING,
                                                isRetryable = false
                                            )
                                        )
                                    )
                                }
                            }
                        })
                        .build()
                    
                    // Register cancellation handler - cancel and release transformer
                    continuation.invokeOnCancellation {
                        Timber.d("Segment $partNumber processing cancelled")
                        transformer.cancel()
                    }
                    
                    // Start the transformer (we're already on Main thread)
                    transformer.start(editedMediaItem, outputPath)
                }
            }
            
            // Handle the result (back on IO dispatcher)
            when (exportResult) {
                is Ok -> {
                    val outputFile = File(outputPath)
                    
                    // Verify output file was created successfully
                    if (!outputFile.exists()) {
                        Timber.w("Output file was not created: $outputPath")
                        return Err(
                            AppError.ProcessingError(
                                message = "Failed to create segment $partNumber: Output file not found",
                                stage = ProcessingStage.SPLITTING,
                                isRetryable = false
                            )
                        )
                    }
                    
                    val fileSizeBytes = outputFile.length()
                    if (fileSizeBytes == 0L) {
                        Timber.w("Output file is empty: $outputPath")
                        return Err(
                            AppError.ProcessingError(
                                message = "Failed to create segment $partNumber: Output file is empty",
                                stage = ProcessingStage.SPLITTING,
                                isRetryable = false
                            )
                        )
                    }
                    
                    val durationMs = endMs - startMs
                    
                    Ok(
                        SplitSegmentInfo(
                            partNumber = partNumber,
                            totalParts = totalParts,
                            filePath = outputPath,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            durationMs = durationMs,
                            fileSizeBytes = fileSizeBytes
                        )
                    )
                }
                is Err -> Err(exportResult.error)
            }
        } catch (e: CancellationException) {
            // Re-throw to maintain structured concurrency
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error processing segment $partNumber")
            Err(
                AppError.ProcessingError(
                    message = "Unexpected error during video splitting: ${e.message ?: "Unknown error"}",
                    stage = ProcessingStage.SPLITTING,
                    isRetryable = false
                )
            )
        }
    }
    
    /**
     * Cleans up segment files that were created before a failure occurred.
     *
     * @param segments List of segments to clean up
     */
    private fun cleanupSegments(segments: List<SplitSegmentInfo>) {
        for (segment in segments) {
            try {
                val file = File(segment.filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Timber.d("Cleanup segment ${segment.partNumber}: deleted=$deleted")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to clean up segment file: ${segment.filePath}")
            }
        }
    }
}
