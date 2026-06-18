package ai.codexa.app.core.data.codex

import ai.codexa.app.core.model.codex.CodexCommandApprovalDecision
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConnectionState
import ai.codexa.app.core.model.codex.CodexConversationEvent
import ai.codexa.app.core.model.codex.CodexConversationState
import ai.codexa.app.core.model.codex.CodexThreadStatus
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalDecision
import ai.codexa.app.core.model.codex.CodexPendingRequest
import ai.codexa.app.core.model.codex.CodexStartThreadRequest
import ai.codexa.app.core.model.codex.CodexThreadState
import ai.codexa.app.core.model.codex.CodexThreadSnapshot
import ai.codexa.app.core.model.codex.CodexThreadSummary
import ai.codexa.app.core.model.codex.CodexTimelineEntry
import ai.codexa.app.core.model.codex.CodexTurnRef
import ai.codexa.app.core.network.codex.CodexRpcClient
import ai.codexa.app.domain.repository.CodexConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodexConversationStore(
    private val rpcClient: CodexRpcClient,
) : CodexConversationRepository {
    private var lastConnectionConfig: CodexConnectionConfig? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(CodexConversationState())

    init {
        scope.launch {
            rpcClient.events.collectLatest(::applyEvent)
        }
    }

    override val state: StateFlow<CodexConversationState> = _state.asStateFlow()

    override suspend fun connect(config: CodexConnectionConfig) {
        lastConnectionConfig = config
        _state.value = _state.value.copy(
            connection = if (_state.value.connection == CodexConnectionState.Connected) {
                CodexConnectionState.Reconnecting
            } else {
                CodexConnectionState.Connecting
            },
            lastError = null,
        )
        runCatching {
            withContext(Dispatchers.IO) {
                rpcClient.connect(config)
            }
        }.onSuccess {
            _state.value = _state.value.copy(
                connection = CodexConnectionState.Connected,
                lastError = null,
            )
            refreshThreads()
            _state.value.selectedThreadId?.let { selectedThreadId ->
                runCatching {
                    val snapshot = withContext(Dispatchers.IO) {
                        readThreadSnapshot(selectedThreadId)
                    }
                    _state.value = _state.value.copy(
                        threads = upsertThreadSnapshot(snapshot),
                        lastWarning = null,
                    )
                }.onFailure { error ->
                    clearSelectedThreadIfMissing(selectedThreadId, error)
                }
            }
        }.onFailure { error ->
            _state.value = _state.value.copy(
                connection = CodexConnectionState.Disconnected,
                lastError = error.message ?: "Failed to connect",
            )
        }
    }

    override suspend fun disconnect() {
        runCatching { withContext(Dispatchers.IO) { rpcClient.disconnect() } }
        _state.value = _state.value.copy(connection = CodexConnectionState.Disconnected)
    }

    override suspend fun refreshThreads(limit: Int) {
        runCatching {
            withContext(Dispatchers.IO) { rpcClient.listThreads(limit) }
        }.onSuccess { threads ->
            _state.value = _state.value.copy(
                threads = mergeThreadSummaries(threads),
                lastError = null,
                lastWarning = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                lastError = error.message ?: "Failed to refresh threads",
            )
        }
    }

    override suspend fun selectThread(threadId: String) {
        val selectedThread = _state.value.selectedThreadId?.let { selectedId ->
            _state.value.threads.firstOrNull { it.summary.id == selectedId }
        }
        if (selectedThread?.activeTurn != null && selectedThread.summary.id != threadId) {
            _state.value = _state.value.copy(
                lastWarning = "Finish or stop the active turn before switching threads.",
            )
            return
        }

        runCatching {
            withContext(Dispatchers.IO) { readThreadSnapshot(threadId = threadId) }
        }.onSuccess { snapshot ->
            _state.value = _state.value.copy(
                selectedThreadId = threadId,
                threads = upsertThreadSnapshot(snapshot),
                lastError = null,
                lastWarning = null,
            )
        }.onFailure { error ->
            clearSelectedThreadIfMissing(threadId, error)
        }
    }

    override suspend fun startThread(
        cwd: String?,
        approvalPolicy: String?,
        sandbox: String?,
        personality: String?,
    ) {
        runCatching {
            withContext(Dispatchers.IO) {
                rpcClient.startThread(
                    CodexStartThreadRequest(
                        cwd = cwd,
                        approvalPolicy = approvalPolicy,
                        sandbox = sandbox,
                        personality = personality,
                    )
                )
            }
        }.onSuccess { summary ->
            _state.value = _state.value.copy(
                selectedThreadId = summary.id,
                threads = upsertThread(summary),
                lastError = null,
                lastWarning = null,
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                lastError = error.message ?: "Failed to start thread",
            )
        }
    }

    override suspend fun sendMessage(threadId: String, text: String) {
        val pendingUserItemId = "local-user:${System.currentTimeMillis()}"
        updateThread(threadId) { thread ->
            thread.copy(
                timeline = thread.timeline + CodexTimelineEntry.UserMessage(
                    item = ai.codexa.app.core.model.codex.CodexItemRef(
                        threadId = threadId,
                        turnId = thread.activeTurn?.turnId ?: "pending",
                        itemId = pendingUserItemId,
                    ),
                    text = text,
                )
            )
        }

        runCatching {
            withContext(Dispatchers.IO) {
                rpcClient.sendUserMessage(threadId, text, clientUserMessageId = pendingUserItemId)
            }
        }.onSuccess { turn ->
            updateThread(threadId) { thread ->
                thread.copy(activeTurn = turn)
            }
        }.onFailure { error ->
            if (!clearSelectedThreadIfMissing(threadId, error)) {
                _state.value = _state.value.copy(lastError = error.message ?: "Failed to send message")
                updateThread(threadId) { thread ->
                    thread.copy(
                        timeline = thread.timeline + CodexTimelineEntry.Error(
                            item = ai.codexa.app.core.model.codex.CodexItemRef(
                                threadId = threadId,
                                turnId = thread.activeTurn?.turnId ?: "pending",
                                itemId = "error:send:$pendingUserItemId",
                            ),
                            message = error.message ?: "Failed to send message",
                            retrying = false,
                        )
                    )
                }
            }
        }
    }

    override suspend fun interruptTurn(threadId: String, turnId: String) {
        runCatching {
            withContext(Dispatchers.IO) { rpcClient.interruptTurn(threadId, turnId) }
        }.onFailure { error ->
            if (!clearSelectedThreadIfMissing(threadId, error)) {
                _state.value = _state.value.copy(lastError = error.message ?: "Failed to interrupt turn")
            }
        }
    }

    override suspend fun respondToCommandApproval(
        requestId: String,
        decision: CodexCommandApprovalDecision,
    ) {
        runCatching {
            withContext(Dispatchers.IO) { rpcClient.respondToCommandApproval(requestId, decision) }
        }.onFailure { error ->
            _state.value = _state.value.copy(lastError = error.message ?: "Failed to respond to command approval")
        }
    }

    override suspend fun respondToFileChangeApproval(
        requestId: String,
        decision: CodexFileChangeApprovalDecision,
    ) {
        runCatching {
            withContext(Dispatchers.IO) { rpcClient.respondToFileChangeApproval(requestId, decision) }
        }.onFailure { error ->
            _state.value = _state.value.copy(lastError = error.message ?: "Failed to respond to file change approval")
        }
    }

    override suspend fun respondToUserInput(
        requestId: String,
        answers: Map<String, List<String>>,
    ) {
        runCatching {
            withContext(Dispatchers.IO) { rpcClient.respondToUserInput(requestId, answers) }
        }.onFailure { error ->
            _state.value = _state.value.copy(lastError = error.message ?: "Failed to submit input")
        }
    }

    private fun applyEvent(event: CodexConversationEvent) {
        when (event) {
            is CodexConversationEvent.ConnectionStateChanged -> {
                _state.value = _state.value.copy(
                    connection = event.state,
                    lastError = when (event.state) {
                        CodexConnectionState.Disconnected -> event.message
                        CodexConnectionState.Connected -> null
                        else -> _state.value.lastError
                    },
                    lastWarning = when (event.state) {
                        CodexConnectionState.Reconnecting -> event.message
                        CodexConnectionState.Connected -> null
                        else -> _state.value.lastWarning
                    },
                )
            }
            is CodexConversationEvent.ThreadStarted -> {
                _state.value = _state.value.copy(
                    threads = upsertThread(event.thread),
                    lastError = null,
                    lastWarning = null,
                )
            }
            is CodexConversationEvent.ThreadStatusChanged -> {
                val shouldRefreshSnapshot = _state.value.threads
                    .firstOrNull { it.summary.id == event.threadId }
                    ?.activeTurn != null && event.status != ai.codexa.app.core.model.codex.CodexThreadStatus.Active

                updateThread(event.threadId) { thread ->
                    thread.copy(
                        summary = thread.summary.copy(status = event.status),
                        activeTurn = if (event.status == ai.codexa.app.core.model.codex.CodexThreadStatus.Active) {
                            thread.activeTurn
                        } else {
                            null
                        },
                    )
                }

                if (shouldRefreshSnapshot) {
                    refreshThreadSnapshot(event.threadId)
                }
            }
            is CodexConversationEvent.TurnStarted -> {
                updateThread(event.turn.threadId) { thread ->
                    thread.copy(activeTurn = event.turn)
                }
            }
            is CodexConversationEvent.AgentMessageDelta -> {
                updateThread(event.item.threadId) { thread ->
                    val currentText = thread.draftAssistantMessages[event.item.itemId].orEmpty() + event.delta
                    thread.copy(
                        draftAssistantMessages = thread.draftAssistantMessages + (event.item.itemId to currentText),
                        timeline = upsertAgentMessageTimeline(
                            timeline = thread.timeline,
                            item = event.item,
                            text = currentText,
                            completed = false,
                        ),
                    )
                }
            }
            is CodexConversationEvent.ItemStarted -> {
                updateThread(event.item.threadId) { thread ->
                    thread.copy(
                        timeline = upsertToolTimeline(
                            timeline = thread.timeline,
                            item = event.item,
                            status = "started",
                        )
                    )
                }
            }
            is CodexConversationEvent.ItemCompleted -> {
                updateThread(event.item.threadId) { thread ->
                    val draft = thread.draftAssistantMessages[event.item.itemId]
                    val updatedTimeline = if (draft != null) {
                        upsertAgentMessageTimeline(
                            timeline = thread.timeline,
                            item = event.item,
                            text = draft,
                            completed = true,
                        )
                    } else {
                        upsertToolTimeline(
                            timeline = thread.timeline,
                            item = event.item,
                            status = "completed",
                        )
                    }
                    thread.copy(
                        timeline = updatedTimeline,
                        draftAssistantMessages = thread.draftAssistantMessages - event.item.itemId,
                    )
                }
            }
            is CodexConversationEvent.TurnCompleted -> {
                updateThread(event.turn.threadId) { thread ->
                    thread.copy(
                        activeTurn = if (event.turn.status == ai.codexa.app.core.model.codex.CodexTurnStatus.InProgress) {
                            event.turn
                        } else {
                            null
                        }
                    )
                }
                if (event.turn.status != ai.codexa.app.core.model.codex.CodexTurnStatus.InProgress) {
                    refreshThreadSnapshot(event.turn.threadId)
                }
            }
            is CodexConversationEvent.CommandApprovalRequested -> {
                updateThread(event.request.item.threadId) { thread ->
                    thread.copy(
                        pendingApprovals = thread.pendingApprovals + CodexPendingRequest.CommandApproval(
                            requestId = event.request.requestId,
                            item = event.request.item,
                            command = event.request.command,
                            cwd = event.request.cwd,
                            reason = event.request.reason,
                        )
                    )
                }
            }
            is CodexConversationEvent.FileChangeApprovalRequested -> {
                updateThread(event.request.item.threadId) { thread ->
                    thread.copy(
                        pendingApprovals = thread.pendingApprovals + CodexPendingRequest.FileChangeApproval(
                            requestId = event.request.requestId,
                            item = event.request.item,
                            reason = event.request.reason,
                            grantRoot = event.request.grantRoot,
                        )
                    )
                }
            }
            is CodexConversationEvent.UserInputRequested -> {
                updateThread(event.request.item.threadId) { thread ->
                    thread.copy(
                        pendingApprovals = thread.pendingApprovals + CodexPendingRequest.UserInput(
                            requestId = event.request.requestId,
                            item = event.request.item,
                            questions = event.request.questions,
                            autoResolutionMs = event.request.autoResolutionMs,
                        )
                    )
                }
            }
            is CodexConversationEvent.ServerRequestResolved -> {
                updateThread(event.threadId) { thread ->
                    thread.copy(
                        pendingApprovals = thread.pendingApprovals.filterNot { it.requestId == event.requestId }
                    )
                }
            }
            is CodexConversationEvent.Warning -> {
                _state.value = _state.value.copy(lastWarning = event.message)
            }
            is CodexConversationEvent.Error -> {
                if (!clearSelectedThreadIfMissing(event.threadId, IllegalStateException(event.message), asWarning = true)) {
                    _state.value = _state.value.copy(lastError = event.message)
                    updateThread(event.threadId) { thread ->
                        thread.copy(
                            timeline = thread.timeline + CodexTimelineEntry.Error(
                                item = ai.codexa.app.core.model.codex.CodexItemRef(
                                    threadId = event.threadId,
                                    turnId = event.turnId,
                                    itemId = "error:${event.turnId}",
                                ),
                                message = event.message,
                                retrying = event.willRetry,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun mergeThreadSummaries(threads: List<CodexThreadSummary>): List<CodexThreadState> {
        val existingById = _state.value.threads.associateBy { it.summary.id }
        return threads.map { summary ->
            val existing = existingById[summary.id]
            if (existing != null) {
                existing.copy(
                    summary = summary,
                    activeTurn = if (summary.status == CodexThreadStatus.Active) existing.activeTurn else null,
                )
            } else {
                CodexThreadState(summary = summary)
            }
        }
    }

    private fun upsertThread(summary: CodexThreadSummary): List<CodexThreadState> {
        val current = _state.value.threads
        val existingIndex = current.indexOfFirst { it.summary.id == summary.id }
        return if (existingIndex >= 0) {
            current.toMutableList().apply {
                this[existingIndex] = this[existingIndex].copy(
                    summary = summary,
                    activeTurn = if (summary.status == CodexThreadStatus.Active) this[existingIndex].activeTurn else null,
                )
            }
        } else {
            current + CodexThreadState(summary = summary)
        }
    }

    private fun upsertThreadSnapshot(snapshot: CodexThreadSnapshot): List<CodexThreadState> {
        val current = _state.value.threads
        val existingIndex = current.indexOfFirst { it.summary.id == snapshot.summary.id }
        val newState = CodexThreadState(
            summary = snapshot.summary,
            activeTurn = snapshot.activeTurn,
            timeline = snapshot.timeline,
        )
        return if (existingIndex >= 0) {
            current.toMutableList().apply {
                this[existingIndex] = newState
            }
        } else {
            current + newState
        }
    }

    private fun refreshThreadSnapshot(threadId: String) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    readThreadSnapshot(threadId)
                }
            }.onSuccess { snapshot ->
                _state.value = _state.value.copy(
                    threads = upsertThreadSnapshot(snapshot),
                    lastWarning = null,
                )
            }.onFailure { error ->
                if (!clearSelectedThreadIfMissing(threadId, error, asWarning = true)) {
                    _state.value = _state.value.copy(
                        lastWarning = error.message ?: "Failed to refresh thread",
                    )
                }
            }
        }
    }

    private suspend fun readThreadSnapshot(threadId: String): CodexThreadSnapshot {
        return try {
            rpcClient.readThread(threadId, includeTurns = true)
        } catch (error: Throwable) {
            val message = error.message.orEmpty()
            if (!message.contains("thread not loaded", ignoreCase = true)) {
                throw error
            }
            rpcClient.resumeThread(threadId, excludeTurns = true)
            rpcClient.readThread(threadId, includeTurns = true)
        }
    }

    private fun clearSelectedThreadIfMissing(
        threadId: String,
        error: Throwable,
        asWarning: Boolean = false,
    ): Boolean {
        val message = error.message ?: return false
        val missingThread = message.contains("thread not found", ignoreCase = true)
        if (!missingThread) {
            _state.value = _state.value.copy(
                lastError = if (asWarning) _state.value.lastError else message,
            )
            return false
        }

        val currentState = _state.value
        val updatedThreads = currentState.threads.filterNot { it.summary.id == threadId }
        val selectedStillExists = updatedThreads.any { it.summary.id == currentState.selectedThreadId }
        _state.value = currentState.copy(
            threads = updatedThreads,
            selectedThreadId = if (currentState.selectedThreadId == threadId || !selectedStillExists) {
                updatedThreads.firstOrNull()?.summary?.id
            } else {
                currentState.selectedThreadId
            },
            lastError = if (asWarning) currentState.lastError else null,
            lastWarning = "Selected thread is no longer available. Refresh or choose another thread.",
        )
        return true
    }

    private fun updateThread(threadId: String, transform: (CodexThreadState) -> CodexThreadState) {
        val current = _state.value.threads
        val index = current.indexOfFirst { it.summary.id == threadId }
        if (index < 0) return
        val updatedThreads = current.toMutableList().apply {
            this[index] = transform(this[index])
        }
        _state.value = _state.value.copy(threads = updatedThreads)
    }

    private fun upsertAgentMessageTimeline(
        timeline: List<CodexTimelineEntry>,
        item: ai.codexa.app.core.model.codex.CodexItemRef,
        text: String,
        completed: Boolean,
    ): List<CodexTimelineEntry> {
        val index = timeline.indexOfFirst { it.item.itemId == item.itemId }
        val entry = CodexTimelineEntry.AgentMessage(item = item, text = text, completed = completed)
        return if (index >= 0) {
            timeline.toMutableList().apply { this[index] = entry }
        } else {
            timeline + entry
        }
    }

    private fun upsertToolTimeline(
        timeline: List<CodexTimelineEntry>,
        item: ai.codexa.app.core.model.codex.CodexItemRef,
        status: String,
    ): List<CodexTimelineEntry> {
        val index = timeline.indexOfFirst { it.item.itemId == item.itemId }
        val entry = CodexTimelineEntry.ToolActivity(
            item = item,
            title = item.kind.name,
            status = status,
        )
        return if (index >= 0) {
            timeline.toMutableList().apply { this[index] = entry }
        } else {
            timeline + entry
        }
    }
}
