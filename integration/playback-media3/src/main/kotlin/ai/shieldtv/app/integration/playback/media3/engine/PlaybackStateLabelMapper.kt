package ai.shieldtv.app.integration.playback.media3.engine

import androidx.media3.common.Player

object PlaybackStateLabelMapper {
    fun fromPlaybackState(playbackState: Int, isPlaying: Boolean): String {
        return when (playbackState) {
            Player.STATE_IDLE -> if (isPlaying) "playing" else "idle"
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_READY -> if (isPlaying) "playing" else "paused"
            Player.STATE_ENDED -> "ended"
            else -> "unknown"
        }
    }

    fun fromIsPlayingChange(
        isPlaying: Boolean,
        isBuffering: Boolean,
        playbackState: Int,
        currentLabel: String
    ): String {
        return when {
            isBuffering -> "buffering"
            isPlaying -> "playing"
            playbackState == Player.STATE_ENDED -> "ended"
            playbackState == Player.STATE_READY -> "paused"
            else -> currentLabel
        }
    }
}
