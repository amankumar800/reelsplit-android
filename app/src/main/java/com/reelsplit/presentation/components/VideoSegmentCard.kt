package com.reelsplit.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelsplit.domain.model.VideoSegment
import com.reelsplit.presentation.theme.ReelSplitTheme

// =============================================================================
// Video Segment Card
// =============================================================================

/**
 * A card displaying an individual video segment with sharing actions.
 *
 * Features:
 * - Prominent part number display with gradient badge
 * - Duration and file size metadata
 * - "Share to Status" and "Share to Chat" buttons
 * - Visual indicator when segment has been shared
 * - Accessibility support with content descriptions
 *
 * @param segment The video segment to display
 * @param modifier Modifier for the composable
 * @param onShareToStatus Callback when "Share to Status" is clicked
 * @param onShareToChat Callback when "Share to Chat" is clicked
 * @param enabled Whether the card actions are enabled
 */
@Composable
fun VideoSegmentCard(
    segment: VideoSegment,
    modifier: Modifier = Modifier,
    onShareToStatus: () -> Unit = {},
    onShareToChat: () -> Unit = {},
    enabled: Boolean = true
) {
    val cardContentDescription = remember(segment) {
        buildString {
            append("Part ${segment.partNumber} of ${segment.totalParts}")
            append(", duration ${segment.formattedDuration}")
            append(", size ${segment.formattedSize}")
            if (segment.isShared) {
                append(", already shared")
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = cardContentDescription
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: Part number badge + shared indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PartNumberBadge(
                    partNumber = segment.partNumber,
                    totalParts = segment.totalParts
                )

                // Shared indicator with animation
                AnimatedVisibility(
                    visible = segment.isShared,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SharedIndicator()
                }
            }

            // Metadata row: Duration and file size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetadataChip(
                    icon = Icons.Outlined.Schedule,
                    label = segment.formattedDuration
                )
                MetadataChip(
                    icon = Icons.Outlined.Storage,
                    label = segment.formattedSize
                )
            }

            // WhatsApp Status validity indicator with animation
            AnimatedVisibility(
                visible = !segment.isValidForWhatsAppStatus,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WarningBanner(
                    message = "Exceeds WhatsApp Status limits"
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share to Status button (primary action)
                Button(
                    onClick = onShareToStatus,
                    enabled = enabled && segment.isValidForWhatsAppStatus,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Share to Chat button (secondary action)
                OutlinedButton(
                    onClick = onShareToChat,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to Chat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// =============================================================================
// Part Number Badge
// =============================================================================

/**
 * A gradient badge displaying the part number prominently.
 *
 * @param partNumber The current part number (1-indexed)
 * @param totalParts Total number of parts
 * @param modifier Modifier for the composable
 */
@Composable
private fun PartNumberBadge(
    partNumber: Int,
    totalParts: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                ),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Part $partNumber of $totalParts",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// =============================================================================
// Shared Indicator
// =============================================================================

/**
 * An indicator showing that the segment has been shared.
 *
 * @param modifier Modifier for the composable
 */
@Composable
private fun SharedIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Shared",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

// =============================================================================
// Metadata Chip
// =============================================================================

/**
 * A small chip displaying metadata with an icon.
 *
 * @param icon The icon to display
 * @param label The text label
 * @param modifier Modifier for the composable
 */
@Composable
private fun MetadataChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// Warning Banner
// =============================================================================

/**
 * A warning banner for invalid segments.
 *
 * @param message The warning message to display
 * @param modifier Modifier for the composable
 */
@Composable
private fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun VideoSegmentCardPreview() {
    ReelSplitTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Normal segment
            VideoSegmentCard(
                segment = VideoSegment(
                    id = "segment-1",
                    videoId = "video-1",
                    partNumber = 1,
                    totalParts = 3,
                    filePath = "/path/to/segment1.mp4",
                    durationSeconds = 85,
                    fileSizeBytes = 12_500_000,
                    startTimeSeconds = 0,
                    endTimeSeconds = 85,
                    isShared = false
                )
            )

            // Already shared segment
            VideoSegmentCard(
                segment = VideoSegment(
                    id = "segment-2",
                    videoId = "video-1",
                    partNumber = 2,
                    totalParts = 3,
                    filePath = "/path/to/segment2.mp4",
                    durationSeconds = 90,
                    fileSizeBytes = 15_000_000,
                    startTimeSeconds = 85,
                    endTimeSeconds = 175,
                    isShared = true
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun VideoSegmentCardDarkPreview() {
    ReelSplitTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VideoSegmentCard(
                segment = VideoSegment(
                    id = "segment-1",
                    videoId = "video-1",
                    partNumber = 1,
                    totalParts = 2,
                    filePath = "/path/to/segment1.mp4",
                    durationSeconds = 60,
                    fileSizeBytes = 8_500_000,
                    startTimeSeconds = 0,
                    endTimeSeconds = 60,
                    isShared = false
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Invalid Segment")
@Composable
private fun VideoSegmentCardInvalidPreview() {
    ReelSplitTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Segment exceeding WhatsApp limits
            VideoSegmentCard(
                segment = VideoSegment(
                    id = "segment-1",
                    videoId = "video-1",
                    partNumber = 1,
                    totalParts = 1,
                    filePath = "/path/to/segment1.mp4",
                    durationSeconds = 95, // Exceeds 90s limit
                    fileSizeBytes = 18_000_000, // Exceeds 16MB limit
                    startTimeSeconds = 0,
                    endTimeSeconds = 95,
                    isShared = false
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Disabled State")
@Composable
private fun VideoSegmentCardDisabledPreview() {
    ReelSplitTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            VideoSegmentCard(
                segment = VideoSegment(
                    id = "segment-1",
                    videoId = "video-1",
                    partNumber = 3,
                    totalParts = 3,
                    filePath = "/path/to/segment3.mp4",
                    durationSeconds = 45,
                    fileSizeBytes = 6_000_000,
                    startTimeSeconds = 175,
                    endTimeSeconds = 220,
                    isShared = false
                ),
                enabled = false
            )
        }
    }
}
