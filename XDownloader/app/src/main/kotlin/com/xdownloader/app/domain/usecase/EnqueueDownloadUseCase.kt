package com.xdownloader.app.domain.usecase

import androidx.work.*
import com.xdownloader.app.data.DownloadRepository
import com.xdownloader.app.domain.model.DownloadJob
import com.xdownloader.app.domain.model.DownloadStatus
import com.xdownloader.app.domain.model.MediaInfo
import com.xdownloader.app.worker.DownloadWorker
import javax.inject.Inject

class EnqueueDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val workManager: WorkManager
) {
    suspend operator fun invoke(mediaInfo: MediaInfo, tweetUrl: String): Result<Unit> =
        runCatching {
            val fileName = "X_${mediaInfo.tweetId}_${System.currentTimeMillis()}.mp4"
            val job = DownloadJob(
                tweetId = mediaInfo.tweetId,
                tweetUrl = tweetUrl,
                videoUrl = mediaInfo.videoUrl,
                status = DownloadStatus.PENDING,
                fileName = fileName
            )
            val insertedId = downloadRepository.insert(job)
            val inputData = workDataOf(
                DownloadWorker.KEY_JOB_ID to insertedId,
                DownloadWorker.KEY_VIDEO_URL to mediaInfo.videoUrl,
                DownloadWorker.KEY_FILE_NAME to fileName,
                DownloadWorker.KEY_TWEET_ID to mediaInfo.tweetId
            )
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("download_${mediaInfo.tweetId}")
                .build()
            workManager.enqueue(request)
        }
}
