package ru.jarvis.telegramka.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

interface ITokenManager {
    fun initialize(context: Context)
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): Flow<String?>
    fun getRefreshToken(): Flow<String?>
    suspend fun clearTokens()
}

object TokenManager : ITokenManager {

    private lateinit var applicationContext: Context

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    override fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    override fun getAccessToken(): Flow<String?> {
        return applicationContext.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    override fun getRefreshToken(): Flow<String?> {
        return applicationContext.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }

    override suspend fun clearTokens() {
        applicationContext.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
