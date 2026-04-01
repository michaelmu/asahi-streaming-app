package ai.shieldtv.app.core.model.media

data class SearchResult(
    val mediaRef: MediaRef,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val badges: List<String> = emptyList()
)
