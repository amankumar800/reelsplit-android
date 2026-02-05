package com.reelsplit.presentation.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.reelsplit.presentation.theme.ReelSplitTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Activity that receives shared content from Instagram and other apps.
 * Appears in the system share menu when sharing reel URLs.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        setContent {
            ReelSplitTheme {
                ShareReceiverScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    Timber.d("Received shared text: $sharedText")
                    processSharedUrl(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                Timber.d("Received view intent: $url")
                processSharedUrl(url)
            }
        }
    }

    private fun processSharedUrl(url: String?) {
        if (url == null) {
            Timber.w("Received null URL")
            finish()
            return
        }

        // Extract Instagram reel URL
        val reelUrl = extractInstagramUrl(url)
        if (reelUrl != null) {
            Timber.d("Processing reel URL: $reelUrl")
            // TODO: Navigate to processing screen or start download worker
        } else {
            Timber.w("Invalid Instagram URL: $url")
            // TODO: Show error toast
            finish()
        }
    }

    private fun extractInstagramUrl(text: String): String? {
        // Match Instagram reel URLs
        val regex = Regex(
            """https?://(www\.)?instagram\.com/(reel|reels|p)/[A-Za-z0-9_-]+/?(\?.*)?"""
        )
        return regex.find(text)?.value
    }
}

@Composable
private fun ShareReceiverScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
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
