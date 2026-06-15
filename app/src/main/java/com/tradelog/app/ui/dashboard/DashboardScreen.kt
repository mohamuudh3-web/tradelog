package com.tradelog.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
        "Pairs / instruments" to Routes.INSTRUMENTS,
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
                    StatTile("Trades", state.totalTrades.toString(), Modifier.weight(1f))
                    StatTile("Goals", state.openGoals.toString(), Modifier.weight(1f))
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
