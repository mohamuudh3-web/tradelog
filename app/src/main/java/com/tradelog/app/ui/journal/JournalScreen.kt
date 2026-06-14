package com.tradelog.app.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.SwipeToDelete
import com.tradelog.app.ui.common.TopLevelScaffold
import com.tradelog.app.ui.common.resultColor
import com.tradelog.app.ui.theme.Amber
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.util.DateUtils
import com.tradelog.app.util.Format

@Composable
fun JournalScreen(
    onAddTrade: () -> Unit,
    onOpenTrade: (Long) -> Unit,
    onOpenDaily: (String) -> Unit
) {
    val vm: JournalViewModel = appViewModel()
    val trades by vm.trades.collectAsStateWithLifecycle()
    val entries by vm.dailyEntries.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    TopLevelScaffold(
        title = "Journal",
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tab == 0) onAddTrade() else onOpenDaily(DateUtils.todayKey())
            }) { Icon(Icons.Filled.Add, "Add") }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Trades") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Daily") })
            }
            if (tab == 0) {
                if (trades.isEmpty()) {
                    EmptyState("No trades logged yet.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trades, key = { it.id }) { t ->
                          SwipeToDelete(onDelete = { vm.deleteTrade(t) }) {
                            SectionCard(modifier = Modifier.clickable { onOpenTrade(t.id) }) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(t.instrument, style = MaterialTheme.typography.titleMedium)
                                            Pill(t.direction.name, if (t.direction.name == "LONG") Teal else Amber)
                                        }
                                        Text(
                                            "${t.setupTag ?: "Untagged"} · ${DateUtils.formatEpochDate(t.openedAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(Format.signedMoney(t.pnl), style = MaterialTheme.typography.titleSmall, color = resultColor(t.result))
                                        Text(Format.rMultiple(t.rMultiple), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                          }
                        }
                    }
                }
            } else {
                if (entries.isEmpty()) {
                    EmptyState("No daily entries yet.")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.id }) { e ->
                          SwipeToDelete(onDelete = { vm.deleteDaily(e) }) {
                            SectionCard(modifier = Modifier.clickable { onOpenDaily(e.date) }) {
                                Text(e.date, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    e.reflection.ifBlank { e.mindset.ifBlank { "Tap to edit" } },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                                Text("Mood ${e.mood}/5 · Discipline ${e.discipline}/5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                          }
                        }
                    }
                }
            }
        }
    }
}
