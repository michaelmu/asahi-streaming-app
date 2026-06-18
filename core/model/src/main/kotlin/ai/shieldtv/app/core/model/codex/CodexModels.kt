package ai.codexa.app.core.model.codex

data class CodexConnectionConfig(
    val websocketUrl: String,
    val bearerToken: String,
    val clientName: String = "codexa_android",
    val clientTitle: String = "Codexa Android Client",
    val clientVersion: String = "0.1.0",
)

data class CodexStartThreadRequest(
    val cwd: String? = null,
    val approvalPolicy: String? = null,
    val sandbox: String? = null,
    val personality: String? = null,
)

sealed interface CodexThreadStatus {
    data object NotLoaded : CodexThreadStatus
    data object Idle : CodexThreadStatus
    data object Active : CodexThreadStatus
    data object SystemError : CodexThreadStatus
}

enum class CodexTurnStatus {
    InProgress,
    Completed,
    Interrupted,
    Failed,
}

data class CodexThreadSummary(
    val id: String,
    val preview: String = "",
    val createdAtEpochSeconds: Long? = null,
    val updatedAtEpochSeconds: Long? = null,
    val status: CodexThreadStatus = CodexThreadStatus.NotLoaded,
)

data class CodexTurnRef(
    val threadId: String,
    val turnId: String,
    val status: CodexTurnStatus,
)

enum class CodexItemKind {
    AgentMessage,
    CommandExecution,
    FileChange,
    Reasoning,
    Unknown,
}

data class CodexItemRef(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val kind: CodexItemKind = CodexItemKind.Unknown,
)

enum class CodexCommandApprovalDecision {
    Accept,
    AcceptForSession,
    Decline,
    Cancel,
}

enum class CodexFileChangeApprovalDecision {
    Accept,
    AcceptForSession,
    Decline,
    Cancel,
}

data class CodexCommandApprovalRequest(
    val requestId: String,
    val item: CodexItemRef,
    val command: String? = null,
    val cwd: String? = null,
    val reason: String? = null,
)

data class CodexFileChangeApprovalRequest(
    val requestId: String,
    val item: CodexItemRef,
    val reason: String? = null,
    val grantRoot: String? = null,
)

data class CodexUserInputQuestion(
    val id: String,
    val prompt: String,
    val placeholder: String? = null,
    val options: List<String> = emptyList(),
)

data class CodexUserInputRequest(
    val requestId: String,
    val item: CodexItemRef,
    val questions: List<CodexUserInputQuestion>,
    val autoResolutionMs: Long? = null,
)

sealed interface CodexConversationEvent {
    data class ConnectionStateChanged(
        val state: CodexConnectionState,
        val message: String? = null,
    ) : CodexConversationEvent

    data class ThreadStarted(val thread: CodexThreadSummary) : CodexConversationEvent
    data class ThreadStatusChanged(
        val threadId: String,
        val status: CodexThreadStatus,
    ) : CodexConversationEvent

    data class TurnStarted(val turn: CodexTurnRef) : CodexConversationEvent
    data class AgentMessageDelta(
        val item: CodexItemRef,
        val delta: String,
    ) : CodexConversationEvent

    data class ItemStarted(
        val item: CodexItemRef,
        val startedAtMs: Long,
    ) : CodexConversationEvent

    data class ItemCompleted(
        val item: CodexItemRef,
        val completedAtMs: Long,
    ) : CodexConversationEvent

    data class TurnCompleted(val turn: CodexTurnRef) : CodexConversationEvent
    data class CommandApprovalRequested(
        val request: CodexCommandApprovalRequest,
    ) : CodexConversationEvent

    data class FileChangeApprovalRequested(
        val request: CodexFileChangeApprovalRequest,
    ) : CodexConversationEvent

    data class UserInputRequested(
        val request: CodexUserInputRequest,
    ) : CodexConversationEvent

    data class ServerRequestResolved(
        val threadId: String,
        val requestId: String,
    ) : CodexConversationEvent

    data class Warning(
        val threadId: String?,
        val message: String,
    ) : CodexConversationEvent

    data class Error(
        val threadId: String,
        val turnId: String,
        val message: String,
        val willRetry: Boolean,
    ) : CodexConversationEvent
}
