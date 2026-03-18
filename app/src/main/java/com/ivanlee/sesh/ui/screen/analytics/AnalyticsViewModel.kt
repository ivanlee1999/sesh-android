package com.ivanlee.sesh.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanlee.sesh.data.db.dao.CategoryBreakdownResult
import com.ivanlee.sesh.data.db.dao.DayFocusResult
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val todayMinutes: Double = 0.0,
    val todaySessionCount: Int = 0,
    val streak: Int = 0,
    val categoryBreakdown: List<CategoryBreakdownResult> = emptyList(),
    val last7Days: List<DayFocusResult> = emptyList(),
    val allTimeMinutes: Double = 0.0
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val todaySessions: StateFlow<List<SessionWithCategory>> =
        repository.getTodaySessionsWithCategory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect reactive flows
        viewModelScope.launch {
            repository.getTodayFocusMinutes().collect { minutes ->
                _uiState.value = _uiState.value.copy(todayMinutes = minutes)
            }
        }
        viewModelScope.launch {
            repository.getTodaySessionCount().collect { count ->
                _uiState.value = _uiState.value.copy(todaySessionCount = count)
            }
        }
        viewModelScope.launch {
            repository.getAllTimeFocusMinutes().collect { minutes ->
                _uiState.value = _uiState.value.copy(allTimeMinutes = minutes)
            }
        }

        // Load one-shot analytics data
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            val streak = repository.getStreak()
            val breakdown = repository.getCategoryBreakdownToday()
            val last7Days = repository.getLast7DaysFocus()
            _uiState.value = _uiState.value.copy(
                streak = streak,
                categoryBreakdown = breakdown,
                last7Days = last7Days
            )
        }
    }
}
