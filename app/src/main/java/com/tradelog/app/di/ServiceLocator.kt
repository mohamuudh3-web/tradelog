package com.tradelog.app.di

import android.content.Context
import com.tradelog.app.data.SettingsStore
import com.tradelog.app.data.db.AppDatabase
import com.tradelog.app.network.NetworkModule
import com.tradelog.app.repository.TradeLogRepository

/** Tiny manual DI container — avoids the weight of a DI framework. */
object ServiceLocator {

    @Volatile private var repo: TradeLogRepository? = null

    fun repository(context: Context): TradeLogRepository =
        repo ?: synchronized(this) {
            repo ?: build(context.applicationContext).also { repo = it }
        }

    private fun build(appContext: Context): TradeLogRepository {
        val db = AppDatabase.get(appContext)
        val settings = SettingsStore(appContext)
        return TradeLogRepository(db, settings, NetworkModule.api)
    }
}
