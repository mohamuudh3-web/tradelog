package com.tradelog.app.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CountdownViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val countdowns: StateFlow<List<Countdown>> =
        repo.countdowns.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, targetMillis: Long, motivation: String, hour: Int, minute: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.saveCountdown(
                Countdown(
                    title = title.trim(),
                    targetDateMillis = targetMillis,
                    motivation = motivation.trim(),
                    reminderHour = hour,
                    reminderMinute = minute
                )
            )
        }
    }

    fun saveReview(c: Countdown, reached: Boolean, wrong: String, improve: String) {
        viewModelScope.launch {
            repo.saveCountdown(c.copy(reviewDone = true, reachedIt = reached, wentWrong = wrong.trim(), improveNext = improve.trim()))
        }
    }

    fun delete(c: Countdown) = viewModelScope.launch { repo.deleteCountdown(c) }
}
