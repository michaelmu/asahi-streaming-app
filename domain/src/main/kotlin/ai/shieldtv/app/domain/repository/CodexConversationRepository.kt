package ai.codexa.app.domain.repository

import ai.codexa.app.core.model.codex.CodexCommandApprovalDecision
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConversationState
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalDecision
import kotlinx.coroutines.flow.StateFlow

interface CodexConversationRepository {
    val state: StateFlow<CodexConversationState>

    suspend fun connect(config: CodexConnectionConfig)

    suspend fun disconnect()

    suspend fun refreshThreads(limit: Int = 50)

    suspend fun selectThread(threadId: String)

    suspend fun startThread(
        cwd: String? = null,
        approvalPolicy: String? = null,
        sandbox: String? = null,
        personality: String? = null,
    )

    suspend fun sendMessage(threadId: String, text: String)

    suspend fun interruptTurn(threadId: String, turnId: String)

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
