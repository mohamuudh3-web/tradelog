package com.tradelog.app.ui.payouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.data.entity.PayoutStatus
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.ConfirmDeleteAction
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.StatTile
import com.tradelog.app.ui.common.SwipeToDelete
import com.tradelog.app.ui.theme.Amber
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.Format

@Composable
fun PayoutsScreen(onAdd: () -> Unit, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val vm: PayoutViewModel = appViewModel()
    val payouts by vm.payouts.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()

    DetailScaffold(
        title = "Payouts",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Add payout") } }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Lifetime paid", Format.money(summary.totalPaid), Modifier.weight(1f), accent = Win)
                    StatTile("Pending", Format.money(summary.pending), Modifier.weight(1f), accent = Amber)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Average", Format.money(summary.average), Modifier.weight(1f))
                    StatTile("Largest", Format.money(summary.largest), Modifier.weight(1f), accent = Win)
                    StatTile("Payouts", summary.count.toString(), Modifier.weight(1f))
                }
            }
            if (summary.byFirm.isNotEmpty()) {
                item {
                    SectionCard(title = "Payouts by firm · top: ${summary.topFirm}") {
                        summary.byFirm.forEach { (firm, amt) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(firm, style = MaterialTheme.typography.bodyMedium)
                                Text(Format.money(amt), style = MaterialTheme.typography.bodyMedium, color = Win)
                            }
                        }
                    }
                }
            }
            if (payouts.isEmpty()) {
                item { EmptyState("No payouts logged yet.") }
            } else {
                items(payouts, key = { it.id }) { p ->
                  SwipeToDelete(onDelete = { vm.delete(p) }) {
                    SectionCard(modifier = Modifier.clickable { onEdit(p.id) }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.accountName.ifBlank { "Payout" }, style = MaterialTheme.typography.titleMedium)
                                Text(p.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Format.money(p.amount, p.currency), style = MaterialTheme.typography.titleSmall)
                                Pill(p.status.name, if (p.status == PayoutStatus.PAID) Win else Amber)
                            }
                        }
                    }
                  }
                }
            }
        }
    }
}

@Composable
fun PayoutEditScreen(payoutId: Long, onBack: () -> Unit) {
    val vm: PayoutEditViewModel = appViewModel()
    val payout by vm.payout.collectAsStateWithLifecycle()
    LaunchedEffect(payoutId) { vm.load(payoutId) }

    DetailScaffold(
        title = if (payoutId == 0L) "New payout" else "Edit payout",
        onBack = onBack,
        actions = {
            if (payoutId != 0L) ConfirmDeleteAction("payout") { vm.delete(onBack) }
            IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(payout.accountName, { v -> vm.update { it.copy(accountName = v) } }, "Account / prop firm")
            FormField(payout.date, { v -> vm.update { it.copy(date = v) } }, "Date (yyyy-MM-dd)")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(if (payout.amount == 0.0) "" else payout.amount.toString(), { v -> vm.update { it.copy(amount = v.toDoubleOrNull() ?: 0.0) } }, "Amount", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(payout.currency.ifBlank { "USD" }, { v -> vm.update { it.copy(currency = v.uppercase()) } }, "Currency", Modifier.weight(1f))
            }
            Text("Status")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PayoutStatus.entries.forEach { s ->
                    FilterChip(payout.status == s, { vm.update { it.copy(status = s) } }, { Text(s.name) })
                }
            }
            FormField(payout.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 2)
        }
    }
}
