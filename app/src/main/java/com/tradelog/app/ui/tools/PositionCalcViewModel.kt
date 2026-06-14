package com.tradelog.app.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.data.entity.PositionPreset
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RiskMode { PERCENT, AMOUNT }

data class CalcForm(
    val instrument: String = "EURUSD",
    val balance: String = "",
    val riskMode: RiskMode = RiskMode.PERCENT,
    val riskPct: String = "1",
    val riskMoney: String = "",
    val stopLoss: String = "",
    val pipValuePerLot: String = "10"
)

data class CalcResult(val riskAmount: Double, val lotSize: Double, val valid: Boolean)

class PositionCalcViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _form = MutableStateFlow(CalcForm())
    val form: StateFlow<CalcForm> = _form.asStateFlow()

    val presets: StateFlow<List<PositionPreset>> =
        repo.presets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val instruments: StateFlow<List<Instrument>> =
        repo.instruments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reactive result — recomputes whenever the form changes. */
    val result: StateFlow<CalcResult> =
        _form.map { compute(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), compute(CalcForm()))

    fun update(transform: (CalcForm) -> CalcForm) = _form.update(transform)

    fun applyInstrument(instrument: Instrument) {
        _form.update { it.copy(instrument = instrument.name, pipValuePerLot = instrument.pipValuePerLot.toString()) }
    }

    fun useAccountBalance(account: Account) {
        _form.update { it.copy(balance = account.balance.toString()) }
    }

    private fun compute(f: CalcForm): CalcResult {
        val stop = f.stopLoss.toDoubleOrNull()
        val pip = f.pipValuePerLot.toDoubleOrNull()
        if (stop == null || pip == null || stop <= 0 || pip <= 0) return CalcResult(0.0, 0.0, false)

        val riskAmount = when (f.riskMode) {
            RiskMode.PERCENT -> {
                val balance = f.balance.toDoubleOrNull()
                val pct = f.riskPct.toDoubleOrNull()
                if (balance == null || pct == null || balance <= 0 || pct <= 0) return CalcResult(0.0, 0.0, false)
                balance * (pct / 100.0)
            }
            RiskMode.AMOUNT -> {
                val amt = f.riskMoney.toDoubleOrNull()
                if (amt == null || amt <= 0) return CalcResult(0.0, 0.0, false)
                amt
            }
        }
        val lots = riskAmount / (stop * pip)
        return CalcResult(riskAmount, lots, true)
    }

    fun savePreset(name: String) {
        if (name.isBlank()) return
        val f = _form.value
        viewModelScope.launch {
            repo.savePreset(
                PositionPreset(
                    name = name.trim(),
                    balance = f.balance.toDoubleOrNull() ?: 0.0,
                    riskPercent = f.riskPct.toDoubleOrNull() ?: 1.0,
                    stopLoss = f.stopLoss.toDoubleOrNull() ?: 10.0,
                    pipValuePerLot = f.pipValuePerLot.toDoubleOrNull() ?: 10.0,
                    instrument = f.instrument.trim()
                )
            )
        }
    }

    fun applyPreset(p: PositionPreset) {
        _form.value = CalcForm(
            instrument = p.instrument,
            balance = if (p.balance == 0.0) "" else p.balance.toString(),
            riskPct = p.riskPercent.toString(),
            stopLoss = if (p.stopLoss == 0.0) "" else p.stopLoss.toString(),
            pipValuePerLot = p.pipValuePerLot.toString()
        )
    }

    fun deletePreset(p: PositionPreset) = viewModelScope.launch { repo.deletePreset(p) }
}
