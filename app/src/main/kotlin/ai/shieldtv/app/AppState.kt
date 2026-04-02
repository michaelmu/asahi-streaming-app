package ai.shieldtv.app

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.navigation.AppDestination

enum class SearchMode(val mediaType: MediaType, val label: String) {
    MOVIES(MediaType.MOVIE, "Movies"),
    SHOWS(MediaType.SHOW, "TV Shows")
}

data class AppState(
    val destination: AppDestination = AppDestination.HOME,
    val searchMode: SearchMode = SearchMode.MOVIES,
    val query: String = "Dune",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedMedia: MediaRef? = null,
    val selectedDetails: TitleDetails? = null,
    val selectedSeasonNumber: Int? = null,
    val selectedEpisodeNumber: Int? = null,
    val selectedSource: SourceResult? = null,
    val selectedSources: List<SourceResult> = emptyList()
)

fun AppState.toBundleMap(): Map<String, String> = buildMap {
    put("destination", destination.name)
    put("searchMode", searchMode.name)
    put("query", query)
    selectedSeasonNumber?.let { put("selectedSeasonNumber", it.toString()) }
    selectedEpisodeNumber?.let { put("selectedEpisodeNumber", it.toString()) }
}

fun appStateFromBundleMap(values: Map<String, String>): AppState {
    return AppState(
        destination = values["destination"]?.let { AppDestination.valueOf(it) } ?: AppDestination.HOME,
        searchMode = values["searchMode"]?.let { SearchMode.valueOf(it) } ?: SearchMode.MOVIES,
        query = values["query"] ?: "Dune",
        selectedSeasonNumber = values["selectedSeasonNumber"]?.toIntOrNull(),
        selectedEpisodeNumber = values["selectedEpisodeNumber"]?.toIntOrNull()
    )
}
