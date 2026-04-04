package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceDeduper

class DefaultSourceDeduper : SourceDeduper {
    override fun dedupe(sources: List<SourceResult>): List<SourceResult> {
        val byHash = linkedMapOf<String, SourceResult>()
        val withoutHash = mutableListOf<SourceResult>()

        sources.forEach { source ->
            val hash = source.infoHash?.lowercase()?.takeIf { it.isNotBlank() }
            if (hash == null) {
                withoutHash += source
            } else {
                val existing = byHash[hash]
                byHash[hash] = if (existing == null) source else merge(existing, source)
            }
        }

        return byHash.values + withoutHash
    }

    private fun merge(a: SourceResult, b: SourceResult): SourceResult {
        val preferred = listOf(a, b).maxWithOrNull(compareBy<SourceResult>({ cacheRank(it.cacheStatus) }, { it.score ?: 0.0 }, { it.sizeBytes ?: 0L })) ?: a
        val allProviderIds = a.providerIds + b.providerIds
        val allProviderNames = a.providerDisplayNames + b.providerDisplayNames
        val mergedMetadata = preferred.rawMetadata.toMutableMap().apply {
            put("provider_count", allProviderIds.size.toString())
            put("provider_names", allProviderNames.joinToString(", "))
        }
        return preferred.copy(
            providerIds = allProviderIds,
            providerDisplayNames = allProviderNames,
            providerDisplayName = if (allProviderNames.size == 1) preferred.providerDisplayName else allProviderNames.joinToString(" + "),
            rawMetadata = mergedMetadata
        )
    }

    private fun cacheRank(cacheStatus: CacheStatus): Int = when (cacheStatus) {
        CacheStatus.CACHED -> 4
        CacheStatus.DIRECT -> 3
        CacheStatus.UNCHECKED -> 2
        CacheStatus.UNCACHED -> 1
    }
}
