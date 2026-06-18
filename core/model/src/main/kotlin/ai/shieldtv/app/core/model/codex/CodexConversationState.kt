package ai.codexa.app.core.model.codex

data class CodexConversationState(
    val threads: List<CodexThreadState> = emptyList(),
    val selectedThreadId: String? = null,
    val connection: CodexConnectionState = CodexConnectionState.Disconnected,
    val lastError: String? = null,
    val lastWarning: String? = null,
)

enum class CodexConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

data class CodexThreadState(
    val summary: CodexThreadSummary,
    val activeTurn: CodexTurnRef? = null,
    val timeline: List<CodexTimelineEntry> = emptyList(),
    val pendingApprovals: List<CodexPendingRequest> = emptyList(),
    val draftAssistantMessages: Map<String, String> = emptyMap(),
)

data class CodexThreadSnapshot(
    val summary: CodexThreadSummary,
    val activeTurn: CodexTurnRef? = null,
    val timeline: List<CodexTimelineEntry> = emptyList(),
)

sealed interface CodexTimelineEntry {
    val item: CodexItemRef

    data class UserMessage(
        override val item: CodexItemRef,
        val text: String,
    ) : CodexTimelineEntry

    data class AgentMessage(
        override val item: CodexItemRef,
        val text: String,
        val completed: Boolean,
    ) : CodexTimelineEntry

    data class ToolActivity(
        override val item: CodexItemRef,
        val title: String,
        val status: String,
        val details: String? = null,
    ) : CodexTimelineEntry

    data class Error(
        override val item: CodexItemRef,
        val message: String,
        val retrying: Boolean,
    ) : CodexTimelineEntry
}

sealed interface CodexPendingRequest {
    val requestId: String
    val item: CodexItemRef

    data class CommandApproval(
        override val requestId: String,
        override val item: CodexItemRef,
        val command: String? = null,
        val cwd: String? = null,
        val reason: String? = null,
    ) : CodexPendingRequest

    data class FileChangeApproval(
        override val requestId: String,
        override val item: CodexItemRef,
        val reason: String? = null,
        val grantRoot: String? = null,
    ) : CodexPendingRequest

    data class UserInput(
        override val requestId: String,
        override val item: CodexItemRef,
        val questions: List<CodexUserInputQuestion>,
        val autoResolutionMs: Long? = null,
    ) : CodexPendingRequest
}
