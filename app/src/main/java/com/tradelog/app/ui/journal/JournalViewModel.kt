package com.tradelog.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class JournalViewModel(repo: TradeLogRepository) : ViewModel() {
    val trades: StateFlow<List<Trade>> =
        repo.trades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyEntries: StateFlow<List<JournalEntry>> =
        repo.journals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
