package com.tradelog.app.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.di.appViewModel
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.util.CsvTradeImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportTradesViewModel(private val repo: TradeLogRepository) : ViewModel() {
    fun import(trades: List<com.tradelog.app.data.entity.Trade>, onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(repo.importTrades(trades)) }
    }
}

@Composable
fun ImportTradesScreen(onBack: () -> Unit) {
    val vm: ImportTradesViewModel = appViewModel()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var status by remember { mutableStateOf("Pick a .csv file exported from your broker (cTrader, MT5, FXPrimus…).") }
    var parsed by remember { mutableStateOf<CsvTradeImport.Result?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            busy = false
            if (text.isNullOrBlank()) { status = "Couldn't read that file."; parsed = null; return@launch }
            val res = CsvTradeImport.parse(text)
            parsed = res
            status = res.error ?: "Found ${res.trades.size} trades${if (res.skipped > 0) " (skipped ${res.skipped} non-trade rows)" else ""}. Review and import."
        }
    }

    DetailScaffold(title = "Import trades (CSV)", onBack = onBack) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard {
                Text("Import from broker", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Export your trade/deal history as CSV from your platform, then pick the file here. " +
                        "Columns are auto-detected (Symbol, Type, Lots, Open/Close, Profit, Time).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                OutlinedButton(
                    onClick = { picker.launch("*/*") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text(if (busy) "Reading…" else "Choose CSV file") }
            }

            Text(status, style = MaterialTheme.typography.bodyMedium)

            val ready = parsed?.trades.orEmpty()
            if (ready.isNotEmpty()) {
                Button(
                    onClick = {
                        busy = true
                        vm.import(ready) { n ->
                            busy = false
                            status = "Imported $n trades. They'll sync to the website automatically."
                            parsed = null
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import ${ready.size} trades") }
            }
        }
    }
}
