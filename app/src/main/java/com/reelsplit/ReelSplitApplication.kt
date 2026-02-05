package com.reelsplit

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for ReelSplit.
 * 
 * Responsible for:
 * - Hilt dependency injection initialization (@HiltAndroidApp)
 * - WorkManager configuration with HiltWorkerFactory
 * - Timber logging setup (DebugTree for debug, CrashlyticsTree for production)
 * - Firebase Crashlytics initialization with custom keys
 * - PRDownloader initialization
 * - YoutubeDL async initialization and updates
 */
@HiltAndroidApp
class ReelSplitApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Exception handler for application scope coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Uncaught exception in application scope")
        // Safely record to Crashlytics if available
        runCatching {
            if (!BuildConfig.DEBUG) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
        }
    }

    // Application-scoped coroutine scope for non-critical async initialization
    // Note: This scope is intentionally NOT cancelled in onTerminate() because
    // onTerminate() is never called on real devices - Android kills the process directly.
    // The coroutine will be terminated when the process dies.
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + exceptionHandler
    )

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber FIRST so all subsequent logs work
        initTimber()
        
        // Initialize Firebase Crashlytics (with error handling)
        initFirebaseCrashlytics()
        
        // Initialize PRDownloader synchronously (fast initialization)
        initPRDownloader()
        
        // Initialize YoutubeDL asynchronously to avoid blocking app start
        applicationScope.launch {
            initYoutubeDL()
        }
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a production tree that sends errors to Crashlytics
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun initFirebaseCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Disable Crashlytics collection in debug builds to avoid cluttering reports
            crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            
            // Set custom keys for better crash diagnostics
            crashlytics.setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("android_version", Build.VERSION.SDK_INT)
            
            Timber.d("Firebase Crashlytics initialized")
        } catch (e: Exception) {
            // Log but don't crash if Crashlytics fails to initialize
            Timber.e(e, "Failed to initialize Firebase Crashlytics")
        }
    }

    private fun initPRDownloader() {
        try {
            val config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setReadTimeout(30_000)
                .setConnectTimeout(30_000)
                .build()
            
            PRDownloader.initialize(this, config)
            Timber.d("PRDownloader initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize PRDownloader")
            if (!BuildConfig.DEBUG) {
                runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            }
        }
    }

    private suspend fun initYoutubeDL() {
        try {
            YoutubeDL.getInstance().init(this)
            Timber.d("YoutubeDL initialized successfully")
            
            // Update yt-dlp to ensure Instagram extraction works with latest changes
            updateYoutubeDL()
        } catch (e: YoutubeDLException) {
            Timber.e(e, "Failed to initialize YoutubeDL")
            if (!BuildConfig.DEBUG) {
                runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error initializing YoutubeDL")
            if (!BuildConfig.DEBUG) {
                runCatching { FirebaseCrashlytics.getInstance().recordException(e) }
            }
        }
    }

    private suspend fun updateYoutubeDL() {
        // Timeout after 60 seconds to avoid blocking indefinitely on slow networks
        val result = withTimeoutOrNull(UPDATE_TIMEOUT_MS) {
            try {
                val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(this@ReelSplitApplication)
                Timber.d("YoutubeDL update status: $updateStatus")
                updateStatus
            } catch (e: YoutubeDLException) {
                // Update failure is non-critical, log and continue
                Timber.w(e, "Failed to update YoutubeDL, using existing version")
                null
            }
        }
        
        if (result == null) {
            Timber.w("YoutubeDL update timed out after ${UPDATE_TIMEOUT_MS}ms")
        }
    }

    /**
     * Custom Timber tree that forwards warning and error logs to Firebase Crashlytics.
     * Used in production builds to capture non-fatal errors for debugging.
     * 
     * Note: INFO and below are filtered out to reduce noise in Crashlytics dashboard.
     */
    private class CrashlyticsTree : Timber.Tree() {
        
        // Cache the Crashlytics instance to avoid repeated getInstance() calls
        private val crashlytics: FirebaseCrashlytics? by lazy {
            runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
        }
        
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings and above to Crashlytics to reduce noise
            if (priority < Log.WARN) return
            
            val instance = crashlytics ?: return
            
            // Format: "W/ReelSplit: message" (use default tag if null)
            val formattedTag = tag ?: DEFAULT_TAG
            instance.log("${priorityToString(priority)}/$formattedTag: $message")
            
            if (t != null) {
                instance.recordException(t)
            }
        }
        
        private fun priorityToString(priority: Int): String = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }
    
    companion object {
        private const val DEFAULT_TAG = "ReelSplit"
        private const val UPDATE_TIMEOUT_MS = 60_000L
    }
}
