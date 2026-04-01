package ai.shieldtv.app.ui

import ai.shieldtv.app.AppState
import ai.shieldtv.app.navigation.AppDestination

class AppScreenRenderer {
    fun render(state: AppState): String {
        return when (state.destination) {
            AppDestination.SEARCH -> "Search Screen"
            AppDestination.DETAILS -> "Details Screen"
            AppDestination.SOURCES -> "Sources Screen"
            AppDestination.PLAYER -> "Player Screen"
            AppDestination.SETTINGS -> "Settings Screen"
        }
    }
}
