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
    suspend fun prepare(source: SourceResult): PlayerUiState {
        return try {
            val resolvedStream = resolveSourceUseCase(source)
            val playbackItem = buildPlaybackItemUseCase(resolvedStream)
            playbackEngine.prepare(playbackItem)
            PlayerUiState(loading = false, error = null)
        } catch (error: Throwable) {
            PlayerUiState(error = error.message)
        }
    }
}
