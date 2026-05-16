package com.xdownloader.app.domain.model

data class MediaInfo(
    val tweetId: String,
    val videoUrl: String,
    val bitrate: Int,
    val contentType: String = "video/mp4"
)
