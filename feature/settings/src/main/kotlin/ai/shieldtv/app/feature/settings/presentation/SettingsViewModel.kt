package ai.shieldtv.app.feature.settings.presentation

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.feature.settings.ui.AccountUiState

class SettingsViewModel(
    private val accountPresenter: AccountPresenter
) {
    suspend fun getAuthState(): AccountUiState {
        return accountPresenter.getAuthState()
    }

    suspend fun startLinking(): AccountUiState {
        return accountPresenter.startLinking()
    }

    suspend fun pollLinking(flow: DeviceCodeFlow): AccountUiState {
        return accountPresenter.pollLinking(flow)
    }
}
