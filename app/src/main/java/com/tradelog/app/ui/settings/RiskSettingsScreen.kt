package com.tradelog.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.SectionCard

@Composable
fun RiskSettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = appViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var perTrade by remember { mutableStateOf("") }
    var perDay by remember { mutableStateOf("") }
    var perGroup by remember { mutableStateOf("") }
    var maxDaily by remember { mutableStateOf("") }
    var maxDd by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Seed the fields once from the stored settings.
    LaunchedEffect(settings) {
        if (perTrade.isBlank()) {
            perTrade = trim(settings.riskPerTradePct)
            perDay = trim(settings.riskPerDayPct)
            perGroup = trim(settings.riskPerGroupPct)
            maxDaily = trim(settings.maxDailyLossPct)
            maxDd = trim(settings.maxDrawdownPct)
        }
    }

    DetailScaffold(title = "Risk settings", onBack = onBack) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Risk plan") {
                Text(
                    "Limits as a percent of each account's starting balance. Your dashboard shows how much room you have left before a breach.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RiskField("Risk per trade %", perTrade, { perTrade = it }, Modifier.weight(1f))
                    RiskField("Risk per day %", perDay, { perDay = it }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RiskField("Risk per group %", perGroup, { perGroup = it }, Modifier.weight(1f))
                    RiskField("Max daily loss %", maxDaily, { maxDaily = it }, Modifier.weight(1f))
                }
                RiskField("Max overall drawdown %", maxDd, { maxDd = it }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            Button(
                onClick = {
                    vm.setRisk(
                        perTrade.toDoubleOrNull() ?: 1.0,
                        perDay.toDoubleOrNull() ?: 2.0,
                        perGroup.toDoubleOrNull() ?: 10.0,
                        maxDaily.toDoubleOrNull() ?: 5.0,
                        maxDd.toDoubleOrNull() ?: 10.0
                    )
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saved) "Saved ✓" else "Save risk plan") }

            Text(
                "Tip: prop firms (e.g. FTMO) usually cap daily loss around 5% and total drawdown around 10%. Keep per-group risk well under your max drawdown so a losing group can't fail the account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun trim(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

@Composable
private fun RiskField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
