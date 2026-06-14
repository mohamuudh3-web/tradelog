package com.tradelog.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.StatTile
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.util.Format

@Composable
fun PositionCalcScreen(onBack: () -> Unit) {
    val vm: PositionCalcViewModel = appViewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val presets by vm.presets.collectAsStateWithLifecycle()
    val instruments by vm.instruments.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    var showSave by remember { mutableStateOf(false) }

    DetailScaffold(title = "Position calculator", onBack = onBack) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Saved pairs (suggested first)
            if (instruments.isNotEmpty()) {
                Text("Pair", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(instruments, key = { it.id }) { ins ->
                        FilterChip(
                            selected = form.instrument.equals(ins.name, ignoreCase = true),
                            onClick = { vm.applyInstrument(ins) },
                            label = { Text(ins.name) }
                        )
                    }
                }
            }
            FormField(form.instrument, { v -> vm.update { it.copy(instrument = v) } }, "Instrument")

            // Account balance source
            if (accounts.isNotEmpty()) {
                Text("Use balance from account", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { it.id }) { acc ->
                        AssistChip(
                            onClick = { vm.useAccountBalance(acc) },
                            label = { Text("${acc.name} · ${Format.money(acc.balance, acc.currency)}") }
                        )
                    }
                }
            }
            FormField(form.balance, { v -> vm.update { it.copy(balance = v) } }, "Account balance (or enter any amount)", keyboardType = KeyboardType.Number)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.riskPct, { v -> vm.update { it.copy(riskPct = v) } }, "Risk %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.stopLoss, { v -> vm.update { it.copy(stopLoss = v) } }, "Stop (pips/pts)", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            FormField(form.pipValuePerLot, { v -> vm.update { it.copy(pipValuePerLot = v) } }, "Pip/point value per 1.0 lot", keyboardType = KeyboardType.Number)

            Text("Result — calculates automatically", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Risk amount", if (result.valid) Format.money(result.riskAmount) else "—", Modifier.weight(1f), accent = MaterialTheme.colorScheme.error)
                StatTile("Lot size", if (result.valid) String.format("%.2f", result.lotSize) else "—", Modifier.weight(1f), accent = Teal)
            }
            if (!result.valid) {
                Text(
                    "Enter balance, risk %, stop and pip value to calculate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(onClick = { showSave = true }, modifier = Modifier.fillMaxWidth()) { Text("Save these inputs as a preset (optional)") }

            if (presets.isNotEmpty()) {
                SectionCard(title = "Presets") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets, key = { it.id }) { p ->
                            AssistChip(onClick = { vm.applyPreset(p) }, label = { Text(p.name) })
                        }
                    }
                }
            }
        }
    }

    if (showSave) {
        var name by remember { mutableStateOf(form.instrument) }
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save preset") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Preset name") }, singleLine = true) },
            confirmButton = { Button(onClick = { vm.savePreset(name); showSave = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } }
        )
    }
}
