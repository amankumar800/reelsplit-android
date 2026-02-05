package com.reelsplit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reelsplit.domain.model.VideoStatus

/**
 * Room entity representing a video stored in the local database.
 * 
 * This entity mirrors the [Video] domain model and provides local persistence
 * for video history and offline access. The table is indexed on [createdAt]
 * for efficient sorting and pagination queries.
 */
@Entity(
    tableName = "videos",
    indices = [
        androidx.room.Index(value = ["source_url"], unique = false)
    ]
)
data class VideoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,
    
    @ColumnInfo(name = "local_path")
    val localPath: String? = null,
    
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,
    
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0L,
    
    @ColumnInfo(name = "created_at", index = true)
    val createdAt: Long,
    
    @ColumnInfo(name = "status")
    val status: String = VideoStatus.PENDING.name,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null
)
