package com.reelsplit.core.constants

/**
 * Application-wide constants for ReelSplit.
 */
object AppConstants {
    
    // App Information
    const val APP_NAME = "ReelSplit"
    const val APP_VERSION = "1.0.0"
    const val APP_VERSION_CODE = 1
    
    // File Naming
    const val FILE_PREFIX = "reelsplit_"
    const val SPLIT_FILE_PREFIX = "split_"
    const val TEMP_FILE_PREFIX = "temp_"
    
    // File Extensions
    const val VIDEO_EXTENSION = ".mp4"
    const val THUMBNAIL_EXTENSION = ".jpg"
    
    // Directory Names
    const val DOWNLOAD_DIRECTORY = "ReelSplit"
    const val SPLIT_DIRECTORY = "Splits"
    const val CACHE_DIRECTORY = "Cache"
    const val THUMBNAILS_DIRECTORY = "Thumbnails"
    
    // MIME Types
    const val VIDEO_MIME_TYPE = "video/mp4"
    const val IMAGE_MIME_TYPE = "image/jpeg"
    
    // WorkManager Tags
    const val WORK_TAG_DOWNLOAD = "download_work"
    const val WORK_TAG_SPLIT = "split_work"
    const val WORK_TAG_CLEANUP = "cleanup_work"
    
    // Notification Channels (IDs)
    const val NOTIFICATION_CHANNEL_DOWNLOAD = "download_channel"
    const val NOTIFICATION_CHANNEL_SPLIT = "split_channel"
    const val NOTIFICATION_CHANNEL_GENERAL = "general_channel"
    
    // Notification Channel Names (User-visible in Settings)
    const val NOTIFICATION_CHANNEL_DOWNLOAD_NAME = "Downloads"
    const val NOTIFICATION_CHANNEL_SPLIT_NAME = "Video Splitting"
    const val NOTIFICATION_CHANNEL_GENERAL_NAME = "General"
    
    // Notification Channel Descriptions (User-visible in Settings)
    const val NOTIFICATION_CHANNEL_DOWNLOAD_DESC = "Progress notifications for video downloads"
    const val NOTIFICATION_CHANNEL_SPLIT_DESC = "Progress notifications for video splitting"
    const val NOTIFICATION_CHANNEL_GENERAL_DESC = "General app notifications"
    
    // Notification IDs
    const val NOTIFICATION_ID_DOWNLOAD = 1001
    const val NOTIFICATION_ID_SPLIT = 1002
    const val NOTIFICATION_ID_FOREGROUND = 1003
    
    // Database
    const val DATABASE_NAME = "reelsplit_database"
    const val DATABASE_VERSION = 1
    
    // Cache Settings
    const val MAX_CACHE_SIZE_MB = 500L
    const val CACHE_CLEANUP_THRESHOLD_DAYS = 7
}
