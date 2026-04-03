package ai.shieldtv.app.integration.scrapers.provider.knaben

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnabenSmokeTest {
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

        val providerRequest = KnabenQueryBuilder().build(request)
        val url = providerRequest.params.getValue("url")

        assertTrue(url.startsWith("https://knaben.eu/search/index.php?cat=003000000&q="))
        assertTrue(url.contains("The+Matrix"))
        assertTrue(url.contains("1999"))
        assertTrue(url.endsWith("&search=fast"))
    }

    @Test
    fun parser_maps_html_row_to_raw_provider_source() {
        val raw = """
            <html><body>
              <table>
                <tr>
                  <td>title</td>
                  <td><a href="magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12&amp;dn=The.Matrix.1999.1080p.BluRay.x265&amp;tr=udp://tracker.example">magnet</a></td>
                  <td>3.4 GB</td>
                  <td>misc</td>
                  <td>152</td>
                </tr>
              </table>
            </body></html>
        """.trimIndent()

        val parsed = KnabenParser().parse(raw)

        assertEquals(1, parsed.size)
        val first = parsed.first()
        assertEquals("knaben", first.providerId)
        assertTrue(first.title.contains("Matrix"))
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", first.infoHash)
        assertTrue(first.url.startsWith("magnet:?xt=urn:btih:ABCDEF1234567890ABCDEF1234567890ABCDEF12"))
        assertEquals(152, first.seeders)
        assertNotNull(first.sizeBytes)
        assertEquals("1080p", first.extra["quality_hint"])
        assertEquals("knaben", first.extra["transport"])
    }
}
