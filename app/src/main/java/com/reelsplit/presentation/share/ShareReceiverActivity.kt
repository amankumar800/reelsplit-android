package com.reelsplit.presentation.share

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reelsplit.core.extensions.isValidInstagramUrl
import com.reelsplit.presentation.MainActivity
import com.reelsplit.presentation.theme.ReelSplitTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Activity that receives shared content from Instagram and other apps.
 *
 * Appears in the system share menu when users share reel URLs or video files.
 * Handles:
 * - [Intent.ACTION_SEND] with `text/plain` — Instagram reel URLs shared as text.
 * - [Intent.ACTION_SEND] with `video/*`  — Direct video files (e.g. from a file manager).
 * - [Intent.ACTION_VIEW] — Deep-link clicks on `instagram.com/reel/…` URLs.
 *
 * After extracting the URL or video URI the activity navigates to [MainActivity]
 * with the reel URL packed under [EXTRA_REEL_URL] and finishes immediately.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    /** Drives the Compose error-screen visibility. */
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            ReelSplitTheme {
                ShareReceiverContent(
                    errorMessage = errorMessage,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        errorMessage = null // Clear stale error before processing the new intent
        handleIntent(intent)
    }

    // -------------------------------------------------------------------------
    // Intent routing
    // -------------------------------------------------------------------------

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            showError("No data received.")
            return
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                when {
                    intent.type == "text/plain" -> {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        Timber.d("Received shared text: $sharedText")
                        handleInstagramUrl(sharedText)
                    }
                    intent.type?.startsWith("video/") == true -> {
                        val videoUri: Uri? = getParcelableExtraCompat(
                            intent,
                            Intent.EXTRA_STREAM
                        )
                        Timber.d("Received shared video URI: $videoUri")
                        handleVideoUri(videoUri)
                    }
                    else -> {
                        Timber.w("Unsupported MIME type: ${intent.type}")
                        showError("Unsupported content type: ${intent.type}")
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                Timber.d("Received ACTION_VIEW URL: $url")
                handleInstagramUrl(url)
            }
            else -> {
                Timber.w("Unsupported action: ${intent.action}")
                showError("This action is not supported.")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    /**
     * Validates and extracts an Instagram reel URL from shared text,
     * then navigates to [MainActivity] for processing.
     */
    private fun handleInstagramUrl(url: String?) {
        if (url.isNullOrBlank()) {
            Timber.w("Received null or blank URL")
            showError("No URL was shared. Please share an Instagram reel link.")
            return
        }

        // The shared text may contain surrounding text; try to extract the URL.
        val reelUrl = extractInstagramUrl(url)
        if (reelUrl != null) {
            Timber.d("Extracted reel URL: $reelUrl")
            navigateToProcessing(reelUrl)
        } else {
            Timber.w("Invalid Instagram URL: $url")
            showError(
                "The shared link doesn't appear to be a valid Instagram reel URL.\n\n" +
                    "Supported formats:\n" +
                    "• instagram.com/reel/…\n" +
                    "• instagram.com/reels/…\n" +
                    "• instagram.com/p/…"
            )
        }
    }

    /**
     * Handles a directly-shared video file URI.
     *
     * The temporary read grant from the sending app is forwarded to [MainActivity]
     * via [ClipData] + [Intent.FLAG_GRANT_READ_URI_PERMISSION] so the downstream
     * activity (and any workers it spawns from the URI) can still read the file.
     */
    private fun handleVideoUri(uri: Uri?) {
        if (uri == null) {
            Timber.w("Received null video URI")
            showError("No video file was received.")
            return
        }

        Timber.d("Processing video URI: $uri")
        navigateToProcessing(reelUrl = uri.toString(), videoUri = uri)
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Launches [MainActivity] with the reel URL (or video URI string) packed
     * under [EXTRA_REEL_URL], then finishes this activity.
     *
     * @param reelUrl The Instagram reel URL or video URI string.
     * @param videoUri If non-null, the original [Uri] whose read grant must be
     *   forwarded to [MainActivity] via [ClipData].
     */
    private fun navigateToProcessing(reelUrl: String, videoUri: Uri? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_PROCESS_REEL
            putExtra(EXTRA_REEL_URL, reelUrl)
            // Clear the back-stack so the user lands directly in MainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Forward the temporary URI read grant so the downstream activity
            // (and its workers) can access the video file.
            if (videoUri != null) {
                clipData = ClipData.newRawUri("video", videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        startActivity(intent)
        finish()
    }

    // -------------------------------------------------------------------------
    // Error helpers
    // -------------------------------------------------------------------------

    private fun showError(message: String) {
        errorMessage = message
    }

    // -------------------------------------------------------------------------
    // URL extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts an Instagram reel URL from arbitrary text.
     *
     * The shared text from Instagram often contains extra copy around the actual
     * link, so we search within the full string rather than matching the whole thing.
     * Uses the same pattern as [com.reelsplit.core.extensions.isValidInstagramUrl]
     * but in a *find* (not full-match) mode to tolerate surrounding text.
     */
    private fun extractInstagramUrl(text: String): String? {
        val match = INSTAGRAM_FIND_REGEX.find(text)?.value

        // Double-check with the canonical full-match validator
        return match?.takeIf { it.isValidInstagramUrl() }
    }

    // -------------------------------------------------------------------------
    // Android 13+ compat
    // -------------------------------------------------------------------------

    /**
     * Version-safe replacement for [Intent.getParcelableExtra].
     *
     * On API 33+ (Tiramisu) the deprecated overload is replaced by a type-safe
     * variant that requires passing the class token.
     */
    @Suppress("DEPRECATION")
    private inline fun <reified T : android.os.Parcelable> getParcelableExtraCompat(
        intent: Intent,
        name: String
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, T::class.java)
        } else {
            intent.getParcelableExtra(name)
        }
    }

    companion object {
        /** Action used when forwarding a reel URL to [MainActivity]. */
        const val ACTION_PROCESS_REEL = "com.reelsplit.action.PROCESS_REEL"

        /** Intent extra key carrying the Instagram reel URL or video URI string. */
        const val EXTRA_REEL_URL = "com.reelsplit.extra.REEL_URL"

        /**
         * Regex for *finding* an Instagram URL inside arbitrary shared text.
         *
         * Unlike [INSTAGRAM_URL_REGEX][com.reelsplit.core.extensions.isValidInstagramUrl]
         * this is un-anchored so it can match URLs embedded in longer strings.
         * Whitespace-aware groups (`[^#\s]`, `\S`) prevent over-matching into
         * surrounding prose.
         */
        private val INSTAGRAM_FIND_REGEX = Regex(
            """https?://(www\.)?instagram\.com/(reel|reels|p|share/reel)/[\w-]+/?(\?[^#\s]*)?(#\S*)?""",
            RegexOption.IGNORE_CASE
        )
    }
}

// =============================================================================
// Compose UI
// =============================================================================

/**
 * Root content for [ShareReceiverActivity].
 *
 * Shows a loading spinner while the intent is being processed.
 * If an error occurs, an inline error screen replaces the spinner.
 */
@Composable
private fun ShareReceiverContent(
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        if (errorMessage != null) {
            ErrorScreen(
                message = errorMessage,
                onDismiss = onDismiss
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
