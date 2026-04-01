package ai.shieldtv.app.core.model.playback

data class PlaybackState(
    val isBuffering: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val errorMessage: String? = null
)
