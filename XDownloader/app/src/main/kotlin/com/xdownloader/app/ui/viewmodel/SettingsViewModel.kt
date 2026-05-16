package com.xdownloader.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdownloader.app.data.CookieRepository
import com.xdownloader.app.domain.model.Cookie
import com.xdownloader.app.domain.usecase.ImportCookieUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    object Idle : SettingsUiState
    object Loading : SettingsUiState
    data class CookieLoaded(val cookie: Cookie) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
    object CookieCleared : SettingsUiState
    object CookieImported : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val importCookieUseCase: ImportCookieUseCase,
    private val cookieRepository: CookieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val hasCookie: Boolean get() = cookieRepository.hasCookie()

    fun importCookie(netscapeContent: String) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val result = importCookieUseCase(netscapeContent)
            result
                .onSuccess { _uiState.value = SettingsUiState.CookieImported }
                .onFailure { e -> _uiState.value = SettingsUiState.Error(e.message ?: "Import failed") }
        }
    }

    fun clearCookie() {
        cookieRepository.clearCookie()
        _uiState.value = SettingsUiState.CookieCleared
    }

    fun resetState() {
        _uiState.value = SettingsUiState.Idle
    }
}
