package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.domain.repository.DebridCacheRepository
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState

class RealDebridCacheRepository(
    private val realDebridApi: RealDebridApi
) : DebridCacheRepository {
    override suspend fun getCachedHashes(infoHashes: List<String>): Set<String> {
        RealDebridDebugState.lastCacheMarkerHashCount = infoHashes.distinct().size.toString()
        if (infoHashes.isEmpty()) {
            RealDebridDebugState.lastCacheMarkerCachedCount = "0"
            return emptySet()
        }
        val response = realDebridApi.instantAvailability(infoHashes)
        val cached = RealDebridInstantAvailabilityParser.parseCachedHashes(response)
        RealDebridDebugState.lastCacheMarkerCachedCount = cached.size.toString()
        return cached
    }
}
