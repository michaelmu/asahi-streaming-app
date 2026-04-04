package ai.shieldtv.app.integration.scrapers.provider.zilean

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZileanSmokeTest {
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

        val providerRequest = ZileanQueryBuilder().build(request)

        assertEquals("/dmm/filtered?ImdbId=tt0133093", providerRequest.params.getValue("path"))
    }

    @Test
    fun parser_maps_json_file_to_raw_provider_source() {
        val raw = """
            [
              {
                "info_hash": "ABCDEF1234567890ABCDEF1234567890ABCDEF12",
                "raw_title": "The.Matrix.1999.1080p.BluRay.x265",
                "size": 2254857830
              }
            ]
        """.trimIndent()

        val parsed = ZileanParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("zilean", first.providerId)
        assertEquals("The.Matrix.1999.1080p.BluRay.x265", first.title)
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12"))
        assertNotNull(first.sizeBytes)
        assertEquals("1080p", first.extra["quality_hint"])
        assertEquals("zilean", first.extra["transport"])
    }
}
