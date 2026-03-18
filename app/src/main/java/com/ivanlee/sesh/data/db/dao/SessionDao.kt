package com.ivanlee.sesh.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.ivanlee.sesh.data.db.entity.PauseEntity
import com.ivanlee.sesh.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertPause(pause: PauseEntity)

    @Query("SELECT * FROM sessions ORDER BY started_at DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("""
        SELECT * FROM sessions
        WHERE date(started_at) = date('now', 'localtime')
        ORDER BY started_at DESC
    """)
    fun getTodaySessions(): Flow<List<SessionEntity>>

    @Query("""
        SELECT COALESCE(SUM(actual_seconds - pause_seconds), 0) / 60.0
        FROM sessions
        WHERE session_type IN ('full_focus', 'partial_focus')
        AND date(started_at) = date('now', 'localtime')
    """)
    fun getTodayFocusMinutes(): Flow<Double>

    @Query("""
        SELECT COUNT(*) FROM sessions
        WHERE session_type IN ('full_focus', 'partial_focus')
        AND date(started_at) = date('now', 'localtime')
    """)
    fun getTodaySessionCount(): Flow<Int>

    @Query("SELECT * FROM pauses WHERE session_id = :sessionId ORDER BY paused_at ASC")
    suspend fun getPausesForSession(sessionId: String): List<PauseEntity>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // --- Analytics queries (ported from Go sessions.go) ---

    @RawQuery
    suspend fun rawQueryInt(query: SupportSQLiteQuery): Int

    @RawQuery
    suspend fun rawQueryDayFocus(query: SupportSQLiteQuery): List<DayFocusResult>

    @Query("""
        SELECT COALESCE(SUM(actual_seconds - pause_seconds), 0) / 60.0
        FROM sessions
        WHERE session_type IN ('full_focus', 'partial_focus')
    """)
    fun getAllTimeFocusMinutes(): Flow<Double>

    @Query("""
        SELECT s.id, s.title, s.category_id,
               COALESCE(c.title, 'Uncategorized') AS category_title,
               COALESCE(c.hex_color, '#ABB2BF') AS category_color,
               s.session_type, s.target_seconds, s.actual_seconds,
               s.pause_seconds, s.overflow_seconds,
               s.started_at, s.ended_at, s.notes
        FROM sessions s
        LEFT JOIN categories c ON s.category_id = c.id
        ORDER BY s.started_at DESC
    """)
    fun getSessionsWithCategory(): Flow<List<SessionWithCategory>>

    @Query("""
        SELECT s.id, s.title, s.category_id,
               COALESCE(c.title, 'Uncategorized') AS category_title,
               COALESCE(c.hex_color, '#ABB2BF') AS category_color,
               s.session_type, s.target_seconds, s.actual_seconds,
               s.pause_seconds, s.overflow_seconds,
               s.started_at, s.ended_at, s.notes
        FROM sessions s
        LEFT JOIN categories c ON s.category_id = c.id
        WHERE s.session_type IN ('full_focus', 'partial_focus', 'rest')
          AND date(s.started_at) = date('now', 'localtime')
        ORDER BY s.started_at ASC
    """)
    fun getTodaySessionsWithCategory(): Flow<List<SessionWithCategory>>

    @Query("""
        SELECT COALESCE(c.title, 'Uncategorized') AS name,
               COALESCE(c.hex_color, '#ABB2BF') AS color,
               SUM(s.actual_seconds - s.pause_seconds) / 60.0 AS minutes
        FROM sessions s
        LEFT JOIN categories c ON s.category_id = c.id
        WHERE s.session_type IN ('full_focus', 'partial_focus')
          AND date(s.started_at) = date('now', 'localtime')
        GROUP BY c.id
        ORDER BY minutes DESC
    """)
    suspend fun getCategoryBreakdownToday(): List<CategoryBreakdownResult>
}
