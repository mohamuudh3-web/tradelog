package com.tradelog.app.ui.backtest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.BacktestImage
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

    data class Item(val backtest: Backtest, val coverPath: String?, val imageCount: Int)

    val items: StateFlow<List<Item>> = combine(repo.backtests, repo.backtestImages) { backtests, images ->
        val byId = images.groupBy { it.backtestId }
        backtests.map { bt ->
            val imgs = byId[bt.id].orEmpty()
            Item(bt, imgs.firstOrNull()?.path, imgs.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

data class BacktestForm(
    val title: String = "",
    val instrument: String = "",
    val bias: String = "",
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
                _form.value = BacktestForm(b.title, b.instrument, b.bias, b.notes, b.dateMillis)
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
