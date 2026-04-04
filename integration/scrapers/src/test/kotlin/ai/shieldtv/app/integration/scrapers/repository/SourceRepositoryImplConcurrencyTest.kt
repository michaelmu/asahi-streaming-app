package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.source.ranking.SourceRanker
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class SourceRepositoryImplConcurrencyTest {
    @Test
    fun findSources_runs_providers_in_parallel_and_reports_progress() = runBlocking {
        val startOrder = Collections.synchronizedList(mutableListOf<String>())
        val finishOrder = Collections.synchronizedList(mutableListOf<String>())
        val progressEvents = Collections.synchronizedList(mutableListOf<SourceFetchProgress>())

        val repository = SourceRepositoryImpl(
            providerRegistry = ProviderRegistry(
                listOf(
                    FakeProvider("slow-a", 150, startOrder, finishOrder),
                    FakeProvider("slow-b", 150, startOrder, finishOrder)
                )
            ),
            sourceNormalizer = FakeNormalizer(),
            sourceRanker = IdentityRanker(),
            providerTimeoutMs = 1000
        )

        val startedAt = System.currentTimeMillis()
        val results = repository.findSources(
            request = SourceSearchRequest(mediaRef()),
            onProgress = { progressEvents += it }
        )
        val elapsed = System.currentTimeMillis() - startedAt

        assertEquals(2, results.size)
        assertEquals(2, startOrder.size)
        assertEquals(2, finishOrder.size)
        assertTrue(progressEvents.count { it.state == SourceFetchProgress.State.STARTED } == 2)
        assertTrue(progressEvents.count { it.state == SourceFetchProgress.State.COMPLETED } == 2)
        assertTrue("Expected parallel execution to complete faster than serial 300ms+ overhead", elapsed < 280)
    }

    private fun mediaRef() = MediaRef(
        mediaType = MediaType.MOVIE,
        ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
        title = "The Matrix",
        year = 1999
    )
}

private class FakeProvider(
    override val id: String,
    private val delayMs: Long,
    private val startOrder: MutableList<String>,
    private val finishOrder: MutableList<String>
) : SourceProvider {
    override val displayName: String = id
    override val kind: ProviderKind = ProviderKind.SCRAPER
    override val capabilities: ProviderCapabilities = ProviderCapabilities()

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        startOrder += id
        delay(delayMs)
        finishOrder += id
        return listOf(
            RawProviderSource(
                providerId = id,
                title = "$id-result",
                url = "https://example.com/$id",
                infoHash = id.padEnd(40, 'a'),
                sizeBytes = 1_000_000,
                seeders = 10,
                extra = mapOf("quality_hint" to "1080p")
            )
        )
    }
}

private class FakeNormalizer : SourceNormalizer {
    override fun normalize(
        request: SourceSearchRequest,
        provider: SourceProvider,
        raw: RawProviderSource
    ) = ai.shieldtv.app.core.model.source.SourceResult(
        id = raw.providerId,
        mediaRef = request.mediaRef,
        providerId = provider.id,
        providerDisplayName = provider.displayName,
        providerKind = provider.kind,
        debridService = DebridService.NONE,
        sourceSite = provider.displayName,
        url = raw.url,
        displayName = raw.title,
        quality = Quality.FHD_1080P,
        cacheStatus = CacheStatus.UNCHECKED,
        infoHash = raw.infoHash,
        sizeBytes = raw.sizeBytes
    )
}

private class IdentityRanker : SourceRanker {
    override fun rank(
        sources: List<ai.shieldtv.app.core.model.source.SourceResult>,
        filters: ai.shieldtv.app.core.model.source.SourceFilters
    ): List<ai.shieldtv.app.core.model.source.SourceResult> = sources
}
