package com.reelsplit.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.reelsplit.presentation.theme.ReelSplitTheme
import java.util.Locale

// =============================================================================
// Video Preview Component
// =============================================================================

/**
 * A video preview component that displays a video thumbnail with playback controls.
 *
 * Features:
 * - Thumbnail display using Coil with video frame extraction
 * - Play button overlay with semi-transparent background
 * - Duration display in the bottom-right corner
 * - Shimmer loading effect while the thumbnail loads
 * - Accessibility support with content descriptions
 *
 * @param videoUri URI of the video file (local or remote)
 * @param durationMs Duration of the video in milliseconds
 * @param modifier Modifier for the composable
 * @param thumbnailFrameMs Frame position in milliseconds to use for thumbnail (default: 1000ms)
 * @param aspectRatio Aspect ratio of the preview (default: 16:9)
 * @param showDuration Whether to show the duration overlay
 * @param showPlayButton Whether to show the play button overlay
 * @param onClick Callback when the preview is clicked
 */
@Composable
fun VideoPreview(
    videoUri: String,
    durationMs: Long,
    modifier: Modifier = Modifier,
    thumbnailFrameMs: Long = 1000L,
    aspectRatio: Float = 16f / 9f,
    showDuration: Boolean = true,
    showPlayButton: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Reset loading/error states when videoUri changes
    var isLoading by remember(videoUri) { mutableStateOf(true) }
    var isError by remember(videoUri) { mutableStateOf(false) }

    val formattedDuration = remember(durationMs) {
        formatDuration(durationMs)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (onClick != null) {
                    role = Role.Button
                }
                contentDescription = if (onClick != null) {
                    "Play video, duration $formattedDuration"
                } else {
                    "Video preview, duration $formattedDuration"
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .aspectRatio(aspectRatio),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
        ) {
            // Shimmer loading effect with fade animation
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Video thumbnail
            key(videoUri) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .videoFrameMillis(thumbnailFrameMs)
                        .crossfade(300)
                        .build(),
                    contentDescription = null, // Handled by parent semantics
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        isLoading = state is AsyncImagePainter.State.Loading
                        isError = state is AsyncImagePainter.State.Error
                    }
                )
            }

            // Error state overlay
            if (isError && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Failed to load",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Gradient overlay for better visibility of overlays
            AnimatedVisibility(
                visible = !isLoading && !isError,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.5f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }

            // Play button overlay with fade animation
            AnimatedVisibility(
                visible = showPlayButton && !isLoading && !isError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                PlayButtonOverlay()
            }

            // Duration badge with fade animation
            AnimatedVisibility(
                visible = showDuration && !isLoading && !isError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                DurationBadge(duration = formattedDuration)
            }
        }
    }
}

// =============================================================================
// Shimmer Loading Effect
// =============================================================================

/**
 * A shimmer loading effect for placeholder content.
 *
 * Creates an animated gradient that moves horizontally to indicate loading state.
 *
 * @param modifier Modifier for the composable
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceVariant
    )

    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation - 500f, y = 0f),
        end = Offset(x = translateAnimation, y = 0f)
    )

    Box(
        modifier = modifier.background(brush = brush)
    )
}

// =============================================================================
// Play Button Overlay
// =============================================================================

/**
 * A circular play button overlay with semi-transparent background.
 *
 * @param modifier Modifier for the composable
 * @param size Size of the play button
 */
@Composable
private fun PlayButtonOverlay(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

// =============================================================================
// Duration Badge
// =============================================================================

/**
 * A badge displaying video duration in the corner.
 *
 * @param duration Formatted duration string (e.g., "1:30")
 * @param modifier Modifier for the composable
 */
@Composable
private fun DurationBadge(
    duration: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Black.copy(alpha = 0.75f)
    ) {
        Text(
            text = duration,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// =============================================================================
// Utility Functions
// =============================================================================

/**
 * Formats a duration in milliseconds to a human-readable string.
 *
 * Examples:
 * - 90000 -> "1:30"
 * - 3661000 -> "1:01:01"
 *
 * @param durationMs Duration in milliseconds
 * @return Formatted duration string
 */
private fun formatDuration(durationMs: Long): String {
    // Handle negative or zero duration
    if (durationMs <= 0) return "0:00"
    
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun VideoPreviewLoadingPreview() {
    ReelSplitTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shimmer loading state preview
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Preview(showBackground = true, name = "Video Preview - Dark Theme")
@Composable
private fun VideoPreviewDarkPreview() {
    ReelSplitTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shimmer loading state preview
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Preview(showBackground = true, name = "Duration Badge Preview")
@Composable
private fun DurationBadgePreview() {
    ReelSplitTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationBadge(duration = "0:30")
            DurationBadge(duration = "1:30")
            DurationBadge(duration = "10:00")
            DurationBadge(duration = "1:00:00")
        }
    }
}

@Preview(showBackground = true, name = "Play Button Preview")
@Composable
private fun PlayButtonPreview() {
    ReelSplitTheme {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            PlayButtonOverlay()
        }
    }
}
