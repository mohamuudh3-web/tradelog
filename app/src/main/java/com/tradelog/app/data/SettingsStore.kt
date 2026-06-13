package com.tradelog.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val briefingEnabled: Boolean = true,
    val briefingHour: Int = 7,
    val briefingMinute: Int = 0,
    val defaultCurrency: String = "USD",
    val lastCalendarSync: Long = 0L
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("briefing_enabled")
        val HOUR = intPreferencesKey("briefing_hour")
        val MINUTE = intPreferencesKey("briefing_minute")
        val CURRENCY = stringPreferencesKey("default_currency")
        val LAST_SYNC = stringPreferencesKey("last_calendar_sync")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            briefingEnabled = p[Keys.ENABLED] ?: true,
            briefingHour = p[Keys.HOUR] ?: 7,
            briefingMinute = p[Keys.MINUTE] ?: 0,
            defaultCurrency = p[Keys.CURRENCY] ?: "USD",
            lastCalendarSync = p[Keys.LAST_SYNC]?.toLongOrNull() ?: 0L
        )
    }

    suspend fun setBriefingEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.ENABLED] = enabled }

    suspend fun setBriefingTime(hour: Int, minute: Int) =
        context.dataStore.edit { it[Keys.HOUR] = hour; it[Keys.MINUTE] = minute }

    suspend fun setDefaultCurrency(currency: String) =
        context.dataStore.edit { it[Keys.CURRENCY] = currency }

    suspend fun setLastSync(epochMillis: Long) =
        context.dataStore.edit { it[Keys.LAST_SYNC] = epochMillis.toString() }
}
