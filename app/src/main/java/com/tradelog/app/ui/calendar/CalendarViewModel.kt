package com.tradelog.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val syncing: Boolean = false,
    val error: String? = null,
    val lastSync: Long = 0L
)

class CalendarViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _impactFilter = MutableStateFlow<Impact?>(null) // null = all
    val impactFilter: StateFlow<Impact?> = _impactFilter.asStateFlow()

    private val _currencyFilter = MutableStateFlow<String?>(null)
    val currencyFilter: StateFlow<String?> = _currencyFilter.asStateFlow()

    private val _ui = MutableStateFlow(CalendarUiState())
    val ui: StateFlow<CalendarUiState> = _ui.asStateFlow()

    val currencies: StateFlow<List<String>> =
        repo.events.map { events -> events.map { it.country }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<EconomicEvent>> = combine(
        repo.events, _impactFilter, _currencyFilter
    ) { events, impact, currency ->
        events.filter { (impact == null || it.impact == impact) && (currency == null || it.country == currency) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val cached = repo.hasCachedEvents()
            repo.settings.settings.collect { _ui.value = _ui.value.copy(lastSync = it.lastCalendarSync) }
        }
        viewModelScope.launch {
            if (!repo.hasCachedEvents()) refresh()
        }
    }

    fun setImpact(i: Impact?) { _impactFilter.value = i }
    fun setCurrency(c: String?) { _currencyFilter.value = c }

    fun refresh() {
        if (_ui.value.syncing) return
        _ui.value = _ui.value.copy(syncing = true, error = null)
        viewModelScope.launch {
            val r = repo.refreshCalendar()
            _ui.value = _ui.value.copy(
                syncing = false,
                error = if (r.isFailure) "Couldn't fetch news. Showing cached data." else null
            )
        }
    }
}
