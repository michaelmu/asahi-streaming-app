package ai.codexa.app.ui

import ai.codexa.app.core.model.codex.CodexConversationState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun StatusCard(
    state: CodexConversationState,
    onReconnect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Status: ${state.connection}")
                state.lastError?.let {
                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onReconnect) {
                        Text("Reconnect")
                    }
                }
                state.lastWarning?.let {
                    Text("Warning: $it")
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    websocketUrl: String,
    onWebsocketUrlChange: (String) -> Unit,
    bearerToken: String,
    onBearerTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onNewThread: () -> Unit,
    onDisconnect: () -> Unit,
    isConnected: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = websocketUrl,
                onValueChange = onWebsocketUrlChange,
                label = { Text("WebSocket URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "Local emulator default: ws://10.0.2.2:8765",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = bearerToken,
                onValueChange = onBearerTokenChange,
                label = { Text("Bearer token (optional)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnect) { Text(if (isConnected) "Reconnect" else "Connect") }
                Button(onClick = onRefresh) { Text("Refresh") }
                Button(onClick = onNewThread) { Text("New") }
                if (isConnected) {
                    Button(onClick = onDisconnect) { Text("Disconnect") }
                }
            }
        }
    }
}

@Composable
fun ThreadPicker(
    state: CodexConversationState,
    onSelectThread: (String) -> Unit,
    threadSwitchLocked: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Threads", style = MaterialTheme.typography.titleMedium)
        if (state.threads.isEmpty()) {
            Text("No threads yet.", style = MaterialTheme.typography.bodyMedium)
            return
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.threads, key = { it.summary.id }) { thread ->
                val isSelected = thread.summary.id == state.selectedThreadId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectThread(thread.summary.id) },
                    enabled = isSelected || !threadSwitchLocked,
                    label = {
                        Text(thread.summary.preview.ifBlank { thread.summary.id.take(8) })
                    },
                )
            }
        }
    }
}
