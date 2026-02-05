package com.reelsplit.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents application-level errors that can occur during video processing.
 *
 * This sealed class provides a type-safe way to handle different error categories
 * and is designed to work with the kotlin-result library for functional error handling.
 *
 * Usage with kotlin-result:
 * ```
 * import com.github.michaelbull.result.Result
 * import com.github.michaelbull.result.Ok
 * import com.github.michaelbull.result.Err
 *
 * fun downloadVideo(url: String): Result<Video, AppError> {
 *     return try {
 *         // download logic
 *         Ok(video)
 *     } catch (e: IOException) {
 *         Err(AppError.NetworkError("Failed to download: ${e.message}"))
 *     }
 * }
 * ```
 */
@Serializable
sealed class AppError {
    /** Human-readable error message */
    abstract val message: String
    
    /** Whether this error is retryable */
    abstract val isRetryable: Boolean

    /**
     * Network-related errors such as connection failures, timeouts, or server errors.
     *
     * @property message Human-readable error description
     * @property statusCode Optional HTTP status code if applicable
     * @property isRetryable Whether this error can be retried (default: true for network errors)
     */
    @Serializable
    data class NetworkError(
        override val message: String,
        val statusCode: Int? = null,
        override val isRetryable: Boolean = true
    ) : AppError()

    /**
     * Errors that occur during video processing (extraction, splitting, encoding).
     *
     * @property message Human-readable error description
     * @property stage Optional stage where the error occurred
     * @property isRetryable Whether this error can be retried
     */
    @Serializable
    data class ProcessingError(
        override val message: String,
        val stage: ProcessingStage? = null,
        override val isRetryable: Boolean = false
    ) : AppError()

    /**
     * Storage-related errors such as insufficient space, file write failures, or missing files.
     *
     * @property message Human-readable error description
     * @property path Optional file path where the error occurred
     * @property requiredBytes Optional bytes required for the operation
     * @property availableBytes Optional bytes available
     * @property isRetryable Whether this error can be retried
     */
    @Serializable
    data class StorageError(
        override val message: String,
        val path: String? = null,
        val requiredBytes: Long? = null,
        val availableBytes: Long? = null,
        override val isRetryable: Boolean = false
    ) : AppError() {
        val isInsufficientSpace: Boolean
            get() = requiredBytes != null && availableBytes != null && availableBytes < requiredBytes
    }

    /**
     * Permission-related errors when the app lacks necessary permissions.
     *
     * @property message Human-readable error description
     * @property permission The specific Android permission that is missing
     * @property isRetryable Whether this error can be retried (after permission grant)
     */
    @Serializable
    data class PermissionError(
        override val message: String,
        val permission: String? = null,
        override val isRetryable: Boolean = true
    ) : AppError()

    /**
     * Error when the URL format is invalid or unsupported.
     *
     * @property message Human-readable error description
     * @property url The invalid URL
     * @property isRetryable Whether this error can be retried
     */
    @Serializable
    data class InvalidUrlError(
        override val message: String,
        val url: String,
        override val isRetryable: Boolean = false
    ) : AppError()

    /**
     * Generic error for unexpected or uncategorized failures.
     *
     * @property message Human-readable error description
     * @property cause Optional underlying exception class name
     * @property isRetryable Whether this error can be retried
     */
    @Serializable
    data class UnknownError(
        override val message: String = "An unknown error occurred",
        val cause: String? = null,
        override val isRetryable: Boolean = false
    ) : AppError()

    companion object {
        /**
         * Creates an appropriate AppError from a Throwable.
         * The order of checks is important - more specific exceptions should be checked first.
         */
        fun fromThrowable(throwable: Throwable): AppError {
            val message = throwable.message ?: throwable::class.simpleName ?: "Unknown error"
            
            return when (throwable) {
                // Network errors - check these BEFORE IOException since they extend it
                is java.net.UnknownHostException -> NetworkError(
                    message = "Unable to connect to server. Please check your internet connection.",
                    isRetryable = true
                )
                is java.net.SocketTimeoutException -> NetworkError(
                    message = "Connection timed out. Please try again.",
                    isRetryable = true
                )
                is java.net.ConnectException -> NetworkError(
                    message = "Could not connect to server. Please check your internet connection.",
                    isRetryable = true
                )
                is javax.net.ssl.SSLException -> NetworkError(
                    message = "Secure connection failed: $message",
                    isRetryable = true
                )
                // Storage errors
                is java.io.FileNotFoundException -> StorageError(
                    message = "File not found: $message",
                    isRetryable = false
                )
                is java.io.IOException -> StorageError(
                    message = "Storage operation failed: $message",
                    isRetryable = false
                )
                // Permission errors
                is SecurityException -> PermissionError(
                    message = "Permission denied: $message",
                    isRetryable = true
                )
                // URL errors
                is java.net.MalformedURLException -> InvalidUrlError(
                    message = "Invalid URL format: $message",
                    url = "",
                    isRetryable = false
                )
                // Processing errors
                is IllegalStateException -> ProcessingError(
                    message = "Processing failed: $message",
                    isRetryable = false
                )
                is IllegalArgumentException -> ProcessingError(
                    message = "Invalid input: $message",
                    isRetryable = false
                )
                // Fallback
                else -> UnknownError(
                    message = message,
                    cause = throwable::class.qualifiedName,
                    isRetryable = false
                )
            }
        }
    }
}
