package com.tradelog.app

import android.app.Application
import com.tradelog.app.data.seed.Seeder
import com.tradelog.app.di.ServiceLocator
import com.tradelog.app.work.BriefingScheduler
import com.tradelog.app.work.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TradeLogApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)

        val repo = ServiceLocator.repository(this)
        val prefs = getSharedPreferences("tradelog_prefs", MODE_PRIVATE)

        appScope.launch {
            if (!prefs.getBoolean("seeded", false)) {
                // Fresh install: full seed already includes v2 data.
                Seeder.seed(repo)
                prefs.edit().putBoolean("seeded", true).putBoolean("seeded_v2", true).apply()
            } else if (!prefs.getBoolean("seeded_v2", false)) {
                // Upgrade from v1: add only the new v2 sample data.
                Seeder.seedV2(repo)
                prefs.edit().putBoolean("seeded_v2", true).apply()
            }
            val settings = repo.settings.settings.first()
            BriefingScheduler.reschedule(
                this@TradeLogApp,
                settings.briefingEnabled,
                settings.briefingHour,
                settings.briefingMinute
            )
        }
    }
}
