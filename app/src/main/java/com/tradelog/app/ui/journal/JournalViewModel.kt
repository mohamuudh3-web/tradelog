package com.tradelog.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyStats(val total: Int = 0, val thisWeek: Int = 0, val streak: Int = 0)

class JournalViewModel(private val repo: TradeLogRepository) : ViewModel() {
    val trades: StateFlow<List<Trade>> =
        repo.trades.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyEntries: StateFlow<List<JournalEntry>> =
        repo.journals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyStats: StateFlow<DailyStats> = repo.journals.map { entries ->
        val dates = entries.map { it.date }.toSet()
        val (monday, sunday) = DateUtils.weekDates()
        val weekStart = DateUtils.dateKey(monday)
        val weekEnd = DateUtils.dateKey(sunday)
        val thisWeek = entries.count { it.date in weekStart..weekEnd }
        var streak = 0
        var day = DateUtils.today()
        while (DateUtils.dateKey(day) in dates) { streak++; day = day.minusDays(1) }
        DailyStats(total = entries.size, thisWeek = thisWeek, streak = streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyStats())

    fun deleteTrade(trade: Trade) = viewModelScope.launch { repo.deleteTrade(trade) }
    fun deleteDaily(entry: JournalEntry) = viewModelScope.launch { repo.deleteJournal(entry) }
}
