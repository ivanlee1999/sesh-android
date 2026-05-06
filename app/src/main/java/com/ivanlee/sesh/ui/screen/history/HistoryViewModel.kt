package com.ivanlee.sesh.ui.screen.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ivanlee.sesh.data.calendar.CalendarSyncWorker
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DayGroup(
    val label: String,
    val sessions: List<SessionWithCategory>
)

data class EditSessionUiState(
    val sessionId: String,
    val title: String,
    val notes: String,
    val selectedCategoryId: String?
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository
) : ViewModel() {

    val groupedSessions: StateFlow<List<DayGroup>> = repository.getSessionsWithCategory()
        .map { sessions -> groupByDay(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editState = MutableStateFlow<EditSessionUiState?>(null)
    val editState: StateFlow<EditSessionUiState?> = _editState.asStateFlow()

    fun startEditing(session: SessionWithCategory) {
        _editState.value = EditSessionUiState(
            sessionId = session.id,
            title = session.title,
            notes = session.notes.orEmpty(),
            selectedCategoryId = session.categoryId
        )
    }

    fun updateEditTitle(title: String) {
        _editState.value = _editState.value?.copy(title = title)
    }

    fun updateEditNotes(notes: String) {
        _editState.value = _editState.value?.copy(notes = notes)
    }

    fun updateEditCategory(categoryId: String?) {
        _editState.value = _editState.value?.copy(selectedCategoryId = categoryId)
    }

    fun cancelEditing() {
        _editState.value = null
    }

    fun saveEdit() {
        val edit = _editState.value ?: return
        viewModelScope.launch {
            val session = repository.getSessionById(edit.sessionId) ?: return@launch
            repository.updateSession(
                session.copy(
                    title = edit.title.trim(),
                    categoryId = edit.selectedCategoryId,
                    notes = edit.notes.trim().ifBlank { null }
                )
            )
            enqueueCalendarSync(edit.sessionId)
            _editState.value = null
        }
    }

    private fun enqueueCalendarSync(sessionId: String) {
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setInputData(workDataOf("session_id" to sessionId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "calendar-sync-$sessionId",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun groupByDay(sessions: List<SessionWithCategory>): List<DayGroup> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val displayFormat = DateTimeFormatter.ofPattern("EEE, MMM d")

        return sessions.groupBy { session ->
            // Parse UTC instant and convert to device-local date
            try {
                Instant.parse(session.startedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } catch (e: Exception) {
                today
            }
        }.map { (date, daySessions) ->
            val label = when (date) {
                today -> "Today, ${today.format(displayFormat).removePrefix("${today.dayOfWeek.name.take(3)}, ")}"
                yesterday -> "Yesterday, ${yesterday.format(displayFormat).removePrefix("${yesterday.dayOfWeek.name.take(3)}, ")}"
                else -> date.format(displayFormat)
            }
            DayGroup(label, daySessions)
        }.sortedByDescending { group ->
            // Sort groups by first session date (most recent first)
            group.sessions.firstOrNull()?.startedAt ?: ""
        }
    }
}
