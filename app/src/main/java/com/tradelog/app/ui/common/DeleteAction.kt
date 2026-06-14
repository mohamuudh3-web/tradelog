package com.tradelog.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** A toolbar delete button that asks for confirmation before invoking [onConfirm]. */
@Composable
fun ConfirmDeleteAction(
    itemLabel: String,
    onConfirm: () -> Unit
) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.Filled.Delete, contentDescription = "Delete")
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Delete $itemLabel?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { show = false; onConfirm() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        )
    }
}
