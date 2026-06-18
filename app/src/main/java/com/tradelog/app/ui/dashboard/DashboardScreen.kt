package com.tradelog.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.calendar.MyfxbookCalendar
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.ProgressRow
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.StatTile
import com.tradelog.app.ui.common.TopLevelScaffold
import com.tradelog.app.ui.common.impactColor
import com.tradelog.app.ui.common.resultColor
import com.tradelog.app.ui.navigation.Routes
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.CountdownMessages
import com.tradelog.app.util.DateUtils
import com.tradelog.app.util.Format

@Composable
fun DashboardScreen(
    onAddTrade: () -> Unit,
    onOpenTrade: (Long) -> Unit,
    onNavigate: (String) -> Unit
) {
    val vm: DashboardViewModel = appViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val countdown by vm.nearestCountdown.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.syncNews(context) }

    val quickLinks = listOf(
        "Analytics" to Routes.ANALYTICS,
        "Goals & Tasks" to Routes.GOALS,
        "Goal countdown" to Routes.COUNTDOWN,
        "Portfolio" to Routes.PORTFOLIO,
        "Backtesting journal" to Routes.BACKTESTS,
        "Notebook" to Routes.NOTEBOOK,
        "Payouts" to Routes.PAYOUTS,
        "Position calculator" to Routes.POSITION_CALC,
        "Economic calendar" to Routes.CALENDAR,
        "Symbols / pairs" to Routes.INSTRUMENTS,
        "Risk settings" to Routes.RISK_SETTINGS,
        "Settings" to Routes.SETTINGS
    )

    TopLevelScaffold(
        title = "TradeLog",
        actions = {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.Menu, "Menu") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                quickLinks.forEach { (label, route) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { menuOpen = false; onNavigate(route) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTrade,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Log trade") }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PortfolioHero(state) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "Net P&L", Format.signedMoney(state.netPnl),
                        Modifier.weight(1f),
                        accent = if (state.netPnl >= 0) Win else Loss
                    )
                    StatTile(
                        "Today", Format.signedMoney(state.todayPnl),
                        Modifier.weight(1f),
                        accent = if (state.todayPnl >= 0) Win else Loss
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Win rate", Format.percent(state.winRate), Modifier.weight(1f))
                    StatTile("Win streak", "${state.streak}🔥", Modifier.weight(1f))
                    StatTile("Trades", state.totalTrades.toString(), Modifier.weight(1f))
                }
            }

            if (state.risk.isNotEmpty()) {
                item {
                    SectionCard(modifier = Modifier.clickable { onNavigate(Routes.RISK_SETTINGS) }) {
                        Text("Risk guardrails", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Room left before a breach · tap to edit your risk plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        state.risk.take(3).forEach { r -> RiskRow(r) }
                    }
                }
            }

            countdown?.let { c ->
                item {
                    val days = DateUtils.daysUntil(c.targetDateMillis)
                    val expired = days < 0
                    SectionCard(modifier = Modifier.clickable { onNavigate(Routes.COUNTDOWN) }) {
                        Text(
                            CountdownMessages.daysLeftLabel(days, c.title),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (expired) "Tap to review this goal." else CountdownMessages.push(days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (state.goals.isNotEmpty()) {
                item {
                    SectionCard(title = "Goals") {
                        state.goals.forEach { g ->
                            ProgressRow(g.goal.title, g.progress, g.goal.target, g.goal.unit)
                        }
                    }
                }
            }

            if (state.tasks.isNotEmpty()) {
                item {
                    SectionCard(title = "Today's tasks") {
                        state.tasks.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = t.doneToday,
                                    onCheckedChange = { vm.toggleTask(t.task, it) }
                                )
                                Text(t.task.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Recent trades") {
                    if (state.recentTrades.isEmpty()) {
                        Text("No trades yet. Tap “Log trade” to start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.recentTrades.forEach { t ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onOpenTrade(t.id) }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.instrument, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${t.direction} · ${DateUtils.formatEpochDate(t.openedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    Format.signedMoney(t.pnl),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = resultColor(t.result)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Economic calendar · myfxbook") {
                    MyfxbookCalendar(
                        impacts = setOf(Impact.MEDIUM, Impact.HIGH),
                        currencies = emptySet(),
                        modifier = Modifier.fillMaxWidth().height(440.dp)
                    )
                    TextButton(onClick = { onNavigate(Routes.CALENDAR) }) { Text("Open full calendar →") }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun PortfolioHero(state: DashboardState) {
    val positive = state.netPnl >= 0
    val pct = if (state.investedBase > 0) state.netPnl / state.investedBase * 100.0 else 0.0
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2D6BFF), Color(0xFF1B4DD8))))
            .padding(22.dp)
    ) {
        Column {
            Text(
                "PORTFOLIO VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Format.money(state.portfolioValue, state.currency),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (positive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "${Format.signedMoney(state.netPnl)} · ${String.format("%+.2f%%", pct)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
        }
    }
}

private fun riskColor(ratio: Double): Color = when {
    ratio >= 1.0 -> Loss
    ratio >= 0.8 -> Color(0xFFE0902A)
    else -> Win
}

@Composable
private fun RiskRow(r: RiskStatusUi) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.accountName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            val leftToday = (r.dailyLimit - r.dailyUsed).coerceAtLeast(0.0)
            Text(
                "${Format.money(leftToday, r.currency)} left today",
                style = MaterialTheme.typography.labelMedium,
                color = riskColor(r.dailyRatio)
            )
        }
        RiskBar("Daily loss", r.dailyUsed, r.dailyLimit, r.dailyRatio, r.currency)
        RiskBar("Drawdown", r.drawdownUsed, r.drawdownLimit, r.drawdownRatio, r.currency)
    }
}

@Composable
private fun RiskBar(label: String, used: Double, limit: Double, ratio: Double, currency: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${Format.money(used, currency)} / ${Format.money(limit, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            Modifier.fillMaxWidth().height(8.dp).padding(top = 3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(riskColor(ratio))
            )
        }
    }
}
