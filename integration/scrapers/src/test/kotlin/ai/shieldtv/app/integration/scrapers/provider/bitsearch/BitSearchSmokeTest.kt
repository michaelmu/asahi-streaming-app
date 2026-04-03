package ai.shieldtv.app.integration.scrapers.provider.bitsearch

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitSearchSmokeTest {
    @Test
    fun query_builder_generates_expected_search_url() {
        val request = SourceSearchRequest(
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
                title = "The Matrix",
                year = 1999
            )
        )

        val providerRequest = BitSearchQueryBuilder().build(request)
        val url = providerRequest.params.getValue("url")

        assertTrue(url.startsWith("https://bitsearch.to/search?q="))
        assertTrue(url.contains("The+Matrix"))
        assertTrue(url.contains("1999"))
        assertTrue(url.endsWith("&sort=size"))
    }

    @Test
    fun parser_maps_html_result_to_raw_provider_source() {
        val raw = """
            <html><body>
              <div>
                <div>
                  <a href="/download/torrent/ABCDEF1234567890ABCDEF1234567890ABCDEF12?title=The.Matrix.1999.1080p.BluRay.x265">Download</a>
                  <span>3.4 GB</span>
                  <span>1200 seeders</span>
                  <a href="magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12&amp;dn=%5BBitsearch.to%5D%20The.Matrix.1999.1080p.BluRay.x265&amp;tr=udp://tracker.example">Magnet</a>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val parsed = BitSearchParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("bitsearch", first.providerId)
        assertTrue(first.title.contains("Matrix"))
        assertTrue(first.title.contains("1999"))
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12"))
        assertTrue(first.seeders == null || first.seeders == 1200)
        assertNotNull(first.sizeBytes)
        assertEquals("1080p", first.extra["quality_hint"])
        assertEquals("bitsearch", first.extra["transport"])
    }
}
