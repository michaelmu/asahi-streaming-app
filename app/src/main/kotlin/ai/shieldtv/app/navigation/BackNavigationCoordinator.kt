package ai.shieldtv.app.navigation

import ai.shieldtv.app.AppCoordinator
import ai.shieldtv.app.AppState

sealed interface BackNavigationResult {
    data object Unhandled : BackNavigationResult

    data class Handled(
        val stopPlayback: Boolean = false,
        val renderRequired: Boolean = true
    ) : BackNavigationResult
}

class BackNavigationCoordinator {
    fun handleBack(
        coordinator: AppCoordinator,
        state: AppState
    ): BackNavigationResult {
        return when (state.destination) {
            AppDestination.HOME,
            AppDestination.SEARCH,
            AppDestination.RESULTS,
            AppDestination.SETTINGS -> BackNavigationResult.Unhandled

            AppDestination.DETAILS -> {
                coordinator.showResults(
                    query = state.query,
                    results = state.searchResults
                )
                BackNavigationResult.Handled()
            }

            AppDestination.EPISODES -> {
                val details = state.selectedDetails ?: return BackNavigationResult.Unhandled
                coordinator.showDetails(details.mediaRef, details)
                BackNavigationResult.Handled()
            }

            AppDestination.SOURCES -> {
                val details = state.selectedDetails
                when {
                    details != null && details.mediaRef.mediaType.name == "SHOW" -> {
                        coordinator.showEpisodes(
                            details,
                            state.selectedSeasonNumber,
                            state.selectedEpisodeNumber
                        )
                    }
                    details != null -> {
                        coordinator.showDetails(details.mediaRef, details)
                    }
                    else -> {
                        coordinator.showResults(state.query, state.searchResults)
                    }
                }
                BackNavigationResult.Handled()
            }

            AppDestination.PLAYER -> {
                val mediaRef = state.selectedMedia ?: return BackNavigationResult.Unhandled
                coordinator.showSources(
                    mediaRef = mediaRef,
                    details = state.selectedDetails,
                    seasonNumber = state.selectedSeasonNumber,
                    episodeNumber = state.selectedEpisodeNumber,
                    sources = state.selectedSources
                )
                BackNavigationResult.Handled(stopPlayback = true)
            }
        }
    }
}
