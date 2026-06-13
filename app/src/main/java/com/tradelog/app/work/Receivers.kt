package com.tradelog.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tradelog.app.di.ServiceLocator
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

/** Re-arm the alarm after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = ServiceLocator.repository(context).settings
        runBlocking {
            val s = settings.settings.first()
            BriefingScheduler.reschedule(context, s.briefingEnabled, s.briefingHour, s.briefingMinute)
        }
    }
}
