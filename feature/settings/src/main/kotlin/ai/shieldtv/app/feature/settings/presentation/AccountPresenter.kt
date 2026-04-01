package ai.shieldtv.app.feature.settings.presentation

import ai.shieldtv.app.domain.usecase.auth.StartRealDebridDeviceFlowUseCase
import ai.shieldtv.app.feature.settings.ui.AccountUiState

class AccountPresenter(
    private val startRealDebridDeviceFlowUseCase: StartRealDebridDeviceFlowUseCase
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
}
