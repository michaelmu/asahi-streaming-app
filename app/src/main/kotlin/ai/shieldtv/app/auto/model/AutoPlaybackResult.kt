package ai.shieldtv.app.auto.model

import ai.shieldtv.app.core.model.source.SourceResult

sealed interface AutoPlaybackResult {
    data class Ready(val source: SourceResult) : AutoPlaybackResult
    data class Blocked(val userMessage: String) : AutoPlaybackResult
    data class Failed(val userMessage: String) : AutoPlaybackResult
}
