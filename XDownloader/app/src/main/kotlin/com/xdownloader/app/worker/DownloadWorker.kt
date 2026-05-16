package com.xdownloader.app.worker

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.xdownloader.app.data.DownloadRepository
import com.xdownloader.app.domain.model.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

private const val TAG = "XDL"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "xdl_download"

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_VIDEO_URL = "video_url"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_TWEET_ID = "tweet_id"
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, -1L)
        val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return Result.failure()
        if (jobId == -1L) return Result.failure()

        return withContext(Dispatchers.IO) {
            runCatching {
                downloadRepository.updateStatus(jobId, DownloadStatus.DOWNLOADING)
                delay(Random.nextLong(5_000L, 12_000L))
                ensureOutputDirectory()
                val dmId = enqueueWithDownloadManager(videoUrl, fileName)
                downloadRepository.updateStatusAndDmId(jobId, DownloadStatus.DOWNLOADING, dmId)
                Log.d(TAG, "Enqueued to DownloadManager: dmId=$dmId file=$fileName")
                Result.success()
            }.getOrElse { e ->
                Log.e(TAG, "DownloadWorker failed for job $jobId: ${e.message}")
                downloadRepository.updateStatusWithError(
                    jobId, DownloadStatus.FAILED, e.message ?: "Unknown error"
                )
                Result.failure()
            }
        }
    }

    private fun enqueueWithDownloadManager(videoUrl: String, fileName: String): Long {
        val dm = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
            setTitle(fileName)
            setDescription("XDownloader")
            setAllowedOverMetered(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "XDownloader/$fileName")
            addRequestHeader("User-Agent", "Mozilla/5.0")
        }
        return dm.enqueue(request)
    }

    private fun ensureOutputDirectory() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val dir = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "XDownloader"
            )
            if (!dir.exists()) dir.mkdirs()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("XDownloader")
            .setContentText("Preparing download…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "XDownloader", NotificationManager.IMPORTANCE_LOW
            )
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
