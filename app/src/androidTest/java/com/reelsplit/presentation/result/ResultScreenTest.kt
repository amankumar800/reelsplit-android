package com.reelsplit.presentation.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.presentation.theme.ReelSplitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests for the ResultScreen composable.
 *
 * Tests verify the rendering of different UI states and button interactions
 * using a test-only content composable that doesn't require a ViewModel.
 */
class ResultScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // Loading State
    // =========================================================================

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(state = ResultUiState.Loading)
            }
        }

        composeTestRule.onNodeWithText("Loading segments…").assertIsDisplayed()
    }

    // =========================================================================
    // Loaded State
    // =========================================================================

    @Test
    fun loadedState_showsSegmentCount() {
        val segments = listOf(
            createSegment("seg-1", 1),
            createSegment("seg-2", 2),
            createSegment("seg-3", 3)
        )
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("3 segments ready").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsShareAllButton() {
        val segments = listOf(createSegment("seg-1", 1))
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Share All to Status").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsSaveToGalleryButton() {
        val segments = listOf(createSegment("seg-1", 1))
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Save to Gallery").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsDoneButton() {
        val segments = listOf(createSegment("seg-1", 1))
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun loadedState_showsSegmentCardWithPartNumber() {
        val segments = listOf(
            createSegment("seg-1", 1),
            createSegment("seg-2", 2)
        )
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Part 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Part 2").assertIsDisplayed()
    }

    // =========================================================================
    // Button Interactions
    // =========================================================================

    @Test
    fun doneButton_clickTriggersCallback() {
        var doneClicked = false
        val segments = listOf(createSegment("seg-1", 1))

        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    ),
                    onDone = { doneClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Done").performClick()
        assertTrue("Done callback should be invoked", doneClicked)
    }

    @Test
    fun shareAllButton_clickTriggersCallback() {
        var shareAllClicked = false
        val segments = listOf(createSegment("seg-1", 1))

        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    ),
                    onShareAll = { shareAllClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Share All to Status").performClick()
        assertTrue("Share All callback should be invoked", shareAllClicked)
    }

    @Test
    fun saveToGalleryButton_clickTriggersCallback() {
        var saveClicked = false
        val segments = listOf(createSegment("seg-1", 1))

        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video"
                    ),
                    onSaveToGallery = { saveClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Save to Gallery").performClick()
        assertTrue("Save to Gallery callback should be invoked", saveClicked)
    }

    // =========================================================================
    // Sharing-All State
    // =========================================================================

    @Test
    fun sharingAllState_showsSharingText() {
        val segments = listOf(createSegment("seg-1", 1))
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Loaded(
                        segments = segments,
                        videoId = "test-video",
                        isSharingAll = true
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Sharing…").assertIsDisplayed()
    }

    // =========================================================================
    // Error State
    // =========================================================================

    @Test
    fun errorState_showsErrorMessage() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Error(message = "No segments found")
                )
            }
        }

        composeTestRule.onNodeWithText("No segments found").assertIsDisplayed()
    }

    @Test
    fun errorState_showsGoHomeButton() {
        composeTestRule.setContent {
            ReelSplitTheme {
                ResultScreenTestContent(
                    state = ResultUiState.Error(message = "Something failed")
                )
            }
        }

        composeTestRule.onNodeWithText("Go Home").assertIsDisplayed()
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun createSegment(id: String, partNumber: Int) = VideoSegment(
        id = id,
        videoId = "test-video",
        partNumber = partNumber,
        totalParts = 3,
        filePath = "/path/to/part_$partNumber.mp4",
        durationSeconds = 60,
        fileSizeBytes = 1024 * 1024,
        startTimeSeconds = (partNumber - 1).toLong() * 60,
        endTimeSeconds = partNumber.toLong() * 60
    )
}
