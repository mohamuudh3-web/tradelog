package com.tradelog.app.ui.portfolio

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.ConfirmDeleteAction
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.SwipeToDelete
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.Format

@Composable
fun PortfolioScreen(onAdd: () -> Unit, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val vm: PortfolioViewModel = appViewModel()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val pnl by vm.pnlByAccount.collectAsStateWithLifecycle()

    LaunchedEffect(accounts.size) { vm.refreshPnl() }

    DetailScaffold(
        title = "Portfolio",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Add account") } }
    ) { inner ->
        if (accounts.isEmpty()) {
            EmptyState("No accounts yet. Tap + to add one.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                val accPnl = pnl[acc.id] ?: 0.0
                SwipeToDelete(onDelete = { vm.delete(acc) }) {
                    SectionCard(modifier = Modifier.clickable { onEdit(acc.id) }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(acc.name, style = MaterialTheme.typography.titleMedium)
                                    if (acc.isPropFirm) Pill("PROP", Teal)
                                }
                                Text(acc.broker, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                // Current balance = starting balance + realized P&L from this account's trades.
                                Text(Format.money(acc.balance + accPnl, acc.currency), style = MaterialTheme.typography.titleMedium)
                                Text(Format.signedMoney(accPnl, acc.currency), style = MaterialTheme.typography.bodySmall, color = if (accPnl >= 0) Win else Loss)
                                Text("start ${Format.money(acc.balance, acc.currency)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountEditScreen(accountId: Long, onBack: () -> Unit) {
    val vm: AccountEditViewModel = appViewModel()
    val account by vm.account.collectAsStateWithLifecycle()
    LaunchedEffect(accountId) { vm.load(accountId) }

    DetailScaffold(
        title = if (accountId == 0L) "New account" else "Edit account",
        onBack = onBack,
        actions = {
            if (accountId != 0L) ConfirmDeleteAction("account") { vm.delete(onBack) }
            IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(account.name, { v -> vm.update { it.copy(name = v) } }, "Account name")
            FormField(account.broker, { v -> vm.update { it.copy(broker = v) } }, "Broker / prop firm")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(if (account.balance == 0.0) "" else account.balance.toString(), { v -> vm.update { it.copy(balance = v.toDoubleOrNull() ?: 0.0) } }, "Balance", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(account.currency.ifBlank { "USD" }, { v -> vm.update { it.copy(currency = v.uppercase()) } }, "Currency", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Prop firm account")
                Switch(checked = account.isPropFirm, onCheckedChange = { c -> vm.update { it.copy(isPropFirm = c) } })
            }

            if (account.isPropFirm) {
                SectionCard(title = "Prop firm challenge") {
                    Text("Phase", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Phase 1", "Phase 2", "Funded").forEach { p ->
                            FilterChip(account.challengePhase == p, { vm.update { it.copy(challengePhase = if (it.challengePhase == p) "" else p) } }, { Text(p) })
                        }
                    }
                    Text("Status", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Active", "Funded", "Breached", "Inactive").forEach { s ->
                            FilterChip(account.status == s, { vm.update { it.copy(status = if (it.status == s) "" else s) } }, { Text(s) })
                        }
                    }
                    FormField(account.website, { v -> vm.update { it.copy(website = v) } }, "Website", Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FormField(numOrEmpty(account.startingBalance), { v -> vm.update { it.copy(startingBalance = v.toDoubleOrNull()) } }, "Start bal", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        FormField(numOrEmpty(account.splitPercent), { v -> vm.update { it.copy(splitPercent = v.toDoubleOrNull()) } }, "Split %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FormField(numOrEmpty(account.drawdownPercent), { v -> vm.update { it.copy(drawdownPercent = v.toDoubleOrNull()) } }, "Drawdown %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        FormField(numOrEmpty(account.targetPercent), { v -> vm.update { it.copy(targetPercent = v.toDoubleOrNull()) } }, "Target %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                }
            }
        }
    }
}

private fun numOrEmpty(v: Double?): String =
    v?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
