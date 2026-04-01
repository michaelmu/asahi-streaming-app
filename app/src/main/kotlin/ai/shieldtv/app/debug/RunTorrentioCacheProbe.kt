package ai.shieldtv.app.debug

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApiFactory
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.debrid.realdebrid.repository.RealDebridCacheRepository
import ai.shieldtv.app.integration.scrapers.normalize.DefaultSourceNormalizer
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioSourceProvider
import ai.shieldtv.app.integration.scrapers.ranking.DefaultSourceRanker
import ai.shieldtv.app.integration.scrapers.ranking.RealDebridSourceCacheMarker
import ai.shieldtv.app.integration.scrapers.repository.SourceRepositoryImpl
import kotlinx.coroutines.runBlocking

object RunTorrentioCacheProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val request = SourceSearchRequest(
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(
                    tmdbId = "438631",
                    imdbId = "tt1160419",
                    tvdbId = null
                ),
                title = "Dune",
                year = 2021
            )
        )

        val tokenStore = RealDebridTokenStore()
        val realDebridApi = RealDebridApiFactory.create(tokenStore)
        val cacheRepository = RealDebridCacheRepository(realDebridApi)
        val sourceRepository = SourceRepositoryImpl(
            providerRegistry = ProviderRegistry(listOf(TorrentioSourceProvider())),
            sourceNormalizer = DefaultSourceNormalizer(),
            sourceRanker = DefaultSourceRanker(),
            sourceCacheMarker = RealDebridSourceCacheMarker(cacheRepository)
        )

        val sources = sourceRepository.findSources(request)
        val cachedCount = sources.count { it.cacheStatus.name == "CACHED" }
        val withHash = sources.count { !it.infoHash.isNullOrBlank() }

        println("Torrentio Cache Probe")
        println("Total sources: ${sources.size}")
        println("Sources with hash: $withHash")
        println("Cached sources: $cachedCount")
        println("Instant availability request: ${RealDebridDebugState.lastInstantAvailabilityRequest.ifBlank { "none" }}")
        println("Instant availability response: ${RealDebridDebugState.lastInstantAvailabilityResponse.ifBlank { "none" }}")
        println("Instant availability error: ${RealDebridDebugState.lastInstantAvailabilityError.ifBlank { "none" }}")
        println("Cache marker hash count: ${RealDebridDebugState.lastCacheMarkerHashCount.ifBlank { "none" }}")
        println("Cache marker cached count: ${RealDebridDebugState.lastCacheMarkerCachedCount.ifBlank { "none" }}")
        sources.take(20).forEachIndexed { index, source ->
            println(
                "${index + 1}. ${source.displayName} cache=${source.cacheStatus} hash=${source.infoHash ?: "none"}"
            )
        }
    }
}
