package ai.shieldtv.app.browse

import ai.shieldtv.app.AppCoordinator
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.TitleDetails

sealed interface SourceNavigationDecision {
    data object RequiresAuth : SourceNavigationDecision

    data class LoadSources(
        val mediaRef: MediaRef,
        val seasonNumber: Int?,
        val episodeNumber: Int?
    ) : SourceNavigationDecision
}

class DetailsNavigationCoordinator {
    fun openEpisodes(
        coordinator: AppCoordinator,
        details: TitleDetails,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        coordinator.showEpisodes(
            details = details,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    fun selectEpisode(
        coordinator: AppCoordinator,
        details: TitleDetails,
        seasonNumber: Int,
        episodeNumber: Int
    ) {
        coordinator.showEpisodes(
            details = details,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    fun requestMovieSources(
        authLinked: Boolean,
        mediaRef: MediaRef
    ): SourceNavigationDecision {
        if (!authLinked) return SourceNavigationDecision.RequiresAuth
        return SourceNavigationDecision.LoadSources(
            mediaRef = mediaRef,
            seasonNumber = null,
            episodeNumber = null
        )
    }

    fun requestEpisodeSources(
        authLinked: Boolean,
        details: TitleDetails,
        seasonNumber: Int,
        episodeNumber: Int,
        autoSelectEpisode: Boolean,
        coordinator: AppCoordinator
    ): SourceNavigationDecision {
        if (!authLinked) return SourceNavigationDecision.RequiresAuth
        if (autoSelectEpisode) {
            coordinator.showEpisodes(details, seasonNumber, episodeNumber)
        }
        return SourceNavigationDecision.LoadSources(
            mediaRef = details.mediaRef,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }
}
