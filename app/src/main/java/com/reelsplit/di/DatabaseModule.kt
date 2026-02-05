package com.reelsplit.di

import android.content.Context
import androidx.room.Room
import com.reelsplit.data.local.AppDatabase
import com.reelsplit.data.local.dao.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances.
 * 
 * Provides:
 * - AppDatabase as a singleton for the entire application lifecycle
 * - VideoDao for video entity operations
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance as a singleton.
     * 
     * The database is created lazily on first access and persists
     * for the entire application lifecycle.
     * 
     * @param context Application context for database creation
     * @return Singleton AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides VideoDao for video entity database operations.
     * 
     * The DAO is obtained from the database instance and can be
     * injected anywhere video database operations are needed.
     * Scoped as singleton since the database is singleton.
     * 
     * @param database The AppDatabase instance
     * @return VideoDao for video operations
     */
    @Provides
    @Singleton
    fun provideVideoDao(database: AppDatabase): VideoDao {
        return database.videoDao()
    }
}
