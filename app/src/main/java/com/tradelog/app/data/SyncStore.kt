package com.tradelog.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(name = "sync")

/** Logged-in cloud account + sync cursors. */
data class SyncSession(
    val email: String = "",
    val userId: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    /** Epoch millis when the access token expires. */
    val expiresAt: Long = 0L
) {
    val isLoggedIn: Boolean get() = accessToken.isNotBlank() && userId.isNotBlank()
}

class SyncStore(private val context: Context) {

    private object Keys {
        val EMAIL = stringPreferencesKey("sync_email")
        val USER_ID = stringPreferencesKey("sync_user_id")
        val ACCESS = stringPreferencesKey("sync_access_token")
        val REFRESH = stringPreferencesKey("sync_refresh_token")
        val EXPIRES = longPreferencesKey("sync_expires_at")
        val LAST_SYNC = longPreferencesKey("sync_last_run")
    }

    val session: Flow<SyncSession> = context.syncDataStore.data.map { p ->
        SyncSession(
            email = p[Keys.EMAIL] ?: "",
            userId = p[Keys.USER_ID] ?: "",
            accessToken = p[Keys.ACCESS] ?: "",
            refreshToken = p[Keys.REFRESH] ?: "",
            expiresAt = p[Keys.EXPIRES] ?: 0L
        )
    }

    suspend fun current(): SyncSession = session.first()

    /** Pull cursor (max updated_at seen) for one table. */
    suspend fun pullCursor(table: String): Long =
        context.syncDataStore.data.first()[longPreferencesKey("cursor_$table")] ?: 0L

    suspend fun setPullCursor(table: String, value: Long) =
        context.syncDataStore.edit { it[longPreferencesKey("cursor_$table")] = value }

    val lastSync: Flow<Long> = context.syncDataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }

    suspend fun setLastSync(millis: Long) =
        context.syncDataStore.edit { it[Keys.LAST_SYNC] = millis }

    suspend fun saveSession(
        email: String,
        userId: String,
        accessToken: String,
        refreshToken: String,
        expiresAt: Long
    ) = context.syncDataStore.edit {
        it[Keys.EMAIL] = email
        it[Keys.USER_ID] = userId
        it[Keys.ACCESS] = accessToken
        it[Keys.REFRESH] = refreshToken
        it[Keys.EXPIRES] = expiresAt
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String, expiresAt: Long) =
        context.syncDataStore.edit {
            it[Keys.ACCESS] = accessToken
            it[Keys.REFRESH] = refreshToken
            it[Keys.EXPIRES] = expiresAt
        }

    suspend fun clear() = context.syncDataStore.edit { it.clear() }
}
