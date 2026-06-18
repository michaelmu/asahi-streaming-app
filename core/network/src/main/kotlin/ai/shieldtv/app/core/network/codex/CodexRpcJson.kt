package ai.codexa.app.core.network.codex

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class CodexJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement,
    val method: String,
    val params: Any? = null,
)

data class CodexJsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Any? = null,
)

data class CodexJsonRpcResponseEnvelope(
    val jsonrpc: String? = null,
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: CodexJsonRpcError? = null,
)

data class CodexJsonRpcIncomingRequestEnvelope(
    val jsonrpc: String? = null,
    val id: JsonElement,
    val method: String,
    val params: JsonElement? = null,
)

data class CodexJsonRpcIncomingNotificationEnvelope(
    val jsonrpc: String? = null,
    val method: String,
    val params: JsonElement? = null,
)

data class CodexJsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)

data class CodexInitializeParams(
    val clientInfo: CodexClientInfo,
    val capabilities: CodexInitializeCapabilities? = null,
)

data class CodexClientInfo(
    val name: String,
    val title: String? = null,
    val version: String,
)

data class CodexInitializeCapabilities(
    val experimentalApi: Boolean = false,
    val requestAttestation: Boolean = false,
    val optOutNotificationMethods: List<String>? = null,
)

data class CodexInitializeResponse(
    val userAgent: String,
    val codexHome: String,
    val platformFamily: String,
    val platformOs: String,
)

data class CodexThreadListParams(
    val limit: Int? = null,
)

data class CodexThreadDto(
    val id: String,
    val preview: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val status: CodexThreadStatusDto? = null,
)

data class CodexThreadListResponse(
    val data: List<CodexThreadDto>,
)

data class CodexThreadStartParams(
    val cwd: String? = null,
    val approvalPolicy: String? = null,
    val sandbox: String? = null,
    val personality: String? = null,
)

data class CodexThreadStartResponse(
    val thread: CodexThreadDto,
)

data class CodexThreadResumeParams(
    val threadId: String,
    val excludeTurns: Boolean = true,
)

data class CodexThreadResumeResponse(
    val thread: CodexThreadDto,
)

data class CodexThreadReadParams(
    val threadId: String,
    val includeTurns: Boolean = true,
)

data class CodexThreadReadResponse(
    val thread: CodexThreadDetailDto,
)

data class CodexThreadDetailDto(
    val id: String,
    val preview: String = "",
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val status: CodexThreadStatusDto? = null,
    val turns: List<CodexTurnDetailDto> = emptyList(),
)

data class CodexTurnDetailDto(
    val id: String,
    val status: String? = null,
    val items: List<CodexItemDetailDto> = emptyList(),
)

data class CodexItemDetailDto(
    val type: String,
    val id: String,
    val text: String? = null,
    val content: List<JsonElement>? = null,
    val summary: List<String>? = null,
    val command: String? = null,
    val cwd: String? = null,
    val aggregatedOutput: String? = null,
    val exitCode: Int? = null,
    val changes: List<CodexFileChangeDetailDto>? = null,
    val status: String? = null,
)

data class CodexFileChangeDetailDto(
    val path: String? = null,
    val kind: String? = null,
)

data class CodexTurnDto(
    val id: String,
    val status: String? = null,
)

data class CodexTurnStartParams(
    val threadId: String,
    val clientUserMessageId: String? = null,
    val input: List<CodexUserInputText>,
)

data class CodexUserInputText(
    val type: String = "text",
    val text: String,
)

data class CodexTurnStartResponse(
    val turn: CodexTurnDto,
)

data class CodexTurnInterruptParams(
    val threadId: String,
    val turnId: String,
)

class CodexTurnInterruptResponse

data class CodexThreadStartedNotificationDto(
    val thread: CodexThreadDto,
)

data class CodexThreadStatusChangedNotificationDto(
    val threadId: String,
    val status: CodexThreadStatusDto,
)

data class CodexThreadStatusDto(
    val type: String,
    @SerializedName("activeFlags")
    val activeFlags: List<String>? = null,
)

data class CodexTurnStartedNotificationDto(
    val threadId: String,
    val turn: CodexTurnDto,
)

data class CodexTurnCompletedNotificationDto(
    val threadId: String,
    val turn: CodexTurnDto,
)

data class CodexItemDto(
    val type: String,
    val id: String,
)

data class CodexItemStartedNotificationDto(
    val threadId: String,
    val turnId: String,
    val item: CodexItemDto,
    val startedAtMs: Long,
)

data class CodexItemCompletedNotificationDto(
    val threadId: String,
    val turnId: String,
    val item: CodexItemDto,
    val completedAtMs: Long,
)

data class CodexAgentMessageDeltaNotificationDto(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val delta: String,
)

data class CodexWarningNotificationDto(
    val threadId: String? = null,
    val message: String,
)

data class CodexErrorNotificationDto(
    val threadId: String,
    val turnId: String,
    val willRetry: Boolean,
    val error: CodexTurnErrorDto,
)

data class CodexTurnErrorDto(
    val message: String,
)

data class CodexServerRequestResolvedNotificationDto(
    val threadId: String,
    val requestId: JsonElement,
)

data class CodexCommandApprovalRequestDto(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val reason: String? = null,
    val command: String? = null,
    val cwd: String? = null,
)

data class CodexCommandApprovalResponseDto(
    val decision: String,
)

data class CodexFileChangeApprovalRequestDto(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val reason: String? = null,
    val grantRoot: String? = null,
)

data class CodexFileChangeApprovalResponseDto(
    val decision: String,
)

data class CodexToolUserInputRequestDto(
    val threadId: String,
    val turnId: String,
    val itemId: String,
    val questions: List<CodexToolQuestionDto>,
    val autoResolutionMs: Long? = null,
)

data class CodexToolQuestionDto(
    val id: String,
    val header: String,
    val question: String,
    val options: List<CodexToolOptionDto>? = null,
)

data class CodexToolOptionDto(
    val label: String,
    val description: String,
)

data class CodexToolUserInputAnswerDto(
    val answers: List<String>,
)

data class CodexToolUserInputResponseDto(
    val answers: Map<String, CodexToolUserInputAnswerDto>,
)
