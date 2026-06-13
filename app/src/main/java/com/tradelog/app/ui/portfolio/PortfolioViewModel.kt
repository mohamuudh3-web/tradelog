package com.tradelog.app.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Account
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PortfolioViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val accounts: StateFlow<List<Account>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pnl = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val pnlByAccount: StateFlow<Map<Long, Double>> = _pnl.asStateFlow()

    init {
        viewModelScope.launch { refreshPnl() }
    }

    fun refreshPnl() = viewModelScope.launch { _pnl.value = repo.accountPnl() }

    fun delete(account: Account) = viewModelScope.launch {
        repo.deleteAccount(account)
        refreshPnl()
    }
}

class AccountEditViewModel(private val repo: TradeLogRepository) : ViewModel() {
    private val _account = MutableStateFlow(Account(name = ""))
    val account: StateFlow<Account> = _account.asStateFlow()
    private var loaded = false

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        if (id == 0L) return
        viewModelScope.launch { repo.getAccount(id)?.let { _account.value = it } }
    }

    fun update(transform: (Account) -> Account) { _account.value = transform(_account.value) }

    fun save(onDone: () -> Unit) {
        if (_account.value.name.isBlank()) return
        viewModelScope.launch { repo.saveAccount(_account.value); onDone() }
    }
}
