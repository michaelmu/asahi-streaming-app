package ai.shieldtv.app

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.navigation.AppDestination

data class ContinueWatchingItem(
    val mediaTitle: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val queryHint: String,
    val progressPercent: Int = 0,
    val mediaRef: MediaRef? = null
)

enum class SearchMode(val mediaType: MediaType, val label: String) {
    MOVIES(MediaType.MOVIE, "Movies"),
    SHOWS(MediaType.SHOW, "TV Shows")
}

data class AppState(
    val destination: AppDestination = AppDestination.HOME,
    val searchMode: SearchMode = SearchMode.MOVIES,
    val query: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedMedia: MediaRef? = null,
    val selectedDetails: TitleDetails? = null,
    val selectedSeasonNumber: Int? = null,
    val selectedEpisodeNumber: Int? = null,
    val selectedSource: SourceResult? = null,
    val selectedSources: List<SourceResult> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val favoritesBrowseMode: SearchMode? = null,
    val historyBrowseMode: SearchMode? = null
)

fun AppState.toBundleMap(): Map<String, String> = buildMap {
    put("destination", destination.name)
    put("searchMode", searchMode.name)
    put("query", query)
    put("recentQueries", recentQueries.joinToString("|"))
    favoritesBrowseMode?.let { put("favoritesBrowseMode", it.name) }
    historyBrowseMode?.let { put("historyBrowseMode", it.name) }
    put(
        "continueWatching",
        continueWatching.joinToString("||") {
            listOf(
                it.mediaTitle,
                it.subtitle,
                it.artworkUrl.orEmpty(),
                it.queryHint,
                it.progressPercent.toString(),
                it.mediaRef?.mediaType?.name.orEmpty(),
                it.mediaRef?.ids?.tmdbId.orEmpty(),
                it.mediaRef?.ids?.imdbId.orEmpty(),
                it.mediaRef?.ids?.tvdbId.orEmpty(),
                it.mediaRef?.year?.toString().orEmpty()
            ).joinToString("::")
        }
    )
    selectedSeasonNumber?.let { put("selectedSeasonNumber", it.toString()) }
    selectedEpisodeNumber?.let { put("selectedEpisodeNumber", it.toString()) }
}

fun appStateFromBundleMap(values: Map<String, String>): AppState {
    return AppState(
        destination = values["destination"]?.let { AppDestination.valueOf(it) } ?: AppDestination.HOME,
        searchMode = values["searchMode"]?.let { SearchMode.valueOf(it) } ?: SearchMode.MOVIES,
        query = values["query"] ?: "",
        favoritesBrowseMode = values["favoritesBrowseMode"]?.let { SearchMode.valueOf(it) },
        historyBrowseMode = values["historyBrowseMode"]?.let { SearchMode.valueOf(it) },
        selectedSeasonNumber = values["selectedSeasonNumber"]?.toIntOrNull(),
        selectedEpisodeNumber = values["selectedEpisodeNumber"]?.toIntOrNull(),
        recentQueries = values["recentQueries"]
            ?.split('|')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        continueWatching = values["continueWatching"]
            ?.split("||")
            ?.mapNotNull { item ->
                val parts = item.split("::")
                if (parts.size < 5) return@mapNotNull null
                ContinueWatchingItem(
                    mediaTitle = parts[0],
                    subtitle = parts[1],
                    artworkUrl = parts[2].ifBlank { null },
                    queryHint = parts[3],
                    progressPercent = parts[4].toIntOrNull() ?: 0,
                    mediaRef = parts.getOrNull(5)?.takeIf { it.isNotBlank() }?.let { mediaTypeName ->
                        MediaRef(
                            mediaType = MediaType.valueOf(mediaTypeName),
                            ids = ai.shieldtv.app.core.model.media.MediaIds(
                                tmdbId = parts.getOrNull(6).takeIf { !it.isNullOrBlank() },
                                imdbId = parts.getOrNull(7).takeIf { !it.isNullOrBlank() },
                                tvdbId = parts.getOrNull(8).takeIf { !it.isNullOrBlank() }
                            ),
                            title = parts[0],
                            year = parts.getOrNull(9)?.toIntOrNull()
                        )
                    }
                )
            }
            ?: emptyList()
    )
}
