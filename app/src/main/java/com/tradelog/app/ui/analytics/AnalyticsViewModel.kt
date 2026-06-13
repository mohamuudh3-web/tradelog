package com.tradelog.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.repository.AnalyticsCalculator
import com.tradelog.app.repository.AnalyticsResult
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(repo: TradeLogRepository) : ViewModel() {
    val result: StateFlow<AnalyticsResult> =
        repo.trades.map { AnalyticsCalculator.compute(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsResult())
}
