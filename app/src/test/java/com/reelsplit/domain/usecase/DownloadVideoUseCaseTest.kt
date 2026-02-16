package com.reelsplit.domain.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Data
import app.cash.turbine.test
import com.reelsplit.data.worker.VideoDownloadWorker
import com.reelsplit.domain.model.DownloadProgress
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [DownloadVideoUseCase].
 *
 * Verifies:
 * - Input validation (blank URL, blank fileName)
 * - WorkManager enqueuing with correct constraints
 * - WorkInfo-to-DownloadProgress state mapping
 * - Cancel delegation to WorkManager
 * - Unique work name generation
 */
class DownloadVideoUseCaseTest {

    private lateinit var workManager: WorkManager
    private lateinit var useCase: DownloadVideoUseCase

    @Before
    fun setUp() {
        workManager = mockk(relaxed = true)
        useCase = DownloadVideoUseCase(workManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Input Validation
    // =========================================================================

    @Test
    fun `invoke with blank videoUrl returns Failed immediately`() = runTest {
        useCase(videoUrl = "", fileName = "test.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Failed)
            val failed = result as DownloadProgress.Failed
            assertEquals("Video URL cannot be empty", failed.message)
            assertFalse(failed.isRetryable)
            awaitComplete()
        }
    }

    @Test
    fun `invoke with blank fileName returns Failed immediately`() = runTest {
        useCase(videoUrl = "https://example.com/video.mp4", fileName = "").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Failed)
            val failed = result as DownloadProgress.Failed
            assertEquals("File name cannot be empty", failed.message)
            assertFalse(failed.isRetryable)
            awaitComplete()
        }
    }

    @Test
    fun `invoke with whitespace-only videoUrl returns Failed`() = runTest {
        useCase(videoUrl = "   ", fileName = "test.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Failed)
            awaitComplete()
        }
    }

    // =========================================================================
    // WorkManager Enqueue
    // =========================================================================

    @Test
    fun `invoke with valid inputs enqueues unique work`() = runTest {
        // Arrange: Return an empty flow so we don't block
        val workInfoFlow = MutableStateFlow<WorkInfo?>(null)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns workInfoFlow

        // Act
        useCase(
            videoUrl = "https://example.com/video.mp4",
            fileName = "test.mp4",
            downloadId = "my-download-id"
        )

        // Assert: enqueueUniqueWork was called with the correct parameters
        verify {
            workManager.enqueueUniqueWork(
                eq("my-download-id"),
                eq(ExistingWorkPolicy.REPLACE),
                any<OneTimeWorkRequest>()
            )
        }
    }

    @Test
    fun `invoke without downloadId uses fileName-based unique work name`() = runTest {
        val workInfoFlow = MutableStateFlow<WorkInfo?>(null)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns workInfoFlow

        useCase(videoUrl = "https://example.com/video.mp4", fileName = "test.mp4")

        verify {
            workManager.enqueueUniqueWork(
                eq("download_test.mp4"),
                eq(ExistingWorkPolicy.REPLACE),
                any<OneTimeWorkRequest>()
            )
        }
    }

    // =========================================================================
    // WorkInfo to DownloadProgress Mapping
    // =========================================================================

    @Test
    fun `null WorkInfo maps to Queued`() = runTest {
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(null)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            val result = awaitItem()
            assertEquals(DownloadProgress.Queued, result)
            awaitComplete()
        }
    }

    @Test
    fun `ENQUEUED state maps to Queued`() = runTest {
        val workInfo = createWorkInfo(WorkInfo.State.ENQUEUED)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            assertEquals(DownloadProgress.Queued, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `RUNNING state maps to Downloading with progress`() = runTest {
        val progressData = Data.Builder()
            .putInt(VideoDownloadWorker.KEY_PROGRESS, 42)
            .build()
        val workInfo = createWorkInfo(WorkInfo.State.RUNNING, progress = progressData)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Downloading)
            assertEquals(42, (result as DownloadProgress.Downloading).percent)
            awaitComplete()
        }
    }

    @Test
    fun `SUCCEEDED state maps to Completed with filePath`() = runTest {
        val outputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_FILE_PATH, "/data/cached/video.mp4")
            .putLong(VideoDownloadWorker.KEY_FILE_SIZE, 12345L)
            .build()
        val workInfo = createWorkInfo(WorkInfo.State.SUCCEEDED, output = outputData)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Completed)
            val completed = result as DownloadProgress.Completed
            assertEquals("/data/cached/video.mp4", completed.filePath)
            assertEquals(12345L, completed.fileSizeBytes)
            awaitComplete()
        }
    }

    @Test
    fun `SUCCEEDED with missing filePath maps to Failed`() = runTest {
        val outputData = Data.Builder().build() // no file path
        val workInfo = createWorkInfo(WorkInfo.State.SUCCEEDED, output = outputData)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Failed)
            awaitComplete()
        }
    }

    @Test
    fun `FAILED state maps to Failed with error message`() = runTest {
        val outputData = Data.Builder()
            .putString(VideoDownloadWorker.KEY_ERROR_MESSAGE, "Network timeout")
            .putBoolean(VideoDownloadWorker.KEY_ERROR_RETRYABLE, true)
            .build()
        val workInfo = createWorkInfo(WorkInfo.State.FAILED, output = outputData)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            val result = awaitItem()
            assertTrue(result is DownloadProgress.Failed)
            val failed = result as DownloadProgress.Failed
            assertEquals("Network timeout", failed.message)
            assertTrue(failed.isRetryable)
            awaitComplete()
        }
    }

    @Test
    fun `CANCELLED state maps to Cancelled`() = runTest {
        val workInfo = createWorkInfo(WorkInfo.State.CANCELLED)
        every { workManager.getWorkInfoByIdFlow(any<UUID>()) } returns flowOf(workInfo)

        useCase(videoUrl = "https://example.com/v.mp4", fileName = "v.mp4").test {
            assertEquals(DownloadProgress.Cancelled, awaitItem())
            awaitComplete()
        }
    }

    // =========================================================================
    // Cancel
    // =========================================================================

    @Test
    fun `cancel delegates to WorkManager cancelUniqueWork`() {
        useCase.cancel("download-123")
        verify { workManager.cancelUniqueWork("download-123") }
    }

    @Test
    fun `cancelByFileName uses generated work name`() {
        useCase.cancelByFileName("video.mp4")
        verify { workManager.cancelUniqueWork("download_video.mp4") }
    }

    @Test
    fun `cancelAll delegates to WorkManager cancelAllWorkByTag`() {
        useCase.cancelAll()
        verify { workManager.cancelAllWorkByTag(any()) }
    }

    // =========================================================================
    // Unique Work Name
    // =========================================================================

    @Test
    fun `getUniqueWorkName returns deterministic name`() {
        val name1 = useCase.getUniqueWorkName("test.mp4")
        val name2 = useCase.getUniqueWorkName("test.mp4")
        assertEquals(name1, name2)
        assertEquals("download_test.mp4", name1)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Creates a mocked [WorkInfo] with the given state, progress, and output data.
     */
    private fun createWorkInfo(
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
        output: Data = Data.EMPTY
    ): WorkInfo {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns state
        every { workInfo.progress } returns progress
        every { workInfo.outputData } returns output
        return workInfo
    }
}
