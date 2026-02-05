package com.reelsplit.core.extensions

/**
 * Extension functions for String operations.
 */

/**
 * Regex pattern for validating Instagram URLs.
 * Matches patterns like:
 * - https://www.instagram.com/reel/ABC123/
 * - https://instagram.com/reel/ABC123
 * - http://www.instagram.com/reels/ABC123/
 * - https://www.instagram.com/p/ABC123/ (posts that may contain videos)
 * - https://www.instagram.com/share/reel/ABC123/ (shared via app share menu)
 */
private val INSTAGRAM_URL_REGEX = Regex(
    pattern = """^https?://(www\.)?instagram\.com/(reel|reels|p|share/reel)/[\w-]+/?(\?[^#]*)?(#.*)?$""",
    option = RegexOption.IGNORE_CASE
)

/**
 * Regex pattern for extracting Reel ID from Instagram URLs.
 * Captures the alphanumeric ID after /reel/, /reels/, /p/, or /share/reel/
 */
private val REEL_ID_REGEX = Regex(
    pattern = """instagram\.com/(?:reel|reels|p|share/reel)/([\w-]+)""",
    option = RegexOption.IGNORE_CASE
)

/**
 * Checks if this string is a valid Instagram URL.
 * Supports reel, reels, post, and share URLs.
 * 
 * @return true if the string matches the Instagram URL pattern, false otherwise.
 */
fun String.isValidInstagramUrl(): Boolean {
    return this.isNotBlank() && INSTAGRAM_URL_REGEX.matches(this.trim())
}

/**
 * Extracts the Reel ID from an Instagram URL.
 * 
 * @return The extracted Reel ID, or null if extraction fails.
 * 
 * Example:
 * - "https://www.instagram.com/reel/ABC123xyz/" -> "ABC123xyz"
 * - "https://instagram.com/p/DEF456/" -> "DEF456"
 * - "https://www.instagram.com/share/reel/XYZ789/" -> "XYZ789"
 */
fun String.extractReelId(): String? {
    if (this.isBlank()) return null
    
    val matchResult = REEL_ID_REGEX.find(this.trim())
    return matchResult?.groupValues?.getOrNull(1)
}

