package com.reelsplit.core.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [UrlParser].
 *
 * Verifies:
 * - All supported Instagram URL patterns (/reel/, /reels/, /p/, /share/reel/)
 * - Query parameter stripping
 * - Extraction from surrounding text (share-intent payloads)
 * - Case-insensitive matching
 * - Negative cases (non-Instagram URLs, empty strings)
 * - Reel ID extraction
 * - [containsInstagramUrl] convenience method
 */
class UrlParserTest {

    // =========================================================================
    // extractInstagramUrl — Positive Cases
    // =========================================================================

    @Test
    fun `extractInstagramUrl with standard reel URL`() {
        val url = "https://www.instagram.com/reel/ABC123def/"
        assertEquals(
            "https://www.instagram.com/reel/ABC123def",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl with reels variant`() {
        val url = "https://www.instagram.com/reels/XYZ789/"
        assertEquals(
            "https://www.instagram.com/reels/XYZ789",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl with post p variant`() {
        val url = "https://www.instagram.com/p/POST_id-123/"
        assertEquals(
            "https://www.instagram.com/p/POST_id-123",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl with share reel variant`() {
        val url = "https://www.instagram.com/share/reel/ShareID99/"
        assertEquals(
            "https://www.instagram.com/share/reel/ShareID99",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl without www prefix`() {
        val url = "https://instagram.com/reel/NoWww123/"
        assertEquals(
            "https://instagram.com/reel/NoWww123",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl with http scheme`() {
        val url = "http://www.instagram.com/reel/HttpId/"
        assertEquals(
            "http://www.instagram.com/reel/HttpId",
            UrlParser.extractInstagramUrl(url)
        )
    }

    @Test
    fun `extractInstagramUrl without trailing slash`() {
        val url = "https://www.instagram.com/reel/NoTrail"
        assertEquals(
            "https://www.instagram.com/reel/NoTrail",
            UrlParser.extractInstagramUrl(url)
        )
    }

    // =========================================================================
    // extractInstagramUrl — Query Parameter Stripping
    // =========================================================================

    @Test
    fun `extractInstagramUrl strips query parameters`() {
        val url = "https://www.instagram.com/reel/ABC123/?igsh=abc123&utm_source=ig"
        val result = UrlParser.extractInstagramUrl(url)
        assertNotNull("Expected URL to be extracted", result)
        // Smart-cast to non-null after assertNotNull
        result!!
        assertFalse("Query params should be stripped", result.contains("?"))
        assertFalse("igsh param should be stripped", result.contains("igsh"))
        assertEquals("https://www.instagram.com/reel/ABC123", result)
    }

    // =========================================================================
    // extractInstagramUrl — Embedded in Text
    // =========================================================================

    @Test
    fun `extractInstagramUrl from share intent text`() {
        val text = "Check out this reel! https://www.instagram.com/reel/ABC123/ 🔥"
        assertEquals(
            "https://www.instagram.com/reel/ABC123",
            UrlParser.extractInstagramUrl(text)
        )
    }

    @Test
    fun `extractInstagramUrl finds first URL in multi-URL text`() {
        val text = """
            First: https://www.instagram.com/reel/First123/
            Second: https://www.instagram.com/reel/Second456/
        """.trimIndent()
        assertEquals(
            "https://www.instagram.com/reel/First123",
            UrlParser.extractInstagramUrl(text)
        )
    }

    // =========================================================================
    // extractInstagramUrl — Case Insensitivity
    // =========================================================================

    @Test
    fun `extractInstagramUrl is case-insensitive for scheme and host`() {
        val url = "HTTPS://WWW.INSTAGRAM.COM/reel/CaseTest/"
        assertNotNull(UrlParser.extractInstagramUrl(url))
    }

    // =========================================================================
    // extractInstagramUrl — Negative Cases
    // =========================================================================

    @Test
    fun `extractInstagramUrl returns null for empty string`() {
        assertNull(UrlParser.extractInstagramUrl(""))
    }

    @Test
    fun `extractInstagramUrl returns null for blank string`() {
        assertNull(UrlParser.extractInstagramUrl("   "))
    }

    @Test
    fun `extractInstagramUrl returns null for non-Instagram URL`() {
        assertNull(UrlParser.extractInstagramUrl("https://www.youtube.com/watch?v=abc"))
    }

    @Test
    fun `extractInstagramUrl returns null for Instagram profile URL`() {
        assertNull(UrlParser.extractInstagramUrl("https://www.instagram.com/username/"))
    }

    @Test
    fun `extractInstagramUrl returns null for plain text without URL`() {
        assertNull(UrlParser.extractInstagramUrl("Just some random text"))
    }

    // =========================================================================
    // extractReelId
    // =========================================================================

    @Test
    fun `extractReelId returns ID from reel URL`() {
        assertEquals(
            "ABC123",
            UrlParser.extractReelId("https://www.instagram.com/reel/ABC123/")
        )
    }

    @Test
    fun `extractReelId returns ID from reels URL`() {
        assertEquals(
            "XYZ789",
            UrlParser.extractReelId("https://www.instagram.com/reels/XYZ789/")
        )
    }

    @Test
    fun `extractReelId returns ID from p URL`() {
        assertEquals(
            "Post_id-1",
            UrlParser.extractReelId("https://www.instagram.com/p/Post_id-1/")
        )
    }

    @Test
    fun `extractReelId returns ID from share reel URL`() {
        assertEquals(
            "ShareABC",
            UrlParser.extractReelId("https://www.instagram.com/share/reel/ShareABC/")
        )
    }

    @Test
    fun `extractReelId returns null for non-Instagram URL`() {
        assertNull(UrlParser.extractReelId("https://www.youtube.com/watch?v=abc"))
    }

    @Test
    fun `extractReelId returns null for empty string`() {
        assertNull(UrlParser.extractReelId(""))
    }

    @Test
    fun `extractReelId handles ID with hyphens and underscores`() {
        assertEquals(
            "A_B-C",
            UrlParser.extractReelId("https://www.instagram.com/reel/A_B-C/")
        )
    }

    // =========================================================================
    // containsInstagramUrl
    // =========================================================================

    @Test
    fun `containsInstagramUrl returns true for text with Instagram URL`() {
        assertTrue(
            UrlParser.containsInstagramUrl(
                "Look at this https://www.instagram.com/reel/ABC123/"
            )
        )
    }

    @Test
    fun `containsInstagramUrl returns true for bare URL`() {
        assertTrue(
            UrlParser.containsInstagramUrl("https://instagram.com/reel/ABC/")
        )
    }

    @Test
    fun `containsInstagramUrl returns false for empty string`() {
        assertFalse(UrlParser.containsInstagramUrl(""))
    }

    @Test
    fun `containsInstagramUrl returns false for non-Instagram URL`() {
        assertFalse(
            UrlParser.containsInstagramUrl("https://www.youtube.com/watch?v=abc")
        )
    }

    @Test
    fun `containsInstagramUrl returns false for plain text`() {
        assertFalse(UrlParser.containsInstagramUrl("No URL here"))
    }
}
