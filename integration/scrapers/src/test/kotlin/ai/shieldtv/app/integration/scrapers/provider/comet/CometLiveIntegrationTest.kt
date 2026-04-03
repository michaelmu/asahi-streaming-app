package ai.shieldtv.app.integration.scrapers.provider.comet

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Test

class CometLiveIntegrationTest {
    @Test
    fun live_movie_lookup_returns_results_when_rd_token_is_available() = runBlocking {
        val rdToken = System.getenv("ASAHI_RD_TOKEN").orEmpty()
        assumeTrue("ASAHI_RD_TOKEN must be set for live Comet integration test", rdToken.isNotBlank())

        val provider = CometSourceProvider(
            tokenProvider = ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenProvider { rdToken }
        )

        val results = provider.search(
            SourceSearchRequest(
                mediaRef = MediaRef(
                    mediaType = MediaType.MOVIE,
                    ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
                    title = "The Matrix",
                    year = 1999
                )
            )
        )

        assertTrue("Expected live Comet to return at least one result", results.isNotEmpty())
        assertTrue(results.all { it.providerId == "comet" })
        assertTrue(results.all { it.infoHash != null && it.url.startsWith("magnet:") })
    }
}
