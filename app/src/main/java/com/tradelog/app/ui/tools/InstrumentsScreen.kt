package com.tradelog.app.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.Instrument
import com.tradelog.app.di.appViewModel
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.SectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstrumentViewModel(private val repo: TradeLogRepository) : ViewModel() {
    val instruments: StateFlow<List<Instrument>> =
        repo.instruments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, pip: Double) = viewModelScope.launch { repo.addInstrument(name, pip) }
    fun delete(instrument: Instrument) = viewModelScope.launch { repo.deleteInstrument(instrument) }
}

@Composable
fun InstrumentsScreen(onBack: () -> Unit) {
    val vm: InstrumentViewModel = appViewModel()
    val instruments by vm.instruments.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "Pairs / instruments",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add pair") } }
    ) { inner ->
        if (instruments.isEmpty()) {
            EmptyState("No saved pairs. Tap + to add one.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(instruments, key = { it.id }) { ins ->
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ins.name, style = MaterialTheme.typography.titleMedium)
                            Text("Pip/point value per lot: ${ins.pipValuePerLot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.delete(ins) }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var pip by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add pair") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.uppercase() }, label = { Text("Symbol (e.g. EURUSD)") }, singleLine = true)
                    OutlinedTextField(pip, { pip = it }, label = { Text("Pip/point value per 1.0 lot") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = { TextButton(onClick = { vm.add(name, pip.toDoubleOrNull() ?: 10.0); showAdd = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
