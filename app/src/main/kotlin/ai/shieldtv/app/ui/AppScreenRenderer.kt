package ai.shieldtv.app.ui

import ai.shieldtv.app.AppState
import ai.shieldtv.app.navigation.AppDestination

class AppScreenRenderer {
    fun render(state: AppState): String {
        return when (state.destination) {
            AppDestination.HOME -> "Home Screen"
            AppDestination.SEARCH -> "Search Screen"
            AppDestination.RESULTS -> "Results Screen"
            AppDestination.DETAILS -> "Details Screen"
            AppDestination.EPISODES -> "Episode Picker Screen"
            AppDestination.SOURCES -> "Sources Screen"
            AppDestination.PLAYER -> "Player Screen"
            AppDestination.SETTINGS -> "Settings Screen"
        }
    }
}
