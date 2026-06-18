package ai.codexa.app.domain.codex

import ai.codexa.app.core.model.codex.CodexCommandApprovalDecision
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConversationState
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalDecision
import ai.codexa.app.domain.repository.CodexConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CodexConversationViewModel(
    private val repository: CodexConversationRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    val state: StateFlow<CodexConversationState> = repository.state

    fun connect(config: CodexConnectionConfig) {
        scope.launch {
            repository.connect(config)
        }
    }

    fun disconnect() {
        scope.launch {
            repository.disconnect()
        }
    }

    fun reconnect(config: CodexConnectionConfig) {
        scope.launch {
            repository.connect(config)
        }
    }

    fun refreshThreads() {
        scope.launch {
            repository.refreshThreads()
        }
    }

    fun selectThread(threadId: String) {
        scope.launch {
            repository.selectThread(threadId)
        }
    }

    fun startThread(
        cwd: String? = null,
        approvalPolicy: String? = null,
        sandbox: String? = null,
        personality: String? = null,
    ) {
        scope.launch {
            repository.startThread(
                cwd = cwd,
                approvalPolicy = approvalPolicy,
                sandbox = sandbox,
                personality = personality,
            )
        }
    }

    fun sendMessage(threadId: String, text: String) {
        scope.launch {
            repository.sendMessage(threadId, text)
        }
    }

    fun interruptTurn(threadId: String, turnId: String) {
        scope.launch {
            repository.interruptTurn(threadId, turnId)
        }
    }

    fun approveCommand(requestId: String, allowForSession: Boolean = false) {
        scope.launch {
            repository.respondToCommandApproval(
                requestId = requestId,
                decision = if (allowForSession) {
                    CodexCommandApprovalDecision.AcceptForSession
                } else {
                    CodexCommandApprovalDecision.Accept
                },
            )
        }
    }

    fun denyCommand(requestId: String, cancelTurn: Boolean = false) {
        scope.launch {
            repository.respondToCommandApproval(
                requestId = requestId,
                decision = if (cancelTurn) {
                    CodexCommandApprovalDecision.Cancel
                } else {
                    CodexCommandApprovalDecision.Decline
                },
            )
        }
    }

    fun approveFileChange(requestId: String, allowForSession: Boolean = false) {
        scope.launch {
            repository.respondToFileChangeApproval(
                requestId = requestId,
                decision = if (allowForSession) {
                    CodexFileChangeApprovalDecision.AcceptForSession
                } else {
                    CodexFileChangeApprovalDecision.Accept
                },
            )
        }
    }

    fun denyFileChange(requestId: String, cancelTurn: Boolean = false) {
        scope.launch {
            repository.respondToFileChangeApproval(
                requestId = requestId,
                decision = if (cancelTurn) {
                    CodexFileChangeApprovalDecision.Cancel
                } else {
                    CodexFileChangeApprovalDecision.Decline
                },
            )
        }
    }

    fun submitUserInput(requestId: String, answers: Map<String, List<String>>) {
        scope.launch {
            repository.respondToUserInput(requestId, answers)
        }
    }
}
