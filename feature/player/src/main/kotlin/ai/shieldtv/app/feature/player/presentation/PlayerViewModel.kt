package ai.shieldtv.app.feature.player.presentation

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.feature.player.ui.PlayerUiState

class PlayerViewModel(
    private val playerPresenter: PlayerPresenter
) {
    suspend fun prepare(source: SourceResult): PlayerUiState {
        return playerPresenter.prepare(source)
    }
}
