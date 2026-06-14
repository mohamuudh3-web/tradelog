package com.tradelog.app.ui.backtest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.di.appViewModel
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.ProgressRow
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.StatTile
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.Format
import com.tradelog.app.util.Grade
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SymbolStat(val name: String, val wins: Int, val losses: Int) {
    val total get() = wins + losses
    val winRate get() = if (total > 0) wins.toDouble() / total else 0.0
}

data class BacktestStats(
    val total: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRate: Double = 0.0,
    val bySymbol: List<SymbolStat> = emptyList(),
    val byScenario: List<SymbolStat> = emptyList(),
    val grades: List<Pair<String, Int>> = emptyList(),
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val cumulative: List<Int> = emptyList()
)

class BacktestStatsViewModel(repo: TradeLogRepository) : ViewModel() {

    val stats: StateFlow<BacktestStats> =
        combine(repo.backtests, repo.checklistRules) { backtests, rules ->
            compute(backtests, rules.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BacktestStats())

    private fun compute(all: List<Backtest>, ruleCount: Int): BacktestStats {
        val decided = all.filter { it.result == "WIN" || it.result == "LOSS" }
            .sortedBy { it.dateMillis }
        if (decided.isEmpty()) return BacktestStats()

        val wins = decided.count { it.result == "WIN" }
        val losses = decided.count { it.result == "LOSS" }
        val total = decided.size

        fun group(key: (Backtest) -> String) = decided.groupBy(key).map { (name, list) ->
            SymbolStat(name, list.count { it.result == "WIN" }, list.count { it.result == "LOSS" })
        }.sortedByDescending { it.total }

        val bySymbol = group { it.instrument.ifBlank { "Unknown" } }
        val byScenario = group { it.session.ifBlank { "—" } }

        val gradeOrder = listOf("A+", "A", "B", "C", "D", "F")
        val gradeCounts = decided.groupingBy {
            Grade.of(it.checkedRules.split(",").count { c -> c.isNotBlank() }, ruleCount)
        }.eachCount()
        val grades = gradeOrder.mapNotNull { g -> gradeCounts[g]?.let { g to it } }

        var longest = 0; var run = 0; var current = 0
        val cumulative = ArrayList<Int>(decided.size + 1).apply { add(0) }
        var acc = 0
        for (b in decided) {
            if (b.result == "WIN") { run++; longest = maxOf(longest, run); acc++ } else { run = 0; acc-- }
            cumulative.add(acc)
        }
        // Current streak = trailing consecutive wins
        for (b in decided.reversed()) { if (b.result == "WIN") current++ else break }

        return BacktestStats(
            total = total, wins = wins, losses = losses,
            winRate = wins.toDouble() / total,
            bySymbol = bySymbol, byScenario = byScenario, grades = grades,
            longestStreak = longest, currentStreak = current, cumulative = cumulative
        )
    }
}

@Composable
fun BacktestStatsScreen(onBack: () -> Unit) {
    val vm: BacktestStatsViewModel = appViewModel()
    val s by vm.stats.collectAsStateWithLifecycle()

    DetailScaffold(title = "Statistics center", onBack = onBack) { inner ->
        if (s.total == 0) {
            EmptyState("Log WIN/LOSS backtests to see statistics.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Backtests", s.total.toString(), Modifier.weight(1f))
                    StatTile("Win rate", Format.percent(s.winRate), Modifier.weight(1f), accent = Teal)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Wins", s.wins.toString(), Modifier.weight(1f), accent = Win)
                    StatTile("Losses", s.losses.toString(), Modifier.weight(1f), accent = Loss)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Longest streak", "${s.longestStreak}W", Modifier.weight(1f), accent = Win)
                    StatTile("Current streak", "${s.currentStreak}W", Modifier.weight(1f), accent = Win)
                }
            }
            item { SectionCard(title = "Cumulative performance") { CumulativeCurve(s.cumulative) } }
            if (s.grades.isNotEmpty()) {
                item {
                    SectionCard(title = "Setup grade distribution") {
                        s.grades.forEach { (g, n) -> ProgressRow(g, n, s.total, "") }
                    }
                }
            }
            item {
                SectionCard(title = "By symbol") {
                    s.bySymbol.forEach { SymbolRow(it) }
                }
            }
            item {
                SectionCard(title = "By scenario") {
                    s.byScenario.forEach { SymbolRow(it) }
                }
            }
        }
    }
}

@Composable
private fun SymbolRow(stat: SymbolStat) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stat.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("${stat.wins}W / ${stat.losses}L", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Format.percent(stat.winRate), style = MaterialTheme.typography.bodyMedium, color = if (stat.winRate >= 0.5) Win else Loss, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CumulativeCurve(points: List<Int>) {
    if (points.size < 2) {
        Text("Not enough data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val lineColor = if ((points.lastOrNull() ?: 0) >= 0) Teal else Loss
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val maxV = points.maxOrNull() ?: 0
        val minV = points.minOrNull() ?: 0
        val range = (maxV - minV).takeIf { it != 0 } ?: 1
        val stepX = size.width / (points.size - 1)
        val zeroY = size.height - ((0 - minV).toFloat() / range * size.height)
        drawLine(track, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.5f)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - ((v - minV).toFloat() / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}
