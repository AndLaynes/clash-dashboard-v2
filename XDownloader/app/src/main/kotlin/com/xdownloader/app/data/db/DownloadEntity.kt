package com.xdownloader.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xdownloader.app.domain.model.DownloadJob
import com.xdownloader.app.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tweetId: String,
    val tweetUrl: String,
    val videoUrl: String,
    val status: String,
    val fileName: String,
    val createdAt: Long,
    val downloadManagerId: Long = -1L,
    val errorMessage: String? = null
)

fun DownloadEntity.toDomain() = DownloadJob(
    id = id,
    tweetId = tweetId,
    tweetUrl = tweetUrl,
    videoUrl = videoUrl,
    status = DownloadStatus.valueOf(status),
    fileName = fileName,
    createdAt = createdAt,
    downloadManagerId = downloadManagerId,
    errorMessage = errorMessage
)

fun DownloadJob.toEntity() = DownloadEntity(
    id = id,
    tweetId = tweetId,
    tweetUrl = tweetUrl,
    videoUrl = videoUrl,
    status = status.name,
    fileName = fileName,
    createdAt = createdAt,
    downloadManagerId = downloadManagerId,
    errorMessage = errorMessage
)
