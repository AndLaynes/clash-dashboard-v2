package com.xdownloader.app.domain.model

data class DownloadJob(
    val id: Long = 0,
    val tweetId: String,
    val tweetUrl: String,
    val videoUrl: String,
    val status: DownloadStatus,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val downloadManagerId: Long = -1L,
    val errorMessage: String? = null
)

enum class DownloadStatus { PENDING, DOWNLOADING, DONE, FAILED }
