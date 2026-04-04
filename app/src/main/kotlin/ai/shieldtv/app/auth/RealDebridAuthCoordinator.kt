package ai.shieldtv.app.auth

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RealDebridLinkStartSuccess(
    val flow: DeviceCodeFlow,
    val authState: RealDebridAuthState,
    val statusMessage: String
)

data class RealDebridLinkStartFailure(
    val authState: RealDebridAuthState,
    val statusMessage: String,
    val debugMessage: String
)

sealed class RealDebridLinkStartResult {
    data class Success(val value: RealDebridLinkStartSuccess) : RealDebridLinkStartResult()
    data class Failure(val value: RealDebridLinkStartFailure) : RealDebridLinkStartResult()
}

data class RealDebridPollSuccess(
    val authState: RealDebridAuthState,
    val activeDeviceFlow: DeviceCodeFlow?,
    val statusMessage: String? = null,
    val linkedMessage: String? = null
)

data class RealDebridPollTimeout(
    val authState: RealDebridAuthState,
    val statusMessage: String,
    val timeoutMessage: String
)

class RealDebridAuthCoordinator(
    private val scope: CoroutineScope,
    private val startDeviceFlow: suspend () -> DeviceCodeFlow,
    private val pollDeviceFlow: suspend (DeviceCodeFlow) -> RealDebridAuthState,
    private val clearAuth: () -> Unit,
    private val buildAuthUrl: (DeviceCodeFlow) -> String,
    private val buildStartFailureMessage: (Throwable) -> String
) {
    private var authPollingJob: Job? = null

    fun cancel() {
        authPollingJob?.cancel()
        authPollingJob = null
    }

    fun resetAuth(): RealDebridAuthState {
        clearAuth()
        cancel()
        return RealDebridAuthState(isLinked = false, authInProgress = false, lastError = null)
    }

    suspend fun startLink(): RealDebridLinkStartResult {
        return runCatching { startDeviceFlow() }
            .fold(
                onSuccess = { flow ->
                    RealDebridLinkStartResult.Success(
                        RealDebridLinkStartSuccess(
                            flow = flow,
                            authState = RealDebridAuthState(
                                isLinked = false,
                                authInProgress = true,
                                lastError = null
                            ),
                            statusMessage = "Real-Debrid device flow started: ${flow.userCode}"
                        )
                    )
                },
                onFailure = { error ->
                    RealDebridLinkStartResult.Failure(
                        RealDebridLinkStartFailure(
                            authState = RealDebridAuthState(
                                isLinked = false,
                                authInProgress = false,
                                lastError = buildStartFailureMessage(error)
                            ),
                            statusMessage = "Failed to start Real-Debrid link flow.",
                            debugMessage = buildStartFailureMessage(error)
                        )
                    )
                }
            )
    }

    fun startAutoPolling(
        flow: DeviceCodeFlow,
        currentAuthState: () -> RealDebridAuthState,
        onStateUpdated: (RealDebridPollSuccess) -> Unit,
        onTimeout: (RealDebridPollTimeout) -> Unit
    ) {
        cancel()
        authPollingJob = scope.launch {
            val startedAt = System.currentTimeMillis()
            val timeoutMs = 2 * 60 * 1000L
            while (System.currentTimeMillis() - startedAt < timeoutMs && !currentAuthState().isLinked) {
                delay(flow.pollIntervalSeconds.coerceAtLeast(2) * 1000L)
                runCatching {
                    pollDeviceFlow(flow)
                }.onSuccess { state ->
                    if (state.isLinked) {
                        cancel()
                        onStateUpdated(
                            RealDebridPollSuccess(
                                authState = state,
                                activeDeviceFlow = null,
                                statusMessage = "Real-Debrid linked successfully.",
                                linkedMessage = state.username?.let { "Connected as $it." } ?: "Real-Debrid linked successfully."
                            )
                        )
                        return@launch
                    }
                    onStateUpdated(
                        RealDebridPollSuccess(
                            authState = state,
                            activeDeviceFlow = flow
                        )
                    )
                }.onFailure { error ->
                    onStateUpdated(
                        RealDebridPollSuccess(
                            authState = RealDebridAuthState(
                                isLinked = false,
                                authInProgress = true,
                                lastError = error.message
                            ),
                            activeDeviceFlow = flow
                        )
                    )
                }
            }
            if (!currentAuthState().isLinked) {
                val timedOut = currentAuthState().copy(
                    authInProgress = false,
                    lastError = currentAuthState().lastError ?: "Real-Debrid link timed out after 2 minutes."
                )
                onTimeout(
                    RealDebridPollTimeout(
                        authState = timedOut,
                        statusMessage = "Real-Debrid link polling timed out.",
                        timeoutMessage = timedOut.lastError ?: "The device link flow expired before authorization completed."
                    )
                )
            }
        }
    }

    fun authUrl(flow: DeviceCodeFlow): String = buildAuthUrl(flow)
}
