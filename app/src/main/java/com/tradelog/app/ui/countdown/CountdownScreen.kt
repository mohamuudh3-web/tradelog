package com.tradelog.app.ui.countdown

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.Countdown
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DatePickerField
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.SwipeToDelete
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.util.CountdownMessages
import com.tradelog.app.util.DateUtils

@Composable
fun CountdownScreen(onBack: () -> Unit) {
    val vm: CountdownViewModel = appViewModel()
    val items by vm.countdowns.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf<Countdown?>(null) }

    DetailScaffold(
        title = "Goal countdown",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add goal") } }
    ) { inner ->
        if (items.isEmpty()) {
            EmptyState("No goals yet. Tap + to set a target with a deadline.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { c ->
                val days = DateUtils.daysUntil(c.targetDateMillis)
                val expired = days < 0
                SwipeToDelete(onDelete = { vm.delete(c) }) {
                    SectionCard {
                        Text(
                            CountdownMessages.daysLeftLabel(days, c.title),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (expired) Loss else Teal
                        )
                        Text(
                            if (c.reviewDone) "Reviewed: ${if (c.reachedIt) "Reached ✅" else "Missed"}" else CountdownMessages.push(days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (c.motivation.isNotBlank()) {
                            Text("“${c.motivation}”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text("Target: ${DateUtils.formatEpochDate(c.targetDateMillis)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        if (expired && !c.reviewDone) {
                            Button(onClick = { reviewing = c }, modifier = Modifier.padding(top = 8.dp)) { Text("Review this goal") }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddCountdownDialog(onDismiss = { showAdd = false }, onAdd = { t, d, m, h, min -> vm.add(t, d, m, h, min); showAdd = false })
    reviewing?.let { c ->
        ReviewDialog(c, onDismiss = { reviewing = null }, onSave = { reached, wrong, improve -> vm.saveReview(c, reached, wrong, improve); reviewing = null })
    }
}

@Composable
private fun AddCountdownDialog(onDismiss: () -> Unit, onAdd: (String, Long, String, Int, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var motivation by remember { mutableStateOf("") }
    var target by remember { mutableLongStateOf(System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000) }
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(0) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Goal (e.g. become profitable)") }, singleLine = true)
                DatePickerField("Target date", target) { target = it }
                OutlinedTextField(motivation, { motivation = it }, label = { Text("Motivation (optional)") }, singleLine = false)
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reminder time: %02d:%02d".format(hour, minute))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, target, motivation, hour, minute) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReviewDialog(c: Countdown, onDismiss: () -> Unit, onSave: (Boolean, String, String) -> Unit) {
    var reached by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf("") }
    var improve by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review: ${c.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Did I reach it?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(reached, { reached = true }, { Text("Yes") })
                    FilterChip(!reached, { reached = false }, { Text("No") })
                }
                OutlinedTextField(wrong, { wrong = it }, label = { Text("What went wrong?") }, singleLine = false, minLines = 2)
                OutlinedTextField(improve, { improve = it }, label = { Text("What improves next month?") }, singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(reached, wrong, improve) }) { Text("Save review") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
