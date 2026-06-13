package com.tradelog.app.ui.analytics

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
import com.tradelog.app.di.appViewModel
import com.tradelog.app.repository.AnalyticsResult
import com.tradelog.app.repository.TagStat
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.StatTile
import com.tradelog.app.ui.common.TopLevelScaffold
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.Format

@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = appViewModel()
    val r by vm.result.collectAsStateWithLifecycle()

    TopLevelScaffold(title = "Analytics") { inner ->
        if (r.totalTrades == 0) {
            EmptyState("Log trades to see your analytics.", modifier = Modifier.padding(inner))
            return@TopLevelScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Win rate", Format.percent(r.winRate), Modifier.weight(1f))
                    StatTile("Net P&L", Format.signedMoney(r.netPnl), Modifier.weight(1f), accent = if (r.netPnl >= 0) Win else Loss)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Avg R", Format.rMultiple(r.avgR), Modifier.weight(1f))
                    StatTile("Profit factor", Format.profitFactor(r.profitFactor), Modifier.weight(1f))
                    StatTile("Expectancy", Format.signedMoney(r.expectancy), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Wins", r.wins.toString(), Modifier.weight(1f), accent = Win)
                    StatTile("Losses", r.losses.toString(), Modifier.weight(1f), accent = Loss)
                    StatTile("BE", r.breakevens.toString(), Modifier.weight(1f))
                }
            }

            item {
                SectionCard(title = "Equity curve") {
                    EquityCurve(r)
                }
            }

            if (r.bestSetup != null || r.worstSetup != null) {
                item {
                    SectionCard(title = "Strengths & weaknesses") {
                        r.bestSetup?.let { Text("Best: ${it.name} (${Format.signedMoney(it.netPnl)}, ${Format.percent(it.winRate)} win)", color = Win) }
                        r.worstSetup?.let { Text("Worst: ${it.name} (${Format.signedMoney(it.netPnl)}, ${Format.percent(it.winRate)} win)", color = Loss) }
                    }
                }
            }

            item { SectionCard(title = "By strategy") { TagStatTable(r.byStrategy) } }
            item { SectionCard(title = "By instrument") { TagStatTable(r.byInstrument) } }
        }
    }
}

@Composable
private fun EquityCurve(r: AnalyticsResult) {
    val points = r.equityCurve
    val lineColor = if ((points.lastOrNull() ?: 0.0) >= 0) Teal else Loss
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    if (points.size < 2) {
        Text("Not enough data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val maxV = points.maxOrNull() ?: 0.0
        val minV = points.minOrNull() ?: 0.0
        val range = (maxV - minV).takeIf { it != 0.0 } ?: 1.0
        val stepX = size.width / (points.size - 1)

        // Zero baseline
        val zeroY = size.height - ((0.0 - minV) / range * size.height).toFloat()
        drawLine(surfaceVariant, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.5f)

        val path = Path()
        points.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - ((v - minV) / range * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    }
}

@Composable
private fun TagStatTable(stats: List<TagStat>) {
    if (stats.isEmpty()) {
        Text("No data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    stats.forEach { s ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(s.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${s.trades}t · ${Format.percent(s.winRate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Format.signedMoney(s.netPnl),
                style = MaterialTheme.typography.bodyMedium,
                color = if (s.netPnl >= 0) Win else Loss,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
