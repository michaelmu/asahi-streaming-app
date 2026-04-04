package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSourceRankerTest {
    private val ranker = DefaultSourceRanker()

    @Test
    fun rank_prefers_cached_sources_over_uncached() {
        val cached = source(id = "cached", cacheStatus = CacheStatus.CACHED)
        val uncached = source(id = "uncached", cacheStatus = CacheStatus.UNCACHED)

        val ranked = ranker.rank(listOf(uncached, cached), SourceFilters())

        assertEquals("cached", ranked.first().id)
    }

    @Test
    fun rank_prefers_reasonable_1080p_file_over_tiny_one() {
        val tiny = source(id = "tiny", sizeBytes = 500L * MB)
        val healthy = source(id = "healthy", sizeBytes = 4L * GB)

        val ranked = ranker.rank(listOf(tiny, healthy), SourceFilters())

        assertEquals("healthy", ranked.first().id)
    }

    @Test
    fun rank_prefers_higher_priority_provider_when_other_signals_match() {
        val torrentio = source(id = "torrentio", providerId = "torrentio", providerDisplayName = "Torrentio")
        val bitsearch = source(id = "bitsearch", providerId = "bitsearch", providerDisplayName = "BitSearch")

        val ranked = ranker.rank(listOf(bitsearch, torrentio), SourceFilters())

        assertEquals("torrentio", ranked.first().id)
    }

    @Test
    fun scorer_exposes_rule_contributions() {
        val scorer = SourceScorer(DefaultSourceScoreRules.create())

        val score = scorer.score(
            source(
                id = "score-test",
                providerId = "torrentio",
                providerDisplayName = "Torrentio",
                cacheStatus = CacheStatus.CACHED,
                displayName = "Movie.Title.2024.1080p.WEB-DL",
                sizeBytes = 4L * GB,
                rawMetadata = mapOf("seeders" to "123")
            )
        )

        assertTrue(score.total > 0)
        assertTrue(score.contributions.any { it.rule == "cache" })
        assertTrue(score.contributions.any { it.rule == "quality" })
        assertTrue(score.contributions.any { it.rule == "provider" })
        assertTrue(score.contributions.any { it.rule == "seeders" })
    }

    @Test
    fun ranker_exposes_explanations_for_debugging() {
        val source = source(
            id = "explained",
            providerId = "torrentio",
            providerDisplayName = "Torrentio",
            cacheStatus = CacheStatus.CACHED,
            rawMetadata = mapOf("seeders" to "55")
        )

        val explanation = ranker.explain(source)

        assertNotNull(explanation)
        requireNotNull(explanation)
        assertTrue(explanation.totalScore > 0)
        assertTrue(explanation.contributions.any { it.rule == "cache" })
        assertTrue(explanation.contributions.any { it.rule == "provider" })
    }

    private fun source(
        id: String,
        providerId: String = "test",
        providerDisplayName: String = "Test",
        displayName: String = "Movie.Title.2024.1080p",
        cacheStatus: CacheStatus = CacheStatus.CACHED,
        sizeBytes: Long = 4L * GB,
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
            displayName = displayName,
            quality = Quality.FHD_1080P,
            cacheStatus = cacheStatus,
            sizeBytes = sizeBytes,
            rawMetadata = rawMetadata
        )
    }

    companion object {
        private const val MB = 1024L * 1024L
        private const val GB = 1024L * 1024L * 1024L
    }
}
