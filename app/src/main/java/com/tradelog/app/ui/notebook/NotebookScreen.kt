package com.tradelog.app.ui.notebook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.common.ConfirmDeleteAction
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.EmptyState
import com.tradelog.app.ui.common.FormField
import com.tradelog.app.ui.common.SectionCard

@Composable
fun NotebookScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val vm: NotebookViewModel = appViewModel()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    DetailScaffold(
        title = "Notebook",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Add note") } }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Search notes & tags") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            if (notes.isEmpty()) {
                EmptyState("No notes found.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        SectionCard(modifier = Modifier.clickable { onOpen(note.id) }) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (note.tags.isNotBlank()) {
                                Text(note.tags, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditScreen(noteId: Long, onBack: () -> Unit) {
    val vm: NoteEditViewModel = appViewModel()
    val note by vm.note.collectAsStateWithLifecycle()
    LaunchedEffect(noteId) { vm.load(noteId) }

    DetailScaffold(
        title = if (noteId == 0L) "New note" else "Edit note",
        onBack = onBack,
        actions = {
            if (vm.canDelete) ConfirmDeleteAction("note") { vm.delete(onBack) }
            IconButton(onClick = { vm.save(onBack) }) { Icon(Icons.Filled.Check, "Save") }
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(note.title, { v -> vm.update { it.copy(title = v) } }, "Title")
            FormField(note.tags, { v -> vm.update { it.copy(tags = v) } }, "Tags (comma separated)")
            FormField(note.body, { v -> vm.update { it.copy(body = v) } }, "Body", singleLine = false, minLines = 8)
        }
    }
}
