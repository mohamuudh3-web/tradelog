package com.tradelog.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DayRange { TODAY, TOMORROW, WEEK }

data class CalendarUiState(
    val syncing: Boolean = false,
    val error: String? = null,
    val lastSync: Long = 0L
)

class CalendarViewModel(private val repo: TradeLogRepository) : ViewModel() {

    // Multi-select impacts (checked = shown). Default: high, medium, holiday (skip low noise).
    private val _impacts = MutableStateFlow(setOf(Impact.HIGH, Impact.MEDIUM, Impact.HOLIDAY))
    val impacts: StateFlow<Set<Impact>> = _impacts.asStateFlow()

    // Currencies that are unchecked/hidden. Empty = show all.
    private val _excludedCurrencies = MutableStateFlow<Set<String>>(emptySet())
    val excludedCurrencies: StateFlow<Set<String>> = _excludedCurrencies.asStateFlow()

    private val _dayRange = MutableStateFlow(DayRange.WEEK)
    val dayRange: StateFlow<DayRange> = _dayRange.asStateFlow()

    private val _ui = MutableStateFlow(CalendarUiState())
    val ui: StateFlow<CalendarUiState> = _ui.asStateFlow()

    val currencies: StateFlow<List<String>> =
        repo.events.map { events -> events.map { it.country }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<EconomicEvent>> = combine(
        repo.events, _impacts, _excludedCurrencies, _dayRange
    ) { events, impacts, excluded, range ->
        val (start, end) = when (range) {
            DayRange.TODAY -> DateUtils.dayEpochBounds(DateUtils.today())
            DayRange.TOMORROW -> DateUtils.dayEpochBounds(DateUtils.today().plusDays(1))
            DayRange.WEEK -> Long.MIN_VALUE to Long.MAX_VALUE
        }
        events.filter {
            it.impact in impacts &&
                it.country !in excluded &&
                it.dateTimeUtc in start..end
        }
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

    fun toggleImpact(i: Impact) = _impacts.update { if (i in it) it - i else it + i }
    fun setAllImpacts(on: Boolean) { _impacts.value = if (on) Impact.entries.toSet() else emptySet() }
    fun toggleCurrency(c: String) = _excludedCurrencies.update { if (c in it) it - c else it + c }
    fun setAllCurrencies(on: Boolean, all: List<String>) {
        _excludedCurrencies.value = if (on) emptySet() else all.toSet()
    }
    fun setDayRange(r: DayRange) { _dayRange.value = r }

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
