package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.repository.SourceRepository
import ai.shieldtv.app.domain.source.ranking.SourceCacheMarker
import ai.shieldtv.app.domain.source.ranking.SourceRanker
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.scrapers.provider.ProviderModeDecider
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry

class SourceRepositoryImpl(
    private val providerRegistry: ProviderRegistry,
    private val sourceNormalizer: SourceNormalizer,
    private val sourceRanker: SourceRanker,
    private val sourceCacheMarker: SourceCacheMarker? = null
) : SourceRepository {
    override suspend fun findSources(request: SourceSearchRequest): List<SourceResult> {
        RealDebridDebugState.lastSourceRepositorySeen = "yes"
        RealDebridDebugState.lastSourceRepositoryMarkerPresent = if (sourceCacheMarker == null) "no" else "yes"
        val providerSummaries = mutableListOf<String>()
        val rawResults = providerRegistry.providers.flatMap { provider ->
            val normalized = provider.search(request).map { raw ->
                sourceNormalizer.normalize(request, provider, raw)
            }
            providerSummaries += "${provider.id}:${normalized.size}"
            normalized
        }
        RealDebridDebugState.lastSourceProviderSummary = providerSummaries.joinToString(",")
        val cacheMarked = sourceCacheMarker?.markCached(rawResults) ?: rawResults
        val shaped = ProviderModeDecider.shapeSources(cacheMarked)
        val liveCount = shaped.count {
            it.providerId == "torrentio" || it.rawMetadata["transport"] == "torrentio"
        }
        val fallbackCount = shaped.count { it.rawMetadata["fallbackMode"] == "true" }
        RealDebridDebugState.lastSourceLiveCount = liveCount.toString()
        RealDebridDebugState.lastSourceFallbackCount = fallbackCount.toString()
        val torrentioPreview = RealDebridDebugState.lastTorrentioResponsePreview
            .replace("\n", " ")
            .replace("\r", " ")
            .take(160)
        println(
            buildString {
                append("AsahiSources ")
                append("title=")
                append(request.mediaRef.title)
                append(" type=")
                append(request.mediaRef.mediaType)
                request.seasonNumber?.let {
                    append(" season=")
                    append(it)
                }
                request.episodeNumber?.let {
                    append(" episode=")
                    append(it)
                }
                append(" providers=")
                append(RealDebridDebugState.lastSourceProviderSummary)
                append(" live=")
                append(liveCount)
                append(" fallback=")
                append(fallbackCount)
                append(" torrentioParsed=")
                append(RealDebridDebugState.lastTorrentioParsedCount)
                RealDebridDebugState.lastTorrentioUrl.takeIf { it.isNotBlank() }?.let {
                    append(" torrentioUrl=")
                    append(it)
                }
                torrentioPreview.takeIf { it.isNotBlank() }?.let {
                    append(" torrentioPreview=")
                    append(it)
                }
            }
        )
        return sourceRanker.rank(shaped, SourceFilters())
    }
}
