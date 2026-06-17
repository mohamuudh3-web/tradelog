package com.tradelog.app.di

import android.content.Context
import com.tradelog.app.data.SettingsStore
import com.tradelog.app.data.SyncStore
import com.tradelog.app.data.db.AppDatabase
import com.tradelog.app.network.NetworkModule
import com.tradelog.app.network.SupabaseClient
import com.tradelog.app.repository.TradeLogRepository
import com.tradelog.app.sync.SyncEngine
import com.tradelog.app.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tiny manual DI container — avoids the weight of a DI framework. */
object ServiceLocator {

    @Volatile private var repo: TradeLogRepository? = null
    @Volatile private var sync: SyncEngine? = null
    @Volatile private var store: SyncStore? = null
    @Volatile private var supa: SupabaseClient? = null
    @Volatile private var manager: SyncManager? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                SyncEngine(app, db, supabase(app), syncStore(app))
            }.also { sync = it }
        }

    fun syncManager(context: Context): SyncManager =
        manager ?: synchronized(this) {
            manager ?: run {
                val app = context.applicationContext
                SyncManager(app, appScope, syncEngine(app), syncStore(app)).also { mgr ->
                    // Any local save/delete triggers an automatic debounced sync.
                    repository(app).onSyncRequested = { mgr.request() }
                }
            }.also { manager = it }
        }

    private fun build(appContext: Context): TradeLogRepository {
        val db = AppDatabase.get(appContext)
        val settings = SettingsStore(appContext)
        return TradeLogRepository(db, settings, NetworkModule.api)
    }
}
