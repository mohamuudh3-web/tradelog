package com.tradelog.app.ui.trades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.SetupTag
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradeForm(
    val id: Long = 0,
    val accountId: Long? = null,
    val instrument: String = "",
    val direction: Direction = Direction.LONG,
    val entry: String = "",
    val exit: String = "",
    val lot: String = "",
    val riskPct: String = "",
    val rMultiple: String = "",
    val result: TradeResult = TradeResult.WIN,
    val pnl: String = "",
    val setupTag: String = "",
    val session: String = "",
    val slPips: String = "",
    val tpPips: String = "",
    val psychology: Set<String> = emptySet(),
    val checkedRules: Set<Long> = emptySet(),
    val imageUrls: List<String> = emptyList(),
    val notes: String = "",
    val screenshotUri: String? = null,
    val openedAt: Long = System.currentTimeMillis()
)

class TradeEditViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _form = MutableStateFlow(TradeForm())
    val form: StateFlow<TradeForm> = _form.asStateFlow()

    private var loaded = false

    val accounts: StateFlow<List<Account>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val setupTags: StateFlow<List<SetupTag>> =
        repo.setupTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val instruments: StateFlow<List<Instrument>> =
        repo.instruments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val checklistRules: StateFlow<List<ChecklistRule>> =
        repo.checklistRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addInstrument(name: String, pip: Double) = viewModelScope.launch { repo.addInstrument(name, pip) }
    fun addChecklistRule(text: String) = viewModelScope.launch { repo.addChecklistRule(text) }
    fun deleteRule(rule: ChecklistRule) = viewModelScope.launch { repo.deleteChecklistRule(rule) }
    fun toggleRule(id: Long) = _form.update {
        it.copy(checkedRules = if (id in it.checkedRules) it.checkedRules - id else it.checkedRules + id)
    }
    fun togglePsychology(tag: String) = _form.update {
        it.copy(psychology = if (tag in it.psychology) it.psychology - tag else it.psychology + tag)
    }
    fun addImageUrl(url: String) = _form.update { it.copy(imageUrls = it.imageUrls + url) }
    fun removeImageUrl(url: String) = _form.update { it.copy(imageUrls = it.imageUrls - url) }

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        if (id == 0L) return
        viewModelScope.launch {
            repo.getTrade(id)?.let { t ->
                _form.value = TradeForm(
                    id = t.id,
                    accountId = t.accountId,
                    instrument = t.instrument,
                    direction = t.direction,
                    entry = t.entryPrice.toCleanString(),
                    exit = t.exitPrice?.toCleanString() ?: "",
                    lot = t.lotSize.toCleanString(),
                    riskPct = t.riskPercent?.toCleanString() ?: "",
                    rMultiple = t.rMultiple?.toCleanString() ?: "",
                    result = t.result,
                    pnl = t.pnl.toCleanString(),
                    setupTag = t.setupTag ?: "",
                    session = t.session,
                    slPips = t.slPips?.toCleanString() ?: "",
                    tpPips = t.tpPips?.toCleanString() ?: "",
                    psychology = t.psychology.toTagSet(),
                    checkedRules = t.checkedRules.toIdSet(),
                    imageUrls = t.imageUrls.toUrlList(),
                    notes = t.notes,
                    screenshotUri = t.screenshotUri,
                    openedAt = t.openedAt
                )
            }
        }
    }

    fun update(transform: (TradeForm) -> TradeForm) = _form.update(transform)

    val isValid: Boolean get() = _form.value.instrument.isNotBlank()

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.instrument.isBlank()) return
        viewModelScope.launch {
            repo.saveTrade(
                Trade(
                    id = f.id,
                    accountId = f.accountId,
                    instrument = f.instrument.trim(),
                    direction = f.direction,
                    entryPrice = f.entry.toDoubleOrNull() ?: 0.0,
                    exitPrice = f.exit.toDoubleOrNull(),
                    lotSize = f.lot.toDoubleOrNull() ?: 0.0,
                    riskPercent = f.riskPct.toDoubleOrNull(),
                    rMultiple = f.rMultiple.toDoubleOrNull(),
                    result = f.result,
                    pnl = f.pnl.toDoubleOrNull() ?: 0.0,
                    setupTag = f.setupTag.trim().ifBlank { null },
                    session = f.session.trim(),
                    slPips = f.slPips.toDoubleOrNull(),
                    tpPips = f.tpPips.toDoubleOrNull(),
                    psychology = f.psychology.joinToString(","),
                    checkedRules = f.checkedRules.joinToString(",") { it.toString() },
                    imageUrls = f.imageUrls.joinToString(","),
                    notes = f.notes.trim(),
                    screenshotUri = f.screenshotUri,
                    openedAt = f.openedAt
                )
            )
            if (f.setupTag.isNotBlank()) repo.addSetupTag(f.setupTag.trim())
            onDone()
        }
    }
}

private fun Double.toCleanString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

internal fun String.toTagSet(): Set<String> = split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
internal fun String.toIdSet(): Set<Long> = split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
internal fun String.toUrlList(): List<String> = split(",").map { it.trim() }.filter { it.isNotBlank() }
