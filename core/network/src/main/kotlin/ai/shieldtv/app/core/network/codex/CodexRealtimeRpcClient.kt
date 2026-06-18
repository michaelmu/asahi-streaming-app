package ai.codexa.app.core.network.codex

import ai.codexa.app.core.model.codex.CodexCommandApprovalDecision
import ai.codexa.app.core.model.codex.CodexCommandApprovalRequest
import ai.codexa.app.core.model.codex.CodexConnectionConfig
import ai.codexa.app.core.model.codex.CodexConnectionState
import ai.codexa.app.core.model.codex.CodexConversationEvent
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalDecision
import ai.codexa.app.core.model.codex.CodexFileChangeApprovalRequest
import ai.codexa.app.core.model.codex.CodexItemKind
import ai.codexa.app.core.model.codex.CodexItemRef
import ai.codexa.app.core.model.codex.CodexStartThreadRequest
import ai.codexa.app.core.model.codex.CodexThreadSnapshot
import ai.codexa.app.core.model.codex.CodexThreadStatus
import ai.codexa.app.core.model.codex.CodexThreadSummary
import ai.codexa.app.core.model.codex.CodexTimelineEntry
import ai.codexa.app.core.model.codex.CodexTurnRef
import ai.codexa.app.core.model.codex.CodexTurnStatus
import ai.codexa.app.core.model.codex.CodexUserInputQuestion
import ai.codexa.app.core.model.codex.CodexUserInputRequest
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class CodexRealtimeRpcClient : CodexRpcClient, Closeable {
    private val gson: Gson = Gson()
    private val okHttpClient: OkHttpClient = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestCounter = AtomicLong(1)
    private val pendingResponses = ConcurrentHashMap<String, CompletableDeferred<JsonElement?>>()
    private val _events = MutableSharedFlow<CodexConversationEvent>(extraBufferCapacity = 64)

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var currentConfig: CodexConnectionConfig? = null

    override val events: Flow<CodexConversationEvent> = _events.asSharedFlow()

    override suspend fun connect(config: CodexConnectionConfig) {
        disconnect()
        currentConfig = config
        _events.tryEmit(CodexConversationEvent.ConnectionStateChanged(CodexConnectionState.Connecting))

        val requestBuilder = Request.Builder()
            .url(config.websocketUrl)

        if (config.bearerToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.bearerToken}")
        }

        val request = requestBuilder.build()

        val socket = okHttpClient.newWebSocket(request, Listener())
        webSocket = socket

        call(
            method = "initialize",
            params = CodexInitializeParams(
                clientInfo = CodexClientInfo(
                    name = config.clientName,
                    title = config.clientTitle,
                    version = config.clientVersion,
                ),
                capabilities = CodexInitializeCapabilities(
                    experimentalApi = false,
                    requestAttestation = false,
                ),
            ),
            responseClass = CodexInitializeResponse::class.java,
        )

        notify("initialized")
        _events.tryEmit(CodexConversationEvent.ConnectionStateChanged(CodexConnectionState.Connected))
    }

    override suspend fun disconnect() {
        val socket = webSocket
        webSocket = null
        socket?.close(1000, "client disconnect")
        pendingResponses.values.forEach { it.completeExceptionally(IOException("Disconnected")) }
        pendingResponses.clear()
        _events.tryEmit(CodexConversationEvent.ConnectionStateChanged(CodexConnectionState.Disconnected))
    }

    override suspend fun listThreads(limit: Int): List<CodexThreadSummary> {
        val response = call(
            method = "thread/list",
            params = CodexThreadListParams(limit = limit),
            responseClass = CodexThreadListResponse::class.java,
        )
        return response.data.map(::mapThread)
    }

    override suspend fun startThread(request: CodexStartThreadRequest): CodexThreadSummary {
        val response = call(
            method = "thread/start",
            params = CodexThreadStartParams(
                cwd = request.cwd,
                approvalPolicy = request.approvalPolicy,
                sandbox = request.sandbox,
                personality = request.personality,
            ),
            responseClass = CodexThreadStartResponse::class.java,
        )
        return mapThread(response.thread)
    }

    override suspend fun resumeThread(threadId: String, excludeTurns: Boolean): CodexThreadSummary {
        val response = call(
            method = "thread/resume",
            params = CodexThreadResumeParams(
                threadId = threadId,
                excludeTurns = excludeTurns,
            ),
            responseClass = CodexThreadResumeResponse::class.java,
        )
        return mapThread(response.thread)
    }

    override suspend fun readThread(threadId: String, includeTurns: Boolean): CodexThreadSnapshot {
        val response = call(
            method = "thread/read",
            params = CodexThreadReadParams(
                threadId = threadId,
                includeTurns = includeTurns,
            ),
            responseClass = CodexThreadReadResponse::class.java,
        )
        return CodexThreadSnapshot(
            summary = mapThread(
                CodexThreadDto(
                    id = response.thread.id,
                    preview = response.thread.preview,
                    createdAt = response.thread.createdAt,
                    updatedAt = response.thread.updatedAt,
                    status = response.thread.status,
                )
            ),
            activeTurn = response.thread.turns.lastOrNull()?.let { turn ->
                val status = mapTurnStatus(turn.status)
                if (status == CodexTurnStatus.InProgress) {
                    CodexTurnRef(
                        threadId = response.thread.id,
                        turnId = turn.id,
                        status = status,
                    )
                } else {
                    null
                }
            },
            timeline = response.thread.turns.flatMap { turn ->
                turn.items.mapNotNull { item -> mapHistoricalTimelineEntry(response.thread.id, turn.id, item) }
            },
        )
    }

    override suspend fun sendUserMessage(
        threadId: String,
        text: String,
        clientUserMessageId: String?,
    ): CodexTurnRef {
        val response = call(
            method = "turn/start",
            params = CodexTurnStartParams(
                threadId = threadId,
                clientUserMessageId = clientUserMessageId,
                input = listOf(CodexUserInputText(text = text)),
            ),
            responseClass = CodexTurnStartResponse::class.java,
        )
        return CodexTurnRef(
            threadId = threadId,
            turnId = response.turn.id,
            status = mapTurnStatus(response.turn.status),
        )
    }

    override suspend fun interruptTurn(threadId: String, turnId: String) {
        call(
            method = "turn/interrupt",
            params = CodexTurnInterruptParams(threadId = threadId, turnId = turnId),
            responseClass = CodexTurnInterruptResponse::class.java,
        )
    }

    override suspend fun respondToCommandApproval(
        requestId: String,
        decision: CodexCommandApprovalDecision,
    ) {
        sendResponse(
            requestId = requestId,
            result = CodexCommandApprovalResponseDto(decision = decision.toWireValue()),
        )
    }

    override suspend fun respondToFileChangeApproval(
        requestId: String,
        decision: CodexFileChangeApprovalDecision,
    ) {
        sendResponse(
            requestId = requestId,
            result = CodexFileChangeApprovalResponseDto(decision = decision.toWireValue()),
        )
    }

    override suspend fun respondToUserInput(
        requestId: String,
        answers: Map<String, List<String>>,
    ) {
        sendResponse(
            requestId = requestId,
            result = CodexToolUserInputResponseDto(
                answers = answers.mapValues { (_, value) -> CodexToolUserInputAnswerDto(value) },
            ),
        )
    }

    override fun close() {
        runCatching { runBlocking { disconnect() } }
        scope.cancel()
    }

    private suspend fun <T> call(
        method: String,
        params: Any? = null,
        responseClass: Class<T>,
    ): T {
        val idValue = requestCounter.getAndIncrement().toString()
        val deferred = CompletableDeferred<JsonElement?>()
        pendingResponses[idValue] = deferred

        sendEnvelope(
            CodexJsonRpcRequest(
                id = JsonPrimitive(idValue),
                method = method,
                params = params,
            )
        )

        val result = deferred.await() ?: error("Missing result for method=$method")
        return gson.fromJson(result, responseClass)
    }

    private suspend fun notify(method: String, params: Any? = null) {
        sendEnvelope(CodexJsonRpcNotification(method = method, params = params))
    }

    private suspend fun sendResponse(requestId: String, result: Any) {
        val socket = webSocket ?: error("WebSocket is not connected")
        val json = gson.toJson(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to requestId,
                "result" to gson.toJsonTree(result),
            )
        )
        check(socket.send(json)) { "Failed to send response" }
    }

    private fun sendEnvelope(envelope: Any) {
        val socket = webSocket ?: error("WebSocket is not connected")
        check(socket.send(gson.toJson(envelope))) { "Failed to send websocket message" }
    }

    private fun handleTextMessage(text: String) {
        val tree = gson.fromJson(text, JsonElement::class.java)
        val obj = tree.asJsonObject
        when {
            obj.has("result") || obj.has("error") -> handleResponse(text)
            obj.has("id") && obj.has("method") -> handleServerRequest(text)
            obj.has("method") -> handleNotification(text)
        }
    }

    private fun handleResponse(text: String) {
        val envelope = gson.fromJson(text, CodexJsonRpcResponseEnvelope::class.java)
        val idKey = envelope.id?.asString ?: return
        val deferred = pendingResponses.remove(idKey) ?: return
        if (envelope.error != null) {
            deferred.completeExceptionally(IOException("RPC error ${envelope.error.code}: ${envelope.error.message}"))
        } else {
            deferred.complete(envelope.result)
        }
    }

    private fun handleServerRequest(text: String) {
        val envelope = gson.fromJson(text, CodexJsonRpcIncomingRequestEnvelope::class.java)
        val requestId = envelope.id.toRequestIdString()
        when (envelope.method) {
            "item/commandExecution/requestApproval" -> {
                val params = gson.fromJson(envelope.params, CodexCommandApprovalRequestDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.CommandApprovalRequested(
                        CodexCommandApprovalRequest(
                            requestId = requestId,
                            item = CodexItemRef(
                                threadId = params.threadId,
                                turnId = params.turnId,
                                itemId = params.itemId,
                                kind = CodexItemKind.CommandExecution,
                            ),
                            command = params.command,
                            cwd = params.cwd,
                            reason = params.reason,
                        )
                    )
                )
            }
            "item/fileChange/requestApproval" -> {
                val params = gson.fromJson(envelope.params, CodexFileChangeApprovalRequestDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.FileChangeApprovalRequested(
                        CodexFileChangeApprovalRequest(
                            requestId = requestId,
                            item = CodexItemRef(
                                threadId = params.threadId,
                                turnId = params.turnId,
                                itemId = params.itemId,
                                kind = CodexItemKind.FileChange,
                            ),
                            reason = params.reason,
                            grantRoot = params.grantRoot,
                        )
                    )
                )
            }
            "item/tool/requestUserInput" -> {
                val params = gson.fromJson(envelope.params, CodexToolUserInputRequestDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.UserInputRequested(
                        CodexUserInputRequest(
                            requestId = requestId,
                            item = CodexItemRef(
                                threadId = params.threadId,
                                turnId = params.turnId,
                                itemId = params.itemId,
                                kind = CodexItemKind.Unknown,
                            ),
                            questions = params.questions.map { question ->
                                CodexUserInputQuestion(
                                    id = question.id,
                                    prompt = listOfNotNull(question.header, question.question)
                                        .joinToString("\n")
                                        .trim(),
                                    options = question.options?.map { it.label }.orEmpty(),
                                )
                            },
                            autoResolutionMs = params.autoResolutionMs,
                        )
                    )
                )
            }
        }
    }

    private fun handleNotification(text: String) {
        val envelope = gson.fromJson(text, CodexJsonRpcIncomingNotificationEnvelope::class.java)
        when (envelope.method) {
            "thread/started" -> {
                val params = gson.fromJson(envelope.params, CodexThreadStartedNotificationDto::class.java)
                _events.tryEmit(CodexConversationEvent.ThreadStarted(mapThread(params.thread)))
            }
            "thread/status/changed" -> {
                val params = gson.fromJson(envelope.params, CodexThreadStatusChangedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.ThreadStatusChanged(
                        threadId = params.threadId,
                        status = mapThreadStatus(params.status),
                    )
                )
            }
            "turn/started" -> {
                val params = gson.fromJson(envelope.params, CodexTurnStartedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.TurnStarted(
                        CodexTurnRef(
                            threadId = params.threadId,
                            turnId = params.turn.id,
                            status = mapTurnStatus(params.turn.status),
                        )
                    )
                )
            }
            "item/started" -> {
                val params = gson.fromJson(envelope.params, CodexItemStartedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.ItemStarted(
                        item = CodexItemRef(
                            threadId = params.threadId,
                            turnId = params.turnId,
                            itemId = params.item.id,
                            kind = mapItemKind(params.item.type),
                        ),
                        startedAtMs = params.startedAtMs,
                    )
                )
            }
            "item/agentMessage/delta" -> {
                val params = gson.fromJson(envelope.params, CodexAgentMessageDeltaNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.AgentMessageDelta(
                        item = CodexItemRef(
                            threadId = params.threadId,
                            turnId = params.turnId,
                            itemId = params.itemId,
                            kind = CodexItemKind.AgentMessage,
                        ),
                        delta = params.delta,
                    )
                )
            }
            "item/completed" -> {
                val params = gson.fromJson(envelope.params, CodexItemCompletedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.ItemCompleted(
                        item = CodexItemRef(
                            threadId = params.threadId,
                            turnId = params.turnId,
                            itemId = params.item.id,
                            kind = mapItemKind(params.item.type),
                        ),
                        completedAtMs = params.completedAtMs,
                    )
                )
            }
            "turn/completed" -> {
                val params = gson.fromJson(envelope.params, CodexTurnCompletedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.TurnCompleted(
                        CodexTurnRef(
                            threadId = params.threadId,
                            turnId = params.turn.id,
                            status = mapTurnStatus(params.turn.status),
                        )
                    )
                )
            }
            "serverRequest/resolved" -> {
                val params = gson.fromJson(envelope.params, CodexServerRequestResolvedNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.ServerRequestResolved(
                        threadId = params.threadId,
                        requestId = params.requestId.toRequestIdString(),
                    )
                )
            }
            "warning" -> {
                val params = gson.fromJson(envelope.params, CodexWarningNotificationDto::class.java)
                _events.tryEmit(CodexConversationEvent.Warning(params.threadId, params.message))
            }
            "error" -> {
                val params = gson.fromJson(envelope.params, CodexErrorNotificationDto::class.java)
                _events.tryEmit(
                    CodexConversationEvent.Error(
                        threadId = params.threadId,
                        turnId = params.turnId,
                        message = params.error.message,
                        willRetry = params.willRetry,
                    )
                )
            }
        }
    }

    private fun mapThread(dto: CodexThreadDto): CodexThreadSummary = CodexThreadSummary(
        id = dto.id,
        preview = dto.preview,
        createdAtEpochSeconds = dto.createdAt,
        updatedAtEpochSeconds = dto.updatedAt,
        status = mapThreadStatus(dto.status),
    )

    private fun mapThreadStatus(dto: CodexThreadStatusDto?): CodexThreadStatus = when (dto?.type) {
        "idle" -> CodexThreadStatus.Idle
        "active" -> CodexThreadStatus.Active
        "systemError" -> CodexThreadStatus.SystemError
        else -> CodexThreadStatus.NotLoaded
    }

    private fun mapTurnStatus(status: String?): CodexTurnStatus = when (status) {
        "completed" -> CodexTurnStatus.Completed
        "interrupted" -> CodexTurnStatus.Interrupted
        "failed" -> CodexTurnStatus.Failed
        else -> CodexTurnStatus.InProgress
    }

    private fun mapHistoricalTimelineEntry(
        threadId: String,
        turnId: String,
        item: CodexItemDetailDto,
    ): CodexTimelineEntry? {
        val itemRef = CodexItemRef(
            threadId = threadId,
            turnId = turnId,
            itemId = item.id,
            kind = mapItemKind(item.type),
        )
        return when (item.type) {
            "userMessage" -> CodexTimelineEntry.UserMessage(
                item = itemRef,
                text = item.content.orEmpty().mapNotNull { it.extractDisplayText() }.joinToString("\n").ifBlank {
                    item.text.orEmpty()
                },
            )
            "agentMessage" -> CodexTimelineEntry.AgentMessage(
                item = itemRef,
                text = item.text.orEmpty(),
                completed = true,
            )
            "reasoning" -> CodexTimelineEntry.ToolActivity(
                item = itemRef,
                title = "Reasoning",
                status = "completed",
                details = (item.summary ?: item.content.orEmpty().mapNotNull { it.extractDisplayText() })
                    .orEmpty()
                    .joinToString("\n"),
            )
            "commandExecution" -> CodexTimelineEntry.ToolActivity(
                item = itemRef,
                title = item.command ?: "Command",
                status = item.status ?: "completed",
                details = listOfNotNull(
                    item.cwd?.let { "cwd: $it" },
                    item.exitCode?.let { "exit: $it" },
                    item.aggregatedOutput?.takeIf { it.isNotBlank() },
                ).joinToString("\n").ifBlank { null },
            )
            "fileChange" -> CodexTimelineEntry.ToolActivity(
                item = itemRef,
                title = "File changes",
                status = item.status ?: "completed",
                details = item.changes
                    ?.mapNotNull { change -> change.path?.let { path -> "${change.kind ?: "change"}: $path" } }
                    ?.joinToString("\n")
                    ?.ifBlank { null },
            )
            else -> null
        }
    }

    private fun JsonElement.extractDisplayText(): String? {
        if (isJsonPrimitive) return asString
        if (!isJsonObject) return null
        val obj = asJsonObject
        return when {
            obj.has("text") && obj.get("text").isJsonPrimitive -> obj.get("text").asString
            obj.has("content") && obj.get("content").isJsonPrimitive -> obj.get("content").asString
            obj.has("summary") && obj.get("summary").isJsonPrimitive -> obj.get("summary").asString
            else -> null
        }
    }

    private fun mapItemKind(type: String): CodexItemKind = when (type) {
        "agentMessage" -> CodexItemKind.AgentMessage
        "commandExecution" -> CodexItemKind.CommandExecution
        "fileChange" -> CodexItemKind.FileChange
        "reasoning" -> CodexItemKind.Reasoning
        else -> CodexItemKind.Unknown
    }

    private fun CodexCommandApprovalDecision.toWireValue(): String = when (this) {
        CodexCommandApprovalDecision.Accept -> "accept"
        CodexCommandApprovalDecision.AcceptForSession -> "acceptForSession"
        CodexCommandApprovalDecision.Decline -> "decline"
        CodexCommandApprovalDecision.Cancel -> "cancel"
    }

    private fun CodexFileChangeApprovalDecision.toWireValue(): String = when (this) {
        CodexFileChangeApprovalDecision.Accept -> "accept"
        CodexFileChangeApprovalDecision.AcceptForSession -> "acceptForSession"
        CodexFileChangeApprovalDecision.Decline -> "decline"
        CodexFileChangeApprovalDecision.Cancel -> "cancel"
    }

    private fun JsonElement.toRequestIdString(): String = when {
        isJsonPrimitive -> asJsonPrimitive.asString
        else -> toString()
    }

    private inner class Listener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching {
                handleTextMessage(text)
            }.onFailure { error ->
                _events.tryEmit(
                    CodexConversationEvent.Warning(
                        threadId = null,
                        message = "Failed to process Codex event: ${error.message}",
                    )
                )
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (this@CodexRealtimeRpcClient.webSocket != webSocket) return
            pendingResponses.values.forEach { it.completeExceptionally(IOException("Disconnected")) }
            pendingResponses.clear()
            this@CodexRealtimeRpcClient.webSocket = null
            _events.tryEmit(
                CodexConversationEvent.ConnectionStateChanged(
                    CodexConnectionState.Disconnected,
                    reason.ifBlank { "WebSocket closed ($code)" },
                )
            )
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (this@CodexRealtimeRpcClient.webSocket != webSocket) return
            pendingResponses.values.forEach { it.completeExceptionally(t) }
            pendingResponses.clear()
            this@CodexRealtimeRpcClient.webSocket = null
            val message = t.message ?: t.javaClass.simpleName ?: "unknown websocket failure"
            _events.tryEmit(
                CodexConversationEvent.ConnectionStateChanged(
                    CodexConnectionState.Disconnected,
                    "Codex websocket failed: $message",
                )
            )
            _events.tryEmit(CodexConversationEvent.Warning(message = "Codex websocket failed: $message", threadId = null))
        }
    }
}
