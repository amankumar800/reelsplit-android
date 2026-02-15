package com.reelsplit.presentation.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.reelsplit.presentation.theme.GradientEnd
import com.reelsplit.presentation.theme.GradientMiddle
import com.reelsplit.presentation.theme.GradientStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// =============================================================================
// Pre-computed gradient brushes (avoid recreation on every recomposition)
// =============================================================================

private val HeroGradientBrush = Brush.linearGradient(
    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
)

private val StepBadgeGradientBrush = Brush.linearGradient(
    colors = listOf(GradientStart, GradientEnd)
)

// =============================================================================
// HomeScreen — Start Destination
// =============================================================================

/**
 * HomeScreen is the landing page of the ReelSplit app.
 *
 * Displays:
 * - An Instagram-gradient branded hero section with app name and tagline
 * - A 3-step visual guide explaining how to use the app
 * - A "View History" CTA button
 * - App version in the footer
 *
 * @param viewModel Injected [HomeViewModel] instance.
 */
@RootNavGraph(start = true)
@Destination
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Staggered entrance animation flag
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150L) // Brief delay so the splash screen transition feels smooth
        showContent = true
    }

    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.NavigateToHistory -> {
                    // TODO: navigator.navigate(HistoryScreenDestination)
                    // Will be wired when HistoryScreen is created
                }
                else -> { /* Ignore unknown events */ }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                // No top padding — the hero gradient extends behind the status bar
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Hero Section ────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                            slideInVertically(
                                initialOffsetY = { -it / 4 },
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            )
                ) {
                    HeroSection()
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // ─── "How It Works" Header ───────────────────────────────
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                ) {
                    Text(
                        text = "How It Works",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ─── Step Cards ──────────────────────────────────────────
            item {
                StepCard(
                    stepNumber = 1,
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Find a Reel",
                    description = "Open Instagram and find the Reel you want to share on WhatsApp Status.",
                    visible = showContent,
                    delayMillis = 100
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                StepCard(
                    stepNumber = 2,
                    icon = Icons.Default.Share,
                    title = "Share to ReelSplit",
                    description = "Tap the share icon on the Reel and select \"ReelSplit\" from the share menu.",
                    visible = showContent,
                    delayMillis = 200
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                StepCard(
                    stepNumber = 3,
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = "Share to WhatsApp",
                    description = "We automatically split it into 90-second parts. Tap to share each part to WhatsApp Status.",
                    visible = showContent,
                    delayMillis = 300
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // ─── View History Button ─────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                    ) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                    )
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.onViewHistoryClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View History",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // ─── Footer ─────────────────────────────────────────────
            item {
                Text(
                    text = "v${uiState.appVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// Hero Section
// =============================================================================

/**
 * Gradient hero banner with app name and tagline.
 * The gradient extends behind the status bar for an edge-to-edge premium feel.
 * Internal content is padded below the status bar.
 */
@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = HeroGradientBrush)
            .statusBarsPadding() // Content stays below status bar, but gradient extends behind it
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // App icon stand-in
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "ReelSplit logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ReelSplit",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Split Reels for WhatsApp Status",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// Step Card
// =============================================================================

/**
 * An individual instruction step with entrance animation.
 *
 * @param stepNumber The 1-based step number displayed as a numbered badge.
 * @param icon The Material icon representing the step.
 * @param title Short title of the step.
 * @param description Detailed description of the step.
 * @param visible Whether the card should be visible (controls entrance animation).
 * @param delayMillis Stagger delay in milliseconds for the entrance animation.
 */
@Composable
private fun StepCard(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    visible: Boolean,
    delayMillis: Long
) {
    // Per-card stagger control
    var showCard by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis)
            showCard = true
        }
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step number badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush = StepBadgeGradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Step content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
