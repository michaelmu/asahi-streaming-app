package ai.shieldtv.app.integration.scrapers.provider.comet

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class CometSmokeTest {
    @Test
    fun query_builder_encodes_realdebrid_payload_for_movie_requests() {
        val builder = CometQueryBuilder { "rd-token-123" }
        val request = SourceSearchRequest(
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
                title = "The Matrix",
                year = 1999
            )
        )

        val providerRequest = builder.build(request)
        val path = providerRequest.params.getValue("path")

        assertTrue(path.endsWith("/stream/movie/tt0133093.json"))
        val encoded = path.removePrefix("/").substringBefore("/stream/movie")
        val decoded = String(Base64.getDecoder().decode(encoded))
        val json = JSONObject(decoded)

        assertEquals("realdebrid", json.getString("debridService"))
        assertEquals("rd-token-123", json.getString("debridApiKey"))
        assertTrue(json.getJSONArray("resultFormat").toString().contains("All"))
    }

    @Test
    fun parser_maps_comet_stream_to_raw_provider_source() {
        val raw = """
            {
              "streams": [
                {
                  "description": "The.Matrix.1999.1080p.BluRay.x265\n💾 2.4 GB\n👤 42",
                  "behaviorHints": {
                    "bingeGroup": "comet|abcdef1234567890abcdef1234567890abcdef12"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = CometParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("comet", first.providerId)
        assertEquals("The.Matrix.1999.1080p.BluRay.x265", first.title)
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:abcdef1234567890abcdef1234567890abcdef12"))
        assertEquals(42, first.seeders)
        assertNotNull(first.sizeBytes)
        assertEquals("realdebrid", first.extra["debrid"])
        assertEquals("cached", first.extra["cache_hint"])
        assertEquals("1080p", first.extra["quality_hint"])
    }
}
