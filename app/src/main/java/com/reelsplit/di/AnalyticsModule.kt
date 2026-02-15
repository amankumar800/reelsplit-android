package com.reelsplit.di

import com.reelsplit.core.analytics.AnalyticsManager
import com.reelsplit.core.analytics.AnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [AnalyticsTracker] interface to its production
 * implementation, [AnalyticsManager].
 *
 * Separated into its own file to follow the project's module-per-file convention
 * (consistent with [DatabaseModule], [NetworkModule], [RepositoryModule]).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(
        analyticsManager: AnalyticsManager
    ): AnalyticsTracker
}
