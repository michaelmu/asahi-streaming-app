package ai.shieldtv.app

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.navigation.AppDestination

data class AppState(
    val destination: AppDestination = AppDestination.SEARCH,
    val selectedMedia: MediaRef? = null,
    val selectedSource: SourceResult? = null
)
