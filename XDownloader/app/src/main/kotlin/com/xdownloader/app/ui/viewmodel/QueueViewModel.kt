package com.xdownloader.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdownloader.app.data.DownloadRepository
import com.xdownloader.app.domain.model.DownloadJob
import com.xdownloader.app.domain.model.DownloadStatus
import com.xdownloader.app.domain.model.MediaInfo
import com.xdownloader.app.domain.usecase.EnqueueDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase
) : ViewModel() {

    val downloads: StateFlow<List<DownloadJob>> =
        downloadRepository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun remove(jobId: Long) {
        viewModelScope.launch {
            downloadRepository.deleteById(jobId)
        }
    }

    fun retry(job: DownloadJob) {
        viewModelScope.launch {
            downloadRepository.updateStatus(job.id, DownloadStatus.PENDING)
            val mediaInfo = MediaInfo(
                tweetId = job.tweetId,
                videoUrl = job.videoUrl,
                bitrate = 0
            )
            enqueueDownloadUseCase(mediaInfo, job.tweetUrl)
        }
    }
}
