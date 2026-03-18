package com.ivanlee.sesh.ui.screen.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanlee.sesh.data.calendar.CalendarAuthManager
import com.ivanlee.sesh.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isCalendarSyncEnabled: Boolean = false,
    val isGoogleAuthorized: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val calendarAuthManager: CalendarAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.isCalendarSyncEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isCalendarSyncEnabled = enabled)
            }
        }
        viewModelScope.launch {
            val authorized = calendarAuthManager.isAuthorized()
            _uiState.value = _uiState.value.copy(isGoogleAuthorized = authorized)
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return calendarAuthManager.getAuthorizationIntent()
    }

    fun handleGoogleSignInResult(intent: Intent) {
        viewModelScope.launch {
            try {
                calendarAuthManager.handleAuthorizationResponse(intent)
                _uiState.value = _uiState.value.copy(isGoogleAuthorized = true)
                preferencesRepository.setCalendarSyncEnabled(true)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isGoogleAuthorized = false)
            }
        }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCalendarSyncEnabled(enabled)
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            calendarAuthManager.clearAuth()
            _uiState.value = _uiState.value.copy(
                isGoogleAuthorized = false,
                isCalendarSyncEnabled = false
            )
        }
    }
}
