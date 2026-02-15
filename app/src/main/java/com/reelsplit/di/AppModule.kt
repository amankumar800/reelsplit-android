package com.reelsplit.di

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module providing app-level dependencies.
 * 
 * Provides:
 * - WorkManager instance for background tasks
 * - System services (NotificationManager, ConnectivityManager)
 * - ContentResolver for MediaStore operations
 * - FirebaseAnalytics for event tracking
 * - Coroutine Dispatchers for testability
 * - Application-scoped CoroutineScope
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==================== WorkManager ====================
    
    /**
     * Provides the WorkManager instance for background task scheduling.
     * Used by download and video splitting workers.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    // ==================== System Services ====================

    /**
     * Provides the NotificationManager for displaying download/split progress notifications.
     * Uses core-ktx getSystemService extension for type-safe access.
     */
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return requireNotNull(context.getSystemService<NotificationManager>()) {
            "NotificationManager system service not available"
        }
    }

    /**
     * Provides the ConnectivityManager for network state monitoring.
     * Used to check connectivity before starting downloads.
     */
    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager {
        return requireNotNull(context.getSystemService<ConnectivityManager>()) {
            "ConnectivityManager system service not available"
        }
    }

    /**
     * Provides ContentResolver for MediaStore operations.
     * Used for saving videos to gallery and file operations.
     */
    @Provides
    @Singleton
    fun provideContentResolver(
        @ApplicationContext context: Context
    ): ContentResolver {
        return context.contentResolver
    }

    // ==================== Firebase ====================

    /**
     * Provides the FirebaseAnalytics instance for event tracking.
     * Used by [com.reelsplit.core.analytics.AnalyticsManager].
     */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    // ==================== Coroutine Dispatchers ====================

    /**
     * Provides IO dispatcher for disk and network operations.
     * Injecting dispatchers enables swapping with TestDispatcher in tests.
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides Default dispatcher for CPU-intensive operations like video processing.
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provides Main dispatcher for UI operations.
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    // ==================== Application Scope ====================

    /**
     * Provides an application-scoped CoroutineScope that lives for the entire app lifetime.
     * Uses SupervisorJob so child coroutine failures don't cancel the scope.
     * Uses Default dispatcher for general background work.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + defaultDispatcher)
    }
}
