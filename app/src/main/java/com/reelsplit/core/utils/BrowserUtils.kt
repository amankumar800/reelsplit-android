package com.reelsplit.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.reelsplit.R
import timber.log.Timber

/**
 * Utility object for opening URLs in Chrome Custom Tabs.
 *
 * Chrome Custom Tabs provide an in-app browsing experience that keeps
 * users within the app while displaying web content. The toolbar color
 * is matched to the app's theme for both light and dark modes via
 * explicit per-scheme color configuration.
 */
object BrowserUtils {

    private const val INSTAGRAM_HELP_URL = "https://help.instagram.com/"

    /**
     * Opens the given [url] in a Chrome Custom Tab styled to match the app theme.
     *
     * Toolbar colors are configured per color scheme (light/dark) so the
     * Custom Tabs API automatically applies the correct color without
     * requiring a manual dark-mode check.
     *
     * If no browser is available on the device, the exception is logged
     * and the call is silently ignored.
     *
     * @param context The context used to launch the Custom Tab and resolve theme colors.
     * @param url The URL to open. Must be a valid HTTP or HTTPS URL.
     */
    fun openUrl(context: Context, url: String) {
        if (url.isBlank()) {
            Timber.w("BrowserUtils: openUrl called with blank URL")
            return
        }

        val parsedUri = Uri.parse(url)
        if (parsedUri.scheme == null) {
            Timber.w("BrowserUtils: openUrl called with URL missing scheme: %s", url)
            return
        }

        val lightColorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.primary))
            .build()

        val darkColorParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.dark_primary))
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, lightColorParams)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkColorParams)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()

        try {
            customTabsIntent.launchUrl(context, parsedUri)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "BrowserUtils: No browser available to open URL: %s", url)
        }
    }

    /**
     * Opens the Instagram Help Center in a Chrome Custom Tab.
     *
     * @param context The context used to launch the Custom Tab.
     */
    fun openInstagramHelp(context: Context) {
        openUrl(context, INSTAGRAM_HELP_URL)
    }
}
