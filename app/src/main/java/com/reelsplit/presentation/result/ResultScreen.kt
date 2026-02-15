package com.reelsplit.presentation.result

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.reelsplit.presentation.components.IndeterminateCircularProgress
import com.reelsplit.presentation.components.VideoSegmentCard
import com.reelsplit.presentation.theme.GradientEnd
import com.reelsplit.presentation.theme.GradientMiddle
import com.reelsplit.presentation.theme.GradientStart
import com.reelsplit.presentation.theme.StatusCompleted

// =============================================================================
// Pre-computed gradient brush
// =============================================================================

private val HeaderGradientBrush = Brush.linearGradient(
    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
)

// =============================================================================
// ResultScreen — Destination
// =============================================================================

/**
 * ResultScreen displays the split video segments with sharing actions.
 *
 * Displays:
 * - A compact gradient header with "Results" title
 * - A summary row showing segment count and total duration
 * - A scrollable list of [VideoSegmentCard]s with individual share buttons
 * - A sticky bottom action bar with "Share All", "Save to Gallery", and "Done"
 *
 * The `videoId` navigation argument is read by [ResultViewModel] via
 * [SavedStateHandle], so it does not appear as an explicit parameter here.
 *
 * @param viewModel Injected [ResultViewModel] instance.
 */
@Destination(navArgsDelegate = ResultNavArgs::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back presses → navigate home
    BackHandler {
        viewModel.onNavigateHome()
    }

    // Handle one-time events — use `collect` (not `collectLatest`) so that
    // suspending snackbar calls aren't cancelled by the next event.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultEvent.NavigateHome -> {
                    // TODO: navigator.navigate(HomeScreenDestination) {
                    //     popUpTo(HomeScreenDestination) { inclusive = true }
                    // }
                    // Will be wired when navigation is integrated
                }
                is ResultEvent.ShowShareError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is ResultEvent.ShowSavedToGallery -> {
                    val message = when {
                        event.failedCount == 0 ->
                            "${event.savedCount} segment${if (event.savedCount != 1) "s" else ""} saved to Gallery"
                        event.savedCount == 0 ->
                            "Failed to save segments to Gallery"
                        else ->
                            "${event.savedCount} saved, ${event.failedCount} failed"
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
                is ResultEvent.RequestInAppReview -> {
                    // TODO: Trigger Google Play In-App Review API
                    // val manager = ReviewManagerFactory.create(context)
                    // Will be wired when review integration is added
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // ─── Header ─────────────────────────────────────────────
                CompactHeader()

                // ─── Main Content ───────────────────────────────────────
                when (val state = uiState) {
                    is ResultUiState.Loading -> {
                        LoadingContent(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }

                    is ResultUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onNavigateHome = viewModel::onNavigateHome,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }

                    is ResultUiState.Loaded -> {
                        // ─── Segment List ───────────────────────────────
                        SegmentList(
                            state = state,
                            onShareToStatus = viewModel::onShareToStatus,
                            onShareToChat = viewModel::onShareToChat,
                            modifier = Modifier.weight(1f)
                        )

                        // ─── Bottom Action Bar ──────────────────────────
                        BottomActionBar(
                            state = state,
                            onShareAll = viewModel::onShareAllToStatus,
                            onSaveToGallery = viewModel::onSaveToGallery,
                            onDone = viewModel::onNavigateHome
                        )
                    }
                }
            }

            // Snackbar host overlay
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 180.dp) // Above the bottom action bar
            )
        }
    }
}

// =============================================================================
// Compact Header
// =============================================================================

/**
 * Slim gradient header with the title "Results".
 * Extends behind the status bar for edge-to-edge feel.
 */
@Composable
private fun CompactHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = HeaderGradientBrush)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Results",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// =============================================================================
// Summary Row
// =============================================================================

/**
 * Displays the segment count, total duration, and shared status.
 */
@Composable
private fun SummaryRow(
    state: ResultUiState.Loaded,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${state.totalSegments} segment${if (state.totalSegments != 1) "s" else ""} ready",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total duration: ${state.formattedTotalDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // All-shared badge
            AnimatedVisibility(
                visible = state.allShared,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                ),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = StatusCompleted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "All shared",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusCompleted
                    )
                }
            }
        }
    }
}

// =============================================================================
// Segment List
// =============================================================================

/**
 * Scrollable list of video segment cards.
 */
@Composable
private fun SegmentList(
    state: ResultUiState.Loaded,
    onShareToStatus: (String) -> Unit,
    onShareToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary row as first item
        item(key = "summary") {
            SummaryRow(
                state = state,
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 16.dp,
                    bottom = 4.dp
                )
            )
        }

        // Segment cards
        itemsIndexed(
            items = state.segments,
            key = { _, segment -> segment.id }
        ) { _, segment ->
            VideoSegmentCard(
                segment = segment,
                onShareToStatus = { onShareToStatus(segment.id) },
                onShareToChat = { onShareToChat(segment.id) },
                enabled = !state.isSharingAll
            )
        }
    }
}

// =============================================================================
// Bottom Action Bar
// =============================================================================

/**
 * Sticky bottom action bar with Share All, Save to Gallery, and Done actions.
 */
@Composable
private fun BottomActionBar(
    state: ResultUiState.Loaded,
    onShareAll: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDone: () -> Unit
) {
    val hasUnsharableSegments = state.validSegmentCount < state.totalSegments
    val allShared = state.allShared

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Share All to Status — primary CTA
            Button(
                onClick = onShareAll,
                enabled = !state.isSharingAll && !allShared && state.validSegmentCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isSharingAll) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        state.isSharingAll -> "Sharing…"
                        allShared -> "All Shared ✓"
                        else -> "Share All to Status"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Secondary actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save to Gallery
                OutlinedButton(
                    onClick = onSaveToGallery,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isSaving) "Saving…" else "Save to Gallery",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Done
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Warning for invalid segments
            AnimatedVisibility(visible = hasUnsharableSegments) {
                Text(
                    text = "${state.totalSegments - state.validSegmentCount} segment${if (state.totalSegments - state.validSegmentCount != 1) "s" else ""} exceed WhatsApp limits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// Loading Content
// =============================================================================

/**
 * Full-screen loading indicator while segments are being fetched.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IndeterminateCircularProgress(size = 64.dp)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Loading segments…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// Error Content
// =============================================================================

/**
 * Error state with message and back-to-home action.
 */
@Composable
private fun ErrorContent(
    message: String,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Home",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
