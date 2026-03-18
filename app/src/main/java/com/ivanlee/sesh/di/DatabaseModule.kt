package com.ivanlee.sesh.di

import android.content.Context
import com.ivanlee.sesh.data.db.SeshDatabase
import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SeshDatabase {
        return SeshDatabase.buildDatabase(context)
    }

    @Provides
    fun provideCategoryDao(database: SeshDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideSessionDao(database: SeshDatabase): SessionDao {
        return database.sessionDao()
    }
}
