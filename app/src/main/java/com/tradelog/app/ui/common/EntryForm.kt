package com.tradelog.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tradelog.app.data.entity.ChecklistRule
import com.tradelog.app.util.DateUtils
import com.tradelog.app.util.Grade

val PSYCHOLOGY_TAGS = listOf(
    "FOMO", "Calm", "Fear", "Greed", "Overconfident",
    "Hesitation", "Revenge", "Impatient", "Focused", "Uncertain"
)

@Composable
fun ConfirmationChecklist(
    rules: List<ChecklistRule>,
    checked: Set<Long>,
    onToggle: (Long) -> Unit,
    onAddRule: (String) -> Unit
) {
    var newRule by remember { mutableStateOf("") }
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Confirmation checklist", style = MaterialTheme.typography.titleMedium)
            Text("${checked.size}/${rules.size}  ·  ${Grade.of(checked.size, rules.size)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        rules.forEach { rule ->
            Row(
                Modifier.fillMaxWidth().clickable { onToggle(rule.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = rule.id in checked, onCheckedChange = { onToggle(rule.id) })
                Text(rule.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newRule,
                onValueChange = { newRule = it },
                label = { Text("Add a rule") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { if (newRule.isNotBlank()) { onAddRule(newRule); newRule = "" } }) { Text("Add") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsychologyChips(selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PSYCHOLOGY_TAGS.forEach { tag ->
            FilterChip(selected = tag in selected, onClick = { onToggle(tag) }, label = { Text(tag) })
        }
    }
}

@Composable
fun ImageUrlField(onAdd: (String) -> Unit, label: String = "Paste image URL") {
    var url by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { if (url.isNotBlank()) { onAdd(url.trim()); url = "" } }, modifier = Modifier.padding(start = 8.dp)) {
            Text("Apply")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, millis: Long, onPick: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${DateUtils.formatEpochDate(millis)}")
    }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let(onPick); show = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
