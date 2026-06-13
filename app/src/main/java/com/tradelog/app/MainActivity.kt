package com.tradelog.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.tradelog.app.ui.navigation.AppRoot
import com.tradelog.app.ui.theme.TradeLogTheme
import com.tradelog.app.work.NotificationHelper

class MainActivity : ComponentActivity() {

    private val openCalendar = mutableStateOf(false)

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openCalendar.value = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CALENDAR, false) == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TradeLogTheme {
                AppRoot(openCalendar = openCalendar.value, onCalendarConsumed = { openCalendar.value = false })
            }
        }
    }
}
