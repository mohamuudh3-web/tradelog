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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.Pill
import com.tradelog.app.ui.theme.Loss
import com.tradelog.app.ui.theme.Neutral
import com.tradelog.app.ui.theme.Teal
import com.tradelog.app.ui.theme.Win
import com.tradelog.app.util.DateUtils
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.Color
import java.io.File

private fun backtestResultColor(result: String): Color = when (result.uppercase()) {
    "WIN" -> Win
    "LOSS" -> Loss
    else -> Neutral
}

@Composable
fun BacktestGalleryScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val vm: BacktestListViewModel = appViewModel()
    val items by vm.items.collectAsStateWithLifecycle()

    DetailScaffold(
        title = "Backtesting journal",
        onBack = onBack,
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
                            if (item.coverPath != null) {
                                AsyncImage(
                                    model = File(item.coverPath),
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
                                Text("#${bt.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val context = LocalContext.current

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
            FormField(form.title, { v -> vm.update { it.copy(title = v) } }, "Title")
            FormField(form.instrument, { v -> vm.update { it.copy(instrument = v) } }, "Instrument")
            if (instruments.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    instruments.take(6).forEach { ins ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { vm.update { it.copy(instrument = ins.name) } }
                        ) { Text(ins.name, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium) }
                    }
                }
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
            Text("Result", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WIN", "LOSS", "BE").forEach { r ->
                    FilterChip(
                        selected = form.result == r,
                        onClick = { vm.update { it.copy(result = if (it.result == r) "" else r) } },
                        label = { Text(r) }
                    )
                }
            }
            FormField(form.session, { v -> vm.update { it.copy(session = v) } }, "Session / strategy tag (e.g. S2, London)")
            FormField(form.bias, { v -> vm.update { it.copy(bias = v) } }, "Bias note (e.g. Bullish, A+ setup)")
            FormField(form.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes — what you saw, why you'd take it", singleLine = false, minLines = 4)

            Text("Date: ${DateUtils.formatEpochDate(form.dateMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Screenshots (${images.size})", style = MaterialTheme.typography.titleMedium)
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
                            AsyncImage(
                                model = File(img.path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
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
}
