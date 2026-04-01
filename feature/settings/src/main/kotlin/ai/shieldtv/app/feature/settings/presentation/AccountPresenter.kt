package ai.shieldtv.app.feature.settings.presentation

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.domain.usecase.auth.PollRealDebridDeviceFlowUseCase
import ai.shieldtv.app.domain.usecase.auth.StartRealDebridDeviceFlowUseCase
import ai.shieldtv.app.feature.settings.ui.AccountUiState

class AccountPresenter(
    private val startRealDebridDeviceFlowUseCase: StartRealDebridDeviceFlowUseCase,
    private val pollRealDebridDeviceFlowUseCase: PollRealDebridDeviceFlowUseCase
) {
    suspend fun startLinking(): AccountUiState {
        return try {
            val flow = startRealDebridDeviceFlowUseCase()
            AccountUiState(
                loading = false,
                authState = AccountUiState().authState.copy(authInProgress = true),
                deviceCodeFlow = flow,
                error = null
            )
        } catch (error: Throwable) {
            AccountUiState(error = error.message)
        }
    }

    suspend fun pollLinking(flow: DeviceCodeFlow): AccountUiState {
        return try {
            val authState = pollRealDebridDeviceFlowUseCase(flow)
            AccountUiState(
                loading = false,
                authState = authState,
                deviceCodeFlow = flow,
                error = authState.lastError
            )
        } catch (error: Throwable) {
            AccountUiState(error = error.message, deviceCodeFlow = flow)
        }
    }
}
