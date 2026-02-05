package com.reelsplit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reelsplit.data.local.dao.VideoDao
import com.reelsplit.data.local.entity.VideoEntity

/**
 * Room database for the ReelSplit application.
 * 
 * This database provides local persistence for video history and enables offline access.
 * The database instance is created and managed by Hilt via [com.reelsplit.di.DatabaseModule].
 * 
 * ### Usage
 * Do not instantiate this database directly. Instead, inject it via Hilt:
 * ```kotlin
 * @Inject lateinit var database: AppDatabase
 * // or inject the DAO directly:
 * @Inject lateinit var videoDao: VideoDao
 * ```
 * 
 * ### Migration Strategy
 * When modifying the schema:
 * 1. Increment the [DATABASE_VERSION]
 * 2. Add migration logic to [com.reelsplit.di.DatabaseModule]
 * 3. Add the migration to the database builder
 * 
 * For development builds, `fallbackToDestructiveMigration()` is used,
 * but production apps should provide proper migrations.
 */
@Database(
    entities = [VideoEntity::class],
    version = AppDatabase.DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Provides access to [VideoDao] for video-related database operations.
     */
    abstract fun videoDao(): VideoDao
    
    companion object {
        /** Current database schema version */
        const val DATABASE_VERSION = 1
        
        /** Database file name */
        const val DATABASE_NAME = "reelsplit_database"
    }
}
