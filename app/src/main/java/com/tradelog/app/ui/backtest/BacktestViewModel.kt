package com.tradelog.app.ui.backtest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.ImageStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Gallery list of backtests with a cover thumbnail per item. */
class BacktestListViewModel(repo: TradeLogRepository) : ViewModel() {

    data class Item(val backtest: Backtest, val coverModel: Any?, val imageCount: Int)

    val items: StateFlow<List<Item>> = combine(repo.backtests, repo.backtestImages) { backtests, images ->
        val byId = images.groupBy { it.backtestId }
        backtests.map { bt ->
            val imgs = byId[bt.id].orEmpty()
            val charts = listOf(bt.chart5Url, bt.chart15Url).filter { it.isNotBlank() }
            // Cover: first device image (File) else first chart URL (String) — Coil handles both.
            val cover: Any? = imgs.firstOrNull()?.let { java.io.File(it.path) } ?: charts.firstOrNull()
            Item(bt, cover, imgs.size + charts.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class BacktestForm(
    val title: String = "",
    val instrument: String = "",
    val direction: String = "",
    val result: String = "",
    val session: String = "",
    val slPips: String = "",
    val tpPips: String = "",
    val bias: String = "",
    val checkedRules: Set<Long> = emptySet(),
    val imageUrls: List<String> = emptyList(),
    val chart5Url: String = "",
    val chart15Url: String = "",
    val notes: String = "",
    val dateMillis: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
class BacktestEditViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _id = MutableStateFlow(0L)
    private val _form = MutableStateFlow(BacktestForm())
    val form: StateFlow<BacktestForm> = _form.asStateFlow()

    private var loaded = false

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
    fun addImageUrl(url: String) = _form.update { it.copy(imageUrls = it.imageUrls + url) }
    fun removeImageUrl(url: String) = _form.update { it.copy(imageUrls = it.imageUrls - url) }

    val images: StateFlow<List<BacktestImage>> =
        _id.flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repo.observeBacktestImages(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        _id.value = id
        if (id == 0L) return
        viewModelScope.launch {
            repo.getBacktest(id)?.let { b ->
                _form.value = BacktestForm(
                    title = b.title, instrument = b.instrument, direction = b.direction,
                    result = b.result, session = b.session,
                    slPips = b.slPips?.toString()?.removeSuffix(".0") ?: "",
                    tpPips = b.tpPips?.toString()?.removeSuffix(".0") ?: "",
                    bias = b.bias,
                    checkedRules = b.checkedRules.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet(),
                    imageUrls = b.imageUrls.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    chart5Url = b.chart5Url, chart15Url = b.chart15Url,
                    notes = b.notes, dateMillis = b.dateMillis
                )
            }
        }
    }

    fun update(transform: (BacktestForm) -> BacktestForm) = _form.update(transform)

    private suspend fun ensureSaved(): Long {
        val f = _form.value
        val saved = repo.saveBacktest(
            Backtest(
                id = _id.value,
                title = f.title.ifBlank { "Untitled backtest" }.trim(),
                instrument = f.instrument.trim(),
                dateMillis = f.dateMillis,
                bias = f.bias.trim(),
                direction = f.direction,
                result = f.result,
                session = f.session.trim(),
                slPips = f.slPips.toDoubleOrNull(),
                tpPips = f.tpPips.toDoubleOrNull(),
                checkedRules = f.checkedRules.joinToString(",") { it.toString() },
                imageUrls = f.imageUrls.joinToString(","),
                chart5Url = f.chart5Url.trim(),
                chart15Url = f.chart15Url.trim(),
                notes = f.notes.trim()
            )
        )
        if (_id.value == 0L) _id.value = saved
        return _id.value
    }

    fun addImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = ImageStorage.importImage(context, uri) ?: return@launch
            val id = ensureSaved()
            repo.addBacktestImage(id, path)
        }
    }

    fun deleteImage(image: BacktestImage) = viewModelScope.launch {
        repo.deleteBacktestImage(image)
        ImageStorage.delete(image.path)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch { ensureSaved(); onDone() }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (_id.value != 0L) repo.getBacktest(_id.value)?.let { repo.deleteBacktest(it) }
            onDone()
        }
    }
}
