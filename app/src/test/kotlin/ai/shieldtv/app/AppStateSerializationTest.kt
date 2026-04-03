package ai.shieldtv.app

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStateSerializationTest {
    @Test
    fun bundle_round_trip_preserves_recent_queries_and_continue_watching() {
        val state = AppState(
            destination = AppDestination.SOURCES,
            searchMode = SearchMode.SHOWS,
            query = "Severance",
            selectedMedia = MediaRef(MediaType.SHOW, MediaIds(null, null, null), "Severance", year = 2022),
            selectedSeasonNumber = 1,
            selectedEpisodeNumber = 2,
            recentQueries = listOf("Severance", "Andor"),
            continueWatching = listOf(
                ContinueWatchingItem(
                    mediaTitle = "Severance",
                    subtitle = "S01E02",
                    artworkUrl = "https://example.com/art.jpg",
                    queryHint = "Severance",
                    progressPercent = 42
                )
            )
        )

        val restored = appStateFromBundleMap(state.toBundleMap())

        assertEquals(AppDestination.SOURCES, restored.destination)
        assertEquals(SearchMode.SHOWS, restored.searchMode)
        assertEquals("Severance", restored.query)
        assertEquals(1, restored.selectedSeasonNumber)
        assertEquals(2, restored.selectedEpisodeNumber)
        assertEquals(listOf("Severance", "Andor"), restored.recentQueries)
        assertEquals(1, restored.continueWatching.size)
        assertEquals("Severance", restored.continueWatching.first().mediaTitle)
        assertEquals(42, restored.continueWatching.first().progressPercent)
    }

    @Test
    fun restore_handles_missing_optional_fields_safely() {
        val restored = appStateFromBundleMap(
            mapOf(
                "destination" to "HOME",
                "searchMode" to "MOVIES",
                "query" to "Dune"
            )
        )

        assertEquals(AppDestination.HOME, restored.destination)
        assertEquals(SearchMode.MOVIES, restored.searchMode)
        assertEquals("Dune", restored.query)
        assertEquals(emptyList<String>(), restored.recentQueries)
        assertEquals(emptyList<ContinueWatchingItem>(), restored.continueWatching)
    }
}
