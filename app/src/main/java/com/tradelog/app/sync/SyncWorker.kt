package com.tradelog.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tradelog.app.di.ServiceLocator

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = ServiceLocator.syncStore(applicationContext)
        if (!store.current().isLoggedIn) return Result.success()

        val result = ServiceLocator.syncEngine(applicationContext).syncAll()
        return when {
            result.ok -> Result.success()
            result.error == "Sync already running." -> Result.retry()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}
