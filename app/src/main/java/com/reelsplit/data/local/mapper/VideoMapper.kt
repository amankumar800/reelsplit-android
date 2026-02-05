package com.reelsplit.data.local.mapper

import com.reelsplit.data.local.entity.VideoEntity
import com.reelsplit.domain.model.Video
import com.reelsplit.domain.model.VideoStatus

/**
 * Mapper functions for converting between [VideoEntity] (data layer) and [Video] (domain layer).
 * 
 * These extension functions provide a clean separation of concerns by keeping
 * mapping logic outside of the entity and domain model classes.
 */

/**
 * Converts a [VideoEntity] to its domain model representation [Video].
 * 
 * @return The corresponding [Video] domain object
 */
fun VideoEntity.toDomain(): Video = Video(
    id = id,
    sourceUrl = sourceUrl,
    localPath = localPath,
    durationSeconds = durationSeconds,
    fileSizeBytes = fileSizeBytes,
    createdAt = createdAt,
    status = try {
        VideoStatus.valueOf(status)
    } catch (e: IllegalArgumentException) {
        VideoStatus.PENDING
    },
    errorMessage = errorMessage,
    thumbnailPath = thumbnailPath
)

/**
 * Converts a [Video] domain model to its entity representation [VideoEntity].
 * 
 * @return The corresponding [VideoEntity] for database storage
 */
fun Video.toEntity(): VideoEntity = VideoEntity(
    id = id,
    sourceUrl = sourceUrl,
    localPath = localPath,
    durationSeconds = durationSeconds,
    fileSizeBytes = fileSizeBytes,
    createdAt = createdAt,
    status = status.name,
    errorMessage = errorMessage,
    thumbnailPath = thumbnailPath
)

/**
 * Converts a list of [VideoEntity] objects to a list of [Video] domain models.
 * 
 * @return List of [Video] domain objects
 */
fun List<VideoEntity>.toDomainList(): List<Video> = map { it.toDomain() }

/**
 * Converts a list of [Video] domain models to a list of [VideoEntity] objects.
 * 
 * @return List of [VideoEntity] for database storage
 */
fun List<Video>.toEntityList(): List<VideoEntity> = map { it.toEntity() }
