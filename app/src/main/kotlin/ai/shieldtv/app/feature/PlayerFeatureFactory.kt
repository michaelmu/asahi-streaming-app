package ai.shieldtv.app.feature

import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.player.presentation.PlayerPresenter
import ai.shieldtv.app.feature.player.presentation.PlayerViewModel

object PlayerFeatureFactory {
    fun createViewModel(): PlayerViewModel {
        val presenter = PlayerPresenter(
            resolveSourceUseCase = AppContainer.resolveSourceUseCase,
            buildPlaybackItemUseCase = AppContainer.buildPlaybackItemUseCase,
            playbackEngine = AppContainer.playbackEngine
        )
        return PlayerViewModel(presenter)
    }
}
