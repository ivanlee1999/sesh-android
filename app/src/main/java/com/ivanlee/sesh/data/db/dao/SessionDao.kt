package com.ivanlee.sesh.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
