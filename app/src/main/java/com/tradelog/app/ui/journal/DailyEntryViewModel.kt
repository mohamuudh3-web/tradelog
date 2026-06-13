package com.tradelog.app.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DailyEntryViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _entry = MutableStateFlow(JournalEntry(date = ""))
    val entry: StateFlow<JournalEntry> = _entry.asStateFlow()

    private var loaded = false

    fun load(date: String) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val existing = repo.getJournal(date)
            _entry.value = existing ?: JournalEntry(date = date)
        }
    }

    fun update(transform: (JournalEntry) -> JournalEntry) = _entry.update(transform)

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.saveJournal(_entry.value)
            onDone()
        }
    }
}
