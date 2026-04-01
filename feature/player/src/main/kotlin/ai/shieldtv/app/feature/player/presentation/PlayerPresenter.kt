package ai.shieldtv.app.feature.player.presentation

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.playback.PlaybackEngine
import ai.shieldtv.app.domain.usecase.playback.BuildPlaybackItemUseCase
import ai.shieldtv.app.domain.usecase.sources.ResolveSourceUseCase
import ai.shieldtv.app.feature.player.ui.PlayerUiState

class PlayerPresenter(
    private val resolveSourceUseCase: ResolveSourceUseCase,
    private val buildPlaybackItemUseCase: BuildPlaybackItemUseCase,
    private val playbackEngine: PlaybackEngine
) {
    suspend fun prepare(
        source: SourceResult,
        seasonNumber: Int? = source.seasonNumber,
        episodeNumber: Int? = source.episodeNumber
    ): PlayerUiState {
        return try {
            val resolvedStream = resolveSourceUseCase(
                source = source,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            val playbackItem = buildPlaybackItemUseCase(resolvedStream)
            playbackEngine.prepare(playbackItem)
            PlayerUiState(
                loading = false,
                prepared = true,
                playbackUrl = resolvedStream.url,
                error = null
            )
        } catch (error: Throwable) {
            PlayerUiState(prepared = false, playbackUrl = null, error = error.message)
        }
    }
}
