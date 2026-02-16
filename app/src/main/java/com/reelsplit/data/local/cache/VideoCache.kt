package com.reelsplit.data.local.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local file caching for downloaded videos.
 *
 * Handles storing, retrieving, and cleaning up cached video files
 * in the app's cache directory. Mutating file-system operations are
 * synchronized to prevent race conditions when accessed from
 * multiple coroutines or threads (e.g., WorkManager + UI thread).
 *
 * **Note:** [getCacheDir] exposes the raw directory so callers like
 * PRDownloader can use it as a download destination. Those callers are
 * responsible for their own filename safety.
 *
 * Cache directory: `<app_cache>/videos/`
 */
@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir: File = File(context.cacheDir, "videos")

    init {
        ensureCacheDirExists()
    }

    fun getCacheDir(): File = cacheDir

    /**
     * Returns the cached video file for the given [videoId], or null if not cached.
     *
     * @param videoId The unique identifier for the video (used as filename stem).
     */
    fun getCachedVideo(videoId: String): File? {
        val sanitized = sanitizeFileName(videoId) ?: return null
        val file = File(cacheDir, "$sanitized.mp4")
        if (!isInsideCacheDir(file)) return null
        return if (file.exists() && file.isFile) file else null
    }

    /**
     * Returns a [File] handle within the cache directory for the given [fileName].
     *
     * This does NOT create the file; it only provides a path reference for
     * downstream consumers to write to. The cache directory is re-created
     * if the OS has evicted it.
     *
     * @param fileName The desired file name (will be sanitized).
     * @return A file reference inside the cache dir, or null if the name is invalid.
     */
    fun saveToCacheDir(fileName: String): File? {
        val sanitized = sanitizeFileName(fileName) ?: return null
        ensureCacheDirExists()
        val file = File(cacheDir, sanitized)
        if (!isInsideCacheDir(file)) return null
        return file
    }

    /**
     * Returns the total size of all cached files in bytes.
     */
    @Synchronized
    fun getCacheSizeBytes(): Long {
        return cacheDir.listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() }
            ?: 0L
    }

    /**
     * Deletes all cached video files.
     */
    @Synchronized
    fun clearCache() {
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.delete()) {
                Timber.w("Failed to delete cached file: %s", file.name)
            }
        }
    }

    /**
     * Deletes cached files older than [maxAgeMs] milliseconds.
     *
     * @param maxAgeMs Maximum age in milliseconds. Must be positive.
     */
    @Synchronized
    fun deleteOlderThan(maxAgeMs: Long) {
        require(maxAgeMs > 0) { "maxAgeMs must be positive, was $maxAgeMs" }

        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        cacheDir.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoffTime }
            ?.forEach { file ->
                if (!file.delete()) {
                    Timber.w("Failed to delete expired cache file: %s", file.name)
                }
            }
    }

    // ────────────────────────── Internal helpers ──────────────────────────

    /**
     * Ensures the cache directory exists, re-creating it if the OS evicted it.
     */
    private fun ensureCacheDirExists() {
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            if (!created) {
                Timber.e("Failed to create video cache directory: %s", cacheDir.absolutePath)
            }
        }
    }

    /**
     * Defense-in-depth: verifies the resolved [file] is actually inside [cacheDir].
     *
     * Catches edge cases that character-stripping in [sanitizeFileName] might miss
     * (e.g., symlinks, OS-specific path resolution quirks).
     */
    private fun isInsideCacheDir(file: File): Boolean {
        return try {
            file.canonicalPath.startsWith(cacheDir.canonicalPath + File.separator) ||
                file.canonicalPath == cacheDir.canonicalPath
        } catch (e: IOException) {
            Timber.w(e, "Failed to resolve canonical path for: %s", file.path)
            false
        }
    }

    /**
     * Sanitizes a file name to prevent path-traversal attacks and invalid characters.
     *
     * @return The sanitized name, or null if the input is blank or entirely invalid.
     */
    private fun sanitizeFileName(name: String): String? {
        if (name.isBlank()) return null

        // Strip path separators and traversal sequences
        val sanitized = name
            .replace("..", "")
            .replace("/", "")
            .replace("\\", "")
            .replace("\u0000", "")
            .trim()

        // Reject reserved filesystem names
        if (sanitized.isBlank() || sanitized == "." || sanitized == "..") return null

        return sanitized
    }
}
