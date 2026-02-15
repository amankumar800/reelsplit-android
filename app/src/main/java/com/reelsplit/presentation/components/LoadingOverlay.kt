package com.reelsplit.presentation.components

import androidx.annotation.IntRange
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
// Loading Overlay
// =============================================================================

/**
 * A full-screen loading overlay that blocks user interaction.
 *
 * Displays a semi-transparent scrim over the content with a centered Lottie
 * loading animation and an optional descriptive message. **All** pointer events
 * (taps, scrolls, drags) are consumed so the UI underneath cannot be interacted
 * with while the overlay is visible.
 *
 * The overlay includes a built-in fade-in / fade-out transition controlled by
 * [visible]. Callers should **not** wrap this composable in an additional
 * [AnimatedVisibility] — the transition is handled internally.
 *
 * Features:
 * - Semi-transparent background scrim
 * - Lottie animation with automatic fallback on load/error
 * - Optional message text below the animation
 * - Blocks **all** pointer events (tap, scroll, drag) underneath
 * - Fade-in / fade-out transition
 * - Accessibility: merged semantics so TalkBack announces a single node
 *
 * @param visible Whether the overlay is shown
 * @param modifier Modifier for the composable
 * @param animationResId Raw resource ID of the Lottie animation JSON. If null, an
 *   indeterminate circular progress indicator is shown as fallback.
 * @param animationAsset Lottie animation file name in the assets folder. Ignored
 *   when [animationResId] is provided.
 * @param message Optional message text displayed below the animation
 * @param animationSize Size of the Lottie animation
 * @param scrimColor Background scrim color (semi-transparent)
 * @param contentColor Color used for the message text and fallback indicator.
 *   Should contrast with [scrimColor].
 * @param fadeDuration Duration of the fade animation in milliseconds
 */
@Composable
fun LoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    @RawRes animationResId: Int? = null,
    animationAsset: String? = null,
    message: String? = null,
    animationSize: Dp = 160.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.6f),
    contentColor: Color = Color.White,
    @IntRange(from = 0) fadeDuration: Int = 300
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = fadeDuration)),
        exit = fadeOut(animationSpec = tween(durationMillis = fadeDuration))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                // Consume ALL pointer events (taps, scrolls, drags) to
                // prevent interaction with content underneath.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                // Merge all child semantics into a single node for TalkBack.
                .clearAndSetSemantics {
                    contentDescription = message ?: "Loading"
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LoadingOverlayAnimation(
                    animationResId = animationResId,
                    animationAsset = animationAsset,
                    size = animationSize,
                    fallbackColor = contentColor
                )

                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// Internal Animation Helper
// =============================================================================

/**
 * Internal composable that renders the Lottie animation or a circular fallback.
 *
 * Resolves the animation source from either a raw resource or an asset file.
 * Falls back to [IndeterminateCircularProgress] while the composition is loading
 * or if loading fails.
 *
 * @param animationResId Raw resource ID of the Lottie animation, or null
 * @param animationAsset Asset file name, or null
 * @param size Size of the animation container
 * @param fallbackColor Color for the indeterminate fallback spinner
 */
@Composable
private fun LoadingOverlayAnimation(
    @RawRes animationResId: Int?,
    animationAsset: String?,
    size: Dp,
    fallbackColor: Color
) {
    val spec: LottieCompositionSpec? = when {
        animationResId != null -> LottieCompositionSpec.RawRes(animationResId)
        animationAsset != null -> LottieCompositionSpec.Asset(animationAsset)
        else -> null
    }

    if (spec != null) {
        val compositionResult = rememberLottieComposition(spec = spec)
        val composition by compositionResult
        val animationProgress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            when {
                compositionResult.isLoading || compositionResult.isFailure -> {
                    IndeterminateCircularProgress(
                        size = size / 2,
                        color = fallbackColor
                    )
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
    } else {
        // No Lottie spec provided — show indeterminate progress as default
        IndeterminateCircularProgress(
            size = size / 2,
            color = fallbackColor
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayWithMessagePreview() {
    ReelSplitTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated background content
            Text(
                text = "Background Content",
                modifier = Modifier.align(Alignment.Center)
            )

            LoadingOverlay(
                visible = true,
                message = "Downloading video…"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayNoMessagePreview() {
    ReelSplitTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingOverlay(visible = true)
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun LoadingOverlayDarkPreview() {
    ReelSplitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingOverlay(
                visible = true,
                message = "Splitting video into parts…"
            )
        }
    }
}
