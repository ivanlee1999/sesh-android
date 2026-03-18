package com.ivanlee.sesh.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sesh_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GOOGLE_AUTH_STATE = stringPreferencesKey("google_auth_state")
        val GOOGLE_CALENDAR_ENABLED = booleanPreferencesKey("google_calendar_enabled")
    }

    val googleAuthState: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.GOOGLE_AUTH_STATE]
    }

    val isCalendarSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.GOOGLE_CALENDAR_ENABLED] ?: false
    }

    suspend fun saveGoogleAuthState(authStateJson: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_AUTH_STATE] = authStateJson
        }
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_CALENDAR_ENABLED] = enabled
        }
    }

    suspend fun clearGoogleAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.GOOGLE_AUTH_STATE)
            prefs[Keys.GOOGLE_CALENDAR_ENABLED] = false
        }
    }
}
