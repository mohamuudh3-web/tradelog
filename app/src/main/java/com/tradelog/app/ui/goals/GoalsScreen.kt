package com.tradelog.app.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.GoalMetric
import com.tradelog.app.data.entity.GoalType
import com.tradelog.app.data.entity.TaskFrequency
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.ProgressRow
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.TopLevelScaffold
import com.tradelog.app.ui.theme.Teal

@Composable
fun GoalsScreen() {
    val vm: GoalsViewModel = appViewModel()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val routine by vm.routine.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showRoutineDialog by remember { mutableStateOf(false) }

    TopLevelScaffold(
        title = "Goals & Tasks",
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    0 -> showGoalDialog = true
                    1 -> showTaskDialog = true
                    else -> showRoutineDialog = true
                }
            }) {
                Icon(Icons.Filled.Add, "Add")
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Goals") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Tasks") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Routine") })
            }
            if (tab == 0) {
                if (goals.isEmpty()) EmptyState("No goals yet. Tap + to add one.")
                else LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(goals, key = { it.goal.id }) { g ->
                        SectionCard {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Pill(g.goal.type.name, Teal)
                                IconButton(onClick = { vm.deleteGoal(g.goal) }) { Icon(Icons.Filled.Delete, "Delete") }
                            }
                            ProgressRow(g.goal.title, g.progress, g.goal.target, g.goal.unit)
                            if (g.goal.metric == GoalMetric.MANUAL) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { vm.increment(g.goal, -1) }) { Icon(Icons.Filled.Remove, "Decrease") }
                                    IconButton(onClick = { vm.increment(g.goal, 1) }) { Icon(Icons.Filled.Add, "Increase") }
                                }
                            } else {
                                Text("Auto-tracked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                TaskChecklist(
                    items = tasks,
                    emptyText = "No tasks yet. Tap + to add one.",
                    showFrequency = true,
                    onToggle = vm::toggleTask,
                    onDelete = vm::deleteTask
                )
            } else {
                TaskChecklist(
                    items = routine,
                    emptyText = "No routine items yet. Tap + to add one.",
                    showFrequency = false,
                    onToggle = vm::toggleTask,
                    onDelete = vm::deleteTask
                )
            }
        }
    }

    if (showGoalDialog) AddGoalDialog(onDismiss = { showGoalDialog = false }, onAdd = { title, type, metric, target, unit ->
        vm.addGoal(title, type, metric, target, unit); showGoalDialog = false
    })
    if (showTaskDialog) AddTaskDialog(onDismiss = { showTaskDialog = false }, onAdd = { title, freq ->
        vm.addTask(title, freq); showTaskDialog = false
    })
    if (showRoutineDialog) AddRoutineDialog(onDismiss = { showRoutineDialog = false }, onAdd = { title ->
        vm.addRoutine(title); showRoutineDialog = false
    })
}

@Composable
private fun TaskChecklist(
    items: List<com.tradelog.app.ui.dashboard.TaskUi>,
    emptyText: String,
    showFrequency: Boolean,
    onToggle: (com.tradelog.app.data.entity.TaskItem, Boolean) -> Unit,
    onDelete: (com.tradelog.app.data.entity.TaskItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items, key = { it.task.id }) { t ->
            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = t.doneToday, onCheckedChange = { onToggle(t.task, it) })
                    Column(Modifier.weight(1f)) {
                        Text(t.task.title, style = MaterialTheme.typography.bodyLarge)
                        if (showFrequency) {
                            Text(t.task.frequency.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { onDelete(t.task) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (String, GoalType, GoalMetric, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GoalType.DAILY) }
    var metric by remember { mutableStateOf(GoalMetric.MANUAL) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalType.entries.forEach { t -> FilterChip(type == t, { type = t }, { Text(t.name) }) }
                }
                Text("Tracking", style = MaterialTheme.typography.labelMedium)
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(metric == GoalMetric.MANUAL, { metric = GoalMetric.MANUAL }, { Text("Manual") })
                        FilterChip(metric == GoalMetric.TRADES, { metric = GoalMetric.TRADES }, { Text("Trades") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(metric == GoalMetric.JOURNAL_ENTRIES, { metric = GoalMetric.JOURNAL_ENTRIES }, { Text("Journals") })
                        FilterChip(metric == GoalMetric.TASKS_COMPLETED, { metric = GoalMetric.TASKS_COMPLETED }, { Text("Tasks") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(target, { target = it.filter { c -> c.isDigit() } }, label = { Text("Target") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, type, metric, target.toIntOrNull() ?: 1, unit) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, TaskFrequency) -> Unit) {
    var title by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf(TaskFrequency.DAILY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskFrequency.entries.forEach { f -> FilterChip(freq == f, { freq = f }, { Text(f.name) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title, freq) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRoutineDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val suggestions = listOf("Woke up early", "Exercise / workout", "Clean room", "Meditate / breathe", "Hydrate", "Read", "Pray", "Plan the day")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to morning routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Routine item") }, singleLine = true)
                Text("Quick add", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { s ->
                        FilterChip(selected = title == s, onClick = { title = s }, label = { Text(s) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
