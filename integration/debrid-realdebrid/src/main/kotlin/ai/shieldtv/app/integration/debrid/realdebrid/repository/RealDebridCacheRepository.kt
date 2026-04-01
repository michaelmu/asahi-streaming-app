package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.domain.repository.DebridCacheRepository
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi

class RealDebridCacheRepository(
    private val realDebridApi: RealDebridApi
) : DebridCacheRepository {
    override suspend fun getCachedHashes(infoHashes: List<String>): Set<String> {
        if (infoHashes.isEmpty()) return emptySet()
        val response = realDebridApi.instantAvailability(infoHashes)
        return RealDebridInstantAvailabilityParser.parseCachedHashes(response)
    }
}
