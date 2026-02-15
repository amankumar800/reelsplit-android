# ============================================================================
# ReelSplit ProGuard / R8 Rules
# ============================================================================
# Covers: Kotlin, Coroutines, kotlinx-serialization, Hilt/Dagger, Room,
#         Retrofit, OkHttp, Firebase, Media3, youtubedl-android, PRDownloader,
#         WorkManager, Coil, Lottie, Compose, Compose Destinations,
#         kotlin-result, Sentry, Google Play In-App Review, DataStore, Paging.
# ============================================================================

# ──────────────────────────────────────────────────────────────────────────────
# General / Debug
# ──────────────────────────────────────────────────────────────────────────────
# Preserve source file names and line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable

# Keep annotation metadata required by many libraries
# *Annotation* is a wildcard that covers RuntimeVisibleAnnotations,
# RuntimeVisibleParameterAnnotations, and AnnotationDefault.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions

# If keeping the line number information, uncomment this to hide the
# original source file name.
#-renamesourcefileattribute SourceFile

# ──────────────────────────────────────────────────────────────────────────────
# Kotlin core
# ──────────────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ──────────────────────────────────────────────────────────────────────────────
# Kotlin Coroutines
# ──────────────────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.atomicfu.**

# ──────────────────────────────────────────────────────────────────────────────
# kotlinx-serialization
# ──────────────────────────────────────────────────────────────────────────────
-dontnote kotlinx.serialization.AnnotationsKt

# Keep companion objects of serializable classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializers for app classes
-keep,includedescriptorclasses class com.reelsplit.**$$serializer { *; }
-keepclassmembers class com.reelsplit.** {
    *** Companion;
}
-keepclasseswithmembers class com.reelsplit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ──────────────────────────────────────────────────────────────────────────────
# Hilt / Dagger
# ──────────────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
# Keep @Inject annotated constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
# Hilt generated components
-keep class *_HiltModules* { *; }
-keep class *_HiltComponents* { *; }
# @AssistedInject constructors (used by @HiltWorker in VideoDownloadWorker)
-keepclasseswithmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}
-keep @dagger.assisted.AssistedFactory interface * { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Room
# ──────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ──────────────────────────────────────────────────────────────────────────────
# Retrofit
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
# Keep generic signatures for Retrofit service interfaces
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
# Keep Retrofit service method annotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# R8 full-mode compatibility for Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit Kotlinx Serialization Converter
-keep class com.jakewharton.retrofit2.converter.kotlinx.serialization.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# OkHttp
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
# R8 full-mode keep for OkHttp platform detection
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ──────────────────────────────────────────────────────────────────────────────
# Firebase Crashlytics & Analytics
# ──────────────────────────────────────────────────────────────────────────────
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**
# Keep Firebase Analytics screen tracking
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**
# Keep FirebaseApp init
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Sentry
# ──────────────────────────────────────────────────────────────────────────────
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# ──────────────────────────────────────────────────────────────────────────────
# Media3 (ExoPlayer + Transformer)
# ──────────────────────────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
# Keep native decoders/renderers accessed via reflection
-keepclassmembers class androidx.media3.decoder.** { *; }
# Transformer uses service-loader pattern
-keep class * extends androidx.media3.common.MediaLibraryInfo { *; }

# ──────────────────────────────────────────────────────────────────────────────
# youtubedl-android
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# PRDownloader
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.downloader.** { *; }
-dontwarn com.downloader.**

# ──────────────────────────────────────────────────────────────────────────────
# WorkManager
# ──────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.WorkerFactory { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Compose
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ──────────────────────────────────────────────────────────────────────────────
# Compose Destinations (navigation code generation)
# ──────────────────────────────────────────────────────────────────────────────
-keep class * implements com.ramcosta.composedestinations.spec.Direction { *; }
-keep class * implements com.ramcosta.composedestinations.spec.NavGraphSpec { *; }
-keep @com.ramcosta.composedestinations.annotation.Destination class * { *; }
# Keep generated destinations (KSP output)
-keep class com.reelsplit.presentation.destinations.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Coil (image loading)
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ──────────────────────────────────────────────────────────────────────────────
# Lottie
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Google Play In-App Review
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.google.android.play.core.review.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }
-dontwarn com.google.android.play.core.**

# ──────────────────────────────────────────────────────────────────────────────
# DataStore Preferences
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ──────────────────────────────────────────────────────────────────────────────
# Paging 3
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn androidx.paging.**

# ──────────────────────────────────────────────────────────────────────────────
# kotlin-result
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.github.michaelbull.result.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Timber (logging)
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn org.jetbrains.annotations.**
# Strip Timber debug logs in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ──────────────────────────────────────────────────────────────────────────────
# App data classes and models
# ──────────────────────────────────────────────────────────────────────────────
# Domain models (used in serialization / Room)
-keep class com.reelsplit.domain.model.** { *; }
# Room entities
-keep class com.reelsplit.data.local.entity.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Native methods
# ──────────────────────────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ──────────────────────────────────────────────────────────────────────────────
# Enums
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ──────────────────────────────────────────────────────────────────────────────
# Parcelable
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ──────────────────────────────────────────────────────────────────────────────
# Serializable
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
