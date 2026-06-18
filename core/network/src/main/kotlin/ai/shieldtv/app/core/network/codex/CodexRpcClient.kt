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

interface CodexRpcClient {
    val events: Flow<CodexConversationEvent>

    suspend fun connect(config: CodexConnectionConfig)

    suspend fun disconnect()

    suspend fun listThreads(limit: Int = 50): List<CodexThreadSummary>

    suspend fun startThread(request: CodexStartThreadRequest = CodexStartThreadRequest()): CodexThreadSummary

    suspend fun resumeThread(
        threadId: String,
        excludeTurns: Boolean = true,
    ): CodexThreadSummary

    suspend fun readThread(
        threadId: String,
        includeTurns: Boolean = true,
    ): CodexThreadSnapshot

    suspend fun sendUserMessage(
        threadId: String,
        text: String,
        clientUserMessageId: String? = null,
    ): CodexTurnRef

    suspend fun interruptTurn(
        threadId: String,
        turnId: String,
    )

    suspend fun respondToCommandApproval(
        requestId: String,
        decision: CodexCommandApprovalDecision,
    )

    suspend fun respondToFileChangeApproval(
        requestId: String,
        decision: CodexFileChangeApprovalDecision,
    )

    suspend fun respondToUserInput(
        requestId: String,
        answers: Map<String, List<String>>,
    )
}
