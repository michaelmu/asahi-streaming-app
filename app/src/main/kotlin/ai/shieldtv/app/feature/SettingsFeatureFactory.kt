package ai.shieldtv.app.feature

import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.settings.presentation.AccountPresenter
import ai.shieldtv.app.feature.settings.presentation.SettingsViewModel

object SettingsFeatureFactory {
    fun createViewModel(): SettingsViewModel {
        val presenter = AccountPresenter(AppContainer.startRealDebridDeviceFlowUseCase)
        return SettingsViewModel(presenter)
    }
}
