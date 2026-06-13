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

        val events = repo.highImpactToday()
        val title = "Today's high-impact news"
        val body = if (events.isEmpty()) {
            "No high-impact economic events scheduled today. Trade your plan."
        } else {
            events.take(6).joinToString("\n") { e ->
                "${DateUtils.formatEpochTime(e.dateTimeUtc)}  ${e.country}  ${e.title}"
            }.let { lines ->
                if (events.size > 6) "$lines\n+${events.size - 6} more" else lines
            }
        }

        NotificationHelper.showBriefing(applicationContext, title, body)
        return Result.success()
    }
}
