package ai.shieldtv.app.feature.player.presentation

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.feature.player.ui.PlayerUiState

class PlayerViewModel(
    private val playerPresenter: PlayerPresenter
) {
    suspend fun prepare(
        source: SourceResult,
        seasonNumber: Int? = source.seasonNumber,
        episodeNumber: Int? = source.episodeNumber
    ): PlayerUiState {
        return playerPresenter.prepare(
            source = source,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }
}
