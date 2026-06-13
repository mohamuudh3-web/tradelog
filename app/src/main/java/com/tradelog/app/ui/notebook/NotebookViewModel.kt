package com.tradelog.app.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.repository.TradeLogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookViewModel(private val repo: TradeLogRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val notes: StateFlow<List<NotebookNote>> =
        _query.flatMapLatest { repo.searchNotes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun delete(note: NotebookNote) = viewModelScope.launch { repo.deleteNote(note) }
}

class NoteEditViewModel(private val repo: TradeLogRepository) : ViewModel() {
    private val _note = MutableStateFlow(NotebookNote(title = ""))
    val note: StateFlow<NotebookNote> = _note.asStateFlow()
    private var loaded = false

    fun load(id: Long) {
        if (loaded) return
        loaded = true
        if (id == 0L) return
        viewModelScope.launch { repo.getNote(id)?.let { _note.value = it } }
    }

    fun update(transform: (NotebookNote) -> NotebookNote) { _note.value = transform(_note.value) }

    fun save(onDone: () -> Unit) {
        if (_note.value.title.isBlank()) return
        viewModelScope.launch { repo.saveNote(_note.value); onDone() }
    }
}
