package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSourceDeduperTest {
    private val deduper = DefaultSourceDeduper()

    @Test
    fun dedupe_merges_provider_sets_and_preserves_origins() {
        val first = source(
            id = "one",
            providerId = "torrentio",
            providerDisplayName = "Torrentio",
            infoHash = "abc123",
            cacheStatus = CacheStatus.CACHED,
            rawMetadata = mapOf("seeders" to "50")
        )
        val second = source(
            id = "two",
            providerId = "comet",
            providerDisplayName = "Comet",
            infoHash = "abc123",
            cacheStatus = CacheStatus.DIRECT,
            rawMetadata = mapOf("seeders" to "30")
        )

        val deduped = deduper.dedupe(listOf(first, second))

        assertEquals(1, deduped.size)
        val merged = deduped.first()
        assertEquals(setOf("torrentio", "comet"), merged.providerIds)
        assertEquals(2, merged.origins.size)
        assertTrue(merged.origins.any { it.providerId == "torrentio" && it.seeders == 50 })
        assertTrue(merged.origins.any { it.providerId == "comet" && it.seeders == 30 })
    }

    @Test
    fun dedupe_keeps_non_hash_sources_separate() {
        val first = source(id = "one", infoHash = null)
        val second = source(id = "two", infoHash = null)

        val deduped = deduper.dedupe(listOf(first, second))

        assertEquals(2, deduped.size)
    }

    private fun source(
        id: String,
        providerId: String = "test",
        providerDisplayName: String = "Test",
        infoHash: String?,
        cacheStatus: CacheStatus = CacheStatus.CACHED,
        rawMetadata: Map<String, String> = emptyMap()
    ): SourceResult {
        return SourceResult(
            id = id,
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(tmdbId = "1", imdbId = "tt1", tvdbId = null),
                title = "Movie Title",
                year = 2024
            ),
            providerId = providerId,
            providerDisplayName = providerDisplayName,
            providerKind = ProviderKind.SCRAPER,
            debridService = DebridService.NONE,
            sourceSite = providerDisplayName,
            url = "https://example.com/$id",
            displayName = "Movie.Title.2024.1080p",
            quality = Quality.FHD_1080P,
            cacheStatus = cacheStatus,
            infoHash = infoHash,
            sizeBytes = 4L * 1024L * 1024L * 1024L,
            rawMetadata = rawMetadata
        )
    }
}
