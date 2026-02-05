package com.reelsplit.core.extensions

import com.reelsplit.core.constants.WhatsAppConstants
import java.io.File

/**
 * Extension functions for File operations.
 */

/**
 * Bytes per megabyte constant for size conversion.
 */
private const val BYTES_PER_MB = 1024.0 * 1024.0

/**
 * Calculates the file size in megabytes.
 * Note: For directories, this returns 0.0 (use directory-specific utilities for folder sizes).
 * 
 * @return The file size in MB as a Double, or 0.0 if the file doesn't exist or is a directory.
 */
fun File.sizeInMB(): Double {
    return if (this.exists() && this.isFile) {
        this.length() / BYTES_PER_MB
    } else {
        0.0
    }
}

/**
 * Deletes the file if it exists.
 * Note: This only works for files, not directories. Use [deleteRecursivelyIfExists] for directories.
 * 
 * @return true if the file was deleted or didn't exist, false if deletion failed or if it's a directory.
 */
fun File.deleteIfExists(): Boolean {
    return when {
        !this.exists() -> true
        this.isFile -> this.delete()
        else -> false // It's a directory, don't delete
    }
}

/**
 * Deletes the file or directory recursively if it exists.
 * Use with caution as this will delete all contents of a directory.
 * 
 * @return true if successfully deleted or didn't exist, false if deletion failed.
 */
fun File.deleteRecursivelyIfExists(): Boolean {
    return if (this.exists()) {
        this.deleteRecursively()
    } else {
        true
    }
}

/**
 * Checks if the file size is within the WhatsApp Status limit.
 * Uses [WhatsAppConstants.MAX_STATUS_SIZE_MB] as the single source of truth.
 * 
 * @return true if the file size is within the limit, false otherwise.
 */
fun File.isWithinWhatsAppLimit(): Boolean {
    return this.sizeInMB() <= WhatsAppConstants.MAX_STATUS_SIZE_MB
}

/**
 * Creates parent directories if they don't exist.
 * 
 * @return true if parent directories exist or were created successfully, false otherwise.
 */
fun File.ensureParentDirsExist(): Boolean {
    val parent = this.parentFile ?: return true
    return parent.exists() || parent.mkdirs()
}

