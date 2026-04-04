package ai.shieldtv.app.integration.scrapers.provider.zilean

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ZileanLiveIntegrationTest {
    @Test
    fun live_movie_lookup_returns_results() = runBlocking {
        val provider = ZileanSourceProvider()
        val results = provider.search(
            SourceSearchRequest(
                mediaRef = MediaRef(
                    mediaType = MediaType.MOVIE,
                    ids = MediaIds(tmdbId = "157336", imdbId = "tt0816692", tvdbId = null),
                    title = "Interstellar",
                    year = 2014
                )
            )
        )

        assertTrue("Expected live Zilean to return at least one result", results.isNotEmpty())
        assertTrue(results.all { it.providerId == "zilean" })
        assertTrue(results.all { it.infoHash != null && it.url.startsWith("magnet:") })
    }
}
