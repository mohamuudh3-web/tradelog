package com.tradelog.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object BriefingScheduler {
    const val ALARM_REQUEST_CODE = 7001
    private const val PERIODIC_WORK = "calendar_refresh_periodic"

    /** (Re)schedule everything based on the user's settings. */
    fun reschedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        cancelAlarm(context)
        if (enabled) {
            scheduleNextAlarm(context, hour, minute)
        }
        scheduleDailyRefresh(context, enabled)
    }

    fun scheduleNextAlarm(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTrigger(hour, minute)
        val pending = alarmPendingIntent(context)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancelAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BriefingAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        var target = LocalDate.now(zone).atTime(LocalTime.of(hour, minute)).atZone(zone)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /** Periodic WorkManager job (~daily) to keep the calendar cache fresh as a backstop. */
    private fun scheduleDailyRefresh(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<MorningBriefingWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
