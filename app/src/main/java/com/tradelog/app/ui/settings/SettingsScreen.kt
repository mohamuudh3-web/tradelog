package com.tradelog.app.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.SectionCard

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = appViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DetailScaffold(title = "Settings", onBack = onBack) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Morning news briefing") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily briefing notification")
                    Switch(
                        checked = settings.briefingEnabled,
                        onCheckedChange = { vm.setBriefingEnabled(context, it) }
                    )
                }
                val timeLabel = "%02d:%02d".format(settings.briefingHour, settings.briefingMinute)
                Row(
                    Modifier.fillMaxWidth().clickable {
                        TimePickerDialog(
                            context,
                            { _, h, m -> vm.setBriefingTime(context, h, m) },
                            settings.briefingHour,
                            settings.briefingMinute,
                            true
                        ).show()
                    }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Briefing time")
                    Text(timeLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Each morning you'll get a summary of today's high-impact economic events. Tapping it opens the calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { vm.sendTestBriefing(context) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Send test briefing now")
                }
            }

            SectionCard(title = "High-impact news alerts") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Alert before high-impact news")
                    Switch(
                        checked = settings.newsAlertEnabled,
                        onCheckedChange = { vm.setNewsAlert(context, it, settings.newsAlertMinutes) }
                    )
                }
                Text("Notify me this many minutes before each event:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(5, 15, 30, 60).forEach { m ->
                        FilterChip(
                            selected = settings.newsAlertMinutes == m,
                            onClick = { vm.setNewsAlert(context, settings.newsAlertEnabled, m) },
                            label = { Text("$m min") }
                        )
                    }
                }
            }

            SectionCard(title = "Defaults") {
                OutlinedTextField(
                    value = settings.defaultCurrency,
                    onValueChange = { vm.setCurrency(it.uppercase()) },
                    label = { Text("Default currency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "About") {
                Text("TradeLog — offline-first trading journal & analytics.", style = MaterialTheme.typography.bodyMedium)
                Text("All data is stored locally on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
