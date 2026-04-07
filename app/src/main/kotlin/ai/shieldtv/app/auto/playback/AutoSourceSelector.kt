package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.core.model.source.SourceResult

interface AutoSourceSelector {
    fun selectBestForAuto(sources: List<SourceResult>): SourceResult?
}
