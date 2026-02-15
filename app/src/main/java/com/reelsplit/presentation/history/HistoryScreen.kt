package com.reelsplit.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.annotation.Destination
import com.reelsplit.domain.model.Video
import com.reelsplit.domain.model.VideoStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// HistoryScreen
// =============================================================================

/**
 * HistoryScreen displays previously processed videos with Paging 3 pagination.
 *
 * Supports:
 * - Paginated video list with automatic next-page loading
 * - Loading indicator during initial load
 * - Error state with retry action for both initial and append loads
 * - Empty state with user-friendly instructions
 * - Tapping a video navigates to the result screen
 *
 * @param viewModel Injected [HistoryViewModel] instance.
 */
@Destination
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.pagingFlow.collectAsLazyPagingItems()

    // Staggered entrance animation
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100L)
        showContent = true
    }

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HistoryEvent.NavigateToResult -> {
                    // TODO: navigator.navigate(ResultScreenDestination(videoId = event.videoId))
                    // Will be wired when navigation is connected
                }
                is HistoryEvent.NavigateBack -> {
                    // TODO: navigator.navigateUp()
                    // Will be wired when navigation is connected
                }
                else -> { /* Ignore unknown events */ }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────
            HistoryTopBar(
                onBackClick = viewModel::onNavigateBack,
                visible = showContent
            )

            // ── Content ──────────────────────────────────────────────
            HistoryContent(
                pagingItems = pagingItems,
                onVideoClick = viewModel::onVideoClick
            )
        }
    }
}

// =============================================================================
// Top Bar
// =============================================================================

/**
 * Top bar with back navigation and "History" title.
 */
@Composable
private fun HistoryTopBar(
    onBackClick: () -> Unit,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// =============================================================================
// Content (Paging States)
// =============================================================================

/**
 * Main content area that handles paging load states.
 * Renders loading, error, empty, or the video list based on paging state.
 */
@Composable
private fun HistoryContent(
    pagingItems: LazyPagingItems<Video>,
    onVideoClick: (String) -> Unit
) {
    val refreshState = pagingItems.loadState.refresh

    when {
        // ── Initial Loading ──────────────────────────────────────
        refreshState is LoadState.Loading -> {
            LoadingState()
        }

        // ── Initial Error ────────────────────────────────────────
        refreshState is LoadState.Error -> {
            ErrorState(
                message = refreshState.error.localizedMessage
                    ?: "Failed to load video history",
                onRetry = { pagingItems.retry() }
            )
        }

        // ── Empty State ──────────────────────────────────────────
        refreshState is LoadState.NotLoading && pagingItems.itemCount == 0 -> {
            EmptyState()
        }

        // ── Video List ───────────────────────────────────────────
        else -> {
            VideoList(
                pagingItems = pagingItems,
                onVideoClick = onVideoClick
            )
        }
    }
}

// =============================================================================
// Loading State
// =============================================================================

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading history…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// Error State
// =============================================================================

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// =============================================================================
// Empty State
// =============================================================================

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No videos yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Share a reel from Instagram to get started.\n" +
                    "Your processed videos will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// Video List
// =============================================================================

/**
 * Paginated lazy list of video history items.
 * Includes a footer for append loading / error states.
 */
@Composable
private fun VideoList(
    pagingItems: LazyPagingItems<Video>,
    onVideoClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Video Items ──────────────────────────────────────────
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems.peek(index)?.id ?: index }
        ) { index ->
            val video = pagingItems[index] ?: return@items

            VideoHistoryItem(
                video = video,
                onClick = { onVideoClick(video.id) }
            )
        }

        // ── Append Loading Footer ────────────────────────────────
        if (pagingItems.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        // ── Append Error Footer ──────────────────────────────────
        if (pagingItems.loadState.append is LoadState.Error) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { pagingItems.retry() }) {
                        Text("Load more")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Video History Item
// =============================================================================

/** Date format pattern for video creation timestamps. */
private const val DATE_FORMAT_PATTERN = "MMM dd, yyyy · h:mm a"

/**
 * A single video history item card.
 *
 * Displays the video source URL, duration, creation date, and processing status.
 *
 * @param video The [Video] domain model to display.
 * @param onClick Callback invoked when the card is tapped.
 */
@Composable
private fun VideoHistoryItem(
    video: Video,
    onClick: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail placeholder / icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Video details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.sourceUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration
                    if (video.durationSeconds > 0) {
                        Text(
                            text = video.formattedDuration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Date
                    Text(
                        text = dateFormatter.format(Date(video.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status badge
            StatusBadge(status = video.status)
        }
    }
}

// =============================================================================
// Status Badge
// =============================================================================

/**
 * Compact status badge for video processing state.
 *
 * @param status The current [VideoStatus] of the video.
 */
@Composable
private fun StatusBadge(status: VideoStatus) {
    val (label, containerColor, contentColor) = when (status) {
        VideoStatus.COMPLETED -> Triple(
            "Done",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        VideoStatus.FAILED -> Triple(
            "Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        VideoStatus.DOWNLOADING, VideoStatus.EXTRACTING, VideoStatus.SPLITTING -> Triple(
            "Processing",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        VideoStatus.PENDING -> Triple(
            "Pending",
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
