package com.reelsplit.presentation.processing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.DownloadProgress
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.usecase.DownloadVideoUseCase
import com.reelsplit.domain.usecase.ExtractVideoUrlUseCase
import com.reelsplit.domain.usecase.SplitVideoUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProcessingViewModel].
 *
 * Verifies the extract → download → split pipeline state transitions,
 * user actions (cancel, retry, dismiss), and exception handling.
 *
 * Uses [UnconfinedTestDispatcher] so that coroutines launched in
 * `viewModelScope` execute eagerly, matching the pattern established
 * in [ResultViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessingViewModelTest {

    private lateinit var extractVideoUrlUseCase: ExtractVideoUrlUseCase
    private lateinit var downloadVideoUseCase: DownloadVideoUseCase
    private lateinit var splitVideoUseCase: SplitVideoUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUrl = "https://www.instagram.com/reel/ABC123/"
    private val directVideoUrl = "https://cdn.instagram.com/video/abc.mp4"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        extractVideoUrlUseCase = mockk()
        downloadVideoUseCase = mockk(relaxed = true)
        splitVideoUseCase = mockk()
        savedStateHandle = SavedStateHandle(mapOf("url" to testUrl))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // =========================================================================
    // Extraction Stage
    // =========================================================================

    @Test
    fun `on init calls extractVideoUrlUseCase with url from SavedStateHandle`() = runTest {
        // Arrange: extraction succeeds but download returns empty flow
        // to stop the pipeline after extraction stage
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf()

        val viewModel = createViewModel()

        // With UnconfinedTestDispatcher, the pipeline runs eagerly during init.
        // The Extracting state is transient and already consumed, but we can
        // verify the use case was called with the correct URL and the state
        // is no longer Queued (proving the pipeline started).
        coVerify(exactly = 1) { extractVideoUrlUseCase(testUrl) }
        assertFalse(
            "State should have advanced past Queued",
            viewModel.uiState.value is ProcessingUiState.Queued
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `init throws when SavedStateHandle has no url`() {
        // SavedStateHandle without the required "url" key
        val emptyHandle = SavedStateHandle()
        ProcessingViewModel(
            extractVideoUrlUseCase = extractVideoUrlUseCase,
            downloadVideoUseCase = downloadVideoUseCase,
            splitVideoUseCase = splitVideoUseCase,
            savedStateHandle = emptyHandle
        )
    }

    @Test
    fun `extraction failure transitions to Error state with EXTRACTION stage`() = runTest {
        val error = AppError.NetworkError(
            message = "Connection timed out",
            isRetryable = true
        )
        coEvery { extractVideoUrlUseCase(testUrl) } returns Err(error)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("Connection timed out", errorState.message)
        assertTrue(errorState.isRetryable)
        assertEquals(ProcessingStage.EXTRACTION, errorState.failedAt)
    }

    @Test
    fun `extraction failure with InvalidUrlError is not retryable`() = runTest {
        val error = AppError.InvalidUrlError(
            message = "Instagram URL cannot be empty",
            url = "",
            isRetryable = false
        )
        coEvery { extractVideoUrlUseCase(testUrl) } returns Err(error)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as ProcessingUiState.Error
        assertFalse(state.isRetryable)
        assertEquals(ProcessingStage.EXTRACTION, state.failedAt)
    }

    // =========================================================================
    // Download Stage
    // =========================================================================

    @Test
    fun `successful extraction transitions to Downloading state`() = runTest {
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(DownloadProgress.Queued)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Expected Downloading state but got $state",
            state is ProcessingUiState.Downloading
        )
    }

    @Test
    fun `download progress updates Downloading state`() = runTest {
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(
            DownloadProgress.Downloading(
                percent = 42,
                downloadedBytes = 1024,
                totalBytes = 2048,
                speedBytesPerSecond = 512
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProcessingUiState.Downloading)
        val downloading = state as ProcessingUiState.Downloading
        assertEquals(42, downloading.percent)
        assertEquals(1024L, downloading.downloadedBytes)
        assertEquals(2048L, downloading.totalBytes)
        assertEquals(512L, downloading.speedBytesPerSecond)
    }

    @Test
    fun `download failure transitions to Error state with DOWNLOAD stage`() = runTest {
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(
            DownloadProgress.Failed(
                message = "Network timeout",
                isRetryable = true
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("Network timeout", errorState.message)
        assertTrue(errorState.isRetryable)
        assertEquals(ProcessingStage.DOWNLOAD, errorState.failedAt)
    }

    @Test
    fun `download cancellation transitions to Error state with retryable`() = runTest {
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(DownloadProgress.Cancelled)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("Download cancelled", errorState.message)
        assertTrue(errorState.isRetryable)
        assertEquals(ProcessingStage.DOWNLOAD, errorState.failedAt)
    }

    // =========================================================================
    // Split Stage
    // =========================================================================

    @Test
    fun `successful download transitions to Splitting then Complete`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))

        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(
            DownloadProgress.Completed(
                filePath = "/cache/video.mp4",
                fileSizeBytes = 1024
            )
        )
        coEvery {
            splitVideoUseCase(videoId = any(), inputPath = "/cache/video.mp4")
        } returns Ok(segments)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Complete state but got $state", state is ProcessingUiState.Complete)
        val complete = state as ProcessingUiState.Complete
        assertEquals(1, complete.segments.size)
    }

    @Test
    fun `successful pipeline emits NavigateToResult event`() = runTest {
        // NOTE: With UnconfinedTestDispatcher set as Main, the entire pipeline
        // runs eagerly during the ViewModel's init block. SharedFlow(replay=0)
        // drops events when there are no subscribers. We work around this by:
        // 1. Letting init FAIL (no event emitted)
        // 2. Subscribing to events via Turbine
        // 3. Calling onRetry() to re-run the pipeline while subscribed

        // Step 1: Let init fail so no event is emitted
        coEvery {
            extractVideoUrlUseCase(testUrl)
        } returns Err(AppError.NetworkError(message = "fail"))

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value is ProcessingUiState.Error)

        // Step 2: Set up mocks for successful retry
        val segments = listOf(createSegment("seg-1", 1))
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(
            DownloadProgress.Completed(
                filePath = "/cache/video.mp4",
                fileSizeBytes = 1024
            )
        )
        coEvery {
            splitVideoUseCase(videoId = any(), inputPath = any())
        } returns Ok(segments)

        // Step 3: Subscribe BEFORE triggering retry
        viewModel.events.test {
            viewModel.onRetry()
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(
                "Expected NavigateToResult but got $event",
                event is ProcessingEvent.NavigateToResult
            )
        }
    }

    @Test
    fun `split failure transitions to Error state with SPLITTING stage`() = runTest {
        val error = AppError.ProcessingError(
            message = "Codec error during split",
            isRetryable = false
        )

        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf(
            DownloadProgress.Completed(
                filePath = "/cache/video.mp4",
                fileSizeBytes = 1024
            )
        )
        coEvery {
            splitVideoUseCase(videoId = any(), inputPath = any())
        } returns Err(error)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("Codec error during split", errorState.message)
        assertFalse(errorState.isRetryable)
        assertEquals(ProcessingStage.SPLITTING, errorState.failedAt)
    }

    // =========================================================================
    // User Actions
    // =========================================================================

    @Test
    fun `onCancel emits NavigateBack event`() = runTest {
        // Arrange: extraction blocks so we can cancel mid-flight
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf()

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onCancel()
            val event = awaitItem()
            assertTrue(
                "Expected NavigateBack but got $event",
                event is ProcessingEvent.NavigateBack
            )
        }
    }

    @Test
    fun `onCancel calls downloadVideoUseCase cancel`() = runTest {
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf()

        val viewModel = createViewModel()
        viewModel.onCancel()

        verify { downloadVideoUseCase.cancel(any()) }
    }

    @Test
    fun `onDismissError emits NavigateBack event`() = runTest {
        val error = AppError.NetworkError(message = "fail")
        coEvery { extractVideoUrlUseCase(testUrl) } returns Err(error)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onDismissError()
            val event = awaitItem()
            assertTrue(event is ProcessingEvent.NavigateBack)
        }
    }

    @Test
    fun `onRetry restarts processing pipeline`() = runTest {
        // First invocation fails
        coEvery {
            extractVideoUrlUseCase(testUrl)
        } returns Err(AppError.NetworkError(message = "fail"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Should be in Error state now
        assertTrue(viewModel.uiState.value is ProcessingUiState.Error)

        // Second invocation succeeds without download
        coEvery { extractVideoUrlUseCase(testUrl) } returns Ok(directVideoUrl)
        every {
            downloadVideoUseCase(any(), any(), any())
        } returns flowOf()

        viewModel.onRetry()
        advanceUntilIdle()

        // Extraction was called twice (init + retry)
        coVerify(exactly = 2) { extractVideoUrlUseCase(testUrl) }
    }

    // =========================================================================
    // Exception Handling
    // =========================================================================

    @Test
    fun `handleException transitions to Error state with retryable flag`() = runTest {
        // Arrange: extraction throws uncaught exception
        coEvery {
            extractVideoUrlUseCase(testUrl)
        } throws RuntimeException("Unexpected crash")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("Unexpected crash", errorState.message)
        assertTrue(errorState.isRetryable)
        assertNull(errorState.failedAt)
    }

    @Test
    fun `handleException with null message uses fallback text`() = runTest {
        // RuntimeException() without a message → throwable.message is null
        coEvery {
            extractVideoUrlUseCase(testUrl)
        } throws RuntimeException()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProcessingUiState.Error)
        val errorState = state as ProcessingUiState.Error
        assertEquals("An unexpected error occurred", errorState.message)
        assertTrue(errorState.isRetryable)
        assertNull(errorState.failedAt)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun createViewModel() = ProcessingViewModel(
        extractVideoUrlUseCase = extractVideoUrlUseCase,
        downloadVideoUseCase = downloadVideoUseCase,
        splitVideoUseCase = splitVideoUseCase,
        savedStateHandle = savedStateHandle
    )

    private fun createSegment(id: String, partNumber: Int) = VideoSegment(
        id = id,
        videoId = "test-video",
        partNumber = partNumber,
        totalParts = 1,
        filePath = "/path/to/part_$partNumber.mp4",
        durationSeconds = 60,
        fileSizeBytes = 1024 * 1024,
        startTimeSeconds = 0L,
        endTimeSeconds = 60L,
        isShared = false
    )
}
