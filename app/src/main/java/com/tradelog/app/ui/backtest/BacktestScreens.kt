package com.tradelog.app.ui.backtest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.ConfirmDeleteAction
import com.tradelog.app.ui.common.ConfirmationChecklist
import com.tradelog.app.ui.common.DatePickerField
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.CurrencyDropdown
import com.tradelog.app.ui.common.DropdownField
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.ImageUrlField
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.ZoomableAsyncImage
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Neutral
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.DateUtils
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.Color
import java.io.File

private val SESSIONS = listOf("ASIA", "LONDON", "NEW YORK")

@Composable
private fun ChartSlot(label: String, accent: Color, url: String, onUrl: (String) -> Unit) {
    SectionCard {
        Text(label, color = accent, style = MaterialTheme.typography.labelLarge)
        androidx.compose.material3.OutlinedTextField(
            value = url,
            onValueChange = onUrl,
            label = { Text("Paste image URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        if (url.isNotBlank()) {
            ZoomableAsyncImage(url, Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp).clip(RoundedCornerShape(10.dp)))
            androidx.compose.material3.TextButton(onClick = { onUrl("") }) { Text("Clear") }
        }
    }
}

private fun backtestResultColor(result: String): Color = when (result.uppercase()) {
    "WIN" -> Win
    "LOSS" -> Loss
    else -> Neutral
}

private fun backtestRiskReward(slPips: String, tpPips: String): String {
    val stop = kotlin.math.abs(slPips.toDoubleOrNull() ?: return "")
    val target = kotlin.math.abs(tpPips.toDoubleOrNull() ?: return "")
    if (stop == 0.0 || target == 0.0) return ""
    val rounded = kotlin.math.round((target / stop) * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

@Composable
fun BacktestGalleryScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit, onStats: () -> Unit, onBack: () -> Unit) {
    val vm: BacktestListViewModel = appViewModel()
    val items by vm.items.collectAsStateWithLifecycle()

    DetailScaffold(
        title = "Backtesting journal",
        onBack = onBack,
        actions = { IconButton(onClick = onStats) { Icon(Icons.Filled.BarChart, "Statistics") } },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "New backtest") } }
    ) { inner ->
        if (items.isEmpty()) {
            EmptyState("No backtests yet. Tap + to add one with screenshots.", modifier = Modifier.padding(inner))
            return@DetailScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.backtest.id }) { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { onOpen(item.backtest.id) }
                ) {
                    val bt = item.backtest
                    Column {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1.3f)
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.coverModel != null) {
                                AsyncImage(
                                    model = item.coverModel,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (bt.result.isNotBlank()) {
                                Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                                    Pill(bt.result, backtestResultColor(bt.result))
                                }
                            }
                        }
                        Column(Modifier.padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(bt.instrument.ifBlank { bt.title }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("#${items.size - items.indexOf(item)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (bt.direction.isNotBlank() || bt.session.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    if (bt.direction.isNotBlank()) Pill(bt.direction, if (bt.direction.equals("Sell", true)) Loss else Teal)
                                    if (bt.session.isNotBlank()) Pill(bt.session, MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                "${item.imageCount} shot${if (item.imageCount == 1) "" else "s"} · ${DateUtils.formatEpochDate(bt.dateMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (bt.notes.isNotBlank()) {
                                Text(
                                    bt.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BacktestEditScreen(backtestId: Long, onBack: () -> Unit) {
    val vm: BacktestEditViewModel = appViewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val images by vm.images.collectAsStateWithLifecycle()
    val instruments by vm.instruments.collectAsStateWithLifecycle()
    val rules by vm.checklistRules.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddPair by remember { mutableStateOf(false) }

    LaunchedEffect(backtestId) { vm.load(backtestId) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addImage(context, uri)
    }

    DetailScaffold(
        title = if (backtestId == 0L) "New backtest" else "Edit backtest",
        onBack = onBack,
        actions = {
            if (backtestId != 0L) ConfirmDeleteAction("backtest") { vm.delete(onBack) }
            IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DatePickerField("Date", form.dateMillis) { picked -> vm.update { it.copy(dateMillis = picked) } }
            DropdownField("Session", SESSIONS, form.session.ifBlank { null }, { it }, { s -> vm.update { it.copy(session = s) } })
            CurrencyDropdown(form.currency, { v -> vm.update { it.copy(currency = v) } }, label = "Currency (recorded only)")

            FormField(form.instrument, { v -> vm.update { it.copy(instrument = v) } }, "Symbol / pair")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    listItems(instruments, key = { it.id }) { ins ->
                        FilterChip(form.instrument.equals(ins.name, true), { vm.update { it.copy(instrument = ins.name) } }, { Text(ins.name) })
                    }
                }
                IconButton(onClick = { showAddPair = true }) { Icon(Icons.Filled.Add, "Add pair") }
            }
            Text("Direction", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Buy", "Sell").forEach { d ->
                    FilterChip(
                        selected = form.direction == d,
                        onClick = { vm.update { it.copy(direction = if (it.direction == d) "" else d) } },
                        label = { Text(d) }
                    )
                }
            }
            Text("Backtest result", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val isWin = form.result == "WIN"
                val isLoss = form.result == "LOSS"
                Button(
                    onClick = { vm.update { it.copy(result = if (it.result == "WIN") "" else "WIN") } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWin) Win else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isWin) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) { Text("WIN") }
                Button(
                    onClick = { vm.update { it.copy(result = if (it.result == "LOSS") "" else "LOSS") } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoss) Loss else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isLoss) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) { Text("LOSS") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.slPips, { v -> vm.update { it.copy(slPips = v) } }, "SL pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.tpPips, { v -> vm.update { it.copy(tpPips = v) } }, "TP pips", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            backtestRiskReward(form.slPips, form.tpPips).takeIf { it.isNotBlank() }?.let { rr ->
                Text("RR 1:$rr", style = MaterialTheme.typography.labelLarge, color = Teal)
            }
            FormField(form.bias, { v -> vm.update { it.copy(bias = v) } }, "Scenario note (e.g. S2 London sweep)")

            ConfirmationChecklist(
                rules = rules,
                checked = form.checkedRules,
                onToggle = vm::toggleRule,
                onAddRule = vm::addChecklistRule,
                onDeleteRule = vm::deleteRule
            )

            FormField(form.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes — what you saw, why you'd take it", singleLine = false, minLines = 4)

            ChartSlot("5MIN CHART", Color(0xFF3B82F6), form.chart5Url) { v -> vm.update { it.copy(chart5Url = v) } }
            ChartSlot("15MIN CHART", Win, form.chart15Url) { v -> vm.update { it.copy(chart15Url = v) } }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Device screenshots (${images.size})", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.Image, null)
                    Text("  Add")
                }
            }

            if (images.isEmpty()) {
                Text("Add chart screenshots to build your backtest gallery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height((((images.size + 1) / 2) * 180).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(images, key = { it.id }) { img ->
                        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(10.dp))) {
                            ZoomableAsyncImage(
                                model = File(img.path),
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { vm.deleteImage(img) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            ) { Icon(Icons.Filled.Close, "Remove", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
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
