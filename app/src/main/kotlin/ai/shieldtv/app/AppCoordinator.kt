package ai.shieldtv.app

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.navigation.AppNavigator

class AppCoordinator(
    private val navigator: AppNavigator = AppNavigator()
) {
    private var state: AppState = AppState()

    fun currentState(): AppState = state

    fun onMediaSelected(mediaRef: MediaRef) {
        state = state.copy(
            destination = AppDestination.DETAILS,
            selectedMedia = mediaRef
        )
        navigator.goTo(AppDestination.DETAILS)
    }

    fun onOpenSources() {
        state = state.copy(destination = AppDestination.SOURCES)
        navigator.goTo(AppDestination.SOURCES)
    }

    fun onSourceSelected(sourceResult: SourceResult) {
        state = state.copy(
            destination = AppDestination.PLAYER,
            selectedSource = sourceResult
        )
        navigator.goTo(AppDestination.PLAYER)
    }

    fun onOpenSettings() {
        state = state.copy(destination = AppDestination.SETTINGS)
        navigator.goTo(AppDestination.SETTINGS)
    }
}
