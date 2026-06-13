package com.tradelog.app.ui.trades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TradeDetailViewModel(private val repo: TradeLogRepository) : ViewModel() {
    private val _trade = MutableStateFlow<Trade?>(null)
    val trade: StateFlow<Trade?> = _trade.asStateFlow()

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    private var loaded = false

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val t = repo.getTrade(id)
            _trade.value = t
            t?.accountId?.let { _account.value = repo.getAccount(it) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            _trade.value?.let { repo.deleteTrade(it) }
            onDone()
        }
    }
}
