package com.reelsplit.di

import androidx.work.WorkManager
import com.reelsplit.domain.repository.VideoRepository
import com.reelsplit.domain.sharing.WhatsAppSharerContract
import com.reelsplit.domain.usecase.DownloadVideoUseCase
import com.reelsplit.domain.usecase.ExtractVideoUrlUseCase
import com.reelsplit.domain.usecase.GetVideoInfoUseCase
import com.reelsplit.domain.usecase.ShareToWhatsAppUseCase
import com.reelsplit.domain.usecase.SplitVideoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing all use case instances as singletons.
 *
 * Use cases do **not** have `@Inject` constructors. This is a deliberate
 * Clean Architecture decision: the domain layer stays free of any
 * framework annotations (`javax.inject`, Dagger, Hilt), keeping it a
 * pure Kotlin module. All wiring is centralized here instead.
 *
 * Each use case is scoped as `@Singleton` so that a single shared instance
 * is used across all consumers (ViewModels, Workers, etc.).
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideExtractVideoUrlUseCase(
        repository: VideoRepository
    ): ExtractVideoUrlUseCase {
        return ExtractVideoUrlUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDownloadVideoUseCase(
        workManager: WorkManager
    ): DownloadVideoUseCase {
        return DownloadVideoUseCase(workManager)
    }

    @Provides
    @Singleton
    fun provideSplitVideoUseCase(
        repository: VideoRepository
    ): SplitVideoUseCase {
        return SplitVideoUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideShareToWhatsAppUseCase(
        whatsAppSharer: WhatsAppSharerContract
    ): ShareToWhatsAppUseCase {
        return ShareToWhatsAppUseCase(whatsAppSharer)
    }

    @Provides
    @Singleton
    fun provideGetVideoInfoUseCase(
        repository: VideoRepository
    ): GetVideoInfoUseCase {
        return GetVideoInfoUseCase(repository)
    }
}
