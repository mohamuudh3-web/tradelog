package com.tradelog.app.ui.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.TradeResult
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.DropdownField
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TradeEditScreen(tradeId: Long, onBack: () -> Unit) {
    val vm: TradeEditViewModel = appViewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val tags by vm.setupTags.collectAsStateWithLifecycle()
    val instruments by vm.instruments.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(tradeId) { vm.load(tradeId) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = ImageStorage.importImage(context, uri)
                if (path != null) vm.update { it.copy(screenshotUri = path) }
            }
        }
    }

    DetailScaffold(
        title = if (tradeId == 0L) "Log trade" else "Edit trade",
        onBack = onBack,
        actions = {
            IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(form.instrument, { v -> vm.update { it.copy(instrument = v) } }, "Instrument (e.g. EURUSD)")
            if (instruments.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(instruments, key = { it.id }) { ins ->
                        FilterChip(
                            selected = form.instrument.equals(ins.name, ignoreCase = true),
                            onClick = { vm.update { it.copy(instrument = ins.name) } },
                            label = { Text(ins.name) }
                        )
                    }
                }
            }

            DropdownField(
                label = "Account",
                options = accounts,
                selected = accounts.firstOrNull { it.id == form.accountId },
                optionLabel = { it.name },
                onSelect = { acc -> vm.update { it.copy(accountId = acc.id) } }
            )

            Text("Direction")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Direction.entries.forEach { d ->
                    FilterChip(
                        selected = form.direction == d,
                        onClick = { vm.update { it.copy(direction = d) } },
                        label = { Text(d.name) }
                    )
                }
            }

            Text("Result")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TradeResult.entries.forEach { r ->
                    FilterChip(
                        selected = form.result == r,
                        onClick = { vm.update { it.copy(result = r) } },
                        label = { Text(r.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.entry, { v -> vm.update { it.copy(entry = v) } }, "Entry", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.exit, { v -> vm.update { it.copy(exit = v) } }, "Exit", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.lot, { v -> vm.update { it.copy(lot = v) } }, "Lot size", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.riskPct, { v -> vm.update { it.copy(riskPct = v) } }, "Risk %", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(form.rMultiple, { v -> vm.update { it.copy(rMultiple = v) } }, "R multiple", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                FormField(form.pnl, { v -> vm.update { it.copy(pnl = v) } }, "P&L", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }

            FormField(form.setupTag, { v -> vm.update { it.copy(setupTag = v) } }, "Strategy / setup tag")
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    tags.take(5).forEach { tag ->
                        FilterChip(
                            selected = form.setupTag == tag.name,
                            onClick = { vm.update { it.copy(setupTag = tag.name) } },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            FormField(form.notes, { v -> vm.update { it.copy(notes = v) } }, "Notes", singleLine = false, minLines = 3)

            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Filled.Image, null)
                Spacer(Modifier.height(0.dp))
                Text(if (form.screenshotUri == null) "  Add screenshot" else "  Replace screenshot")
            }
            form.screenshotUri?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = "Trade screenshot",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
    }
}
