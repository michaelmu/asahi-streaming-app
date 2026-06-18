package ai.codexa.app.core.network.codex

import ai.codexa.app.core.model.codex.CodexCommandApprovalDecision
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConversationEvent
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalDecision
import ai.codexa.app.core.model.codex.CodexStartThreadRequest
import ai.codexa.app.core.model.codex.CodexThreadSnapshot
import ai.codexa.app.core.model.codex.CodexThreadSummary
import ai.codexa.app.core.model.codex.CodexTurnRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Placeholder implementation so the rest of the app can integrate against a stable surface
 * before the websocket transport is built.
 */
class CodexRpcClientStub : CodexRpcClient {
    private val _events = MutableSharedFlow<CodexConversationEvent>(extraBufferCapacity = 32)

    override val events: Flow<CodexConversationEvent> = _events.asSharedFlow()

    override suspend fun connect(config: CodexConnectionConfig) {
        error("Not implemented: websocket connect/initialize")
    }

    override suspend fun disconnect() {
        _events.tryEmit(CodexConversationEvent.ConnectionStateChanged(ai.codexa.app.core.model.codex.CodexConnectionState.Disconnected))
        error("Not implemented: websocket disconnect")
    }

    override suspend fun listThreads(limit: Int): List<CodexThreadSummary> {
        error("Not implemented: thread/list")
    }

    override suspend fun startThread(request: CodexStartThreadRequest): CodexThreadSummary {
        error("Not implemented: thread/start")
    }

    override suspend fun resumeThread(threadId: String, excludeTurns: Boolean): CodexThreadSummary {
        error("Not implemented: thread/resume")
    }

    override suspend fun readThread(threadId: String, includeTurns: Boolean): CodexThreadSnapshot {
        error("Not implemented: thread/read")
    }

    override suspend fun sendUserMessage(
        threadId: String,
        text: String,
        clientUserMessageId: String?,
    ): CodexTurnRef {
        error("Not implemented: turn/start")
    }

    override suspend fun interruptTurn(threadId: String, turnId: String) {
        error("Not implemented: turn/interrupt")
    }

    override suspend fun respondToCommandApproval(
        requestId: String,
        decision: CodexCommandApprovalDecision,
    ) {
        error("Not implemented: item/commandExecution/requestApproval response")
    }

    override suspend fun respondToFileChangeApproval(
        requestId: String,
        decision: CodexFileChangeApprovalDecision,
    ) {
        error("Not implemented: item/fileChange/requestApproval response")
    }

    override suspend fun respondToUserInput(
        requestId: String,
        answers: Map<String, List<String>>,
    ) {
        error("Not implemented: item/tool/requestUserInput response")
    }
}
