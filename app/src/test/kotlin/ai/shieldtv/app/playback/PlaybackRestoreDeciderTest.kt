package ai.shieldtv.app.playback

import ai.shieldtv.app.AppState
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackRestoreDeciderTest {
    @Test
    fun keeps_player_when_selected_source_exists() {
        val state = AppState(destination = AppDestination.PLAYER, selectedSource = sourceResult())
        assertEquals(RestoreTarget.KEEP_PLAYER, PlaybackRestoreDecider.decide(state))
    }

    @Test
    fun falls_back_to_sources_when_selected_sources_exist() {
        val state = AppState(destination = AppDestination.PLAYER, selectedSources = listOf(sourceResult()))
        assertEquals(RestoreTarget.SOURCES, PlaybackRestoreDecider.decide(state))
    }

    @Test
    fun falls_back_to_episodes_for_show_details() {
        val state = AppState(
            destination = AppDestination.PLAYER,
            selectedDetails = showDetails()
        )
        assertEquals(RestoreTarget.EPISODES, PlaybackRestoreDecider.decide(state))
    }

    @Test
    fun falls_back_to_results_when_search_results_exist() {
        val state = AppState(
            destination = AppDestination.PLAYER,
            searchMode = SearchMode.MOVIES,
            searchResults = listOf(searchResult())
        )
        assertEquals(RestoreTarget.RESULTS, PlaybackRestoreDecider.decide(state))
    }

    @Test
    fun falls_back_to_home_when_no_other_context_exists() {
        val state = AppState(destination = AppDestination.PLAYER)
        assertEquals(RestoreTarget.HOME, PlaybackRestoreDecider.decide(state))
    }

    private fun mediaRef(type: MediaType = MediaType.MOVIE) = MediaRef(type, MediaIds(null, null, null), if (type == MediaType.SHOW) "Severance" else "Dune", year = 2024)

    private fun searchResult() = SearchResult(mediaRef = mediaRef(), subtitle = "Test")

    private fun showDetails() = TitleDetails(mediaRef = mediaRef(MediaType.SHOW))

    private fun sourceResult() = ai.shieldtv.app.core.model.source.SourceResult(
        id = "source",
        mediaRef = mediaRef(),
        providerId = "provider",
        providerDisplayName = "Provider",
        providerKind = ai.shieldtv.app.core.model.source.ProviderKind.SCRAPER,
        debridService = ai.shieldtv.app.core.model.source.DebridService.REAL_DEBRID,
        sourceSite = "Site",
        url = "https://example.com/source",
        displayName = "Source",
        quality = ai.shieldtv.app.core.model.source.Quality.FHD_1080P,
        cacheStatus = ai.shieldtv.app.core.model.source.CacheStatus.CACHED
    )
}
