package ai.shieldtv.app.playback

data class ActivePlaybackResumeRecord(
    val mediaTitle: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val queryHint: String,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Int,
    val playbackUrl: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
