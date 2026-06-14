package com.tradelog.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.theme.Teal

private data class Goal(val emoji: String, val label: String)

private val GOALS = listOf(
    Goal("📓", "Journal every trade"),
    Goal("🎯", "Pass a prop-firm challenge"),
    Goal("📈", "Improve my win rate"),
    Goal("🔁", "Build a backtesting habit"),
    Goal("🧠", "Stay disciplined")
)
private val CURRENCIES = listOf("USD", "EUR", "GBP", "JPY")

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val vm: OnboardingViewModel = appViewModel()
    val context = LocalContext.current
    val totalSteps = 5
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var style by remember { mutableStateOf("") }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            // Progress dots
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(totalSteps) { i ->
                    Box(
                        Modifier.weight(1f).height(4.dp).background(
                            if (i <= step) Teal else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(2.dp)
                        )
                    )
                }
            }
            if (step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.padding(top = 4.dp)) { Text("Back") }
            }

            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                when (step) {
                    0 -> Welcome()
                    1 -> GoalStep(goal) { goal = it }
                    2 -> SetupStep(currency, { currency = it }, style, { style = it })
                    3 -> NotificationsStep()
                    else -> DoneStep()
                }
            }

            // Bottom actions
            when (step) {
                0 -> PrimaryButton("Get started") { step = 1 }
                1 -> PrimaryButton("Continue", enabled = goal.isNotBlank()) { step = 2 }
                2 -> PrimaryButton("Continue") { vm.setCurrency(currency); step = 3 }
                3 -> {
                    PrimaryButton("Enable notifications") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        vm.enableBriefing(context, 7, 0)
                        step = 4
                    }
                    TextButton(onClick = { step = 4 }, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
                }
                else -> PrimaryButton("Enter TradeLog") { onFinish() }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)
    ) { Text(text, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Welcome() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📈", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("Trade with discipline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Your offline-first trading journal, analytics, backtesting and prop-firm tracker — everything on your device, no account needed.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoalStep(selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("What's your main focus?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        GOALS.forEach { g ->
            val active = selected == g.label
            Surface(
                color = if (active) Teal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(g.label) }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(g.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(
                        g.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (active) Teal else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStep(currency: String, onCurrency: (String) -> Unit, style: String, onStyle: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Quick setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Default currency", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CURRENCIES.forEach { c -> FilterChip(currency == c, { onCurrency(c) }, { Text(c) }) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Trading style", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Personal", "Prop firm").forEach { s -> FilterChip(style == s, { onStyle(s) }, { Text(s) }) }
        }
    }
}

@Composable
private fun NotificationsStep() {
    Column(Modifier.fillMaxWidth()) {
        Text("Never miss high-impact news", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        listOf(
            "🌅  A morning briefing of today's red-folder events",
            "⏰  Alerts a few minutes before each high-impact release",
            "📵  Works without keeping the app open"
        ).forEach {
            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "We'll send a daily 07:00 briefing — change the time or turn it off anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DoneStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✅", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("You're all set", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Tap “Log trade” on the home screen to add your first trade — or open the ☰ menu to explore backtesting, analytics and the economic calendar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
