package com.tradelog.app.sync

import com.tradelog.app.data.SyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drives automatic sync so the user never has to open the sync screen.
 *  - [request] is called after any local save/delete; it debounces, then pushes+pulls.
 *  - [syncNow] runs immediately (app launch / resume / manual button).
 * All calls are no-ops when signed out.
 */
class SyncManager(
    private val scope: CoroutineScope,
    private val engine: SyncEngine,
    private val store: SyncStore
) {
    @Volatile private var debounceJob: Job? = null

    fun request(debounceMs: Long = 2500L) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs)
            runCatching {
                if (store.current().isLoggedIn) engine.syncAll()
            }
        }
    }

    fun syncNow() {
        scope.launch {
            runCatching {
                if (store.current().isLoggedIn) engine.syncAll()
            }
        }
    }
}
