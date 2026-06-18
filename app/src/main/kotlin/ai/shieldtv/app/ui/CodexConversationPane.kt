package ai.codexa.app.ui

import ai.codexa.app.core.model.codex.CodexConversationState
import ai.codexa.app.core.model.codex.CodexPendingRequest
import ai.codexa.app.core.model.codex.CodexTimelineEntry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConversationPane(
    state: CodexConversationState,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: (String, String) -> Unit,
    onApproveCommand: (String) -> Unit,
    onDenyCommand: (String) -> Unit,
    onApproveFileChange: (String) -> Unit,
    onDenyFileChange: (String) -> Unit,
    onSubmitUserInput: (String, Map<String, List<String>>) -> Unit,
) {
    val thread = state.threads.firstOrNull { it.summary.id == state.selectedThreadId } ?: return

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(thread.summary.preview.ifBlank { thread.summary.id }, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Turn: ${thread.activeTurn?.turnId ?: "idle"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            thread.activeTurn?.let { turn ->
                Button(onClick = { onStop(turn.threadId, turn.turnId) }) {
                    Text("Stop")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(thread.timeline, key = { it.item.itemId }) { entry ->
                when (entry) {
                    is CodexTimelineEntry.UserMessage -> UserMessageCard(entry)
                    is CodexTimelineEntry.AgentMessage -> MessageCard(entry)
                    is CodexTimelineEntry.ToolActivity -> ToolCard(entry)
                    is CodexTimelineEntry.Error -> ErrorCard(entry)
                }
            }
        }

        thread.pendingApprovals.forEach { request ->
            when (request) {
                is CodexPendingRequest.CommandApproval -> {
                    ApprovalCard(
                        title = "Command approval",
                        body = listOfNotNull(request.command, request.cwd, request.reason).joinToString("\n"),
                        approveLabel = "Allow",
                        denyLabel = "Deny",
                        onApprove = { onApproveCommand(request.requestId) },
                        onDeny = { onDenyCommand(request.requestId) },
                    )
                }
                is CodexPendingRequest.FileChangeApproval -> {
                    ApprovalCard(
                        title = "File change approval",
                        body = listOfNotNull(request.reason, request.grantRoot).joinToString("\n"),
                        approveLabel = "Allow",
                        denyLabel = "Deny",
                        onApprove = { onApproveFileChange(request.requestId) },
                        onDeny = { onDenyFileChange(request.requestId) },
                    )
                }
                is CodexPendingRequest.UserInput -> {
                    UserInputCard(
                        request = request,
                        onSubmit = { answers -> onSubmitUserInput(request.requestId, answers) },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                label = { Text("Message") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSend) {
                Text("Send")
            }
        }
    }
}

@Composable
fun UserMessageCard(entry: CodexTimelineEntry.UserMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("You", style = MaterialTheme.typography.titleSmall)
            Text(entry.text.ifBlank { "…" })
        }
    }
}

@Composable
fun MessageCard(entry: CodexTimelineEntry.AgentMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Assistant", style = MaterialTheme.typography.titleSmall)
            Text(entry.text.ifBlank { "…" })
        }
    }
}

@Composable
fun ToolCard(entry: CodexTimelineEntry.ToolActivity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall)
            Text(entry.status)
            entry.details?.takeIf { it.isNotBlank() }?.let {
                Text(it)
            }
        }
    }
}

@Composable
fun ErrorCard(entry: CodexTimelineEntry.Error) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Error", style = MaterialTheme.typography.titleSmall)
            Text(entry.message)
            if (entry.retrying) {
                Text("Retrying…")
            }
        }
    }
}

@Composable
fun UserInputCard(
    request: CodexPendingRequest.UserInput,
    onSubmit: (Map<String, List<String>>) -> Unit,
) {
    val answers = remember(request.requestId) { mutableStateMapOf<String, String>() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Input needed", style = MaterialTheme.typography.titleSmall)
            request.questions.forEach { question ->
                OutlinedTextField(
                    value = answers[question.id].orEmpty(),
                    onValueChange = { answers[question.id] = it },
                    label = { Text(question.prompt) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(onClick = {
                onSubmit(request.questions.associate { question ->
                    question.id to listOf(answers[question.id].orEmpty())
                })
            }) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun ApprovalCard(
    title: String,
    body: String,
    approveLabel: String,
    denyLabel: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (body.isNotBlank()) {
                Text(body)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text(approveLabel) }
                Button(onClick = onDeny) { Text(denyLabel) }
            }
        }
    }
}

@Composable
fun EmptyConversationHint() {
    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("No thread selected", style = MaterialTheme.typography.titleMedium)
                Text("Connect, refresh threads, or start a new one.")
            }
        }
    }
}
