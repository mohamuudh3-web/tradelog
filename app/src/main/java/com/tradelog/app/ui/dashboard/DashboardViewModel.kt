package com.tradelog.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalProgressUi(val goal: Goal, val progress: Int)

data class TaskUi(val task: TaskItem, val doneToday: Boolean)

data class DashboardState(
    val netPnl: Double = 0.0,
    val todayPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTrades: Int = 0,
    val openGoals: Int = 0,
    val goals: List<GoalProgressUi> = emptyList(),
    val tasks: List<TaskUi> = emptyList(),
    val recentTrades: List<Trade> = emptyList(),
    val loading: Boolean = true
)

class DashboardViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val state = combine(
        repo.trades, repo.activeGoals, repo.tasks
    ) { trades, goals, tasks ->
        val (todayStart, todayEnd) = DateUtils.dayEpochBounds()
        val todayPnl = trades.filter { it.openedAt in todayStart..todayEnd }.sumOf { it.pnl }
        val wins = trades.count { it.result == TradeResult.WIN }
        val losses = trades.count { it.result == TradeResult.LOSS }
        val decisive = wins + losses
        DashboardState(
            netPnl = trades.sumOf { it.pnl },
            todayPnl = todayPnl,
            winRate = if (decisive > 0) wins.toDouble() / decisive else 0.0,
            totalTrades = trades.size,
            openGoals = goals.size,
            goals = goals.take(4).map { GoalProgressUi(it, repo.goalProgress(it)) },
            tasks = tasks.map { TaskUi(it, repo.isTaskDoneToday(it)) },
            recentTrades = trades.take(5),
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun toggleTask(task: TaskItem, done: Boolean) {
        viewModelScope.launch { repo.setTaskDone(task, done) }
    }
}
