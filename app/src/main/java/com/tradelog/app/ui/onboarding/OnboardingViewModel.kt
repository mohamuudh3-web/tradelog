package com.tradelog.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.work.BriefingScheduler
import com.tradelog.app.work.NewsAlertScheduler
import kotlinx.coroutines.launch

class OnboardingViewModel(private val repo: TradeLogRepository) : ViewModel() {

    fun setCurrency(code: String) = viewModelScope.launch {
        repo.settings.setDefaultCurrency(code)
    }

    /** Turn on the morning briefing + schedule it (called when the user enables notifications). */
    fun enableBriefing(context: Context, hour: Int, minute: Int) = viewModelScope.launch {
        repo.settings.setBriefingEnabled(true)
        repo.settings.setBriefingTime(hour, minute)
        BriefingScheduler.reschedule(context.applicationContext, true, hour, minute)
        NewsAlertScheduler.scheduleAll(context.applicationContext)
    }
}
