package com.tradelog.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tradelog.app.di.ServiceLocator
import kotlinx.coroutines.flow.first

/** Schedules a notification a configurable number of minutes before each upcoming high-impact event. */
object NewsAlertScheduler {
    private const val BASE_REQUEST = 9000
    private const val MAX_ALERTS = 30
    const val EXTRA_TITLE = "title"
    const val EXTRA_COUNTRY = "country"
    const val EXTRA_MINUTES = "minutes"
    const val EXTRA_INDEX = "index"

    suspend fun scheduleAll(context: Context) {
        val appCtx = context.applicationContext
        val repo = ServiceLocator.repository(appCtx)
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Clear any previously scheduled alerts.
        for (i in 0 until MAX_ALERTS) am.cancel(pendingIntent(appCtx, i, Intent(appCtx, NewsAlertReceiver::class.java)))

        val settings = repo.settings.settings.first()
        if (!settings.newsAlertEnabled) return

        val now = System.currentTimeMillis()
        val leadMs = settings.newsAlertMinutes * 60_000L
        val events = repo.upcomingHighImpact().take(MAX_ALERTS)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true

        events.forEachIndexed { i, e ->
            val triggerAt = e.dateTimeUtc - leadMs
            if (triggerAt <= now) return@forEachIndexed
            val intent = Intent(appCtx, NewsAlertReceiver::class.java).apply {
                putExtra(EXTRA_TITLE, e.title)
                putExtra(EXTRA_COUNTRY, e.country)
                putExtra(EXTRA_MINUTES, settings.newsAlertMinutes)
                putExtra(EXTRA_INDEX, i)
            }
            val pi = pendingIntent(appCtx, i, intent)
            try {
                if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    private fun pendingIntent(context: Context, index: Int, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, BASE_REQUEST + index, intent, flags)
    }
}
