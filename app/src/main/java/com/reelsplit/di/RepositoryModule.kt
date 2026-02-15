package com.reelsplit.di

import com.reelsplit.data.repository.PreferencesRepositoryImpl
import com.reelsplit.data.repository.VideoRepositoryImpl
import com.reelsplit.domain.repository.PreferencesRepository
import com.reelsplit.domain.repository.VideoRepository
import com.reelsplit.domain.sharing.WhatsAppSharerContract
import com.reelsplit.sharing.WhatsAppSharer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository and contract interfaces to their implementations.
 *
 * Uses `@Binds` instead of `@Provides` for interface binding, which is more
 * efficient as it doesn't require creating a provider method.
 *
 * Note: The implementation classes must have `@Inject` constructor annotations
 * for Hilt to create them.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [VideoRepositoryImpl] to the [VideoRepository] interface.
     *
     * This enables injection of [VideoRepository] throughout the app,
     * with [VideoRepositoryImpl] as the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    /**
     * Binds [PreferencesRepositoryImpl] to the [PreferencesRepository] interface.
     *
     * This enables injection of [PreferencesRepository] throughout the app,
     * with [PreferencesRepositoryImpl] as the concrete implementation.
     */
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    /**
     * Binds [WhatsAppSharer] to the [WhatsAppSharerContract] interface.
     *
     * This enables injection of [WhatsAppSharerContract] in use cases and
     * ViewModels, with [WhatsAppSharer] providing the actual Android Intent
     * and FileProvider operations.
     */
    @Binds
    @Singleton
    abstract fun bindWhatsAppSharer(impl: WhatsAppSharer): WhatsAppSharerContract
}
