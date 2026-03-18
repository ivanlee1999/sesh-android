package com.ivanlee.sesh.data.repository

import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.SessionDao
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
}
