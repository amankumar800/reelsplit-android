package com.reelsplit.di

import javax.inject.Qualifier

/**
 * Qualifier for IO dispatcher - used for disk/network operations.
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

/**
 * Qualifier for Default dispatcher - used for CPU-intensive work (video processing).
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

/**
 * Qualifier for Main dispatcher - used for UI operations.
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@MainDispatcher private val mainDispatcher: CoroutineDispatcher)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher

/**
 * Qualifier for Application-scoped CoroutineScope.
 * Lives for the entire application lifetime.
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@ApplicationScope private val scope: CoroutineScope)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

// ==================== Network Qualifiers ====================

/**
 * Qualifier for the default OkHttpClient configured for general API usage.
 * Uses standard timeouts (30s connect, 60s read/write).
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@DefaultOkHttpClient private val client: OkHttpClient)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultOkHttpClient

/**
 * Qualifier for OkHttpClient configured for video downloading.
 * Uses extended read timeout (10 minutes) for large file transfers.
 * 
 * Usage:
 * ```kotlin
 * @Inject
 * constructor(@DownloadOkHttpClient private val client: OkHttpClient)
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DownloadOkHttpClient

/**
 * Qualifier for HttpLoggingInterceptor configured for download operations.
 * Uses HEADERS level to avoid memory issues with large video bodies.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DownloadLoggingInterceptor
