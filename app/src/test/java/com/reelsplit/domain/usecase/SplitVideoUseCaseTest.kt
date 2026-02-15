package com.reelsplit.domain.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.repository.VideoRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [SplitVideoUseCase].
 *
 * Verifies:
 * - Input validation (blank videoId, blank inputPath, invalid extension)
 * - File validation (non-existent, unreadable)
 * - Successful delegation to VideoRepository
 * - Error propagation from VideoRepository
 */
class SplitVideoUseCaseTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var videoRepository: VideoRepository
    private lateinit var useCase: SplitVideoUseCase

    @Before
    fun setUp() {
        videoRepository = mockk()
        useCase = SplitVideoUseCase(videoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Input Validation — videoId
    // =========================================================================

    @Test
    fun `blank videoId returns ProcessingError`() = runTest {
        val result = useCase(videoId = "", inputPath = "/some/path.mp4")
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.ProcessingError)
    }

    @Test
    fun `whitespace-only videoId returns ProcessingError`() = runTest {
        val result = useCase(videoId = "   ", inputPath = "/some/path.mp4")
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.ProcessingError)
    }

    // =========================================================================
    // Input Validation — inputPath
    // =========================================================================

    @Test
    fun `blank inputPath returns ProcessingError`() = runTest {
        val result = useCase(videoId = "video-123", inputPath = "")
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.ProcessingError)
    }

    @Test
    fun `whitespace-only inputPath returns ProcessingError`() = runTest {
        val result = useCase(videoId = "video-123", inputPath = "   ")
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.ProcessingError)
    }

    // =========================================================================
    // File Validation
    // =========================================================================

    @Test
    fun `non-existent file returns StorageError`() = runTest {
        val result = useCase(
            videoId = "video-123",
            inputPath = "/does/not/exist.mp4"
        )
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.StorageError)
    }

    @Test
    fun `unsupported file extension returns ProcessingError`() = runTest {
        val file = tempFolder.newFile("video.xyz")
        file.writeText("content")

        val result = useCase(
            videoId = "video-123",
            inputPath = file.absolutePath
        )
        val error = result.getError()
        assertNotNull(error)
        assertTrue(error is AppError.ProcessingError)
    }

    // =========================================================================
    // Successful Delegation
    // =========================================================================

    @Test
    fun `valid input delegates to repository and returns segments`() = runTest {
        val file = tempFolder.newFile("video.mp4")
        file.writeText("fake video content")

        val expectedSegments = listOf(
            createSegment("seg-1", "video-123", 1),
            createSegment("seg-2", "video-123", 2)
        )
        coEvery {
            videoRepository.splitVideo(eq("video-123"), eq(file.absolutePath))
        } returns Ok(expectedSegments)

        val result = useCase(
            videoId = "video-123",
            inputPath = file.absolutePath
        )

        val segments = result.get()
        assertNotNull(segments)
        assertEquals(2, segments!!.size)
        assertEquals("seg-1", segments[0].id)
        assertEquals("seg-2", segments[1].id)

        coVerify { videoRepository.splitVideo("video-123", file.absolutePath) }
    }

    @Test
    fun `supported extensions are accepted`() = runTest {
        val supportedExtensions = listOf("mp4", "mkv", "webm", "avi", "mov", "3gp")

        for (ext in supportedExtensions) {
            val file = tempFolder.newFile("video_$ext.$ext")
            file.writeText("content")

            coEvery {
                videoRepository.splitVideo(any(), any())
            } returns Ok(listOf(createSegment("s1", "v1", 1)))

            val result = useCase(videoId = "v1", inputPath = file.absolutePath)
            assertTrue(
                "Extension .$ext should be accepted but was rejected",
                result.get() != null
            )
        }
    }

    // =========================================================================
    // Error Propagation
    // =========================================================================

    @Test
    fun `repository error is propagated`() = runTest {
        val file = tempFolder.newFile("video.mp4")
        file.writeText("content")

        val expectedError = AppError.ProcessingError(
            message = "Splitting failed internally"
        )
        coEvery {
            videoRepository.splitVideo(any(), any())
        } returns Err(expectedError)

        val result = useCase(videoId = "v1", inputPath = file.absolutePath)
        val error = result.getError()
        assertNotNull(error)
        assertEquals("Splitting failed internally", error!!.message)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun createSegment(
        id: String,
        videoId: String,
        partNumber: Int
    ) = VideoSegment(
        id = id,
        videoId = videoId,
        partNumber = partNumber,
        totalParts = 2,
        filePath = "/path/to/part_$partNumber.mp4",
        durationSeconds = 60,
        fileSizeBytes = 1024 * 1024,
        startTimeSeconds = (partNumber - 1).toLong() * 60,
        endTimeSeconds = partNumber.toLong() * 60
    )
}
