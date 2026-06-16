package com.tradelog.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tradelog.app.di.ServiceLocator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Schedules a daily notification per task/routine/goal that has a reminder time set. */
object ReminderScheduler {
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_HOUR = "reminder_hour"
    const val EXTRA_MINUTE = "reminder_minute"
    const val EXTRA_REQUEST = "reminder_request"
    const val EXTRA_KIND = "reminder_kind"

    private const val TASK_BASE = 300_000
    private const val GOAL_BASE = 400_000

    /** Re-arm every item's reminder from the database (cancels ones that no longer have a time). */
    suspend fun rescheduleAll(context: Context) {
        val repo = ServiceLocator.repository(context.applicationContext)
        repo.tasks.first().forEach { t ->
            val rc = TASK_BASE + t.id.toInt()
            if (t.reminderHour in 0..23) schedule(context, rc, t.title, "task", t.reminderHour, t.reminderMinute)
            else cancel(context, rc)
        }
        repo.allGoals.first().forEach { g ->
            val rc = GOAL_BASE + g.id.toInt()
            if (!g.archived && g.reminderHour in 0..23) schedule(context, rc, g.title, "goal", g.reminderHour, g.reminderMinute)
            else cancel(context, rc)
        }
    }

    fun schedule(context: Context, requestCode: Int, title: String, kind: String, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, requestCode, title, kind, hour, minute)
        val triggerAt = nextTrigger(hour, minute)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, requestCode, "", "task", 0, 0))
    }

    fun cancelTask(context: Context, id: Long) = cancel(context, TASK_BASE + id.toInt())
    fun cancelGoal(context: Context, id: Long) = cancel(context, GOAL_BASE + id.toInt())

    private fun pendingIntent(
        context: Context, requestCode: Int, title: String, kind: String, hour: Int, minute: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
            putExtra(EXTRA_REQUEST, requestCode)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var target = LocalDate.now(zone).atTime(LocalTime.of(hour, minute)).atZone(zone)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }
}
