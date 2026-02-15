package com.reelsplit.presentation.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelsplit.presentation.theme.ReelSplitTheme

// =============================================================================
// Error Screen Component
// =============================================================================

/**
 * Full-screen error display used by [ShareReceiverActivity].
 *
 * Shows a vertically and horizontally centered layout with:
 * - A 64dp warning icon in the error color
 * - "Oops! Something went wrong" title (marked as a heading for accessibility)
 * - A descriptive error message body
 * - A dismiss button that triggers [onDismiss]
 *
 * The content is scrollable to handle unusually long error messages without
 * clipping on smaller screens or large font sizes.
 *
 * @param message Descriptive error message to display
 * @param onDismiss Callback invoked when the dismiss button is tapped
 * @param modifier Modifier for the composable
 */
@Composable
fun ErrorScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null, // Decorative; title conveys semantics
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onDismiss) {
            Text(text = "Dismiss")
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    ReelSplitTheme {
        ErrorScreen(
            message = "The shared link doesn't appear to be a valid Instagram reel URL.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun ErrorScreenDarkPreview() {
    ReelSplitTheme(darkTheme = true) {
        ErrorScreen(
            message = "No URL was shared. Please share an Instagram reel link.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Long Message")
@Composable
private fun ErrorScreenLongMessagePreview() {
    ReelSplitTheme {
        ErrorScreen(
            message = "The shared link doesn't appear to be a valid Instagram reel URL.\n\n" +
                "Supported formats:\n" +
                "• instagram.com/reel/…\n" +
                "• instagram.com/reels/…\n" +
                "• instagram.com/p/…",
            onDismiss = {}
        )
    }
}
