package com.xdownloader.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdownloader.app.data.CookieRepository
import com.xdownloader.app.domain.model.MediaInfo
import com.xdownloader.app.domain.usecase.EnqueueDownloadUseCase
import com.xdownloader.app.domain.usecase.ResolveMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "XDL"

sealed interface HomeUiState {
    object Idle : HomeUiState
    object Loading : HomeUiState
    data class Resolved(val mediaInfo: MediaInfo) : HomeUiState
    data class Error(val message: String) : HomeUiState
    object Enqueued : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val resolveMediaUseCase: ResolveMediaUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val cookieRepository: CookieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val hasCookie: Boolean get() = cookieRepository.hasCookie()

    fun resolveAndEnqueue(url: String) {
        if (url.isBlank()) {
            _uiState.value = HomeUiState.Error("URL cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            resolveMediaUseCase(url)
                .onSuccess { mediaInfo ->
                    enqueueDownloadUseCase(mediaInfo, url)
                        .onSuccess { _uiState.value = HomeUiState.Enqueued }
                        .onFailure { e ->
                            Log.e(TAG, "Enqueue failed: ${e.message}")
                            _uiState.value = HomeUiState.Error(e.message ?: "Enqueue failed")
                        }
                }
                .onFailure { e ->
                    Log.e(TAG, "Resolve failed: ${e.message}")
                    _uiState.value = HomeUiState.Error(e.message ?: "Could not resolve media")
                }
        }
    }

    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }
}
