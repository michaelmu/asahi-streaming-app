package ai.shieldtv.app.core.model.media

data class EpisodeSummary(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String? = null,
    val overview: String? = null,
    val thumbnailUrl: String? = null,
    val airDate: String? = null
)
