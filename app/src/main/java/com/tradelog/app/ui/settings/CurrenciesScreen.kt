package com.tradelog.app.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.Currency
import com.tradelog.app.di.appViewModel
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.SectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurrencyViewModel(private val repo: TradeLogRepository) : ViewModel() {
    val currencies: StateFlow<List<Currency>> =
        repo.currencies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(code: String) = viewModelScope.launch { repo.addCurrency(code) }
    fun delete(currency: Currency) = viewModelScope.launch { repo.deleteCurrency(currency) }
}

@Composable
fun CurrenciesScreen(onBack: () -> Unit) {
    val vm: CurrencyViewModel = appViewModel()
    val currencies by vm.currencies.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "Currencies",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add currency") } }
    ) { inner ->
        if (currencies.isEmpty()) {
            EmptyState("No currencies. Tap + to add one.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currencies, key = { it.id }) { cur ->
                SectionCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(cur.code, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.delete(cur) }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add currency") },
            text = {
                OutlinedTextField(
                    code,
                    { code = it.uppercase() },
                    label = { Text("Code (e.g. USD, AED, ZAR)") },
                    singleLine = true
                )
            },
            confirmButton = { TextButton(onClick = { vm.add(code); showAdd = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
