package com.tradelog.app.ui.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PayoutStatus
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PayoutSummary(
    val totalPaid: Double = 0.0,
    val pending: Double = 0.0,
    val count: Int = 0
)

class PayoutViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val payouts: StateFlow<List<PayoutRecord>> =
        repo.payouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<PayoutSummary> = repo.payouts.map { list ->
        PayoutSummary(
            totalPaid = list.filter { it.status == PayoutStatus.PAID }.sumOf { it.amount },
            pending = list.filter { it.status == PayoutStatus.PENDING }.sumOf { it.amount },
            count = list.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PayoutSummary())

    fun delete(payout: PayoutRecord) = viewModelScope.launch { repo.deletePayout(payout) }
}

class PayoutEditViewModel(private val repo: TradeLogRepository) : ViewModel() {
    private val _payout = MutableStateFlow(PayoutRecord(date = DateUtils.todayKey()))
    val payout: StateFlow<PayoutRecord> = _payout.asStateFlow()
    private var loaded = false

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        if (id == 0L) return
        viewModelScope.launch { repo.getPayout(id)?.let { _payout.value = it } }
    }

    fun update(transform: (PayoutRecord) -> PayoutRecord) { _payout.value = transform(_payout.value) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch { repo.savePayout(_payout.value); onDone() }
    }

    val canDelete: Boolean get() = _payout.value.id != 0L

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            if (_payout.value.id != 0L) repo.deletePayout(_payout.value)
            onDone()
        }
    }
}
