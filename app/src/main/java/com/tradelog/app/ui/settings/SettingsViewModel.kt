package com.tradelog.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.AppSettings
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.work.BriefingScheduler
import com.tradelog.app.work.MorningBriefingWorker
import com.tradelog.app.work.NewsAlertScheduler
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repo.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setBriefingEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repo.settings.setBriefingEnabled(enabled)
            val s = settings.value
            BriefingScheduler.reschedule(context.applicationContext, enabled, s.briefingHour, s.briefingMinute)
        }
    }

    fun setBriefingTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.settings.setBriefingTime(hour, minute)
            val s = settings.value
            BriefingScheduler.reschedule(context.applicationContext, s.briefingEnabled, hour, minute)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { repo.settings.setDefaultCurrency(currency) }
    }

    fun setRisk(perTrade: Double, perDay: Double, perGroup: Double, maxDailyLoss: Double, maxDrawdown: Double) {
        viewModelScope.launch {
            repo.settings.setRisk(perTrade, perDay, perGroup, maxDailyLoss, maxDrawdown)
        }
    }

    fun setNewsAlert(context: Context, enabled: Boolean, minutes: Int) {
        viewModelScope.launch {
            repo.settings.setNewsAlert(enabled, minutes.coerceAtLeast(1))
            NewsAlertScheduler.scheduleAll(context.applicationContext)
        }
    }

    /** Fire the briefing right now so the user can preview it. */
    fun sendTestBriefing(context: Context) {
        val work = OneTimeWorkRequestBuilder<MorningBriefingWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueue(work)
    }
}
