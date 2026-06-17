package com.tradelog.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.data.entity.EconomicEvent
import com.tradelog.app.data.entity.Goal
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.data.entity.TaskCategory
import com.tradelog.app.data.entity.TaskItem
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    val streak: Int = 0,
    val goals: List<GoalProgressUi> = emptyList(),
    val tasks: List<TaskUi> = emptyList(),
    val recentTrades: List<Trade> = emptyList(),
    val risk: List<RiskStatusUi> = emptyList(),
    val loading: Boolean = true
)

/** Per-account risk-room status for the dashboard guardrail card. */
data class RiskStatusUi(
    val accountName: String,
    val currency: String,
    val dailyUsed: Double,
    val dailyLimit: Double,
    val drawdownUsed: Double,
    val drawdownLimit: Double
) {
    val dailyRatio: Double get() = if (dailyLimit > 0) dailyUsed / dailyLimit else 0.0
    val drawdownRatio: Double get() = if (drawdownLimit > 0) drawdownUsed / drawdownLimit else 0.0
    val worstRatio: Double get() = maxOf(dailyRatio, drawdownRatio)
}

class DashboardViewModel(private val repo: TradeLogRepository) : ViewModel() {

    val state = combine(
        repo.trades, repo.activeGoals, repo.tasks, repo.accounts, repo.settings.settings
    ) { trades, goals, tasks, accounts, settings ->
        val (todayStart, todayEnd) = DateUtils.dayEpochBounds()
        val todayPnl = trades.filter { it.openedAt in todayStart..todayEnd }.sumOf { it.pnl }
        val wins = trades.count { it.result == TradeResult.WIN }
        val losses = trades.count { it.result == TradeResult.LOSS }
        val decisive = wins + losses

        // Current win streak: most-recent consecutive WIN trades.
        var streak = 0
        for (t in trades.sortedByDescending { it.openedAt }) {
            if (t.result == TradeResult.WIN) streak++
            else if (t.result == TradeResult.LOSS) break
        }

        // Per-account risk room vs the plan (only accounts with a starting balance).
        val risk = accounts.mapNotNull { acc ->
            val startBal = (acc.startingBalance ?: acc.balance).takeIf { it > 0 } ?: return@mapNotNull null
            val accTrades = trades.filter { it.accountId == acc.id }
            val realized = accTrades.sumOf { it.pnl }
            val dayNet = accTrades.filter { it.openedAt in todayStart..todayEnd }.sumOf { it.pnl }
            RiskStatusUi(
                accountName = acc.name,
                currency = acc.currency,
                dailyUsed = maxOf(0.0, -dayNet),
                dailyLimit = startBal * settings.maxDailyLossPct / 100.0,
                drawdownUsed = maxOf(0.0, -realized),
                drawdownLimit = startBal * settings.maxDrawdownPct / 100.0
            )
        }.sortedByDescending { it.worstRatio }

        DashboardState(
            netPnl = trades.sumOf { it.pnl },
            todayPnl = todayPnl,
            winRate = if (decisive > 0) wins.toDouble() / decisive else 0.0,
            totalTrades = trades.size,
            openGoals = goals.size,
            streak = streak,
            goals = goals.take(4).map { GoalProgressUi(it, repo.goalProgress(it)) },
            tasks = tasks.filter { it.category == TaskCategory.TASK }.map { TaskUi(it, repo.isTaskDoneToday(it)) },
            recentTrades = trades.take(5),
            risk = risk,
            loading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    /** Next few upcoming economic events for the dashboard preview. */
    val upcomingEvents: StateFlow<List<EconomicEvent>> = repo.events.map { events ->
        val now = System.currentTimeMillis()
        // Home preview: high & medium impact plus bank holidays — skip low-impact noise.
        events.filter {
            it.dateTimeUtc >= now && it.impact in setOf(Impact.HIGH, Impact.MEDIUM, Impact.HOLIDAY)
        }.sortedBy { it.dateTimeUtc }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Nearest goal that still needs attention (active or awaiting review). */
    val nearestCountdown: StateFlow<Countdown?> = repo.countdowns.map { list ->
        list.filter { !it.reviewDone }.minByOrNull { it.targetDateMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Refresh the economic feed and (re)schedule high-impact alerts when the home screen opens. */
    fun syncNews(context: android.content.Context) {
        viewModelScope.launch {
            repo.refreshCalendar()
            com.tradelog.app.work.NewsAlertScheduler.scheduleAll(context.applicationContext)
        }
    }

    fun toggleTask(task: TaskItem, done: Boolean) {
        viewModelScope.launch { repo.setTaskDone(task, done) }
    }
}
