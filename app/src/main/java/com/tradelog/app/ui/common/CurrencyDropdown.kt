package com.tradelog.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tradelog.app.di.appViewModel
import com.tradelog.app.ui.settings.CurrencyViewModel

/** Currency picker backed by the user-managed currency list. Add new codes inline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Currency"
) {
    val vm: CurrencyViewModel = appViewModel()
    val currencies by vm.currencies.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { c ->
                DropdownMenuItem(text = { Text(c.code) }, onClick = { onValueChange(c.code); expanded = false })
            }
            DropdownMenuItem(
                text = { Text("＋ Add currency…") },
                onClick = { expanded = false; showAdd = true }
            )
        }
    }

    if (showAdd) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add currency") },
            text = {
                OutlinedTextField(
                    code,
                    { code = it.uppercase() },
                    label = { Text("Code (e.g. USD, AED, ZAR)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val c = code.trim().uppercase()
                    if (c.isNotBlank()) { vm.add(c); onValueChange(c) }
                    showAdd = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}
