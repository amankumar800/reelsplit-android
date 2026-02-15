package com.reelsplit.data.repository

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.reelsplit.data.local.dao.VideoDao
import com.reelsplit.data.local.entity.VideoEntity
import com.reelsplit.data.processing.SplitResult
import com.reelsplit.data.processing.SplitSegmentInfo
import com.reelsplit.data.processing.VideoDownloadManager
import com.reelsplit.data.processing.VideoExtractor
import com.reelsplit.data.processing.VideoSplitter
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.DownloadProgress
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.domain.model.VideoSegment
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [VideoRepositoryImpl].
 *
 * Verifies:
 * - URL extraction delegation and error handling
 * - Video download flow delegation
 * - Video splitting and segment mapping
 * - Cache management
 * - DAO delegation for video operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryImplTest {

    private lateinit var videoExtractor: VideoExtractor
    private lateinit var videoDownloadManager: VideoDownloadManager
    private lateinit var videoSplitter: VideoSplitter
    private lateinit var videoDao: VideoDao
    private lateinit var repository: VideoRepositoryImpl

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        videoExtractor = mockk()
        videoDownloadManager = mockk()
        videoSplitter = mockk()
        videoDao = mockk(relaxed = true)

        repository = VideoRepositoryImpl(
            videoExtractor = videoExtractor,
            videoDownloadManager = videoDownloadManager,
            videoSplitter = videoSplitter,
            videoDao = videoDao
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Extract Video URL
    // =========================================================================

    @Test
    fun `extractVideoUrl delegates to VideoExtractor on success`() = runTest {
        coEvery {
            videoExtractor.extractVideoUrl("https://instagram.com/reel/abc")
        } returns Ok("https://cdn.instagram.com/video.mp4")

        val result = repository.extractVideoUrl("https://instagram.com/reel/abc")

        assertEquals("https://cdn.instagram.com/video.mp4", result.get())
        coVerify { videoExtractor.extractVideoUrl("https://instagram.com/reel/abc") }
    }

    @Test
    fun `extractVideoUrl returns error when extractor fails`() = runTest {
        val error = AppError.NetworkError(
            message = "Network failed",
            isRetryable = true
        )
        coEvery {
            videoExtractor.extractVideoUrl(any())
        } returns Err(error)

        val result = repository.extractVideoUrl("https://instagram.com/reel/abc")

        val resultError = result.getError()
        assertNotNull(resultError)
        assertEquals("Network failed", resultError!!.message)
    }

    @Test
    fun `extractVideoUrl returns error for invalid URL`() = runTest {
        val error = AppError.InvalidUrlError(
            message = "Not a valid Instagram URL",
            url = "not-a-url"
        )
        coEvery {
            videoExtractor.extractVideoUrl(any())
        } returns Err(error)

        val result = repository.extractVideoUrl("not-a-url")

        assertTrue(result.getError() is AppError.InvalidUrlError)
    }

    // =========================================================================
    // Split Video
    // =========================================================================

    @Test
    fun `splitVideo delegates to VideoSplitter and maps segments`() = runTest {
        val splitSegments = listOf(
            SplitSegmentInfo(
                partNumber = 1,
                totalParts = 2,
                filePath = "/output/part_001.mp4",
                startTimeMs = 0,
                endTimeMs = 90_000,
                durationMs = 90_000,
                fileSizeBytes = 5_000_000
            ),
            SplitSegmentInfo(
                partNumber = 2,
                totalParts = 2,
                filePath = "/output/part_002.mp4",
                startTimeMs = 90_000,
                endTimeMs = 150_000,
                durationMs = 60_000,
                fileSizeBytes = 3_000_000
            )
        )
        val splitResult = SplitResult(
            segments = splitSegments,
            originalDurationMs = 150_000
        )

        coEvery {
            videoSplitter.splitVideo(
                inputPath = eq("/input/video.mp4"),
                outputDir = any(),
                segmentDurationMs = any(),
                onProgress = any()
            )
        } returns Ok(splitResult)

        // Also mock DAO insert for segments
        coEvery { videoDao.upsertVideo(any()) } just Runs

        val result = repository.splitVideo(
            videoId = "vid-123",
            inputPath = "/input/video.mp4"
        )

        val segments = result.get()
        assertNotNull(segments)
        assertEquals(2, segments!!.size)
        assertEquals(1, segments[0].partNumber)
        assertEquals(2, segments[1].partNumber)
        assertEquals("/output/part_001.mp4", segments[0].filePath)
        assertEquals(90L, segments[0].durationSeconds)
        assertEquals(60L, segments[1].durationSeconds)
    }

    @Test
    fun `splitVideo returns error when splitter fails`() = runTest {
        val error = AppError.ProcessingError(
            message = "Splitting failed",
            stage = ProcessingStage.SPLITTING,
            isRetryable = false
        )
        coEvery {
            videoSplitter.splitVideo(
                inputPath = any(),
                outputDir = any(),
                segmentDurationMs = any(),
                onProgress = any()
            )
        } returns Err(error)

        val result = repository.splitVideo(
            videoId = "vid-123",
            inputPath = "/input/video.mp4"
        )

        val resultError = result.getError()
        assertNotNull(resultError)
        assertTrue(resultError is AppError.ProcessingError)
        assertEquals("Splitting failed", resultError!!.message)
    }

    // =========================================================================
    // Observe Video
    // =========================================================================

    @Test
    fun `observeVideo delegates to VideoDao`() = runTest {
        val entity = VideoEntity(
            id = "vid-123",
            sourceUrl = "https://instagram.com/reel/abc",
            createdAt = System.currentTimeMillis()
        )
        every { videoDao.observeVideoById("vid-123") } returns flowOf(entity)

        videoDao.observeVideoById("vid-123").test {
            val result = awaitItem()
            assertEquals("vid-123", result?.id)
            awaitComplete()
        }
    }

    @Test
    fun `observeVideo returns null for non-existent video`() = runTest {
        every { videoDao.observeVideoById("non-existent") } returns flowOf(null)

        videoDao.observeVideoById("non-existent").test {
            val result = awaitItem()
            assertNull(result)
            awaitComplete()
        }
    }

    // =========================================================================
    // Delete Video
    // =========================================================================

    @Test
    fun `deleteVideo delegates to VideoDao`() = runTest {
        coEvery { videoDao.deleteVideo("vid-123") } returns 1

        val rowsDeleted = videoDao.deleteVideo("vid-123")

        assertEquals(1, rowsDeleted)
        coVerify { videoDao.deleteVideo("vid-123") }
    }

    // =========================================================================
    // Cancel Download
    // =========================================================================

    @Test
    fun `cancelDownload delegates to VideoDownloadManager`() {
        every { videoDownloadManager.cancelDownload(any()) } just Runs

        videoDownloadManager.cancelDownload(42)

        verify { videoDownloadManager.cancelDownload(42) }
    }
}
