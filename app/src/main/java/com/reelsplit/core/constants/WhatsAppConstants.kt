package com.reelsplit.core.constants

/**
 * WhatsApp Status-related constants for video splitting.
 * These limits ensure compatibility with WhatsApp Status sharing.
 */
object WhatsAppConstants {
    
    // Duration Limits
    /** Maximum duration for WhatsApp Status in seconds */
    const val MAX_STATUS_DURATION_SECONDS = 90
    
    /** Maximum duration in milliseconds for Media3 operations */
    const val MAX_STATUS_DURATION_MS = MAX_STATUS_DURATION_SECONDS * 1000L
    
    // Size Limits
    /** Maximum file size for WhatsApp Status in bytes (16MB) */
    const val MAX_STATUS_SIZE_BYTES = 16L * 1024 * 1024
    
    /** Maximum file size in MB for display purposes */
    const val MAX_STATUS_SIZE_MB = 16
    
    // Video Encoding Requirements
    /** Required video codec for WhatsApp compatibility */
    const val VIDEO_CODEC = "video/avc" // H.264
    
    /** Required audio codec for WhatsApp compatibility */
    const val AUDIO_CODEC = "audio/mp4a-latm" // AAC
    
    /** Recommended video bitrate for quality/size balance (4 Mbps) */
    const val RECOMMENDED_VIDEO_BITRATE = 4_000_000
    
    /** Recommended audio bitrate (128 kbps) */
    const val RECOMMENDED_AUDIO_BITRATE = 128_000
    
    /** Audio sample rate (44.1 kHz) */
    const val AUDIO_SAMPLE_RATE = 44100
    
    /** Audio channel count (stereo) */
    const val AUDIO_CHANNEL_COUNT = 2
    
    /** Recommended video frame rate (30 fps) */
    const val RECOMMENDED_FRAME_RATE = 30
    
    // Package Names
    /** WhatsApp regular version package name */
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    
    /** WhatsApp Business version package name */
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    
    // Safety Margins
    /** 
     * Safety margin for duration (in seconds) to account for encoding variations.
     * Actual split duration = MAX_STATUS_DURATION_SECONDS - DURATION_SAFETY_MARGIN
     */
    const val DURATION_SAFETY_MARGIN_SECONDS = 1
    
    /** 
     * Safety margin for file size (in bytes) to ensure we stay under the limit.
     * ~500KB buffer
     */
    const val SIZE_SAFETY_MARGIN_BYTES = 500 * 1024L
    
    /** 
     * Effective max duration after applying safety margin.
     */
    const val EFFECTIVE_MAX_DURATION_SECONDS = MAX_STATUS_DURATION_SECONDS - DURATION_SAFETY_MARGIN_SECONDS
    
    /** 
     * Effective max duration in milliseconds for Media3 operations.
     */
    const val EFFECTIVE_MAX_DURATION_MS = EFFECTIVE_MAX_DURATION_SECONDS * 1000L
    
    /**
     * Effective max size after applying safety margin.
     */
    const val EFFECTIVE_MAX_SIZE_BYTES = MAX_STATUS_SIZE_BYTES - SIZE_SAFETY_MARGIN_BYTES
}
