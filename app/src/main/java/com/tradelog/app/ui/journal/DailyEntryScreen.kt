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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.FormField

@Composable
fun DailyEntryScreen(date: String, onBack: () -> Unit) {
    val vm: DailyEntryViewModel = appViewModel()
    val entry by vm.entry.collectAsStateWithLifecycle()

    LaunchedEffect(date) { vm.load(date) }

    DetailScaffold(
        title = "Daily journal · $date",
        onBack = onBack,
        actions = { IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") } }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormField(entry.mindset, { v -> vm.update { it.copy(mindset = v) } }, "Mindset", singleLine = false, minLines = 2)
            FormField(entry.routine, { v -> vm.update { it.copy(routine = v) } }, "Routine", singleLine = false, minLines = 2)
            FormField(entry.reflection, { v -> vm.update { it.copy(reflection = v) } }, "Reflection", singleLine = false, minLines = 3)

            RatingRow("Mood", entry.mood) { v -> vm.update { it.copy(mood = v) } }
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
