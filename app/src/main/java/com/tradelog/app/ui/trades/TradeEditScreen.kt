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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.tradelog.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

private val SESSIONS = listOf("S1", "S2", "S3", "S4", "London", "New York", "Asia", "Other")

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
                    FilterChip(form.result == r, { vm.update { it.copy(result = r) } }, { Text(r.name) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.riskPct, { v -> vm.update { it.copy(riskPct = v) } }, "Risk %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.slPips, { v -> vm.update { it.copy(slPips = v) } }, "SL pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.tpPips, { v -> vm.update { it.copy(tpPips = v) } }, "TP pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
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
                onAddRule = vm::addChecklistRule
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
                        AsyncImage(File(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)))
                        RemoveBadge(Modifier.align(Alignment.TopEnd)) { vm.update { it.copy(screenshotUri = null) } }
                    }
                }
                form.imageUrls.forEach { url ->
                    Box(Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp)) {
                        AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)))
                        RemoveBadge(Modifier.align(Alignment.TopEnd)) { vm.removeImageUrl(url) }
                    }
                }
            }

            FormField(form.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 3)
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
