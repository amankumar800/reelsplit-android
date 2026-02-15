package com.reelsplit.core.utils

/**
 * Utility object for parsing and extracting Instagram reel URLs from shared text.
 *
 * Handles the following URL patterns (case-insensitive):
 * - `https://www.instagram.com/reel/<id>/`
 * - `https://instagram.com/reels/<id>/`
 * - `https://www.instagram.com/p/<id>/`
 * - `https://www.instagram.com/share/reel/<id>/`
 *
 * Query parameters (e.g. `?igsh=...`) are intentionally stripped from extracted URLs
 * to produce clean, canonical URLs suitable for video extraction.
 */
object UrlParser {

    /**
     * Matches Instagram content URLs including the `/share/` variant used in
     * modern Instagram share intents on Android.
     *
     * - Case-insensitive so that `HTTPS://`, `Instagram.com`, etc. are accepted
     *   (scheme and host are case-insensitive per RFC 3986).
     * - Longer alternative `reels` is listed before `reel` to avoid an
     *   unnecessary backtracking step in the regex engine.
     *
     * Capture group 1: the reel/post ID (case-preserved from original URL).
     */
    private val INSTAGRAM_URL_REGEX = Regex(
        """https?://(?:www\.)?instagram\.com/(?:share/)?(?:reels|reel|p)/([A-Za-z0-9_-]+)/?""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Extracts the first Instagram URL found in the given [text].
     *
     * The returned URL is normalized (trailing slash removed) and does not
     * include query parameters or fragments, making it suitable for direct
     * use with video extraction libraries.
     *
     * @param text The shared text that may contain an Instagram URL.
     * @return The matched Instagram URL, or `null` if none is found.
     */
    fun extractInstagramUrl(text: String): String? {
        return INSTAGRAM_URL_REGEX.find(text)?.value?.trimEnd('/')
    }

    /**
     * Extracts the reel/post ID from an Instagram [url].
     *
     * For example, given `https://www.instagram.com/reel/ABC123/`,
     * this returns `"ABC123"`.
     *
     * @param url The Instagram URL to extract the ID from.
     * @return The reel/post ID, or `null` if the URL doesn't match.
     */
    fun extractReelId(url: String): String? {
        return INSTAGRAM_URL_REGEX.find(url)?.groupValues?.get(1)
    }

    /**
     * Checks whether the given [text] contains a valid Instagram URL.
     *
     * Note: this performs a **substring** match — it returns `true` even if
     * [text] contains additional content surrounding the URL (which is typical
     * for text received from a share intent).
     *
     * @param text The text to check.
     * @return `true` if the text contains an Instagram URL, `false` otherwise.
     */
    fun containsInstagramUrl(text: String): Boolean {
        return INSTAGRAM_URL_REGEX.containsMatchIn(text)
    }
}
