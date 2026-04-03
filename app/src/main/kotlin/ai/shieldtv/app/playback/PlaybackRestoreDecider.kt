package ai.shieldtv.app.playback

import ai.shieldtv.app.AppState
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.navigation.AppDestination

enum class RestoreTarget {
    HOME,
    RESULTS,
    DETAILS,
    EPISODES,
    SOURCES,
    KEEP_PLAYER
}

object PlaybackRestoreDecider {
    fun decide(state: AppState): RestoreTarget {
        if (state.destination != AppDestination.PLAYER || state.selectedSource != null) {
            return RestoreTarget.KEEP_PLAYER
        }

        return when {
            state.selectedSources.isNotEmpty() -> RestoreTarget.SOURCES
            state.selectedDetails != null && state.selectedDetails.mediaRef.mediaType == MediaType.SHOW -> RestoreTarget.EPISODES
            state.selectedDetails != null -> RestoreTarget.DETAILS
            state.searchResults.isNotEmpty() -> RestoreTarget.RESULTS
            else -> RestoreTarget.HOME
        }
    }
}
