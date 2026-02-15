package com.reelsplit.presentation.result

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.reelsplit.core.base.UiEvent
import com.reelsplit.domain.model.AppError
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.domain.repository.VideoRepository
import com.reelsplit.domain.sharing.WhatsAppSharerContract
import com.reelsplit.domain.usecase.ShareToWhatsAppUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * Unit tests for [ResultViewModel].
 *
 * Verifies:
 * - Segment loading and state transitions (Loading → Loaded, Loading → Error)
 * - Share to Status (single segment)
 * - Share to Chat (single segment)
 * - Share All to Status (bulk)
 * - Navigate Home event emission
 * - In-app review request (once per session)
 * - Exception handling → Error state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private lateinit var videoRepository: VideoRepository
    private lateinit var shareToWhatsAppUseCase: ShareToWhatsAppUseCase
    private lateinit var whatsAppSharer: WhatsAppSharerContract
    private lateinit var applicationContext: Context
    private lateinit var savedStateHandle: SavedStateHandle

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        videoRepository = mockk(relaxed = true)
        shareToWhatsAppUseCase = mockk()
        whatsAppSharer = mockk()
        applicationContext = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("videoId" to "test-video-id"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // =========================================================================
    // Segment Loading
    // =========================================================================

    @Test
    fun `on init loads segments and transitions to Loaded state`() = runTest {
        val segments = listOf(
            createSegment("seg-1", 1),
            createSegment("seg-2", 2)
        )
        every {
            videoRepository.getSegmentsByVideoId("test-video-id")
        } returns flowOf(segments)

        val viewModel = createViewModel()

        // Wait for the state to settle
        val state = viewModel.uiState.value
        assertTrue("Expected Loaded state but got $state", state is ResultUiState.Loaded)
        val loaded = state as ResultUiState.Loaded
        assertEquals(2, loaded.segments.size)
        assertEquals("test-video-id", loaded.videoId)
    }

    @Test
    fun `on init with empty segments transitions to Error state`() = runTest {
        every {
            videoRepository.getSegmentsByVideoId("test-video-id")
        } returns flowOf(emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but got $state", state is ResultUiState.Error)
    }

    // =========================================================================
    // Share to Status
    // =========================================================================

    @Test
    fun `onShareToStatus calls use case and marks segment shared on success`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()
        viewModel.onShareToStatus("seg-1")
        advanceUntilIdle()

        verify { shareToWhatsAppUseCase("/path/to/part_1.mp4") }
        coVerify { videoRepository.markSegmentAsShared("seg-1") }
    }

    @Test
    fun `onShareToStatus emits ShowShareError on failure`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every {
            shareToWhatsAppUseCase(any())
        } returns Err(AppError.ProcessingError(message = "WhatsApp not installed"))

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onShareToStatus("seg-1")
            val event = awaitItem()
            assertTrue("Expected ShowShareError but got $event", event is ResultEvent.ShowShareError)
            assertEquals("WhatsApp not installed", (event as ResultEvent.ShowShareError).message)
        }
    }

    @Test
    fun `onShareToStatus with invalid segmentId does nothing`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)

        val viewModel = createViewModel()
        viewModel.onShareToStatus("non-existent-id")
        advanceUntilIdle()

        verify(exactly = 0) { shareToWhatsAppUseCase(any()) }
    }

    // =========================================================================
    // Share to Chat
    // =========================================================================

    @Test
    fun `onShareToChat calls whatsAppSharer and marks segment shared`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { whatsAppSharer.shareToWhatsAppChat(any()) } just Runs
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()
        viewModel.onShareToChat("seg-1")
        advanceUntilIdle()

        verify { whatsAppSharer.shareToWhatsAppChat("/path/to/part_1.mp4") }
        coVerify { videoRepository.markSegmentAsShared("seg-1") }
    }

    @Test
    fun `onShareToChat emits error when WhatsApp throws`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { whatsAppSharer.shareToWhatsAppChat(any()) } throws RuntimeException("Share failed")

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onShareToChat("seg-1")
            val event = awaitItem()
            assertTrue(event is ResultEvent.ShowShareError)
            assertEquals("Share failed", (event as ResultEvent.ShowShareError).message)
        }
    }

    // =========================================================================
    // Share All to Status
    // =========================================================================

    @Test
    fun `onShareAllToStatus shares all valid unshared segments`() = runTest {
        val segments = listOf(
            createSegment("seg-1", 1, isShared = false),
            createSegment("seg-2", 2, isShared = false)
        )
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()
        viewModel.onShareAllToStatus()
        advanceUntilIdle()

        verify(exactly = 2) { shareToWhatsAppUseCase(any()) }
        coVerify(exactly = 2) { videoRepository.markSegmentAsShared(any()) }
    }

    @Test
    fun `onShareAllToStatus skips already-shared segments`() = runTest {
        val segments = listOf(
            createSegment("seg-1", 1, isShared = true),  // already shared
            createSegment("seg-2", 2, isShared = false)
        )
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()
        viewModel.onShareAllToStatus()
        advanceUntilIdle()

        // Only seg-2 should be shared
        verify(exactly = 1) { shareToWhatsAppUseCase(any()) }
    }

    @Test
    fun `onShareAllToStatus sets isSharingAll flag during operation`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()
        viewModel.onShareAllToStatus()
        advanceUntilIdle()

        // After completion, isSharingAll should be reset to false
        val state = viewModel.uiState.value as? ResultUiState.Loaded
        assertNotNull(state)
        assertFalse(state!!.isSharingAll)
    }

    // =========================================================================
    // Navigate Home
    // =========================================================================

    @Test
    fun `onNavigateHome emits NavigateHome event`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onNavigateHome()
            val event = awaitItem()
            assertTrue(event is ResultEvent.NavigateHome)
        }
    }

    // =========================================================================
    // In-App Review
    // =========================================================================

    @Test
    fun `first successful share triggers in-app review`() = runTest {
        val segments = listOf(createSegment("seg-1", 1))
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onShareToStatus("seg-1")
            val event = awaitItem()
            assertTrue(
                "Expected RequestInAppReview but got $event",
                event is ResultEvent.RequestInAppReview
            )
        }
    }

    @Test
    fun `second successful share does not trigger in-app review again`() = runTest {
        val segments = listOf(
            createSegment("seg-1", 1),
            createSegment("seg-2", 2)
        )
        every { videoRepository.getSegmentsByVideoId(any()) } returns flowOf(segments)
        every { shareToWhatsAppUseCase(any()) } returns Ok(Unit)
        coEvery { videoRepository.markSegmentAsShared(any()) } returns Ok(Unit)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onShareToStatus("seg-1")
            val event1 = awaitItem()
            assertTrue(event1 is ResultEvent.RequestInAppReview)

            viewModel.onShareToStatus("seg-2")
            // No second RequestInAppReview should be emitted
            expectNoEvents()
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun createViewModel() = ResultViewModel(
        videoRepository = videoRepository,
        shareToWhatsAppUseCase = shareToWhatsAppUseCase,
        whatsAppSharer = whatsAppSharer,
        applicationContext = applicationContext,
        savedStateHandle = savedStateHandle
    )

    private fun createSegment(
        id: String,
        partNumber: Int,
        isShared: Boolean = false
    ) = VideoSegment(
        id = id,
        videoId = "test-video-id",
        partNumber = partNumber,
        totalParts = 2,
        filePath = "/path/to/part_$partNumber.mp4",
        durationSeconds = 60,
        fileSizeBytes = 1024 * 1024,
        startTimeSeconds = (partNumber - 1).toLong() * 60,
        endTimeSeconds = partNumber.toLong() * 60,
        isShared = isShared
    )
}
