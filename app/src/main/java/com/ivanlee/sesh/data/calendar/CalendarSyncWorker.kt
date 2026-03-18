package com.ivanlee.sesh.data.calendar

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.SessionDao
import com.ivanlee.sesh.data.preferences.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val calendarRepository: GoogleCalendarRepository,
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if calendar sync is enabled
        val enabled = preferencesRepository.isCalendarSyncEnabled.firstOrNull() ?: false
        if (!enabled) return Result.success()

        val sessionId = inputData.getString("session_id") ?: return Result.failure()
        val session = sessionDao.getSessionById(sessionId) ?: return Result.failure()

        // Look up category name
        val categoryName = session.categoryId?.let { catId ->
            try {
                categoryDao.getCategoryById(catId)?.title
            } catch (_: Exception) {
                null
            }
        }

        val success = calendarRepository.createEvent(session, categoryName)
        return if (success) Result.success() else Result.retry()
    }
}
