package com.reelsplit.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reelsplit.presentation.theme.ReelSplitTheme

// =============================================================================
// Error Dialog Component
// =============================================================================

/**
 * A Material 3 styled error dialog with retry and dismiss options.
 *
 * Features:
 * - Large error icon with error color theming
 * - Customizable title and message
 * - Optional retry button with callback
 * - Dismiss button for closing the dialog
 * - Follows Material 3 design guidelines
 *
 * @param title Title of the error dialog
 * @param message Detailed error message to display
 * @param onDismiss Callback invoked when the dialog is dismissed (via dismiss button
 *   or back press / scrim tap)
 * @param modifier Modifier for the composable
 * @param onRetry Optional callback for retry action. If null, the retry button is hidden.
 * @param dismissButtonText Text for the dismiss button
 * @param retryButtonText Text for the retry button
 * @param icon Icon to display in the dialog header
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    dismissButtonText: String = "Dismiss",
    retryButtonText: String = "Retry",
    icon: ImageVector = Icons.Outlined.Warning
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null, // Decorative; title conveys semantics
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            if (onRetry != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onRetry()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = retryButtonText)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = dismissButtonText)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.error,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    )
}

/**
 * A simplified error dialog without a retry option.
 *
 * Use this for non-recoverable errors where retry is not applicable.
 *
 * @param title Title of the error dialog
 * @param message Detailed error message to display
 * @param onDismiss Callback invoked when the dialog is dismissed
 * @param modifier Modifier for the composable
 * @param dismissButtonText Text for the dismiss button
 */
@Composable
fun SimpleErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissButtonText: String = "OK"
) {
    ErrorDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        modifier = modifier,
        onRetry = null,
        dismissButtonText = dismissButtonText
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun ErrorDialogWithRetryPreview() {
    ReelSplitTheme {
        ErrorDialog(
            title = "Download Failed",
            message = "Unable to download the video. Please check your internet connection and try again.",
            onDismiss = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDialogWithoutRetryPreview() {
    ReelSplitTheme {
        SimpleErrorDialog(
            title = "Video Not Found",
            message = "The requested video could not be found. It may have been deleted or is no longer available.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun ErrorDialogDarkThemePreview() {
    ReelSplitTheme(darkTheme = true) {
        ErrorDialog(
            title = "Processing Error",
            message = "An error occurred while splitting the video. The file may be corrupted.",
            onDismiss = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark No Retry")
@Composable
private fun SimpleErrorDialogDarkThemePreview() {
    ReelSplitTheme(darkTheme = true) {
        SimpleErrorDialog(
            title = "WhatsApp Not Installed",
            message = "WhatsApp is required to share videos to your Status. Please install WhatsApp from the Play Store and try again.",
            onDismiss = {},
            dismissButtonText = "Got it"
        )
    }
}
