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
        episodeNumber: Int? = source.episodeNumber,
        startPositionMs: Long = 0L
    ): PlayerUiState {
        val resolvedStream = try {
            resolveSourceUseCase(
                source = source,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
        } catch (error: Throwable) {
            return PlayerUiState(
                prepared = false,
                playbackUrl = null,
                error = error.message,
                errorType = PlaybackPrepareErrorType.ResolveFailed::class.simpleName
            )
        }

        return try {
            val playbackItem = buildPlaybackItemUseCase(resolvedStream)
            playbackEngine.prepare(playbackItem, startPositionMs)
            PlayerUiState(
                loading = false,
                prepared = true,
                playbackUrl = resolvedStream.url,
                error = null,
                errorType = null
            )
        } catch (error: Throwable) {
            PlayerUiState(
                prepared = false,
                playbackUrl = null,
                error = error.message,
                errorType = PlaybackPrepareErrorType.PrepareFailed::class.simpleName
            )
        }
    }
}
