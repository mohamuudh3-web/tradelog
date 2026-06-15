package com.tradelog.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tradelog.app.di.ServiceLocator
import com.tradelog.app.util.DateUtils

class MorningBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = ServiceLocator.repository(applicationContext)

        // Best-effort refresh; fall back to cached events if offline.
        repo.refreshCalendar()

        val today = repo.importantToday()
        val soon = repo.highImpactSoon()
        val title = "Today's economic events"

        val body = buildString {
            if (today.isNotEmpty()) {
                append(today.take(6).joinToString("\n") { e ->
                    "${DateUtils.formatEpochTime(e.dateTimeUtc)}  ${e.country}  ${e.title} (${e.impact.name})"
                })
                if (today.size > 6) append("\n+${today.size - 6} more today")
            } else {
                append("No high/medium events today.")
            }
            if (soon.isNotEmpty()) {
                append("\n\nComing up:")
                append("\n" + soon.take(4).joinToString("\n") { e ->
                    "${DateUtils.formatEpochDateTime(e.dateTimeUtc)}  ${e.country}  ${e.title}"
                })
            }
        }

        NotificationHelper.showBriefing(applicationContext, title, body)

        // Refresh upcoming high-impact alerts now that the calendar is updated.
        NewsAlertScheduler.scheduleAll(applicationContext)
        return Result.success()
    }
}
