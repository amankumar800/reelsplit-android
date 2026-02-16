package com.reelsplit.domain.usecase

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.repository.VideoRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ExtractVideoUrlUseCase].
 *
 * Verifies:
 * - Input validation (blank URL returns [AppError.InvalidUrlError] without calling repo)
 * - Delegation to [VideoRepository.extractVideoUrl] for valid input
 * - Correct propagation of repository success / failure results
 */
class ExtractVideoUrlUseCaseTest {

    private lateinit var videoRepository: VideoRepository
    private lateinit var useCase: ExtractVideoUrlUseCase

    @Before
    fun setUp() {
        videoRepository = mockk()
        useCase = ExtractVideoUrlUseCase(videoRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Input Validation
    // =========================================================================

    @Test
    fun `invoke with blank URL returns InvalidUrlError`() = runTest {
        val result = useCase("")

        val error = result.getError()
        assertTrue(error is AppError.InvalidUrlError)
        assertEquals("Instagram URL cannot be empty", error!!.message)
        assertEquals("", (error as AppError.InvalidUrlError).url)
    }

    @Test
    fun `invoke with whitespace-only URL returns InvalidUrlError`() = runTest {
        val result = useCase("   ")

        val error = result.getError()
        assertTrue(error is AppError.InvalidUrlError)
        // Verify the url field preserves the original input
        assertEquals("   ", (error as AppError.InvalidUrlError).url)
    }

    @Test
    fun `invoke with blank URL does not call repository`() = runTest {
        useCase("")

        coVerify(exactly = 0) { videoRepository.extractVideoUrl(any()) }
    }

    // =========================================================================
    // Successful Extraction
    // =========================================================================

    @Test
    fun `invoke with valid URL delegates to repository`() = runTest {
        val instagramUrl = "https://www.instagram.com/reel/ABC123/"
        val directUrl = "https://cdn.instagram.com/video/abc.mp4"
        coEvery { videoRepository.extractVideoUrl(instagramUrl) } returns Ok(directUrl)

        val result = useCase(instagramUrl)

        assertEquals(directUrl, result.get())
        coVerify(exactly = 1) { videoRepository.extractVideoUrl(instagramUrl) }
    }

    // =========================================================================
    // Error Propagation
    // =========================================================================

    @Test
    fun `invoke propagates NetworkError from repository`() = runTest {
        val instagramUrl = "https://www.instagram.com/reel/ABC123/"
        val networkError = AppError.NetworkError(
            message = "Connection timed out",
            statusCode = 504
        )
        coEvery { videoRepository.extractVideoUrl(instagramUrl) } returns Err(networkError)

        val result = useCase(instagramUrl)

        assertTrue(result.getError() is AppError.NetworkError)
        assertEquals("Connection timed out", result.getError()!!.message)
    }

    @Test
    fun `invoke propagates ProcessingError from repository`() = runTest {
        val instagramUrl = "https://www.instagram.com/reel/ABC123/"
        val processingError = AppError.ProcessingError(
            message = "Rate limited, try again later"
        )
        coEvery { videoRepository.extractVideoUrl(instagramUrl) } returns Err(processingError)

        val result = useCase(instagramUrl)

        assertTrue(result.getError() is AppError.ProcessingError)
        assertEquals("Rate limited, try again later", result.getError()!!.message)
    }

    @Test
    fun `invoke preserves isRetryable from repository error`() = runTest {
        val instagramUrl = "https://www.instagram.com/reel/X/"
        val retryableError = AppError.NetworkError(
            message = "Timeout",
            isRetryable = true
        )
        coEvery { videoRepository.extractVideoUrl(instagramUrl) } returns Err(retryableError)

        val result = useCase(instagramUrl)

        assertTrue(result.getError()!!.isRetryable)
    }

    @Test
    fun `invoke with blank URL returns non-retryable error`() = runTest {
        val result = useCase("")

        assertFalse(result.getError()!!.isRetryable)
    }
}
