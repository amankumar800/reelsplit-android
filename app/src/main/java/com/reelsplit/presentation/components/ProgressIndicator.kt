package com.reelsplit.presentation.components

import androidx.annotation.FloatRange
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.reelsplit.presentation.theme.ReelSplitTheme

// =============================================================================
// Circular Progress Indicator
// =============================================================================

/**
 * A custom circular progress indicator with percentage text in the center.
 *
 * Features:
 * - Gradient stroke using theme colors
 * - Animated progress transitions
 * - Customizable size and stroke width
 * - Percentage text display in the center
 * - Accessibility support with semantics
 *
 * @param progress Current progress value (0f to 1f)
 * @param modifier Modifier for the composable
 * @param size Diameter of the circular progress indicator
 * @param strokeWidth Width of the progress arc stroke
 * @param trackColor Color of the background track
 * @param progressColors Colors for the progress gradient (start, end)
 * @param showPercentage Whether to show percentage text in the center
 * @param animationDuration Duration of the progress animation in milliseconds
 */
@Composable
fun ReelSplitCircularProgress(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    ),
    showPercentage: Boolean = true,
    animationDuration: Int = 300
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    
    // Animate progress changes smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = animationDuration),
        label = "CircularProgressAnimation"
    )

    val percentageValue = (animatedProgress * 100).toInt()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clampedProgress,
                    range = 0f..1f
                )
                contentDescription = "Progress: $percentageValue percent"
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokeWidth.toPx()) / 2
            val topLeftOffset = (canvasSize - radius * 2) / 2

            // Draw background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(topLeftOffset, topLeftOffset),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Draw progress arc with gradient
            // Rotate to align gradient with the arc starting from the top
            if (animatedProgress > 0f) {
                rotate(degrees = -90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = progressColors + progressColors.first()
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(topLeftOffset, topLeftOffset),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Percentage text in center
        if (showPercentage) {
            Text(
                text = "$percentageValue%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Indeterminate circular progress indicator.
 *
 * Wraps Material 3's CircularProgressIndicator with theme-consistent styling.
 *
 * @param modifier Modifier for the composable
 * @param size Diameter of the progress indicator
 * @param strokeWidth Width of the progress arc stroke
 * @param color Color of the progress indicator
 */
@Composable
fun IndeterminateCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = strokeWidth,
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// =============================================================================
// Linear Progress Indicator
// =============================================================================

/**
 * A custom linear progress bar with animated transitions.
 *
 * Features:
 * - Gradient progress bar
 * - Animated progress transitions
 * - Rounded track corners
 * - Optional percentage label
 * - Accessibility support with semantics
 *
 * @param progress Current progress value (0f to 1f)
 * @param modifier Modifier for the composable
 * @param height Height of the progress bar
 * @param trackColor Color of the background track
 * @param progressColors Colors for the progress gradient
 * @param showPercentage Whether to show percentage text above the bar
 * @param animationDuration Duration of the progress animation in milliseconds
 */
@Composable
fun ReelSplitLinearProgress(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    ),
    showPercentage: Boolean = false,
    animationDuration: Int = 300
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    
    // Animate progress changes smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = animationDuration),
        label = "LinearProgressAnimation"
    )

    val percentageValue = (animatedProgress * 100).toInt()

    Column(
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = clampedProgress,
                range = 0f..1f
            )
            contentDescription = "Progress: $percentageValue percent"
        },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showPercentage) {
            Text(
                text = "$percentageValue%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val cornerRadiusValue = this.size.height / 2

            // Draw background track
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(cornerRadiusValue),
                size = this.size
            )

            // Draw progress bar with gradient
            // Ensure minimum width to display rounded corners properly
            if (animatedProgress > 0f) {
                val progressWidth = this.size.width * animatedProgress
                val minWidth = this.size.height // Minimum width = height for proper rounded corners
                val effectiveWidth = progressWidth.coerceAtLeast(minWidth)
                
                drawRoundRect(
                    brush = Brush.horizontalGradient(progressColors),
                    cornerRadius = CornerRadius(cornerRadiusValue),
                    size = this.size.copy(width = effectiveWidth.coerceAtMost(this.size.width))
                )
            }
        }
    }
}

/**
 * Indeterminate linear progress indicator.
 *
 * Wraps Material 3's LinearProgressIndicator with theme-consistent styling.
 *
 * @param modifier Modifier for the composable
 * @param color Color of the progress indicator
 * @param height Height of the progress bar
 */
@Composable
fun IndeterminateLinearProgress(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 4.dp
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

// =============================================================================
// Lottie Animation Progress
// =============================================================================

/**
 * Internal composable that handles the common Lottie animation logic.
 *
 * @param spec The Lottie composition spec (RawRes or Asset)
 * @param modifier Modifier for the composable
 * @param size Size of the animation
 * @param iterations Number of times to repeat the animation
 * @param contentDescriptionText Accessibility description
 */
@Composable
private fun LottieProgressContent(
    spec: LottieCompositionSpec,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    iterations: Int = LottieConstants.IterateForever,
    contentDescriptionText: String = "Loading animation"
) {
    val compositionResult = rememberLottieComposition(spec = spec)
    val composition by compositionResult
    val animationProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = contentDescriptionText
            }
    ) {
        when {
            compositionResult.isLoading -> {
                // Show fallback while loading
                IndeterminateCircularProgress(size = size / 2)
            }
            compositionResult.isFailure -> {
                // Show fallback on error
                IndeterminateCircularProgress(size = size / 2)
            }
            composition != null -> {
                LottieAnimation(
                    composition = composition,
                    progress = { animationProgress },
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

/**
 * A progress indicator using Lottie animation.
 *
 * Provides smooth, visually appealing loading animations using Lottie files.
 * Shows an indeterminate circular progress as fallback while loading or on error.
 *
 * @param animationResId Raw resource ID of the Lottie animation JSON file
 * @param modifier Modifier for the composable
 * @param size Size of the animation
 * @param iterations Number of times to repeat the animation (use [LottieConstants.IterateForever] for infinite)
 * @param contentDescriptionText Accessibility description for the animation
 */
@Composable
fun LottieProgressIndicator(
    @RawRes animationResId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    iterations: Int = LottieConstants.IterateForever,
    contentDescriptionText: String = "Loading"
) {
    LottieProgressContent(
        spec = LottieCompositionSpec.RawRes(animationResId),
        modifier = modifier,
        size = size,
        iterations = iterations,
        contentDescriptionText = contentDescriptionText
    )
}

/**
 * A progress indicator using a Lottie animation from assets.
 * Shows an indeterminate circular progress as fallback while loading or on error.
 *
 * @param assetName Name of the Lottie animation file in the assets folder
 * @param modifier Modifier for the composable
 * @param size Size of the animation
 * @param iterations Number of times to repeat the animation
 * @param contentDescriptionText Accessibility description for the animation
 */
@Composable
fun LottieProgressIndicatorFromAsset(
    assetName: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    iterations: Int = LottieConstants.IterateForever,
    contentDescriptionText: String = "Loading"
) {
    LottieProgressContent(
        spec = LottieCompositionSpec.Asset(assetName),
        modifier = modifier,
        size = size,
        iterations = iterations,
        contentDescriptionText = contentDescriptionText
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun CircularProgressIndicatorPreview() {
    ReelSplitTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            ReelSplitCircularProgress(progress = 0.75f)
            ReelSplitCircularProgress(
                progress = 0.5f,
                size = 64.dp,
                strokeWidth = 6.dp
            )
            IndeterminateCircularProgress()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LinearProgressPreview() {
    ReelSplitTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ReelSplitLinearProgress(progress = 0.6f)
            ReelSplitLinearProgress(
                progress = 0.8f,
                showPercentage = true,
                height = 12.dp
            )
            ReelSplitLinearProgress(progress = 0.05f) // Test small progress
            IndeterminateLinearProgress()
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun DarkThemePreview() {
    ReelSplitTheme(darkTheme = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            ReelSplitCircularProgress(progress = 0.65f)
            ReelSplitLinearProgress(
                progress = 0.45f,
                showPercentage = true
            )
        }
    }
}
