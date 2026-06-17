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
                val slPips = t.slPips?.toCleanString() ?: ""
                val tpPips = t.tpPips?.toCleanString() ?: ""
                _form.value = TradeForm(
                    id = t.id,
                    accountId = t.accountId,
                    instrument = t.instrument,
                    direction = t.direction,
                    entry = t.entryPrice.toCleanString(),
                    exit = t.exitPrice?.toCleanString() ?: "",
                    lot = t.lotSize.toCleanString(),
                    riskPct = t.riskPercent?.toCleanString() ?: "",
                    rMultiple = t.rMultiple?.let { kotlin.math.abs(it).toCleanString() } ?: calculateRiskReward(slPips, tpPips),
                    result = t.result,
                    pnl = t.pnl.toCleanString(),
                    setupTag = t.setupTag ?: "",
                    session = t.session,
                    slPips = slPips,
                    tpPips = tpPips,
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

    fun updateSlPips(value: String) = _form.update { form ->
        val next = form.copy(slPips = value)
        next.copy(rMultiple = calculateRiskReward(next.slPips, next.tpPips))
    }

    fun updateTpPips(value: String) = _form.update { form ->
        val next = form.copy(tpPips = value)
        next.copy(rMultiple = calculateRiskReward(next.slPips, next.tpPips))
    }

    val isValid: Boolean get() = _form.value.instrument.isNotBlank()

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (f.instrument.isBlank()) return
        // Normalize sign to the result: WIN = positive, LOSS = negative.
        val pnlMag = kotlin.math.abs(f.pnl.toDoubleOrNull() ?: 0.0)
        val rInput = f.rMultiple.ifBlank { calculateRiskReward(f.slPips, f.tpPips) }
        val rMag = rInput.toDoubleOrNull()?.let { kotlin.math.abs(it) }
        val signedPnl = when (f.result) {
            TradeResult.WIN -> pnlMag
            TradeResult.LOSS -> -pnlMag
            TradeResult.BREAKEVEN -> 0.0
        }
        val signedR = rMag?.let {
            when (f.result) {
                TradeResult.WIN -> it
                TradeResult.LOSS -> -it
                TradeResult.BREAKEVEN -> 0.0
            }
        }
        viewModelScope.launch {
            val accountId = f.accountId ?: accounts.value.firstOrNull()?.id
            repo.saveTrade(
                Trade(
                    id = f.id,
                    accountId = accountId,
                    instrument = f.instrument.trim(),
                    direction = f.direction,
                    entryPrice = f.entry.toDoubleOrNull() ?: 0.0,
                    exitPrice = f.exit.toDoubleOrNull(),
                    lotSize = f.lot.toDoubleOrNull() ?: 0.0,
                    riskPercent = f.riskPct.toDoubleOrNull(),
                    rMultiple = signedR,
                    result = f.result,
                    pnl = signedPnl,
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

private fun calculateRiskReward(slPips: String, tpPips: String): String {
    val stop = kotlin.math.abs(slPips.toDoubleOrNull() ?: return "")
    val target = kotlin.math.abs(tpPips.toDoubleOrNull() ?: return "")
    if (stop == 0.0 || target == 0.0) return ""
    val rounded = kotlin.math.round((target / stop) * 100.0) / 100.0
    return rounded.toCleanString()
}

internal fun String.toTagSet(): Set<String> = split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
internal fun String.toIdSet(): Set<Long> = split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
internal fun String.toUrlList(): List<String> = split(",").map { it.trim() }.filter { it.isNotBlank() }
