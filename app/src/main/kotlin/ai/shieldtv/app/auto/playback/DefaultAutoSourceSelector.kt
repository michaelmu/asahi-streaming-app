package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult

class DefaultAutoSourceSelector : AutoSourceSelector {
    override fun selectBestForAuto(sources: List<SourceResult>): SourceResult? {
        return sources.firstOrNull { it.cacheStatus == CacheStatus.CACHED }
            ?: sources.firstOrNull { it.cacheStatus == CacheStatus.DIRECT }
    }
}
