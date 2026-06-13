package com.tradelog.app.ui.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.resultColor
import com.tradelog.app.ui.theme.Amber
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.util.DateUtils
import com.tradelog.app.util.Format
import java.io.File

@Composable
fun TradeDetailScreen(tradeId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val vm: TradeDetailViewModel = appViewModel()
    val trade by vm.trade.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(tradeId) { vm.load(tradeId) }

    DetailScaffold(
        title = trade?.instrument ?: "Trade",
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
        }
    ) { inner ->
        val t = trade
        if (t == null) {
            Text("Loading…", Modifier.padding(inner).padding(16.dp))
            return@DetailScaffold
        }
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(t.direction.name, if (t.direction.name == "LONG") Teal else Amber)
                Pill(t.result.name, resultColor(t.result))
                t.setupTag?.let { Pill(it, MaterialTheme.colorScheme.primary) }
            }

            SectionCard {
                DetailRow("P&L", Format.signedMoney(t.pnl))
                DetailRow("R multiple", Format.rMultiple(t.rMultiple))
                DetailRow("Entry", t.entryPrice.toString())
                DetailRow("Exit", t.exitPrice?.toString() ?: "—")
                DetailRow("Lot size", t.lotSize.toString())
                DetailRow("Risk %", t.riskPercent?.let { "$it%" } ?: "—")
                DetailRow("Account", account?.name ?: "—")
                DetailRow("Date", DateUtils.formatEpochDateTime(t.openedAt))
            }

            if (t.notes.isNotBlank()) {
                SectionCard(title = "Notes") { Text(t.notes, style = MaterialTheme.typography.bodyMedium) }
            }

            t.screenshotUri?.let { path ->
                SectionCard(title = "Screenshot") {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Trade screenshot",
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete trade?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
