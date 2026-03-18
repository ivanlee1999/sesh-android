package com.ivanlee.sesh.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ivanlee.sesh.data.db.dao.CategoryBreakdownResult
import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.DayFocusResult
import com.ivanlee.sesh.data.db.dao.SessionDao
import com.ivanlee.sesh.data.db.dao.SessionWithCategory
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.data.db.entity.PauseEntity
import com.ivanlee.sesh.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao
) {
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getTodaySessions(): Flow<List<SessionEntity>> = sessionDao.getTodaySessions()

    fun getTodayFocusMinutes(): Flow<Double> = sessionDao.getTodayFocusMinutes()

    fun getTodaySessionCount(): Flow<Int> = sessionDao.getTodaySessionCount()

    suspend fun saveSession(session: SessionEntity) = sessionDao.insertSession(session)

    suspend fun savePause(pause: PauseEntity) = sessionDao.insertPause(pause)

    suspend fun deleteSession(id: String) = sessionDao.deleteSession(id)

    fun getActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    // --- Analytics ---

    fun getAllTimeFocusMinutes(): Flow<Double> = sessionDao.getAllTimeFocusMinutes()

    fun getSessionsWithCategory(): Flow<List<SessionWithCategory>> =
        sessionDao.getSessionsWithCategory()

    fun getTodaySessionsWithCategory(): Flow<List<SessionWithCategory>> =
        sessionDao.getTodaySessionsWithCategory()

    suspend fun getCategoryBreakdownToday(): List<CategoryBreakdownResult> =
        sessionDao.getCategoryBreakdownToday()

    suspend fun getStreak(): Int {
        val query = SimpleSQLiteQuery(
            """
            WITH RECURSIVE dates AS (
                SELECT date('now', 'localtime') AS d
                UNION ALL
                SELECT date(d, '-1 day') FROM dates
                WHERE EXISTS (
                    SELECT 1 FROM sessions
                    WHERE session_type IN ('full_focus', 'partial_focus')
                      AND date(started_at, 'localtime') = date(d, '-1 day')
                )
            )
            SELECT COUNT(*) - 1 FROM dates
            WHERE EXISTS (
                SELECT 1 FROM sessions
                WHERE session_type IN ('full_focus', 'partial_focus')
                  AND date(started_at, 'localtime') = d
            )
            """
        )
        return try {
            val result = sessionDao.rawQueryInt(query)
            if (result < 0) 0 else result
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getLast7DaysFocus(): List<DayFocusResult> {
        val query = SimpleSQLiteQuery(
            """
            WITH RECURSIVE cnt(n) AS (
                SELECT 0 UNION ALL SELECT n+1 FROM cnt WHERE n < 6
            )
            SELECT date('now', 'localtime', '-' || (6-n) || ' days') AS date,
                   COALESCE(SUM(s.actual_seconds - s.pause_seconds), 0) / 3600.0 AS hours
            FROM cnt
            LEFT JOIN sessions s
                ON date(s.started_at, 'localtime') = date('now', 'localtime', '-' || (6-n) || ' days')
                AND s.session_type IN ('full_focus', 'partial_focus')
            GROUP BY n
            ORDER BY n ASC
            """
        )
        return sessionDao.rawQueryDayFocus(query)
    }
}
