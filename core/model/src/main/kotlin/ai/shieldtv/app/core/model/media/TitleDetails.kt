package ai.shieldtv.app.core.model.media

data class TitleDetails(
    val mediaRef: MediaRef,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val genres: List<String> = emptyList(),
    val runtimeMinutes: Int? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val episodesBySeason: Map<Int, List<EpisodeSummary>> = emptyMap()
)
