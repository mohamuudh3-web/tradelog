package com.tradelog.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.SectionCard

private val MOODS = listOf("Happy", "Calm", "Excited", "Annoyed", "Sad")

@Composable
fun DailyEntryScreen(date: String, onBack: () -> Unit) {
    val vm: DailyEntryViewModel = appViewModel()
    val entry by vm.entry.collectAsStateWithLifecycle()

    LaunchedEffect(date) { vm.load(date) }

    fun numStr(v: Double?) = v?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""

    DetailScaffold(
        title = "Daily journal · $date",
        onBack = onBack,
        actions = { IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") } }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormField(entry.title, { v -> vm.update { it.copy(title = v) } }, "Title")

            Text("How are you feeling?", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOODS.forEach { m ->
                    FilterChip(entry.moodLabel == m, { vm.update { it.copy(moodLabel = if (it.moodLabel == m) "" else m) } }, { Text(m) })
                }
            }

            FormField(entry.gratitude, { v -> vm.update { it.copy(gratitude = v) } }, "Gratitude", singleLine = false, minLines = 2)
            FormField(entry.battlePlan, { v -> vm.update { it.copy(battlePlan = v) } }, "Daily battle plan", singleLine = false, minLines = 3)

            SectionCard(title = "Targets") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(numStr(entry.accountBalance), { v -> vm.update { it.copy(accountBalance = v.toDoubleOrNull()) } }, "Balance", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    FormField(entry.tradesTarget?.toString() ?: "", { v -> vm.update { it.copy(tradesTarget = v.toIntOrNull()) } }, "No. trades", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 10.dp)) {
                    FormField(numStr(entry.pipsTarget), { v -> vm.update { it.copy(pipsTarget = v.toDoubleOrNull()) } }, "Pips target", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    FormField(numStr(entry.riskPercent), { v -> vm.update { it.copy(riskPercent = v.toDoubleOrNull()) } }, "Risk %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    FormField(numStr(entry.riskAmount), { v -> vm.update { it.copy(riskAmount = v.toDoubleOrNull()) } }, "Risk amt", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
            }

            FormField(entry.focusTasks, { v -> vm.update { it.copy(focusTasks = v) } }, "Focus tasks (one per line)", singleLine = false, minLines = 3)
            FormField(entry.affirmation, { v -> vm.update { it.copy(affirmation = v) } }, "Today's affirmation")
            FormField(entry.tags, { v -> vm.update { it.copy(tags = v) } }, "Tags (comma separated)")

            FormField(entry.mindset, { v -> vm.update { it.copy(mindset = v) } }, "Mindset", singleLine = false, minLines = 2)
            FormField(entry.routine, { v -> vm.update { it.copy(routine = v) } }, "Routine", singleLine = false, minLines = 2)
            FormField(entry.reflection, { v -> vm.update { it.copy(reflection = v) } }, "Reflection", singleLine = false, minLines = 3)

            RatingRow("Mood rating", entry.mood) { v -> vm.update { it.copy(mood = v) } }
            RatingRow("Discipline", entry.discipline) { v -> vm.update { it.copy(discipline = v) } }
        }
    }
}

@Composable
private fun RatingRow(label: String, value: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { n ->
                FilterChip(selected = value == n, onClick = { onSelect(n) }, label = { Text(n.toString()) })
            }
        }
    }
}
