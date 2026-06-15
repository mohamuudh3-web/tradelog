package com.tradelog.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.SyncSession
import com.tradelog.app.data.SyncStore
import com.tradelog.app.di.ServiceLocator
import com.tradelog.app.network.SupabaseClient
import com.tradelog.app.sync.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SyncUiState(
    val working: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val lastSync: Long = 0L
)

class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val engine: SyncEngine = ServiceLocator.syncEngine(app)
    private val client: SupabaseClient = ServiceLocator.supabase(app)
    private val store: SyncStore = ServiceLocator.syncStore(app)

    val session: StateFlow<SyncSession> =
        store.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncSession())

    val lastSync: StateFlow<Long> =
        store.lastSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _ui = MutableStateFlow(SyncUiState())
    val ui: StateFlow<SyncUiState> = _ui

    fun signIn(email: String, password: String, isSignUp: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _ui.value = _ui.value.copy(error = "Enter your email and password.")
            return
        }
        _ui.value = _ui.value.copy(working = true, error = null, message = null)
        viewModelScope.launch {
            val result = if (isSignUp) client.signUp(email, password) else client.signIn(email, password)
            result.onSuccess {
                _ui.value = _ui.value.copy(message = "Signed in. Syncing…")
                runSync()
            }.onFailure {
                _ui.value = _ui.value.copy(working = false, error = it.message ?: "Sign-in failed.")
            }
        }
    }

    fun syncNow() {
        if (_ui.value.working) return
        _ui.value = _ui.value.copy(working = true, error = null, message = null)
        viewModelScope.launch { runSync() }
    }

    private suspend fun runSync() {
        val r = engine.syncAll()
        _ui.value = if (r.ok) {
            SyncUiState(working = false, message = "Synced ↓${r.pulled} ↑${r.pushed}", error = null)
        } else {
            _ui.value.copy(working = false, error = r.error)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            store.clear()
            _ui.value = SyncUiState(message = "Signed out.")
        }
    }
}
