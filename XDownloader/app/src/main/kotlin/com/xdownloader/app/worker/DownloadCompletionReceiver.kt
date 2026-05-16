package com.xdownloader.app.worker

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xdownloader.app.data.DownloadRepository
import com.xdownloader.app.domain.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "XDL"

@AndroidEntryPoint
class DownloadCompletionReceiver : BroadcastReceiver() {

    @Inject lateinit var downloadRepository: DownloadRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val dmId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (dmId == -1L) return

        val pendingResult = goAsync()
        scope.launch {
            runCatching {
                val job = downloadRepository.findByDownloadManagerId(dmId)
                if (job == null) {
                    Log.w(TAG, "No job found for dmId=$dmId")
                    return@launch
                }
                val dmStatus = queryDownloadManagerStatus(context, dmId)
                val newStatus = if (dmStatus == DownloadManager.STATUS_SUCCESSFUL)
                    DownloadStatus.DONE else DownloadStatus.FAILED
                if (newStatus == DownloadStatus.FAILED) {
                    val reason = queryFailureReason(context, dmId)
                    downloadRepository.updateStatusWithError(job.id, newStatus, "System error code: $reason")
                } else {
                    downloadRepository.updateStatus(job.id, newStatus)
                }
                Log.d(TAG, "dmId=$dmId -> $newStatus")
            }.onFailure { e ->
                Log.e(TAG, "Error in DownloadCompletionReceiver: ${e.message}")
            }
            pendingResult.finish()
        }
    }

    private fun queryDownloadManagerStatus(context: Context, dmId: Long): Int {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            else -1
        }
    }

    private fun queryFailureReason(context: Context, dmId: Long): Int {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(dmId))
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            else -1
        }
    }
}
