package com.reelsplit.presentation.processing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.reelsplit.domain.model.ProcessingStage
import com.reelsplit.presentation.components.IndeterminateCircularProgress
import com.reelsplit.presentation.components.ReelSplitCircularProgress
import com.reelsplit.presentation.components.ReelSplitLinearProgress
import com.reelsplit.presentation.theme.GradientEnd
import com.reelsplit.presentation.theme.GradientMiddle
import com.reelsplit.presentation.theme.GradientStart
import com.reelsplit.presentation.theme.StatusCompleted
import com.reelsplit.presentation.theme.StatusDownloading
import com.reelsplit.presentation.theme.StatusFailed
import com.reelsplit.presentation.theme.StatusProcessing
import kotlinx.coroutines.flow.collectLatest

// =============================================================================
// Pre-computed gradient brush
// =============================================================================

private val HeaderGradientBrush = Brush.linearGradient(
    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
)

// =============================================================================
// ProcessingScreen — Destination
// =============================================================================

/**
 * ProcessingScreen shows the progress of the extract → download → split pipeline.
 *
 * Displays:
 * - A compact Instagram-gradient header
 * - A pipeline step indicator (Extract → Download → Split → Done)
 * - Animated progress section that changes per pipeline stage
 * - Cancel / Retry buttons
 *
 * The `url` navigation argument is read by [ProcessingViewModel] via
 * [SavedStateHandle], so it does not appear as an explicit parameter here.
 *
 * @param viewModel Injected [ProcessingViewModel] instance.
 */
@Destination(navArgsDelegate = ProcessingNavArgs::class)
@Composable
fun ProcessingScreen(
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Intercept back presses → treat as cancel during active processing
    val isActive = uiState !is ProcessingUiState.Complete &&
            uiState !is ProcessingUiState.Error
    BackHandler(enabled = isActive) {
        viewModel.onCancel()
    }

    // Handle one-time navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ProcessingEvent.NavigateToResult -> {
                    // TODO: navigator.navigate(ResultScreenDestination(videoId = event.videoId))
                    // Will be wired when ResultScreen is created
                }
                is ProcessingEvent.NavigateBack -> {
                    // TODO: navigator.navigateUp()
                    // Will be wired when navigation is integrated
                }
                else -> { /* Ignore unknown events */ }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Header ─────────────────────────────────────────────
            CompactHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Step Pipeline Indicator ─────────────────────────────
            PipelineStepIndicator(
                currentState = uiState,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // ─── Main Content (animated per state) ──────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                            .togetherWith(
                                fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                            )
                    },
                    contentKey = { it::class },
                    label = "ProcessingStateTransition"
                ) { state ->
                    when (state) {
                        is ProcessingUiState.Queued -> QueuedContent()
                        is ProcessingUiState.Extracting -> ExtractingContent()
                        is ProcessingUiState.Downloading -> DownloadingContent(state)
                        is ProcessingUiState.Splitting -> SplittingContent(state)
                        is ProcessingUiState.Complete -> CompleteContent(state)
                        is ProcessingUiState.Error -> ErrorContent(
                            state = state,
                            onRetry = viewModel::onRetry,
                            onDismiss = viewModel::onDismissError
                        )
                    }
                }
            }

            // ─── Cancel Button (visible during active states) ───────
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = viewModel::onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Navigation arguments for ProcessingScreen.
 */
data class ProcessingNavArgs(
    val url: String
)

// =============================================================================
// Compact Header
// =============================================================================

/**
 * Slim gradient header with the title "Processing".
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
            text = "Processing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// =============================================================================
// Pipeline Step Indicator
// =============================================================================

/**
 * Horizontal step indicator showing the four processing stages.
 * Each stage dot lights up based on the current state.
 */
@Composable
private fun PipelineStepIndicator(
    currentState: ProcessingUiState,
    modifier: Modifier = Modifier
) {
    val stages = remember {
        listOf("Extract", "Download", "Split", "Done")
    }
    val activeIndex = when (currentState) {
        is ProcessingUiState.Queued -> -1
        is ProcessingUiState.Extracting -> 0
        is ProcessingUiState.Downloading -> 1
        is ProcessingUiState.Splitting -> 2
        is ProcessingUiState.Complete -> 3
        is ProcessingUiState.Error -> when (currentState.failedAt) {
            ProcessingStage.EXTRACTION -> 0
            ProcessingStage.DOWNLOAD -> 1
            ProcessingStage.SPLITTING -> 2
            ProcessingStage.SAVING -> 3
            null -> -1
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, label ->
            val isCompleted = index < activeIndex
            val isActive = index == activeIndex
            val isError = currentState is ProcessingUiState.Error && isActive

            val dotColor = when {
                isError -> StatusFailed
                isCompleted -> StatusCompleted
                isActive -> StatusDownloading
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val textColor = when {
                isError -> StatusFailed
                isCompleted || isActive -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// State-specific Content Composables
// =============================================================================

@Composable
private fun QueuedContent() {
    StateContent(
        icon = Icons.Default.Search,
        iconTint = StatusDownloading,
        title = "Preparing…",
        subtitle = "Setting up the processing pipeline"
    ) {
        IndeterminateCircularProgress(size = 64.dp)
    }
}

@Composable
private fun ExtractingContent() {
    StateContent(
        icon = Icons.Default.Search,
        iconTint = StatusDownloading,
        title = "Extracting Video URL",
        subtitle = "Resolving the Instagram link…"
    ) {
        IndeterminateCircularProgress(size = 80.dp)
    }
}

@Composable
private fun DownloadingContent(state: ProcessingUiState.Downloading) {
    val subtitle = if (state.downloadedBytes == 0L && state.totalBytes == 0L) {
        "Preparing download…"
    } else {
        state.formattedProgress
    }
    StateContent(
        icon = Icons.Default.Download,
        iconTint = StatusDownloading,
        title = "Downloading",
        subtitle = subtitle
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReelSplitCircularProgress(
                progress = state.percent / 100f,
                size = 120.dp,
                strokeWidth = 10.dp
            )

            ReelSplitLinearProgress(
                progress = state.percent / 100f,
                modifier = Modifier.fillMaxWidth(0.8f),
                height = 6.dp
            )

            // Speed and ETA
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.formattedSpeed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.estimatedTimeRemaining?.let { seconds ->
                    Text(
                        text = formatEta(seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SplittingContent(state: ProcessingUiState.Splitting) {
    val splittingColors = remember { listOf(StatusProcessing, StatusCompleted) }
    StateContent(
        icon = Icons.Default.ContentCut,
        iconTint = StatusProcessing,
        title = "Splitting Video",
        subtitle = "Part ${state.currentPart} of ${state.totalParts}"
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReelSplitCircularProgress(
                progress = state.progressPercent / 100f,
                size = 120.dp,
                strokeWidth = 10.dp,
                progressColors = splittingColors
            )

            ReelSplitLinearProgress(
                progress = state.progressPercent / 100f,
                modifier = Modifier.fillMaxWidth(0.8f),
                height = 6.dp,
                progressColors = splittingColors
            )

            Text(
                text = "Creating WhatsApp Status–compatible segments…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompleteContent(state: ProcessingUiState.Complete) {
    StateContent(
        icon = Icons.Default.Check,
        iconTint = StatusCompleted,
        title = "Complete!",
        subtitle = "${state.segments.size} segment${if (state.segments.size != 1) "s" else ""} ready"
    ) {
        // The NavigateToResult event auto-navigates; this is a brief "done" screen
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusCompleted.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete",
                tint = StatusCompleted,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Error state content with retry and dismiss actions.
 *
 * @param state The current error state.
 * @param onRetry Callback to restart the pipeline.
 * @param onDismiss Callback to navigate back (distinct from cancel — does not re-set error state).
 */
@Composable
private fun ErrorContent(
    state: ProcessingUiState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(StatusFailed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = StatusFailed,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Stage info
        state.failedAt?.let { stage ->
            Text(
                text = "Failed during: ${stage.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        if (state.isRetryable) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// =============================================================================
// Shared State Content Layout
// =============================================================================

/**
 * Shared layout for active processing states: icon badge + title + subtitle + custom content.
 */
@Composable
private fun StateContent(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status icon badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        content()
    }
}

// =============================================================================
// Helpers
// =============================================================================

/**
 * Formats seconds into a human-readable ETA string.
 */
private fun formatEta(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s remaining"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s remaining"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m remaining"
}
