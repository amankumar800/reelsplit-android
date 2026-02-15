package com.reelsplit.presentation.processing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Test-only helper composable that mirrors the ProcessingScreen content
 * structure but without requiring a ViewModel or navigation.
 *
 * This allows UI tests to render each state without Hilt/ViewModel overhead.
 */
@Composable
fun ProcessingScreenTestContent(
    state: ProcessingUiState,
    showCancel: Boolean = state !is ProcessingUiState.Complete && state !is ProcessingUiState.Error,
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main content based on state
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                when (state) {
                    is ProcessingUiState.Queued -> {
                        Text("Preparing…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Setting up the processing pipeline", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ProcessingUiState.Extracting -> {
                        Text("Extracting Video URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Resolving the Instagram link…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ProcessingUiState.Downloading -> {
                        Text("Downloading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    is ProcessingUiState.Splitting -> {
                        Text("Splitting Video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Part ${state.currentPart} of ${state.totalParts}", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ProcessingUiState.Complete -> {
                        Text("Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.segments.size} segment${if (state.segments.size != 1) "s" else ""} ready",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is ProcessingUiState.Error -> {
                        Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.failedAt?.let { stage ->
                            Text(
                                "Failed during: ${stage.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isRetryable) {
                            OutlinedButton(onClick = onRetry) {
                                Text("Retry")
                            }
                        }

                        OutlinedButton(onClick = onDismiss) {
                            Text("Go Back")
                        }
                    }
                }
            }

            // Cancel button
            AnimatedVisibility(visible = showCancel) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
    }
}
