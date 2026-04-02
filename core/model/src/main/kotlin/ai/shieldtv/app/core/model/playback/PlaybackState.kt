package ai.shieldtv.app.core.model.playback

data class PlaybackState(
    val isBuffering: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val playerStateLabel: String = "idle",
    val videoFormat: String? = null,
    val videoSizeLabel: String? = null,
    val errorMessage: String? = null
)
