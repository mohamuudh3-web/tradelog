package com.tradelog.app.ui.trades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.ConfirmationChecklist
import com.tradelog.app.ui.common.DatePickerField
import com.tradelog.app.ui.common.DropdownField
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.ImageUrlField
import com.tradelog.app.ui.common.PsychologyChips
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.ZoomableAsyncImage
import com.tradelog.app.ui.common.resultColor
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Neutral
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.Grade
import com.tradelog.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

private val SESSIONS = listOf("LONDON", "NEW YORK", "ASIA")

@Composable
fun TradeEditScreen(tradeId: Long, onBack: () -> Unit) {
    val vm: TradeEditViewModel = appViewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val instruments by vm.instruments.collectAsStateWithLifecycle()
    val rules by vm.checklistRules.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddPair by remember { mutableStateOf(false) }

    LaunchedEffect(tradeId) { vm.load(tradeId) }
    // Auto-select an account for a new trade so P&L always affects portfolio equity.
    LaunchedEffect(accounts, tradeId) {
        if (tradeId == 0L && form.accountId == null && accounts.isNotEmpty()) {
            vm.update { it.copy(accountId = accounts.first().id) }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            ImageStorage.importImage(context, uri)?.let { path -> vm.update { it.copy(screenshotUri = path) } }
        }
    }

    DetailScaffoldTrade(
        title = if (tradeId == 0L) "Add Trade" else "Edit Trade",
        onBack = onBack,
        onSave = { vm.save(onBack) }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DatePickerField("Date", form.openedAt) { picked -> vm.update { it.copy(openedAt = picked) } }

            DropdownField("Session", SESSIONS, form.session.ifBlank { null }, { it }, { s -> vm.update { it.copy(session = s) } })

            // Account this trade belongs to (its win/loss adjusts that account's balance).
            if (accounts.isNotEmpty()) {
                DropdownField(
                    "Account",
                    accounts,
                    accounts.find { it.id == form.accountId },
                    { it.name },
                    { acc -> vm.update { it.copy(accountId = acc.id) } }
                )
            }

            // Symbol / pair
            FormField(form.instrument, { v -> vm.update { it.copy(instrument = v) } }, "Symbol / pair")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(instruments, key = { it.id }) { ins ->
                        FilterChip(form.instrument.equals(ins.name, true), { vm.update { it.copy(instrument = ins.name) } }, { Text(ins.name) })
                    }
                }
                IconButton(onClick = { showAddPair = true }) { Icon(Icons.Filled.Add, "Add pair") }
            }

            // Direction (Buy/Sell)
            Text("Direction")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(form.direction == Direction.LONG, { vm.update { it.copy(direction = Direction.LONG) } }, { Text("Buy") })
                FilterChip(form.direction == Direction.SHORT, { vm.update { it.copy(direction = Direction.SHORT) } }, { Text("Sell") })
            }

            // Result
            Text("Result")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TradeResult.entries.forEach { r ->
                    FilterChip(
                        selected = form.result == r,
                        onClick = { vm.update { it.copy(result = r) } },
                        label = { Text(r.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = resultColor(r),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.riskPct, { v -> vm.update { it.copy(riskPct = v) } }, "Risk %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.slPips, vm::updateSlPips, "SL pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.tpPips, vm::updateTpPips, "TP pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.entry, { v -> vm.update { it.copy(entry = v) } }, "Entry", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.exit, { v -> vm.update { it.copy(exit = v) } }, "Exit", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.lot, { v -> vm.update { it.copy(lot = v) } }, "Lot", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.rMultiple, { v -> vm.update { it.copy(rMultiple = v) } }, "R", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.pnl, { v -> vm.update { it.copy(pnl = v) } }, "P&L", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }

            FormField(form.setupTag, { v -> vm.update { it.copy(setupTag = v) } }, "Strategy / setup tag")

            ConfirmationChecklist(
                rules = rules,
                checked = form.checkedRules,
                onToggle = vm::toggleRule,
                onAddRule = vm::addChecklistRule,
                onDeleteRule = vm::deleteRule
            )

            Text("Psychology (before entry)")
            PsychologyChips(selected = form.psychology, onToggle = vm::togglePsychology)

            SectionCard(title = "Chart snapshots") {
                ImageUrlField(onAdd = vm::addImageUrl)
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Icon(Icons.Filled.Image, null); Text("  Add from device") }

                form.screenshotUri?.let { path ->
                    Box(Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)) {
                        ZoomableAsyncImage(File(path), Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)))
                        RemoveBadge(Modifier.align(Alignment.TopEnd)) { vm.update { it.copy(screenshotUri = null) } }
                    }
                }
                form.imageUrls.forEach { url ->
                    Box(Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)) {
                        ZoomableAsyncImage(url, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)))
                        RemoveBadge(Modifier.align(Alignment.TopEnd)) { vm.removeImageUrl(url) }
                    }
                }
            }

            FormField(form.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 3)

            // Trade summary
            val total = rules.size
            val checked = form.checkedRules.size
            val missing = (total - checked).coerceAtLeast(0)
            val grade = Grade.of(checked, total)
            SectionCard(title = "Trade summary") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grade", style = MaterialTheme.typography.titleMedium)
                    Text(grade, style = MaterialTheme.typography.headlineSmall, color = gradeColor(grade))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem("Direction", if (form.direction == Direction.LONG) "Buy" else "Sell")
                    SummaryItem("Session", form.session.ifBlank { "—" })
                    SummaryItem("Pair", form.instrument.ifBlank { "—" })
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem("Total rules", total.toString())
                    SummaryItem("Checked", checked.toString())
                    SummaryItem("Missing", missing.toString())
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.save(onBack) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Win, contentColor = Color.White)
                ) { Text("TAKE TRADE", style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Loss)
                ) { Text("Cancel") }
            }
        }
    }

    if (showAddPair) {
        var name by remember { mutableStateOf("") }
        var pip by remember { mutableStateOf("10") }
        AlertDialog(
            onDismissRequest = { showAddPair = false },
            title = { Text("Add pair") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.uppercase() }, label = { Text("Symbol (e.g. EURUSD)") }, singleLine = true)
                    OutlinedTextField(pip, { pip = it }, label = { Text("Pip value per lot") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) { vm.addInstrument(name, pip.toDoubleOrNull() ?: 10.0); vm.update { it.copy(instrument = name) } }
                    showAddPair = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddPair = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun gradeColor(grade: String): Color = when (grade) {
    "A+", "A" -> Win
    "F", "D" -> Loss
    else -> Neutral
}

@Composable
private fun RemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(6.dp).size(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.45f))
    ) { Icon(Icons.Filled.Close, "Remove", tint = Color.White) }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DetailScaffoldTrade(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    com.tradelog.app.ui.common.DetailScaffold(
        title = title,
        onBack = onBack,
        actions = { IconButton(onClick = onSave) { Icon(Icons.Filled.Check, "Save") } },
        content = content
    )
}
