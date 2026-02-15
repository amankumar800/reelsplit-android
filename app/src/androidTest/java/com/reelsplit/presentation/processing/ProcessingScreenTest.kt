package com.reelsplit.presentation.processing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.presentation.theme.ReelSplitTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests for the ProcessingScreen composable.
 *
 * Since the top-level [ProcessingScreen] composable requires a Hilt ViewModel,
 * these tests verify the internal content composables indirectly by rendering
 * the full screen with a simplified approach — or if needed, each state's
 * content can be wrapped in a standalone composable.
 *
 * **Note:** As the sub-composables (QueuedContent, ExtractingContent, etc.)
 * are `private`, the tests verify text rendered on screen for each state.
 * We test using a "screen content" composable that exposes state-driven rendering.
 */
class ProcessingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // Queued State
    // =========================================================================

    @Test
    fun queuedState_showsPreparingText() {
        composeTestRule.setContent {
            ReelSplitTheme {
                // Render the pipeline indicator with Queued state
                // The pipeline shows: "Extract", "Download", "Split", "Done"
                ProcessingScreenTestContent(state = ProcessingUiState.Queued)
            }
        }

        composeTestRule.onNodeWithText("Preparing…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Setting up the processing pipeline").assertIsDisplayed()
    }

    // =========================================================================
    // Extracting State
    // =========================================================================

    @Test
    fun extractingState_showsExtractingText() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Extracting(
                        sourceUrl = "https://instagram.com/reel/test"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Extracting Video URL").assertIsDisplayed()
    }

    // =========================================================================
    // Downloading State
    // =========================================================================

    @Test
    fun downloadingState_showsDownloadingText() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Downloading(
                        percent = 42,
                        downloadedBytes = 5_000_000,
                        totalBytes = 12_000_000,
                        speedBytesPerSecond = 500_000
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Downloading").assertIsDisplayed()
    }

    // =========================================================================
    // Splitting State
    // =========================================================================

    @Test
    fun splittingState_showsSplittingTextWithPartInfo() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Splitting(
                        currentPart = 2,
                        totalParts = 3,
                        progressPercent = 50
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Splitting Video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Part 2 of 3").assertIsDisplayed()
    }

    // =========================================================================
    // Complete State
    // =========================================================================

    @Test
    fun completeState_showsCompleteText() {
        val segments = listOf(
            createSegment("seg-1", 1),
            createSegment("seg-2", 2)
        )
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Complete(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Complete!").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 segments ready").assertIsDisplayed()
    }

    // =========================================================================
    // Error State
    // =========================================================================

    @Test
    fun errorState_showsErrorMessageAndRetryButton() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Error(
                        message = "Network connection lost",
                        isRetryable = true,
                        failedAt = ProcessingStage.DOWNLOAD
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network connection lost").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun errorState_nonRetryable_hidesRetryButton() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Error(
                        message = "Unsupported format",
                        isRetryable = false,
                        failedAt = ProcessingStage.EXTRACTION
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Unsupported format").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun errorState_showsDismissButton() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Error(
                        message = "Something broke",
                        isRetryable = true,
                        failedAt = null
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Go Back").assertIsDisplayed()
    }

    // =========================================================================
    // Cancel Button
    // =========================================================================

    @Test
    fun activeState_showsCancelButton() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ProcessingScreenTestContent(
                    state = ProcessingUiState.Extracting(sourceUrl = "https://example.com"),
                    showCancel = true
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun createSegment(id: String, partNumber: Int) = VideoSegment(
        id = id,
        videoId = "test-video",
        partNumber = partNumber,
        totalParts = 2,
        filePath = "/path/to/part_$partNumber.mp4",
        durationSeconds = 60,
        fileSizeBytes = 1024 * 1024,
        startTimeSeconds = (partNumber - 1).toLong() * 60,
        endTimeSeconds = partNumber.toLong() * 60
    )
}
