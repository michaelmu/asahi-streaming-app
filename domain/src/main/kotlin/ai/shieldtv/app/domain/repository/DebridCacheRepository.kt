package ai.shieldtv.app.domain.repository

interface DebridCacheRepository {
    suspend fun getCachedHashes(infoHashes: List<String>): Set<String>
}
