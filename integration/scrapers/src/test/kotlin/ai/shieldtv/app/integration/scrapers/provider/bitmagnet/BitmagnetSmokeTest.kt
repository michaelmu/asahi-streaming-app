package ai.shieldtv.app.integration.scrapers.provider.bitmagnet

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitmagnetSmokeTest {
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

        val providerRequest = BitmagnetQueryBuilder().build(request)

        assertEquals("/torznab/api?t=movie&imdbid=tt0133093", providerRequest.params.getValue("path"))
    }

    @Test
    fun parser_maps_torznab_item_to_raw_provider_source() {
        val raw = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:torznab="http://torznab.com/schemas/2015/feed">
              <channel>
                <item>
                  <title>The.Matrix.1999.1080p.BluRay.x265</title>
                  <torznab:attr name="infohash" value="ABCDEF1234567890ABCDEF1234567890ABCDEF12" />
                  <torznab:attr name="size" value="2254857830" />
                  <torznab:attr name="seeders" value="14" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val parsed = BitmagnetParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("bitmagnet", first.providerId)
        assertEquals("The.Matrix.1999.1080p.BluRay.x265", first.title)
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:abcdef1234567890abcdef1234567890abcdef12"))
        assertEquals(14, first.seeders)
        assertNotNull(first.sizeBytes)
        assertEquals("1080p", first.extra["quality_hint"])
        assertEquals("bitmagnet", first.extra["transport"])
    }
}
