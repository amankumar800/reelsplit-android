package com.reelsplit.core.constants

/**
 * Network-related constants for video downloading and API requests.
 */
object NetworkConstants {
    
    // Timeouts (in milliseconds)
    /** Connection timeout for establishing a connection */
    const val CONNECT_TIMEOUT_MS = 30_000L
    
    /** Read timeout for reading data from the server */
    const val READ_TIMEOUT_MS = 60_000L
    
    /** Write timeout for writing data to the server */
    const val WRITE_TIMEOUT_MS = 60_000L
    
    /** Download timeout for large video files (10 minutes) */
    const val DOWNLOAD_TIMEOUT_MS = 600_000L
    
    // Timeouts in seconds for OkHttp
    const val CONNECT_TIMEOUT_SECONDS = CONNECT_TIMEOUT_MS / 1000L
    const val READ_TIMEOUT_SECONDS = READ_TIMEOUT_MS / 1000L
    const val WRITE_TIMEOUT_SECONDS = WRITE_TIMEOUT_MS / 1000L
    const val DOWNLOAD_TIMEOUT_SECONDS = DOWNLOAD_TIMEOUT_MS / 1000L
    
    // Retry Configuration
    /** Maximum number of retry attempts for failed requests */
    const val MAX_RETRY_ATTEMPTS = 3
    
    /** Initial delay between retries in milliseconds */
    const val RETRY_INITIAL_DELAY_MS = 1000L
    
    /** Multiplier for exponential backoff */
    const val RETRY_BACKOFF_MULTIPLIER = 2.0
    
    /** Maximum delay between retries in milliseconds */
    const val RETRY_MAX_DELAY_MS = 30_000L
    
    // User Agents
    /** Default user agent for requests */
    const val USER_AGENT_DEFAULT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    
    /** User agent for Instagram requests - mimics mobile app */
    const val USER_AGENT_INSTAGRAM = "Instagram 300.0.0.0.0 Android (33/13; 420dpi; 1080x2400; Google/google; Pixel 7; panther; panther; en_US; 516620451)"
    
    // HTTP Headers
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_ACCEPT = "Accept"
    const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
    const val HEADER_ACCEPT_ENCODING = "Accept-Encoding"
    const val HEADER_CONNECTION = "Connection"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_RANGE = "Range"
    const val HEADER_REFERER = "Referer"
    
    // Common Header Values
    const val ACCEPT_ALL = "*/*"
    const val ACCEPT_VIDEO = "video/*"
    const val ACCEPT_LANGUAGE_EN = "en-US,en;q=0.9"
    const val ACCEPT_ENCODING_GZIP = "gzip, deflate, br"
    const val CONNECTION_KEEP_ALIVE = "keep-alive"
    const val CONTENT_TYPE_JSON = "application/json"
    
    // Instagram URL Patterns (for substring matching with contains())
    const val INSTAGRAM_REFERER = "https://www.instagram.com/"
    const val INSTAGRAM_BASE_URL = "https://www.instagram.com"
    const val INSTAGRAM_REEL_URL_PATTERN = "instagram.com/reel/"
    const val INSTAGRAM_REELS_URL_PATTERN = "instagram.com/reels/"
    const val INSTAGRAM_P_URL_PATTERN = "instagram.com/p/"
    
    // Download Buffer Size
    /** Buffer size for download streams (8 KB) */
    const val DOWNLOAD_BUFFER_SIZE = 8 * 1024
    
    // Progress Update Interval
    /** Minimum interval between progress updates in milliseconds */
    const val PROGRESS_UPDATE_INTERVAL_MS = 100L
}
