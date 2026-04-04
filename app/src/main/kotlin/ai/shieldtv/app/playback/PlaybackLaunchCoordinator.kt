package ai.shieldtv.app.playback

import ai.shieldtv.app.AppCoordinator
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.playback.PlaybackEngine
import ai.shieldtv.app.feature.player.presentation.PlayerViewModel
import ai.shieldtv.app.feature.player.ui.PlayerUiState
import ai.shieldtv.app.history.WatchHistoryCoordinator

sealed interface PlaybackLaunchResult {
    data class Blocked(
        val playbackError: String,
        val playbackMessage: String,
        val statusMessage: String
    ) : PlaybackLaunchResult

    data class Prepared(
        val statusMessage: String,
        val playerState: PlayerUiState,
        val currentPlaybackState: PlaybackState,
        val selectedSource: SourceResult
    ) : PlaybackLaunchResult

    data class Failed(
        val statusMessage: String,
        val playbackError: String?,
        val playbackMessage: String,
        val playerState: PlayerUiState
    ) : PlaybackLaunchResult
}

class PlaybackLaunchCoordinator(
    private val playerViewModel: PlayerViewModel,
    private val playbackEngine: PlaybackEngine,
    private val playbackSessionStore: PlaybackSessionStore,
    private val watchHistoryCoordinator: WatchHistoryCoordinator
) {
    fun shouldBlockPlayback(source: SourceResult, authLinked: Boolean): PlaybackLaunchResult.Blocked? {
        if (source.debridService != DebridService.REAL_DEBRID || authLinked) return null
        return PlaybackLaunchResult.Blocked(
            playbackError = "Real-Debrid link required before playback.",
            playbackMessage = "This source needs Real-Debrid. Link your account in Settings / Accounts, then try again.",
            statusMessage = "Real-Debrid link required before playback."
        )
    }

    suspend fun launch(
        source: SourceResult,
        seasonNumber: Int?,
        episodeNumber: Int?,
        startPositionMs: Long,
        latestPlaybackState: PlaybackState,
        coordinator: AppCoordinator,
        artworkUrl: String?
    ): PlaybackLaunchResult {
        val playerState = playerViewModel.prepare(
            source = source,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            startPositionMs = startPositionMs
        )

        if (!playerState.prepared) {
            return PlaybackLaunchResult.Failed(
                statusMessage = playerState.error ?: "Playback not prepared.",
                playbackError = playerState.error,
                playbackMessage = buildFailureMessage(
                    source = source,
                    playerState = playerState,
                    currentPlaybackState = latestPlaybackState
                ),
                playerState = playerState
            )
        }

        val currentItem = playbackEngine.getCurrentItem()
        if (currentItem != null) {
            val progressPercent = when {
                latestPlaybackState.durationMs > 0 -> ((latestPlaybackState.positionMs * 100) / latestPlaybackState.durationMs).toInt()
                else -> 8
            }
            coordinator.recordContinueWatching(
                mediaRef = source.mediaRef,
                artworkUrl = artworkUrl,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                progressPercent = progressPercent
            )
            playbackSessionStore.saveActiveResume(
                item = currentItem,
                state = latestPlaybackState,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            watchHistoryCoordinator.recordPlayback(
                item = currentItem,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
        }

        return PlaybackLaunchResult.Prepared(
            statusMessage = "Playback item prepared.",
            playerState = playerState,
            currentPlaybackState = latestPlaybackState,
            selectedSource = source
        )
    }

    private fun buildFailureMessage(
        source: SourceResult,
        playerState: PlayerUiState,
        currentPlaybackState: PlaybackState
    ): String {
        return buildString {
            appendLine("Playback preparation failed.")
            appendLine("Current item: ${playbackEngine.getCurrentItem()?.title ?: source.displayName}")
            appendLine("Stream URL: ${playerState.playbackUrl ?: playbackEngine.getCurrentUrl() ?: source.url}")
            appendLine("Player state: ${currentPlaybackState.playerStateLabel}")
            appendLine("Video format: ${currentPlaybackState.videoFormat ?: "unknown"}")
            appendLine("Video size: ${currentPlaybackState.videoSizeLabel ?: "unknown"}")
            append("Playback error type: ${playerState.errorType ?: "unknown"}")
            appendLine()
            append("Playback error: ${currentPlaybackState.errorMessage ?: playerState.error ?: "none"}")
        }
    }
}
