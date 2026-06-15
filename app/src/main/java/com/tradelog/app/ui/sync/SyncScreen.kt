package com.tradelog.app.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradelog.app.ui.common.DetailScaffold
import com.tradelog.app.ui.common.SectionCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(onBack: () -> Unit) {
    val vm: SyncViewModel = viewModel()
    val session by vm.session.collectAsStateWithLifecycle()
    val lastSync by vm.lastSync.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    DetailScaffold(title = "Cloud sync", onBack = onBack) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (session.isLoggedIn) {
                SectionCard(title = "Account") {
                    Text(session.email, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your trades, backtests, accounts, payouts, journal and notes sync with the TradeLog website.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    val lastLabel = if (lastSync > 0L)
                        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastSync))
                    else "Never"
                    Text(
                        "Last sync: $lastLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { vm.syncNow() }, enabled = !ui.working) {
                            Text(if (ui.working) "Syncing…" else "Sync now")
                        }
                        OutlinedButton(onClick = { vm.signOut() }, enabled = !ui.working) {
                            Text("Sign out")
                        }
                        if (ui.working) CircularProgressIndicator(Modifier.padding(start = 4.dp))
                    }
                }
            } else {
                SectionCard(title = if (isSignUp) "Create account" else "Sign in") {
                    Text(
                        "Use the same email and password as the TradeLog website so both stay in sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = { vm.signIn(email.trim(), password, isSignUp) },
                        enabled = !ui.working,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(if (ui.working) "Please wait…" else if (isSignUp) "Create account" else "Sign in")
                    }
                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(if (isSignUp) "I already have an account" else "Need a new account?")
                    }
                }
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            ui.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
