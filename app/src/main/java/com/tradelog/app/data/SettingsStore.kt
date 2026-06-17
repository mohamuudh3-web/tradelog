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
    val lastCalendarSync: Long = 0L,
    val newsAlertEnabled: Boolean = true,
    val newsAlertMinutes: Int = 30,
    // Risk plan (Millionaire Plan defaults). Percentages of account starting balance.
    val riskPerTradePct: Double = 1.0,
    val riskPerDayPct: Double = 2.0,
    val riskPerGroupPct: Double = 10.0,
    val maxDailyLossPct: Double = 5.0,
    val maxDrawdownPct: Double = 10.0
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("briefing_enabled")
        val HOUR = intPreferencesKey("briefing_hour")
        val MINUTE = intPreferencesKey("briefing_minute")
        val CURRENCY = stringPreferencesKey("default_currency")
        val LAST_SYNC = stringPreferencesKey("last_calendar_sync")
        val NEWS_ALERT = booleanPreferencesKey("news_alert_enabled")
        val NEWS_ALERT_MIN = intPreferencesKey("news_alert_minutes")
        val RISK_TRADE = stringPreferencesKey("risk_per_trade_pct")
        val RISK_DAY = stringPreferencesKey("risk_per_day_pct")
        val RISK_GROUP = stringPreferencesKey("risk_per_group_pct")
        val MAX_DAILY_LOSS = stringPreferencesKey("max_daily_loss_pct")
        val MAX_DRAWDOWN = stringPreferencesKey("max_drawdown_pct")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            briefingEnabled = p[Keys.ENABLED] ?: true,
            briefingHour = p[Keys.HOUR] ?: 7,
            briefingMinute = p[Keys.MINUTE] ?: 0,
            defaultCurrency = p[Keys.CURRENCY] ?: "USD",
            lastCalendarSync = p[Keys.LAST_SYNC]?.toLongOrNull() ?: 0L,
            newsAlertEnabled = p[Keys.NEWS_ALERT] ?: true,
            newsAlertMinutes = p[Keys.NEWS_ALERT_MIN] ?: 30,
            riskPerTradePct = p[Keys.RISK_TRADE]?.toDoubleOrNull() ?: 1.0,
            riskPerDayPct = p[Keys.RISK_DAY]?.toDoubleOrNull() ?: 2.0,
            riskPerGroupPct = p[Keys.RISK_GROUP]?.toDoubleOrNull() ?: 10.0,
            maxDailyLossPct = p[Keys.MAX_DAILY_LOSS]?.toDoubleOrNull() ?: 5.0,
            maxDrawdownPct = p[Keys.MAX_DRAWDOWN]?.toDoubleOrNull() ?: 10.0
        )
    }

    suspend fun setRisk(
        perTrade: Double, perDay: Double, perGroup: Double, maxDailyLoss: Double, maxDrawdown: Double
    ) = context.dataStore.edit {
        it[Keys.RISK_TRADE] = perTrade.toString()
        it[Keys.RISK_DAY] = perDay.toString()
        it[Keys.RISK_GROUP] = perGroup.toString()
        it[Keys.MAX_DAILY_LOSS] = maxDailyLoss.toString()
        it[Keys.MAX_DRAWDOWN] = maxDrawdown.toString()
    }

    suspend fun setBriefingEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.ENABLED] = enabled }

    suspend fun setBriefingTime(hour: Int, minute: Int) =
        context.dataStore.edit { it[Keys.HOUR] = hour; it[Keys.MINUTE] = minute }

    suspend fun setDefaultCurrency(currency: String) =
        context.dataStore.edit { it[Keys.CURRENCY] = currency }

    suspend fun setLastSync(epochMillis: Long) =
        context.dataStore.edit { it[Keys.LAST_SYNC] = epochMillis.toString() }

    suspend fun setNewsAlert(enabled: Boolean, minutes: Int) =
        context.dataStore.edit { it[Keys.NEWS_ALERT] = enabled; it[Keys.NEWS_ALERT_MIN] = minutes }
}
