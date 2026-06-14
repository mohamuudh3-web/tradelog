package com.tradelog.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.impactColor
import com.tradelog.app.util.DateUtils

@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val vm: CalendarViewModel = appViewModel()
    val events by vm.events.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val impacts by vm.impacts.collectAsStateWithLifecycle()
    val excluded by vm.excludedCurrencies.collectAsStateWithLifecycle()
    val currencies by vm.currencies.collectAsStateWithLifecycle()
    val dayRange by vm.dayRange.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "Economic calendar",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showFilter = true }) { Icon(Icons.Filled.FilterList, "Filter") }
            if (ui.syncing) CircularProgressIndicator(Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
            else IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, "Refresh") }
        }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            // Day range filter (quick access)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(dayRange == DayRange.TODAY, { vm.setDayRange(DayRange.TODAY) }, { Text("Today") })
                FilterChip(dayRange == DayRange.TOMORROW, { vm.setDayRange(DayRange.TOMORROW) }, { Text("Tomorrow") })
                FilterChip(dayRange == DayRange.WEEK, { vm.setDayRange(DayRange.WEEK) }, { Text("This week") })
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (events.isEmpty()) {
                EmptyState(if (ui.syncing) "Fetching events…" else "No events match your filters. Tap the filter or refresh.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.id }) { e ->
                        SectionCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = impactColor(e.impact), shape = RoundedCornerShape(2.dp)) {
                                    Box(Modifier.size(width = 4.dp, height = 44.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp, end = 12.dp)) {
                                    Text(DateUtils.formatEpochTime(e.dateTimeUtc), style = MaterialTheme.typography.titleSmall)
                                    Text(e.country, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(DateUtils.formatEpochDate(e.dateTimeUtc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val stats = buildList {
                                        if (e.previous.isNotBlank()) add("Prev ${e.previous}")
                                        if (e.forecast.isNotBlank()) add("Cons ${e.forecast}")
                                        if (e.actual.isNotBlank()) add("Act ${e.actual}")
                                    }
                                    if (stats.isNotEmpty()) {
                                        Text(stats.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Pill(e.impact.name, impactColor(e.impact))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        CalendarFilterDialog(
            impacts = impacts,
            currencies = currencies,
            excluded = excluded,
            onToggleImpact = vm::toggleImpact,
            onAllImpacts = vm::setAllImpacts,
            onToggleCurrency = vm::toggleCurrency,
            onAllCurrencies = { on -> vm.setAllCurrencies(on, currencies) },
            onDismiss = { showFilter = false }
        )
    }
}

@Composable
private fun CalendarFilterDialog(
    impacts: Set<Impact>,
    currencies: List<String>,
    excluded: Set<String>,
    onToggleImpact: (Impact) -> Unit,
    onAllImpacts: (Boolean) -> Unit,
    onToggleCurrency: (String) -> Unit,
    onAllCurrencies: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Filter events") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Impact", style = MaterialTheme.typography.titleSmall)
                    Row {
                        TextButton(onClick = { onAllImpacts(true) }) { Text("All") }
                        TextButton(onClick = { onAllImpacts(false) }) { Text("None") }
                    }
                }
                Impact.entries.forEach { i ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onToggleImpact(i) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = i in impacts, onCheckedChange = { onToggleImpact(i) })
                        Box(Modifier.size(14.dp).background(impactColor(i), RoundedCornerShape(3.dp)))
                        Text(
                            i.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (currencies.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Currencies", style = MaterialTheme.typography.titleSmall)
                        Row {
                            TextButton(onClick = { onAllCurrencies(true) }) { Text("All") }
                            TextButton(onClick = { onAllCurrencies(false) }) { Text("None") }
                        }
                    }
                    currencies.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggleCurrency(c) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = c !in excluded, onCheckedChange = { onToggleCurrency(c) })
                            Text(c, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    )
}
