package com.tradelog.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tradelog.app.ui.common.SectionCard
import com.tradelog.app.ui.common.TopLevelScaffold
import com.tradelog.app.ui.navigation.Routes

private data class MoreItem(val label: String, val route: String, val icon: ImageVector)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val items = listOf(
        MoreItem("Portfolio", Routes.PORTFOLIO, Icons.Filled.AccountBalanceWallet),
        MoreItem("Backtesting journal", Routes.BACKTESTS, Icons.Filled.CandlestickChart),
        MoreItem("Goal countdown", Routes.COUNTDOWN, Icons.Filled.Flag),
        MoreItem("Notebook", Routes.NOTEBOOK, Icons.AutoMirrored.Filled.MenuBook),
        MoreItem("Payouts", Routes.PAYOUTS, Icons.Filled.Payments),
        MoreItem("Trading tools", Routes.TOOLS, Icons.Filled.Build),
        MoreItem("Cloud sync (website)", Routes.SYNC, Icons.Filled.CloudSync),
        MoreItem("Settings", Routes.SETTINGS, Icons.Filled.Settings)
    )
    TopLevelScaffold(title = "More") { inner ->
        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                SectionCard(modifier = Modifier.clickable { onNavigate(item.route) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Text(item.label, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
