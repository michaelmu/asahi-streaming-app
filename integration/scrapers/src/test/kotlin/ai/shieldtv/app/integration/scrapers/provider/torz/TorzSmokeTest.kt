package ai.shieldtv.app.integration.scrapers.provider.torz

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorzSmokeTest {
    @Test
    fun query_builder_generates_expected_movie_path() {
        val request = SourceSearchRequest(
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
                title = "The Matrix",
                year = 1999
            )
        )

        val providerRequest = TorzQueryBuilder().build(request)

        assertEquals("/v0/torrents?sid=tt0133093", providerRequest.params.getValue("path"))
    }

    @Test
    fun parser_maps_json_item_to_raw_provider_source() {
        val raw = """
            {
              "data": {
                "items": [
                  {
                    "hash": "ABCDEF1234567890ABCDEF1234567890ABCDEF12",
                    "name": "The.Matrix.1999.1080p.BluRay.x265",
                    "size": 2254857830,
                    "seeders": 27
                  }
                ]
              }
            }
        """.trimIndent()

        val parsed = TorzParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("torz", first.providerId)
        assertEquals("The.Matrix.1999.1080p.BluRay.x265", first.title)
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12"))
        assertEquals(27, first.seeders)
        assertNotNull(first.sizeBytes)
        assertEquals("1080p", first.extra["quality_hint"])
        assertEquals("torz", first.extra["transport"])
    }
}
