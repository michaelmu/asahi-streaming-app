package ai.shieldtv.app.feature.settings.presentation

import ai.shieldtv.app.feature.settings.ui.AccountUiState

class SettingsViewModel(
    private val accountPresenter: AccountPresenter
) {
    suspend fun startLinking(): AccountUiState {
        return accountPresenter.startLinking()
    }
}
