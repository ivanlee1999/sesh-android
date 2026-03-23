package com.ivanlee.sesh.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DayGroup(
    val label: String,
    val sessions: List<SessionWithCategory>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: SessionRepository
) : ViewModel() {

    val groupedSessions: StateFlow<List<DayGroup>> = repository.getSessionsWithCategory()
        .map { sessions -> groupByDay(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
