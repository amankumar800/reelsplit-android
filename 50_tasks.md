# ReelSplit - 58 Implementation Tasks

> **How to Use**: Copy-paste any task (or range of tasks) into a new chat. Each task contains:
> - Full project context
> - Specific implementation scope
> - Verification steps
> - Files to create/modify

---

## 📋 Project Context (Include with EVERY task)

**Project**: ReelSplit - Android app that appears in Instagram's Share menu, downloads reels, splits them into 90-second parts (WhatsApp Status limit), and allows one-tap sharing to WhatsApp.

**Architecture**: Clean Architecture + MVVM + Hilt DI

**Key Libraries**: Media3 Transformer (video splitting), youtubedl-android (video extraction), PRDownloader, WorkManager, Jetpack Compose, Room, Firebase Crashlytics/Analytics, Compose Destinations, kotlin-result

**Package**: `com.reelsplit`

**WhatsApp Status Limits**: 90 seconds, 16MB max, MP4 (H.264) format

---

# PHASE 1: Project Foundation (Tasks 1-8)

---

## Task 1: Create New Android Project & Configure Gradle

### Context
This is the first task for the ReelSplit Android app. We need to create the project structure and configure all Gradle files with the required dependencies.

### Scope
1. Create new Android project with package `com.reelsplit`
2. Configure `build.gradle.kts` (project-level) with all plugins
3. Configure `settings.gradle.kts` with JitPack repository
4. Configure `app/build.gradle.kts` with all dependencies

### Files to Create
- `build.gradle.kts` (project-level)
- `settings.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml` (version catalog)

### Key Dependencies
- Kotlin 1.9.22, compileSdk 34, minSdk 24
- Hilt 2.50, Compose BOM 2024.02.00, Media3 1.2.1
- Firebase, WorkManager, Room, Paging 3
- youtubedl-android, PRDownloader, Lottie, Timber

### Verification
- [ ] Project syncs without errors
- [ ] All dependencies resolve correctly
- [ ] JitPack repository is configured

---

## Task 2: Create Project Directory Structure

### Context
ReelSplit follows Clean Architecture with MVVM. We need to create the complete package structure with all directories.

### Scope
Create directory structure under `app/src/main/java/com/reelsplit/`:

```
ReelSplitApp.kt
di/
  AppModule.kt, NetworkModule.kt, RepositoryModule.kt, UseCaseModule.kt, DispatcherModule.kt
core/
  base/, error/, extensions/, constants/, utils/
domain/
  model/, repository/, usecase/
data/
  repository/, remote/api/, remote/dto/, remote/mapper/, local/datastore/, local/cache/, processing/
presentation/
  MainActivity.kt, navigation/, share/, processing/, result/, home/, main/, history/, components/, theme/
sharing/
```

Also create test directories:
- `app/src/test/java/com/reelsplit/`
- `app/src/androidTest/java/com/reelsplit/`

### Verification
- [ ] All directories created
- [ ] Package structure matches Clean Architecture
- [ ] Test directories exist

---

## Task 3: Create Application Class with Hilt

### Context
The Application class is the entry point for Hilt DI and WorkManager configuration.

### Scope
Create `ReelSplitApp.kt` with:
- `@HiltAndroidApp` annotation
- WorkManager configuration with `HiltWorkerFactory`
- Timber initialization (debug only)
- Firebase Crashlytics custom keys

### Files to Create
- `ReelSplitApp.kt`

### Code Reference
```kotlin
@HiltAndroidApp
class ReelSplitApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Firebase Crashlytics setup
    }
}
```

### Verification
- [ ] App compiles with @HiltAndroidApp
- [ ] Timber logs in debug build
- [ ] WorkManager configured with Hilt

---

## Task 4: Create Core Constants Classes

### Context
Define all app-wide constants including WhatsApp limits, network timeouts, and app configuration.

### Scope
Create constant files in `core/constants/`:

1. **AppConstants.kt** - App name, version, file prefixes
2. **WhatsAppConstants.kt** - Duration (90s), size (16MB), package name
3. **NetworkConstants.kt** - Timeouts, headers, user agents

### Files to Create
- `core/constants/AppConstants.kt`
- `core/constants/WhatsAppConstants.kt`
- `core/constants/NetworkConstants.kt`

### Verification
- [ ] Constants are accessible project-wide
- [ ] WhatsApp limits correctly defined (90s, 16MB)

---

## Task 5: Create Domain Models

### Context
Domain models are the core entities used throughout the app, independent of any data source.

### Scope
Create models in `domain/model/`:

1. **Video.kt** - id, sourceUrl, localPath, duration, fileSize, createdAt, status
2. **VideoSegment.kt** - partNumber, filePath, durationSeconds, fileSize
3. **DownloadProgress.kt** - Queued, Downloading(percent), Completed(filePath), Failed(message)
4. **ProcessingState.kt** - Idle, Extracting, Downloading, Splitting, Complete, Error
5. **AppError.kt** - NetworkError, ProcessingError, StorageError, PermissionError, UnknownError

### Files to Create
- `domain/model/Video.kt`
- `domain/model/VideoSegment.kt`
- `domain/model/DownloadProgress.kt`
- `domain/model/ProcessingState.kt`
- `domain/model/AppError.kt`

### Code Reference (AppError)
```kotlin
sealed class AppError(open val message: String) {
    data class NetworkError(override val message: String) : AppError(message)
    data class ProcessingError(override val message: String) : AppError(message)
    data class StorageError(override val message: String) : AppError(message)
    data class PermissionError(override val message: String) : AppError(message)
    data class UnknownError(override val message: String = "An unknown error occurred") : AppError(message)
}
```

### Verification
- [ ] All models are data classes or sealed classes
- [ ] Use `@Serializable` annotation for JSON serialization
- [ ] AppError uses kotlin-result library pattern

---

## Task 6: Create Domain Repository Interfaces

### Context
Repository interfaces define contracts between domain and data layers, enabling testability.

### Scope
Create repository interfaces in `domain/repository/`:

1. **VideoRepository.kt**
   - `extractVideoUrl(instagramUrl: String): Flow<Result<String, AppError>>`
   - `downloadVideo(url: String, fileName: String): Flow<DownloadProgress>`
   - `splitVideo(inputPath: String): Result<List<VideoSegment>, AppError>`
   - `getVideoById(id: String): Flow<Video?>`
   - `getAllVideos(): Flow<List<Video>>`
   - `saveVideo(video: Video)`
   - `deleteVideo(id: String)`

2. **PreferencesRepository.kt**
   - `getShareCount(): Int`
   - `incrementShareCount()`
   - `getLastReviewPromptTime(): Long`
   - `setLastReviewPromptTime(time: Long)`
   - `loadSettings()`

### Files to Create
- `domain/repository/VideoRepository.kt`
- `domain/repository/PreferencesRepository.kt`

### Verification
- [ ] Interfaces use `Result<T, AppError>` from kotlin-result
- [ ] All operations return Flow or suspend functions
- [ ] No data layer dependencies in domain

---

## Task 7: Create Core Extension Functions

### Context
Extension functions reduce boilerplate and improve code readability across the app.

### Scope
Create extensions in `core/extensions/`:

1. **ContextExtensions.kt**
   - `isDarkMode(): Boolean`
   - `showToast(message: String)`
   - `getColorCompat(colorRes: Int): Int`

2. **FlowExtensions.kt**
   - `Flow<T>.collectIn(scope, action)`
   - `Flow<T>.stateIn(scope, initialValue)`

3. **StringExtensions.kt**
   - `String.isValidInstagramUrl(): Boolean`
   - `String.extractReelId(): String?`

4. **FileExtensions.kt**
   - `File.sizeInMB(): Double`
   - `File.deleteIfExists(): Boolean`

### Files to Create
- `core/extensions/ContextExtensions.kt`
- `core/extensions/FlowExtensions.kt`
- `core/extensions/StringExtensions.kt`
- `core/extensions/FileExtensions.kt`

### Verification
- [ ] Extensions are importable project-wide
- [ ] `isDarkMode()` correctly checks UI_MODE_NIGHT_MASK

---

## Task 8: Create Base ViewModel & UI State Classes

### Context
Base classes provide common functionality for all ViewModels and standardize UI state handling.

### Scope
Create base classes in `core/base/`:

1. **BaseViewModel.kt**
   - Protected MutableStateFlow for UI state
   - Error handling with LiveData
   - Loading state management

2. **UiState.kt**
   - Generic sealed class: Idle, Loading, Success<T>, Error(message)

### Files to Create
- `core/base/BaseViewModel.kt`
- `core/base/UiState.kt`

### Code Reference
```kotlin
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### Verification
- [ ] BaseViewModel can be extended by feature ViewModels
- [ ] UiState is generic and reusable

---

# PHASE 2: Dependency Injection (Tasks 9-13)

---

## Task 9: Create AppModule for Hilt

### Context
AppModule provides app-level dependencies like Context, Application, and system services.

### Scope
Create `di/AppModule.kt` with:
- `@Singleton` application context
- WorkManager instance
- System services (NotificationManager, etc.)

### Files to Create
- `di/AppModule.kt`

### Code Reference
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
```

### Verification
- [ ] Module installs in SingletonComponent
- [ ] WorkManager is injectable

---

## Task 10: Create NetworkModule for Hilt

### Context
NetworkModule provides OkHttp, Retrofit, and networking dependencies.

### Scope
Create `di/NetworkModule.kt` with:
- OkHttpClient with logging interceptor
- Retrofit with Kotlinx Serialization converter
- Network timeout configurations

### Files to Create
- `di/NetworkModule.kt`

### Key Configurations
- Connect timeout: 30 seconds
- Read timeout: 60 seconds
- Logging: DEBUG only
- HTTPS-only for security

### Verification
- [ ] OkHttp client is injectable
- [ ] Logging only in debug builds
- [ ] Kotlinx Serialization converter used (not Gson)

---

## Task 11: Create DatabaseModule for Hilt

### Context
DatabaseModule provides Room database and DAO instances.

### Scope
Create `di/DatabaseModule.kt` with:
- Room database builder
- VideoDao provider

### Files to Create
- `di/DatabaseModule.kt`

### Code Reference
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "reelsplit_database"
        ).build()
    }
    
    @Provides
    fun provideVideoDao(database: AppDatabase): VideoDao = database.videoDao()
}
```

### Verification
- [ ] Database is singleton
- [ ] DAO is injectable

---

## Task 12: Create RepositoryModule for Hilt

### Context
RepositoryModule binds repository interfaces to their implementations.

### Scope
Create `di/RepositoryModule.kt` with:
- `@Binds` for VideoRepository → VideoRepositoryImpl
- `@Binds` for PreferencesRepository → PreferencesRepositoryImpl

### Files to Create
- `di/RepositoryModule.kt`

### Code Reference
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository
    
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
```

### Verification
- [ ] Interfaces bound to implementations
- [ ] Use `@Binds` (not `@Provides`) for interface binding

---

## Task 13: Create DispatcherModule for Hilt

### Context
DispatcherModule provides Coroutine dispatchers for testing flexibility.

### Scope
Create `di/DispatcherModule.kt` with:
- IO Dispatcher
- Default Dispatcher
- Main Dispatcher

### Files to Create
- `di/DispatcherModule.kt`

### Code Reference
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
```

### Verification
- [ ] Qualifiers defined for each dispatcher
- [ ] Dispatchers are injectable with qualifiers

---

# PHASE 3: Data Layer (Tasks 14-21)

---

## Task 14: Create Room Database & VideoEntity

### Context
Room provides local caching for video history and offline access.

### Scope
Create Room components in `data/local/`:

1. **VideoEntity.kt** - Room entity matching Video domain model
2. **VideoDao.kt** - Data access object with CRUD operations
3. **AppDatabase.kt** - Room database class

### Files to Create
- `data/local/entity/VideoEntity.kt`
- `data/local/dao/VideoDao.kt`
- `data/local/AppDatabase.kt`

### Key DAO Methods
- `getAllVideos(): Flow<List<VideoEntity>>`
- `getVideoById(id: String): VideoEntity?`
- `insertVideo(video: VideoEntity)`
- `deleteVideo(id: String)`
- `updateStatus(id: String, status: String)`
- `getVideosPage(limit: Int, offset: Int): List<VideoEntity>` (for Paging 3)

### Verification
- [ ] Entity has `@Entity` and `@PrimaryKey` annotations
- [ ] DAO uses Flow for reactive queries
- [ ] `getVideosPage()` exists for Paging 3

---

## Task 15: Create VideoMapper (DTO → Domain)

### Context
Mappers convert between data layer DTOs/entities and domain models.

### Scope
Create `data/remote/mapper/VideoMapper.kt` with:
- `VideoEntity.toDomain(): Video`
- `Video.toEntity(): VideoEntity`
- Extension functions for lists

### Files to Create
- `data/remote/mapper/VideoMapper.kt`

### Verification
- [ ] Bidirectional mapping works
- [ ] List extensions exist

---

## Task 16: Create PreferencesDataStore

### Context
DataStore replaces SharedPreferences for type-safe preference storage.

### Scope
Create `data/local/datastore/PreferencesDataStore.kt` with:
- Share count tracking
- Last review prompt time
- App settings

### Files to Create
- `data/local/datastore/PreferencesDataStore.kt`

### Key Methods
- `getShareCount(): Flow<Int>`
- `incrementShareCount()`
- `getLastReviewPromptTime(): Flow<Long>`
- `setLastReviewPromptTime(time: Long)`

### Verification
- [ ] Uses `dataStore: DataStore<Preferences>`
- [ ] All operations are suspend functions or Flows

---

## Task 17: Create VideoExtractor (youtubedl-android)

### Context
VideoExtractor uses youtubedl-android to extract video URLs from Instagram.

### Scope
Create `data/processing/VideoExtractor.kt` with:
- YoutubeDL initialization (once per app lifecycle)
- `extractVideoUrl(instagramUrl: String): Result<String, AppError>`
- Options: best mp4, no playlist

### Files to Create
- `data/processing/VideoExtractor.kt`

### Code Reference
```kotlin
class VideoExtractor @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun extractVideoUrl(instagramUrl: String): Result<String, AppError> = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().init(context)
            val request = YoutubeDLRequest(instagramUrl).apply {
                addOption("-f", "best[ext=mp4]")
                addOption("--no-playlist")
            }
            YoutubeDL.getInstance().getInfo(request).url
        }.mapError { e ->
            AppError.NetworkError(e.message ?: "Failed to extract video URL")
        }
    }
}
```

### Verification
- [ ] Uses kotlin-result `Result<T, AppError>`
- [ ] YoutubeDL initialized before use
- [ ] Error mapping works

---

## Task 18: Create VideoDownloadManager (PRDownloader)

### Context
VideoDownloadManager wraps PRDownloader for HTTP downloads with progress.

### Scope
Create `data/processing/VideoDownloadManager.kt` with:
- PRDownloader initialization
- `downloadVideo(url, fileName, onProgress, onComplete, onError): Int`
- `cancelDownload(downloadId: Int)`

### Files to Create
- `data/processing/VideoDownloadManager.kt`

### Verification
- [ ] Returns download ID for cancellation
- [ ] Progress callback reports percentage
- [ ] HTTPS-only recommended

---

## Task 19: Create VideoDownloadWorker (WorkManager)

### Context
WorkManager ensures downloads complete even if the app is closed.

### Scope
Create `data/worker/VideoDownloadWorker.kt` with:
- `@HiltWorker` annotation
- `doWork()` with download logic
- Progress notification with `setForeground()`
- Retry with exponential backoff

### Files to Create
- `data/worker/VideoDownloadWorker.kt`

### Code Reference
```kotlin
@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val videoDownloader: VideoDownloadManager
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val videoUrl = inputData.getString("video_url") ?: return Result.failure()
        setForeground(createForegroundInfo())
        // Download logic with retry
    }
}
```

### Verification
- [ ] Uses `@HiltWorker` and `@AssistedInject`
- [ ] Shows foreground notification
- [ ] Retries up to 3 times

---

## Task 20: Create VideoSplitter (Media3 Transformer)

### Context
Media3 Transformer splits videos into 90-second segments using hardware acceleration.

### Scope
Create `data/processing/VideoSplitter.kt` with:
- `splitVideo(inputPath, segmentDurationMs): SplitResult`
- Video duration detection with MediaMetadataRetriever
- Clipping configuration for each segment
- Transformer.start() on Main thread

### Files to Create
- `data/processing/VideoSplitter.kt`

### Key Details
- WHATSAPP_STATUS_DURATION_MS = 90_000L
- WHATSAPP_STATUS_MAX_SIZE_MB = 16
- Output files: `part_001.mp4`, `part_002.mp4`, etc.
- Must call `transformer.start()` on Main thread

### Verification
- [ ] Short videos (<90s) are not split
- [ ] Long videos are split into multiple parts
- [ ] Each part is under 90 seconds
- [ ] Uses `withContext(Dispatchers.Main)` for transformer

---

## Task 21: Create VideoRepositoryImpl

### Context
VideoRepositoryImpl implements the VideoRepository interface using data sources.

### Scope
Create `data/repository/VideoRepositoryImpl.kt` implementing:
- All VideoRepository interface methods
- Coordination between remote, local, and processing sources
- Proper error handling with Result types

### Files to Create
- `data/repository/VideoRepositoryImpl.kt`

### Dependencies
- VideoExtractor
- VideoDownloadManager
- VideoSplitter
- VideoDao
- VideoMapper

### Verification
- [ ] Implements all VideoRepository methods
- [ ] Properly injects dependencies with Hilt
- [ ] Uses Flow for reactive operations

---

# PHASE 4: Domain Use Cases (Tasks 22-26)

---

## Task 22: Create ExtractVideoUrlUseCase

### Context
Use cases encapsulate single business operations and are called by ViewModels.

### Scope
Create `domain/usecase/ExtractVideoUrlUseCase.kt`:
- Injects VideoRepository
- Single `invoke(instagramUrl)` operator function
- Returns `Result<String, AppError>`

### Files to Create
- `domain/usecase/ExtractVideoUrlUseCase.kt`

### Code Pattern
```kotlin
class ExtractVideoUrlUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(instagramUrl: String): Result<String, AppError> {
        return videoRepository.extractVideoUrl(instagramUrl)
    }
}
```

### Verification
- [ ] Uses `operator fun invoke()` pattern
- [ ] Returns kotlin-result Result type

---

## Task 23: Create DownloadVideoUseCase

### Context
DownloadVideoUseCase handles video download with WorkManager.

### Scope
Create `domain/usecase/DownloadVideoUseCase.kt`:
- Injects WorkManager
- Creates OneTimeWorkRequest with constraints
- Returns `Flow<DownloadProgress>`
- Maps WorkInfo states to domain model

### Files to Create
- `domain/usecase/DownloadVideoUseCase.kt`

### Key Features
- NetworkType.CONNECTED constraint
- RequiresBatteryNotLow constraint
- Exponential backoff
- Flow of DownloadProgress (Queued, Downloading, Completed, Failed)

### Verification
- [ ] WorkManager request is properly configured
- [ ] WorkInfo maps to DownloadProgress correctly

---

## Task 24: Create SplitVideoUseCase

### Context
SplitVideoUseCase handles video splitting into WhatsApp-compatible segments.

### Scope
Create `domain/usecase/SplitVideoUseCase.kt`:
- Injects VideoRepository
- Validates file exists before splitting
- Returns `Result<List<VideoSegment>, AppError>`

### Files to Create
- `domain/usecase/SplitVideoUseCase.kt`

### Verification
- [ ] Validates input file exists
- [ ] Returns list of VideoSegments on success

---

## Task 25: Create ShareToWhatsAppUseCase

### Context
ShareToWhatsAppUseCase handles sharing video segments to WhatsApp Status.

### Scope
Create `domain/usecase/ShareToWhatsAppUseCase.kt`:
- Injects WhatsAppSharer
- Checks if WhatsApp is installed
- Returns `Result<Unit, AppError>`

### Files to Create
- `domain/usecase/ShareToWhatsAppUseCase.kt`

### Verification
- [ ] Returns error if WhatsApp not installed
- [ ] Launches WhatsApp Status intent

---

## Task 26: Create GetVideoInfoUseCase

### Context
GetVideoInfoUseCase retrieves video metadata from the repository.

### Scope
Create `domain/usecase/GetVideoInfoUseCase.kt`:
- Injects VideoRepository
- Gets video by ID or from URL
- Returns `Flow<Video?>`

### Files to Create
- `domain/usecase/GetVideoInfoUseCase.kt`

### Verification
- [ ] Returns Flow for reactive updates
- [ ] Handles null case for missing videos

---

# PHASE 5: Presentation - Theme & Components (Tasks 27-32)

---

## Task 27: Create Material 3 Theme

### Context
Define the app's visual identity with Material 3 theming.

### Scope
Create theme files in `presentation/theme/`:

1. **Color.kt** - Light and dark color schemes
2. **Typography.kt** - Text styles
3. **Shape.kt** - Shape definitions
4. **Theme.kt** - ReelSplitTheme composable

### Files to Create
- `presentation/theme/Color.kt`
- `presentation/theme/Typography.kt`
- `presentation/theme/Shape.kt`
- `presentation/theme/Theme.kt`

### Key Colors
- Primary: Instagram-inspired gradient colors
- Surface: Light/dark adaptive
- Error: Red for error states

### Verification
- [ ] Theme supports light and dark mode
- [ ] Dynamic color support for Android 12+

---

## Task 28: Create ProgressIndicator Component

### Context
Custom progress indicator for download and processing states.

### Scope
Create `presentation/components/ProgressIndicator.kt`:
- Circular progress with percentage
- Linear progress bar option
- Animated transitions
- Lottie animation support

### Files to Create
- `presentation/components/ProgressIndicator.kt`

### Verification
- [ ] Shows percentage text
- [ ] Smooth animations
- [ ] Uses Material 3 colors

---

## Task 29: Create VideoPreview Component

### Context
VideoPreview displays video thumbnails with playback controls.

### Scope
Create `presentation/components/VideoPreview.kt`:
- Thumbnail display with Coil
- Play button overlay
- Duration display
- Shimmer loading effect

### Files to Create
- `presentation/components/VideoPreview.kt`

### Verification
- [ ] Uses Coil for video thumbnails
- [ ] Shimmer effect while loading
- [ ] Play button overlay

---

## Task 30: Create VideoSegmentCard Component

### Context
VideoSegmentCard displays individual video segments with share actions.

### Scope
Create `presentation/components/VideoSegmentCard.kt`:
- Part number display (Part 1, Part 2, etc.)
- Duration text
- File size
- "Share to Status" button
- "Share to Chat" button

### Files to Create
- `presentation/components/VideoSegmentCard.kt`

### Verification
- [ ] Shows part number prominently
- [ ] Duration formatted (MM:SS)
- [ ] Two share buttons

---

## Task 31: Create ErrorDialog Component

### Context
ErrorDialog displays error messages with retry option.

### Scope
Create `presentation/components/ErrorDialog.kt`:
- Error icon
- Error message text
- Retry button
- Dismiss button

### Files to Create
- `presentation/components/ErrorDialog.kt`

### Verification
- [ ] Centers on screen
- [ ] Retry callback works
- [ ] Uses Material 3 dialog

---

## Task 32: Create LoadingOverlay Component

### Context
LoadingOverlay covers the screen during blocking operations.

### Scope
Create `presentation/components/LoadingOverlay.kt`:
- Semi-transparent background
- Lottie loading animation
- Optional message text
- Blocks touch events

### Files to Create
- `presentation/components/LoadingOverlay.kt`

### Verification
- [ ] Blocks touch events underneath
- [ ] Uses Lottie animation
- [ ] Customizable message

---

# PHASE 6: Presentation - Screens (Tasks 33-40)

---

## Task 33: Create HomeScreen

### Context
HomeScreen is the landing page with instructions on how to use the app.

### Scope
Create `presentation/home/HomeScreen.kt` with:
- `@Destination(start = true)` annotation
- Instructions on how to share from Instagram
- Visual guide/illustrations
- Link to history

### Files to Create
- `presentation/home/HomeScreen.kt`
- `presentation/home/HomeViewModel.kt`

### Verification
- [ ] Set as start destination
- [ ] Clear usage instructions
- [ ] Navigation to history works

---

## Task 34: Create ProcessingScreen

### Context
ProcessingScreen shows download and splitting progress.

### Scope
Create `presentation/processing/ProcessingScreen.kt` with:
- `@Destination` annotation with `url: String` argument
- Download progress indicator
- Splitting progress indicator
- Cancel button
- Error handling

### Files to Create
- `presentation/processing/ProcessingScreen.kt`
- `presentation/processing/ProcessingViewModel.kt`
- `presentation/processing/ProcessingUiState.kt`

### States
- Queued → Downloading → Downloaded → Splitting → Complete → Error

### Verification
- [ ] Progress updates smoothly
- [ ] Cancel button works
- [ ] Navigates to ResultScreen on complete

---

## Task 35: Create ProcessingViewModel

### Context
ProcessingViewModel orchestrates the download and split workflow.

### Scope
Create `presentation/processing/ProcessingViewModel.kt`:
- Inject use cases (Extract, Download, Split)
- StateFlow of ProcessingUiState
- SavedStateHandle for process death survival
- Analytics logging

### Files to Create
- `presentation/processing/ProcessingViewModel.kt`

### Key Methods
- `startProcessing(url: String)`
- `cancelProcessing()`
- `retryProcessing()`

### Verification
- [ ] Uses SavedStateHandle
- [ ] Properly chains use cases
- [ ] Handles errors gracefully

---

## Task 36: Create ResultScreen

### Context
ResultScreen displays the split video segments with share buttons.

### Scope
Create `presentation/result/ResultScreen.kt` with:
- `@Destination` with video paths argument
- LazyColumn of VideoSegmentCards
- "Share All to Status" button
- "Save to Gallery" option
- Navigation back to home

### Files to Create
- `presentation/result/ResultScreen.kt`
- `presentation/result/ResultViewModel.kt`
- `presentation/result/ResultUiState.kt`

### Verification
- [ ] Lists all video segments
- [ ] Share buttons work
- [ ] In-app review triggered after share

---

## Task 37: Create HistoryScreen with Paging 3

### Context
HistoryScreen shows previously processed videos with pagination.

### Scope
Create `presentation/history/HistoryScreen.kt` with:
- `@Destination` annotation
- Paging 3 integration with `collectAsLazyPagingItems()`
- Loading and error states
- Empty state with instructions

### Files to Create
- `presentation/history/HistoryScreen.kt`
- `presentation/history/HistoryViewModel.kt`
- `data/paging/VideoHistoryPagingSource.kt`

### Verification
- [ ] Paging loads correctly
- [ ] Loading indicator shows
- [ ] Retry on error works

---

## Task 38: Create ShareReceiverActivity

### Context
ShareReceiverActivity handles incoming shares from Instagram's share menu.

### Scope
Create `presentation/share/ShareReceiverActivity.kt`:
- `@AndroidEntryPoint` annotation
- Handle `ACTION_SEND` for text/plain (Instagram URLs)
- Handle `ACTION_SEND` for video/* (direct video files)
- Version-safe `getParcelableExtra()` for Android 13+
- Navigate to MainActivity with processing args

### Files to Create
- `presentation/share/ShareReceiverActivity.kt`

### Key Methods
- `handleInstagramUrl(url: String?)`
- `handleVideoUri(uri: Uri?)`
- `navigateToProcessing(reelUrl: String)`
- `getParcelableExtraCompat<T>(intent, name)` (Android 13+ safe)

### Verification
- [ ] Receives Instagram share intents
- [ ] Parses Instagram URLs correctly
- [ ] Handles video URIs
- [ ] Shows ErrorScreen on failure

---

## Task 39: Create MainActivity with Navigation

### Context
MainActivity is the single activity hosting all Compose destinations.

### Scope
Create `presentation/MainActivity.kt`:
- `@AndroidEntryPoint` annotation
- SplashScreen API integration
- Compose Destinations NavHost
- Handle navigation from ShareReceiverActivity
- In-App Update checking

### Files to Create
- `presentation/MainActivity.kt`
- `presentation/main/MainViewModel.kt`

### Key Features
- `installSplashScreen()` before `super.onCreate()`
- `setKeepOnScreenCondition` for loading state
- Handle `NAVIGATE_TO` extras from ShareReceiver
- Check for app updates

### Verification
- [ ] Splash screen shows correctly
- [ ] Navigation works from share intents
- [ ] In-app update prompt shows

---

## Task 40: Create ErrorScreen Composable

### Context
ErrorScreen is a full-screen error display used by ShareReceiverActivity.

### Scope
Create `presentation/share/ErrorScreen.kt`:
- Error icon (64dp)
- "Oops! Something went wrong" title
- Error message body
- Dismiss button

### Files to Create
- `presentation/share/ErrorScreen.kt` (or in ShareReceiverActivity.kt)

### Verification
- [ ] Centers content vertically and horizontally
- [ ] Dismiss button calls callback
- [ ] Uses Material 3 theme

---

# PHASE 7: Sharing & Integration (Tasks 41-44)

---

## Task 41: Create WhatsAppSharer

### Context
WhatsAppSharer handles all WhatsApp integration for sharing videos.

### Scope
Create `sharing/WhatsAppSharer.kt`:
- `isWhatsAppInstalled(): Boolean`
- `shareToWhatsAppStatus(videoPath: String)`
- `shareToWhatsAppChat(videoPath: String)`
- FileProvider URI generation

### Files to Create
- `sharing/WhatsAppSharer.kt`

### Key Constants
- `WHATSAPP_PACKAGE = "com.whatsapp"`
- `ACTION_SEND_TO_STATUS = "com.whatsapp.intent.action.SEND_MEDIA_TO_STATUS"`

### Verification
- [ ] Detects WhatsApp installation
- [ ] Status intent works
- [ ] Chat intent works

---

## Task 42: Create UrlParser Utility

### Context
UrlParser extracts Instagram reel URLs from shared text.

### Scope
Create `core/utils/UrlParser.kt`:
- Regex for Instagram reel patterns
- `extractInstagramUrl(text: String): String?`
- `extractReelId(url: String): String?`
- `isInstagramUrl(text: String): Boolean`

### Files to Create
- `core/utils/UrlParser.kt`

### Regex Pattern
```kotlin
"""https?://(?:www\.)?instagram\.com/(?:reel|reels|p)/([A-Za-z0-9_-]+)/?"""
```

### Verification
- [ ] Matches instagram.com/reel/ URLs
- [ ] Matches instagram.com/reels/ URLs
- [ ] Matches instagram.com/p/ URLs
- [ ] Extracts reel ID correctly

---

## Task 43: Create FileProvider Configuration

### Context
FileProvider enables secure file sharing with other apps.

### Scope
Create `res/xml/file_paths.xml` and update manifest:
- Cache path for temporary videos
- External path for saved videos
- Configure provider in AndroidManifest.xml

### Files to Create
- `res/xml/file_paths.xml`
- Update `AndroidManifest.xml` with provider

### Verification
- [ ] FileProvider registered in manifest
- [ ] Paths include cache and external
- [ ] authority = `${applicationId}.fileprovider`

---

## Task 44: Create AndroidManifest.xml

### Context
Complete AndroidManifest with all activities, permissions, and configurations.

### Scope
Update `AndroidManifest.xml` with:
- All required permissions
- MainActivity with launcher intent
- ShareReceiverActivity with share intents
- FileProvider configuration
- Application theme and name
- Cleartext traffic disabled

### Files to Update
- `AndroidManifest.xml`

### Permissions Needed
- INTERNET
- READ_EXTERNAL_STORAGE
- WRITE_EXTERNAL_STORAGE (maxSdkVersion 28)
- READ_MEDIA_VIDEO
- POST_NOTIFICATIONS

### Verification
- [ ] App appears in Instagram share menu
- [ ] Permissions declared correctly
- [ ] cleartext traffic disabled for security

---

# PHASE 8: Firebase & Analytics (Tasks 45-47)

---

## Task 45: Setup Firebase Project & Add google-services.json

### Context
Firebase provides crash reporting and analytics.

### Scope
1. Create Firebase project in Firebase Console
2. Add Android app with package `com.reelsplit`
3. Download `google-services.json`
4. Place in `app/` directory

### Files to Create
- `app/google-services.json` (download from Firebase)

### Verification
- [ ] google-services.json exists in app/
- [ ] Package name matches in Firebase console
- [ ] App builds with Firebase

---

## Task 46: Create AnalyticsManager

### Context
AnalyticsManager wraps Firebase Analytics for event tracking.

### Scope
Create `core/analytics/AnalyticsManager.kt`:
- `logVideoProcessed(duration, partsCreated)`
- `logShareToWhatsApp(partNumber)`
- `logError(errorType, errorMessage)`
- `setUserProperty(key, value)`

### Files to Create
- `core/analytics/AnalyticsManager.kt`

### Verification
- [ ] Events appear in Firebase console
- [ ] Singleton with Hilt injection

---

## Task 47: Create InAppReviewManager

### Context
InAppReviewManager prompts users to rate the app after successful shares.

### Scope
Create `core/review/InAppReviewManager.kt`:
- `requestReviewIfEligible(activity)` - checks share count ≥ 3, days since last prompt > 30
- `launchReviewFlow(activity)` - launches Play In-App Review
- Uses `kotlinx-coroutines-play-services` for `await()`

### Files to Create
- `core/review/InAppReviewManager.kt`

### Verification
- [ ] Only prompts after 3+ shares
- [ ] Only prompts once per 30 days
- [ ] Review flow launches correctly

---

# PHASE 9: Notifications & Polish (Tasks 48-49)

---

## Task 48: Create DownloadNotificationManager

### Context
Shows download and processing progress in the notification bar.

### Scope
Create `core/notification/DownloadNotificationManager.kt`:
- Create notification channel (Android 8+)
- `showProgress(title, progress, maxProgress)`
- `showComplete(title, filePath)` with tap-to-open
- `cancel()`
- Check POST_NOTIFICATIONS permission (Android 13+)

### Files to Create
- `core/notification/DownloadNotificationManager.kt`

### Verification
- [ ] Notification channel created
- [ ] Progress updates work
- [ ] Tap-to-open on completion
- [ ] Permission check for Android 13+

---

## Task 49: Create ProGuard Rules

### Context
ProGuard rules prevent obfuscation of critical classes.

### Scope
Create `app/proguard-rules.pro` with rules for:
- kotlinx-serialization
- Retrofit
- OkHttp
- Firebase
- Room
- Hilt
- Media3
- youtubedl-android

### Files to Create
- `app/proguard-rules.pro`

### Verification
- [ ] Release build works correctly
- [ ] No reflection errors in release
- [ ] Firebase crash reports include proper stack traces

---

# PHASE 10: Additional Components (Tasks 51-58)

---

## Task 51: Create UseCaseModule for Hilt

### Context
UseCaseModule provides all use cases via dependency injection for clean ViewModel construction.

### Scope
Create `di/UseCaseModule.kt` with:
- Provides all use cases as singletons
- Alternative to constructor injection if needed

### Files to Create
- `di/UseCaseModule.kt`

### Code Reference
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideExtractVideoUrlUseCase(repository: VideoRepository): ExtractVideoUrlUseCase {
        return ExtractVideoUrlUseCase(repository)
    }
    
    @Provides
    @Singleton
    fun provideDownloadVideoUseCase(workManager: WorkManager): DownloadVideoUseCase {
        return DownloadVideoUseCase(workManager)
    }
    
    @Provides
    @Singleton
    fun provideSplitVideoUseCase(repository: VideoRepository): SplitVideoUseCase {
        return SplitVideoUseCase(repository)
    }
    
    @Provides
    @Singleton
    fun provideShareToWhatsAppUseCase(whatsAppSharer: WhatsAppSharer): ShareToWhatsAppUseCase {
        return ShareToWhatsAppUseCase(whatsAppSharer)
    }
}
```

### Verification
- [ ] All use cases are injectable
- [ ] Module installs in SingletonComponent

---

## Task 52: Create PreferencesRepositoryImpl

### Context
PreferencesRepositoryImpl implements the PreferencesRepository interface using DataStore.

### Scope
Create `data/repository/PreferencesRepositoryImpl.kt` implementing:
- All PreferencesRepository interface methods
- Uses PreferencesDataStore

### Files to Create
- `data/repository/PreferencesRepositoryImpl.kt`

### Code Reference
```kotlin
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : PreferencesRepository {
    
    override suspend fun getShareCount(): Int {
        return preferencesDataStore.getShareCount().first()
    }
    
    override suspend fun incrementShareCount() {
        preferencesDataStore.incrementShareCount()
    }
    
    override suspend fun getLastReviewPromptTime(): Long {
        return preferencesDataStore.getLastReviewPromptTime().first()
    }
    
    override suspend fun setLastReviewPromptTime(time: Long) {
        preferencesDataStore.setLastReviewPromptTime(time)
    }
}
```

### Verification
- [ ] Implements all PreferencesRepository methods
- [ ] Properly injects PreferencesDataStore
- [ ] Bound in RepositoryModule

---

## Task 53: Create Android Resource Files

### Context
XML resource files define strings, colors, themes for SplashScreen, and backup rules.

### Scope
Create resource files in `res/`:

1. **values/strings.xml** - All app strings (no hardcoded strings)
2. **values/colors.xml** - XML color definitions for SplashScreen
3. **values/themes.xml** - Light theme with SplashScreen
4. **values-night/themes.xml** - Dark theme variant
5. **xml/backup_rules.xml** - Backup configuration
6. **xml/data_extraction_rules.xml** - Data extraction rules for Android 12+
7. **drawable/** - App icons and vector drawables

### Files to Create
- `res/values/strings.xml`
- `res/values/colors.xml`
- `res/values/themes.xml`
- `res/values-night/themes.xml`
- `res/xml/backup_rules.xml`
- `res/xml/data_extraction_rules.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/drawable/ic_launcher_background.xml`
- `res/drawable/ic_share.xml`
- `res/mipmap-xxxhdpi/ic_launcher.webp`

### strings.xml Content
```xml
<resources>
    <string name="app_name">ReelSplit</string>
    <string name="processing_title">Processing Video</string>
    <string name="downloading">Downloading...</string>
    <string name="splitting">Splitting into parts...</string>
    <string name="share_to_status">Share to Status</string>
    <string name="share_to_chat">Share to Chat</string>
    <string name="error_whatsapp_not_installed">WhatsApp is not installed</string>
    <string name="error_invalid_url">Invalid Instagram URL</string>
    <string name="error_download_failed">Download failed</string>
    <string name="retry">Retry</string>
    <string name="cancel">Cancel</string>
    <string name="part_number">Part %1$d of %2$d</string>
</resources>
```

### themes.xml Content (SplashScreen)
```xml
<resources>
    <style name="Theme.ReelSplit" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowSplashScreenBackground">@color/splash_background</item>
        <item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    </style>
</resources>
```

### Verification
- [ ] No hardcoded strings in Kotlin code
- [ ] SplashScreen theme configured
- [ ] Backup rules exist for Android Auto Backup
- [ ] App icons in all required sizes

---

## Task 54: Create Lottie Animation Files

### Context
Lottie JSON files provide smooth loading animations for the app.

### Scope
1. Download or create Lottie animation JSON files
2. Place in `assets/` directory
3. Reference in LoadingOverlay and ProgressIndicator

### Files to Create
- `assets/loading_animation.json` - General loading spinner
- `assets/processing_animation.json` - Video processing animation
- `assets/success_animation.json` - Success checkmark
- `assets/error_animation.json` - Error/failure animation

### Sources for Free Lottie Animations
- [LottieFiles](https://lottiefiles.com/free-animations)
- Search for: "loading", "processing", "success", "error"

### Usage in Code
```kotlin
@Composable
fun LoadingOverlay(message: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.Asset("loading_animation.json")
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )
            message?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = Color.White)
            }
        }
    }
}
```

### Verification
- [ ] Animation files exist in assets/
- [ ] Animations load without errors
- [ ] File sizes reasonable (<100KB each)

---

## Task 55: Create Additional Unit & Instrumented Tests

### Context
Complete test coverage for all use cases, repositories, and ViewModels.

### Scope
Create additional test files:

**Unit Tests:**
1. `DownloadVideoUseCaseTest.kt` - Test WorkManager integration
2. `SplitVideoUseCaseTest.kt` - Test video splitting logic
3. `VideoRepositoryImplTest.kt` - Test repository with mocked sources
4. `ResultViewModelTest.kt` - Test result state management

**Instrumented Tests:**
5. `ProcessingScreenTest.kt` - UI test for processing flow
6. `ResultScreenTest.kt` - UI test for result display
7. `ShareReceiverActivityTest.kt` - Test share intent handling

### Files to Create
- `test/domain/usecase/DownloadVideoUseCaseTest.kt`
- `test/domain/usecase/SplitVideoUseCaseTest.kt`
- `test/data/repository/VideoRepositoryImplTest.kt`
- `test/presentation/ResultViewModelTest.kt`
- `androidTest/presentation/ProcessingScreenTest.kt`
- `androidTest/presentation/ResultScreenTest.kt`
- `androidTest/ShareReceiverActivityTest.kt`

### Test Example (VideoRepositoryImplTest)
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var repository: VideoRepositoryImpl
    private val mockVideoDao = mockk<VideoDao>()
    private val mockVideoExtractor = mockk<VideoExtractor>()
    private val mockVideoSplitter = mockk<VideoSplitter>()
    
    @Before
    fun setup() {
        repository = VideoRepositoryImpl(
            videoDao = mockVideoDao,
            videoExtractor = mockVideoExtractor,
            videoSplitter = mockVideoSplitter
        )
    }
    
    @Test
    fun `extractVideoUrl returns success for valid URL`() = runTest {
        val testUrl = "https://instagram.com/reel/ABC123"
        val expectedVideoUrl = "https://cdn.instagram.com/video.mp4"
        
        coEvery { mockVideoExtractor.extractVideoUrl(testUrl) } returns Ok(expectedVideoUrl)
        
        val result = repository.extractVideoUrl(testUrl)
        
        assertTrue(result.isOk)
        assertEquals(expectedVideoUrl, result.value)
    }
}
```

### Verification
- [ ] All unit tests pass
- [ ] Instrumented tests run on emulator
- [ ] Code coverage > 70%
- [ ] CI/CD integration ready

---

## Task 56: Create VideoCache

### Context
VideoCache manages local file caching for downloaded videos, handling cleanup and storage management.

### Scope
Create `data/local/cache/VideoCache.kt`:
- Cache video files in app cache directory
- Track cached files and their sizes
- Cleanup old/unused files
- Get cached video by ID

### Files to Create
- `data/local/cache/VideoCache.kt`

### Code Reference
```kotlin
@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir: File = File(context.cacheDir, "videos")
    
    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }
    
    fun getCacheDir(): File = cacheDir
    
    fun getCachedVideo(videoId: String): File? {
        val file = File(cacheDir, "$videoId.mp4")
        return if (file.exists()) file else null
    }
    
    fun saveToCacheDir(fileName: String): File {
        return File(cacheDir, fileName)
    }
    
    fun getCacheSizeBytes(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
    
    fun deleteOlderThan(maxAgeMs: Long) {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        cacheDir.listFiles()?.filter { it.lastModified() < cutoffTime }?.forEach { it.delete() }
    }
}
```

### Verification
- [ ] Cache directory is created on init
- [ ] Videos are stored in cache directory
- [ ] Cleanup removes old files

---

## Task 57: Create SecurePreferences

### Context
SecurePreferences uses Security Crypto library for encrypted storage of sensitive data.

### Scope
Create `data/local/SecurePreferences.kt`:
- AES-256 encryption using Android Keystore
- Store/retrieve sensitive tokens
- Clear all secure data

### Files to Create
- `data/local/SecurePreferences.kt`

### Code Reference
```kotlin
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiToken(token: String) {
        encryptedPrefs.edit().putString("api_token", token).apply()
    }
    
    fun getApiToken(): String? = encryptedPrefs.getString("api_token", null)
    
    fun clearAll() = encryptedPrefs.edit().clear().apply()
}
```

### Verification
- [ ] Uses MasterKey with AES256_GCM
- [ ] Data is encrypted at rest
- [ ] clearAll removes all sensitive data

---

## Task 58: Create BrowserUtils

### Context
BrowserUtils uses Chrome Custom Tabs for in-app web browsing without leaving the app.

### Scope
Create `core/utils/BrowserUtils.kt`:
- Open URLs in Chrome Custom Tabs
- Match app theme colors
- Support dark mode

### Files to Create
- `core/utils/BrowserUtils.kt`

### Code Reference
```kotlin
object BrowserUtils {
    fun openUrl(context: Context, url: String) {
        val colorScheme = if (context.isDarkMode()) {
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.dark_primary))
                .build()
        } else {
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.primary))
                .build()
        }
        
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorScheme)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
        
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
    
    fun openInstagramHelp(context: Context) {
        openUrl(context, "https://help.instagram.com/")
    }
}
```

### Verification
- [ ] Opens URLs in Chrome Custom Tabs
- [ ] Toolbar color matches app theme
- [ ] Works in dark mode

---

# PHASE 11: Final Testing & Verification (Task 50)

### Context
Final task to ensure the app works end-to-end.

### Scope
1. Create unit tests for:
   - `UrlParserTest.kt` - Test URL regex patterns
   - `ExtractVideoUrlUseCaseTest.kt` - Mock repository
   - `ProcessingViewModelTest.kt` - Test state changes

2. Manual verification:
   - Build and install APK
   - Open Instagram, share a reel
   - Verify app appears in share menu
   - Process a short video (<90s) - should not split
   - Process a long video (>90s) - should split
   - Share to WhatsApp Status
   - Check history screen

### Files to Create
- `test/core/utils/UrlParserTest.kt`
- `test/domain/usecase/ExtractVideoUrlUseCaseTest.kt`
- `test/presentation/ProcessingViewModelTest.kt`

### Test Cases
| Test | Expected Result |
|------|-----------------|
| Short video (<90s) | Not split, 1 part |
| Medium video (2 min) | 2 parts (90s + 30s) |
| Long video (5 min) | 4 parts |
| Invalid URL | Error screen |
| No WhatsApp | Error message |

### Verification
- [ ] All unit tests pass
- [ ] Share intent works from Instagram
- [ ] Video splits correctly
- [ ] WhatsApp sharing works
- [ ] No crashes in release build

---

# 📋 Task Checklist Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-8 | Project Foundation |
| 2 | 9-13 | Dependency Injection |
| 3 | 14-21 | Data Layer |
| 4 | 22-26 | Domain Use Cases |
| 5 | 27-32 | Theme & Components |
| 6 | 33-40 | Screens |
| 7 | 41-44 | Sharing & Integration |
| 8 | 45-47 | Firebase & Analytics |
| 9 | 48-49 | Notifications & Polish |
| 10 | 51-58 | Additional Components |
| 11 | 50 | Final Testing & Verification |

---

## 🚀 How to Use These Tasks

1. **Start New Chat**: Open a new conversation with the AI
2. **Copy Task**: Copy the task you want to implement (e.g., Task 1)
3. **Paste & Execute**: Paste the task and ask the AI to implement it
4. **Verify**: Follow the verification steps before moving to the next task
5. **Continue**: Repeat with the next task

**Example prompt:**
```
Please implement Task 1 from the ReelSplit implementation plan. Here's the task:

[Paste Task 1 content here]
```

---

*Generated from the ReelSplit Implementation Plan - February 2026*
