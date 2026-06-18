package ai.codexa.app.ui

import ai.codexa.app.CodexSavedConnection
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConnectionState
import ai.codexa.app.core.model.codex.CodexConversationState
import ai.codexa.app.domain.codex.CodexConversationViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CodexRoute(
    viewModel: CodexConversationViewModel,
    savedConnection: CodexSavedConnection,
    onSaveConnection: (CodexSavedConnection) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var websocketUrl by rememberSaveable { mutableStateOf(savedConnection.websocketUrl) }
    var bearerToken by rememberSaveable { mutableStateOf(savedConnection.bearerToken) }
    var messageText by rememberSaveable { mutableStateOf("") }

    val selectedThread = state.threads.firstOrNull { it.summary.id == state.selectedThreadId }
    val threadSwitchLocked = selectedThread?.activeTurn != null
    val connectionConfig = CodexConnectionConfig(
        websocketUrl = websocketUrl,
        bearerToken = bearerToken,
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Codex", style = MaterialTheme.typography.headlineMedium)
            StatusCard(
                state = state,
                onReconnect = {
                    onSaveConnection(CodexSavedConnection(websocketUrl, bearerToken))
                    viewModel.reconnect(connectionConfig)
                },
            )

            ConnectionCard(
                websocketUrl = websocketUrl,
                onWebsocketUrlChange = { websocketUrl = it },
                bearerToken = bearerToken,
                onBearerTokenChange = { bearerToken = it },
                onConnect = {
                    onSaveConnection(CodexSavedConnection(websocketUrl, bearerToken))
                    viewModel.connect(connectionConfig)
                },
                onRefresh = viewModel::refreshThreads,
                onNewThread = { viewModel.startThread() },
                onDisconnect = viewModel::disconnect,
                isConnected = state.connection == CodexConnectionState.Connected,
            )

            ThreadPicker(
                state = state,
                onSelectThread = viewModel::selectThread,
                threadSwitchLocked = threadSwitchLocked,
            )

            if (selectedThread == null) {
                EmptyConversationHint()
            } else {
                ConversationPane(
                    state = state,
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(selectedThread.summary.id, messageText)
                            messageText = ""
                        }
                    },
                    onStop = viewModel::interruptTurn,
                    onApproveCommand = viewModel::approveCommand,
                    onDenyCommand = viewModel::denyCommand,
                    onApproveFileChange = viewModel::approveFileChange,
                    onDenyFileChange = viewModel::denyFileChange,
                    onSubmitUserInput = viewModel::submitUserInput,
                )
            }
        }
    }
}
