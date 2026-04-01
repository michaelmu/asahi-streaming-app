package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioConfig

object ProviderModeDecider {
    fun shapeSources(sources: List<SourceResult>): List<SourceResult> {
        if (!TorrentioConfig.isEnabled()) return sources

        val liveSources = sources.filter { isLiveProvider(it) }
        return if (liveSources.isNotEmpty()) liveSources else sources
    }

    private fun isLiveProvider(source: SourceResult): Boolean {
        return source.providerId == "torrentio" ||
            source.rawMetadata["transport"] == "torrentio"
    }
}
