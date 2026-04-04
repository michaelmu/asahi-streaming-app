package ai.shieldtv.app.domain.source

import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.SourceResult

class SourceEligibilityPolicy {
    fun filterForAuth(
        sources: List<SourceResult>,
        authLinked: Boolean
    ): List<SourceResult> {
        return if (authLinked) {
            sources
        } else {
            sources.filter { it.debridService == DebridService.NONE }
        }
    }
}
