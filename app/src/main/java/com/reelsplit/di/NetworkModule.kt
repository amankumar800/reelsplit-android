package com.reelsplit.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.reelsplit.BuildConfig
import com.reelsplit.core.constants.NetworkConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies.
 * 
 * Provides:
 * - Configured OkHttpClient with logging (DEBUG only) and timeouts
 * - Retrofit with Kotlinx Serialization converter
 * - JSON configuration for API responses
 * 
 * Security considerations:
 * - Logging interceptor only enabled in DEBUG builds
 * - HTTPS-only enforced by OkHttp's default settings
 * - Reasonable timeouts to prevent resource exhaustion
 * 
 * @see DefaultOkHttpClient for general API usage
 * @see DownloadOkHttpClient for video download operations
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ==================== JSON Configuration ====================

    /**
     * Provides a configured Json instance for Kotlinx Serialization.
     * 
     * Configuration:
     * - ignoreUnknownKeys: true to gracefully handle API changes
     * - isLenient: true to handle non-standard JSON
     * - encodeDefaults: false to reduce payload size
     * - coerceInputValues: true to handle null values for non-null types
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            coerceInputValues = true
            prettyPrint = BuildConfig.DEBUG
        }
    }

    // ==================== Logging ====================

    /**
     * Provides HttpLoggingInterceptor that only logs in DEBUG builds.
     * In RELEASE builds, logging is disabled for security (prevents credential leakage).
     * Uses BODY level for full request/response logging.
     */
    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    /**
     * Provides HttpLoggingInterceptor for download operations.
     * Uses HEADERS level only to avoid memory issues with large video bodies.
     */
    @Provides
    @DownloadLoggingInterceptor
    fun provideDownloadLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // ==================== OkHttpClient ====================

    /**
     * Provides the default OkHttpClient for general API usage.
     * 
     * Configuration:
     * - Connect timeout: 30 seconds
     * - Read timeout: 60 seconds
     * - Write timeout: 60 seconds
     * - Logging interceptor (DEBUG only)
     * - Follow redirects enabled
     * - Follow SSL redirects enabled (HTTPS-only)
     */
    @Provides
    @Singleton
    @DefaultOkHttpClient
    fun provideDefaultOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provides an OkHttpClient optimized for video downloading.
     * Uses longer timeouts to accommodate large file transfers.
     * 
     * Configuration:
     * - Connect timeout: 30 seconds
     * - Read timeout: 10 minutes (for large video files)
     * - Write timeout: 60 seconds
     * - Minimal logging (HEADERS only in DEBUG) to avoid memory issues with large bodies
     */
    @Provides
    @Singleton
    @DownloadOkHttpClient
    fun provideDownloadOkHttpClient(
        @DownloadLoggingInterceptor downloadLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(downloadLoggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provides an unqualified OkHttpClient for general use.
     * Defaults to the standard configured client.
     * Use @DefaultOkHttpClient or @DownloadOkHttpClient for explicit selection.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @DefaultOkHttpClient defaultClient: OkHttpClient
    ): OkHttpClient = defaultClient

    // ==================== Retrofit ====================

    /**
     * Provides a base Retrofit.Builder configured with Kotlinx Serialization.
     * 
     * Note: Base URL must be set when creating API service interfaces.
     * Use this builder with .baseUrl("your-api-url").build().create(YourService::class.java)
     * 
     * Configuration:
     * - Uses Kotlinx Serialization JSON converter
     * - Uses the default OkHttpClient
     */
    @Provides
    @Singleton
    fun provideRetrofitBuilder(
        @DefaultOkHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit.Builder {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
    }
}
