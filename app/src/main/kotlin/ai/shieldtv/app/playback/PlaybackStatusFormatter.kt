package ai.shieldtv.app.playback

import ai.shieldtv.app.feature.player.ui.PlayerUiState

object PlaybackStatusFormatter {
    fun formatFailure(
        playerState: PlayerUiState,
        latestPlaybackErrorMessage: String?
    ): String {
        return buildString {
            append("Playback error type: ${playerState.errorType ?: "unknown"}")
            appendLine()
            append("Playback error: ${latestPlaybackErrorMessage ?: playerState.error ?: "none"}")
        }
    }
}
