package com.tradelog.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val impact by vm.impactFilter.collectAsStateWithLifecycle()
    val currency by vm.currencyFilter.collectAsStateWithLifecycle()
    val currencies by vm.currencies.collectAsStateWithLifecycle()

    DetailScaffold(
        title = "Economic calendar",
        onBack = onBack,
        actions = {
            if (ui.syncing) CircularProgressIndicator(Modifier.padding(end = 12.dp), strokeWidth = 2.dp)
            else IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, "Refresh") }
        }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            // Impact filter
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(impact == null, { vm.setImpact(null) }, { Text("All") })
                Impact.entries.forEach { i ->
                    FilterChip(impact == i, { vm.setImpact(i) }, { Text(i.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            // Currency filter
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(currency == null, { vm.setCurrency(null) }, { Text("All ccy") }) }
                items(currencies) { c -> FilterChip(currency == c, { vm.setCurrency(c) }, { Text(c) }) }
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
            }

            if (events.isEmpty()) {
                EmptyState(if (ui.syncing) "Fetching events…" else "No events. Pull refresh to fetch this week.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.id }) { e ->
                        SectionCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                                    Text(DateUtils.formatEpochTime(e.dateTimeUtc), style = MaterialTheme.typography.titleSmall)
                                    Text(e.country, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(DateUtils.formatEpochDate(e.dateTimeUtc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Pill(e.impact.name, impactColor(e.impact))
                            }
                        }
                    }
                }
            }
        }
    }
}
