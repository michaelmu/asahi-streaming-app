package ai.shieldtv.app.domain.source.ranking

import ai.shieldtv.app.core.model.source.SourceResult

interface SourceDeduper {
    fun dedupe(sources: List<SourceResult>): List<SourceResult>
}
