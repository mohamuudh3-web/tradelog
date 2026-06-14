package com.tradelog.app.ui.calendar

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tradelog.app.data.entity.Impact
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.impactColor

private val MYFX_CURRENCIES = listOf("AUD", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "NZD", "USD")

/** Myfxbook impact codes: 1 = low, 2 = medium, 3 = high. */
private fun buildWidgetUrl(impacts: Set<Impact>, currencies: Set<String>): String {
    val codes = buildList {
        if (Impact.LOW in impacts) add("1")
        if (Impact.MEDIUM in impacts) add("2")
        if (Impact.HIGH in impacts) add("3")
    }.ifEmpty { listOf("1", "2", "3") }.joinToString(",")
    val symbols = currencies.ifEmpty { MYFX_CURRENCIES.toSet() }.sorted().joinToString(",")
    return "https://widget.myfxbook.com/widget/calendar.html?lang=en&impacts=$codes&symbols=$symbols"
}

@Composable
fun CalendarScreen(onBack: () -> Unit) {
    var impacts by remember { mutableStateOf(setOf(Impact.MEDIUM, Impact.HIGH)) }
    var currencies by remember { mutableStateOf(MYFX_CURRENCIES.toSet()) }
    var reloadKey by remember { mutableStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }

    val url = buildWidgetUrl(impacts, currencies)

    DetailScaffold(
        title = "Economic calendar",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showFilter = true }) { Icon(Icons.Filled.FilterList, "Filter") }
            IconButton(onClick = { reloadKey++ }) { Icon(Icons.Filled.Refresh, "Reload") }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Text(
                "Live from myfxbook · needs internet",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        tag = "$url#$reloadKey"
                        loadUrl(url)
                    }
                },
                update = { wv ->
                    val key = "$url#$reloadKey"
                    if (wv.tag != key) {
                        wv.tag = key
                        wv.loadUrl(url)
                    }
                }
            )
        }
    }

    if (showFilter) {
        AlertDialog(
            onDismissRequest = { showFilter = false },
            confirmButton = { TextButton(onClick = { showFilter = false }) { Text("Done") } },
            title = { Text("Filter calendar") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Impact", style = MaterialTheme.typography.titleSmall)
                    listOf(Impact.HIGH, Impact.MEDIUM, Impact.LOW).forEach { i ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                impacts = if (i in impacts) impacts - i else impacts + i
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = i in impacts, onCheckedChange = { impacts = if (i in impacts) impacts - i else impacts + i })
                            Box(Modifier.size(14.dp).background(impactColor(i), RoundedCornerShape(3.dp)))
                            Text(
                                i.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text("Currencies", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { currencies = MYFX_CURRENCIES.toSet() }) { Text("All") }
                        TextButton(onClick = { currencies = emptySet() }) { Text("None") }
                    }
                    MYFX_CURRENCIES.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                currencies = if (c in currencies) currencies - c else currencies + c
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = c in currencies, onCheckedChange = { currencies = if (c in currencies) currencies - c else currencies + c })
                            Text(c, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        )
    }
}
