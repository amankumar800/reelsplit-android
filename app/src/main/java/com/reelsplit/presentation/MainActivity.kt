package com.reelsplit.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import com.reelsplit.presentation.destinations.ProcessingScreenDestination
import com.reelsplit.presentation.main.MainViewModel
import com.reelsplit.presentation.share.ShareReceiverActivity
import com.reelsplit.presentation.theme.ReelSplitTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Single-activity host for all Compose Destinations screens.
 *
 * Responsibilities:
 * - Installs the Android 12+ SplashScreen API and keeps it visible until
 *   [MainViewModel] signals readiness.
 * - Sets up the [DestinationsNavHost] with the generated `NavGraphs.root`.
 * - Handles share intents forwarded from
 *   [ShareReceiverActivity][com.reelsplit.presentation.share.ShareReceiverActivity]
 *   by navigating to the processing screen.
 * - Triggers an in-app update check on first launch (stub for now).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash visible while one-time startup completes
        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }

        enableEdgeToEdge()

        // Handle the launching intent (cold start from share receiver).
        // This stores the URL in ViewModel state (not a one-time event)
        // so it survives the gap before LaunchedEffect subscribes.
        handleShareIntent(intent)

        // Trigger in-app update check (stub)
        viewModel.checkForUpdates()

        setContent {
            ReelSplitTheme {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Navigate to ProcessingScreen when a pending reel URL becomes
                // available. Uses state observation (not SharedFlow events) so
                // the URL is never lost — even on cold start where
                // handleShareIntent() runs before this LaunchedEffect subscribes.
                LaunchedEffect(uiState.pendingReelUrl) {
                    val url = uiState.pendingReelUrl ?: return@LaunchedEffect

                    Timber.d("Navigating to ProcessingScreen with URL: $url")
                    navController.navigate(
                        ProcessingScreenDestination(url = url)
                    ) {
                        // Prevent stacking multiple processing screens
                        launchSingleTop = true
                    }

                    // Clear the pending URL so we don't navigate again on
                    // the next recomposition or config change.
                    viewModel.onPendingReelUrlConsumed()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        navController = navController
                    )
                }
            }
        }
    }

    /**
     * Handles re-delivery of a share intent while the activity is already
     * running (warm start from share receiver).
     *
     * Calls [setIntent] so the activity's [getIntent] property stays current,
     * then delegates to [handleShareIntent].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    // -------------------------------------------------------------------------
    // Share Intent Handling
    // -------------------------------------------------------------------------

    /**
     * Extracts the reel URL from a share intent and forwards it to
     * [MainViewModel] for navigation.
     *
     * Only processes intents with [ShareReceiverActivity.ACTION_PROCESS_REEL]
     * to avoid accidentally consuming unrelated intents (e.g. the launcher
     * intent on a normal cold start).
     */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != ShareReceiverActivity.ACTION_PROCESS_REEL) return

        val reelUrl = intent.getStringExtra(ShareReceiverActivity.EXTRA_REEL_URL)
        if (reelUrl.isNullOrBlank()) {
            Timber.w("ACTION_PROCESS_REEL received but EXTRA_REEL_URL is missing or blank")
            return
        }

        Timber.d("Share intent received with URL: $reelUrl")
        viewModel.onReelUrlReceived(reelUrl)

        // Clear the action so the same URL isn't re-processed on
        // configuration change (ViewModel survives but onCreate re-runs
        // with the same intent).
        intent.action = null
    }
}
