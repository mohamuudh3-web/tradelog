package com.tradelog.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tradelog.app.di.ServiceLocator
import com.tradelog.app.sync.SyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Fired by AlarmManager at the user's chosen briefing time. */
class BriefingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Run the (networked) briefing off the main thread via WorkManager.
        val work = OneTimeWorkRequestBuilder<MorningBriefingWorker>().build()
        WorkManager.getInstance(context).enqueue(work)

        // Schedule tomorrow's alarm.
        val settings = ServiceLocator.repository(context).settings
        runBlocking {
            val s = settings.settings.first()
            if (s.briefingEnabled) {
                BriefingScheduler.scheduleNextAlarm(context, s.briefingHour, s.briefingMinute)
            }
        }
    }
}

/** Fired before a high-impact economic event. */
class NewsAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(NewsAlertScheduler.EXTRA_TITLE) ?: return
        val country = intent.getStringExtra(NewsAlertScheduler.EXTRA_COUNTRY) ?: ""
        val minutes = intent.getIntExtra(NewsAlertScheduler.EXTRA_MINUTES, 30)
        val index = intent.getIntExtra(NewsAlertScheduler.EXTRA_INDEX, 0)
        NotificationHelper.showNewsAlert(
            context,
            title = "$country news in $minutes min",
            body = title,
            notifId = 2000 + index
        )
    }
}

/** Re-arm alarms after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = ServiceLocator.repository(context).settings
        runBlocking {
            val s = settings.settings.first()
            BriefingScheduler.reschedule(context, s.briefingEnabled, s.briefingHour, s.briefingMinute)
            NewsAlertScheduler.scheduleAll(context)
            ReminderScheduler.rescheduleAll(context)
            SyncScheduler.schedulePeriodic(context)
            SyncScheduler.enqueueNow(context)
        }
    }
}

/** Fired by AlarmManager at a task/routine/goal's reminder time. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: return
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: "task"
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, 0)
        val rc = intent.getIntExtra(ReminderScheduler.EXTRA_REQUEST, 0)
        val label = if (kind == "goal") "Goal reminder" else "Reminder"
        NotificationHelper.showSimple(context, label, title, rc)
        // Re-arm for tomorrow.
        if (hour in 0..23 && rc != 0) ReminderScheduler.schedule(context, rc, title, kind, hour, minute)
    }
}
