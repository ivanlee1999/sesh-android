package com.ivanlee.sesh.di

import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.SessionDao
import com.ivanlee.sesh.data.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        categoryDao: CategoryDao
    ): SessionRepository {
        return SessionRepository(sessionDao, categoryDao)
    }
}
