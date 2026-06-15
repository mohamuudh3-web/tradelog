package com.tradelog.app.di

import android.content.Context
import com.tradelog.app.data.SettingsStore
import com.tradelog.app.data.SyncStore
import com.tradelog.app.data.db.AppDatabase
import com.tradelog.app.network.NetworkModule
import com.tradelog.app.network.SupabaseClient
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.sync.SyncEngine

/** Tiny manual DI container — avoids the weight of a DI framework. */
object ServiceLocator {

    @Volatile private var repo: TradeLogRepository? = null
    @Volatile private var sync: SyncEngine? = null
    @Volatile private var store: SyncStore? = null
    @Volatile private var supa: SupabaseClient? = null

    fun repository(context: Context): TradeLogRepository =
        repo ?: synchronized(this) {
            repo ?: build(context.applicationContext).also { repo = it }
        }

    fun syncStore(context: Context): SyncStore =
        store ?: synchronized(this) {
            store ?: SyncStore(context.applicationContext).also { store = it }
        }

    fun supabase(context: Context): SupabaseClient =
        supa ?: synchronized(this) {
            supa ?: SupabaseClient(syncStore(context.applicationContext)).also { supa = it }
        }

    fun syncEngine(context: Context): SyncEngine =
        sync ?: synchronized(this) {
            sync ?: run {
                val app = context.applicationContext
                val db = AppDatabase.get(app)
                SyncEngine(db, supabase(app), syncStore(app))
            }.also { sync = it }
        }

    private fun build(appContext: Context): TradeLogRepository {
        val db = AppDatabase.get(appContext)
        val settings = SettingsStore(appContext)
        return TradeLogRepository(db, settings, NetworkModule.api)
    }
}
