package com.tradelog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.tradelog.app.di.ServiceLocator
import com.tradelog.app.ui.navigation.AppRoot
import com.tradelog.app.ui.onboarding.OnboardingScreen
import com.tradelog.app.ui.theme.TradeLogTheme
import com.tradelog.app.work.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val openCalendar = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openCalendar.value = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CALENDAR, false) == true

        val prefs = getSharedPreferences("tradelog_prefs", MODE_PRIVATE)

        // Pull/push cloud data on launch when the user is signed in (no-op otherwise).
        lifecycleScope.launch {
            runCatching {
                if (ServiceLocator.syncStore(applicationContext).current().isLoggedIn) {
                    ServiceLocator.syncEngine(applicationContext).syncAll()
                }
            }
        }

        setContent {
            TradeLogTheme {
                var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
                if (!onboarded) {
                    OnboardingScreen(onFinish = {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        onboarded = true
                    })
                } else {
                    AppRoot(openCalendar = openCalendar.value, onCalendarConsumed = { openCalendar.value = false })
                }
            }
        }
    }
}
