package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.core.model.media.TitleDetails

interface AutoShowProgressResolver {
    fun resolveDefaultEpisode(
        titleDetails: TitleDetails,
        resumeTarget: EpisodeTarget? = null,
        watchedEpisodes: Set<EpisodeTarget> = emptySet()
    ): EpisodeTarget?
}

data class EpisodeTarget(
    val seasonNumber: Int,
    val episodeNumber: Int
)

class DefaultAutoShowProgressResolver : AutoShowProgressResolver {
    override fun resolveDefaultEpisode(
        titleDetails: TitleDetails,
        resumeTarget: EpisodeTarget?,
        watchedEpisodes: Set<EpisodeTarget>
    ): EpisodeTarget? {
        resumeTarget?.let { return it }

        val availableEpisodes = titleDetails.episodesBySeason
            .toSortedMap()
            .flatMap { (seasonNumber, episodes) ->
                episodes
                    .sortedBy { it.episodeNumber }
                    .map { EpisodeTarget(seasonNumber = seasonNumber, episodeNumber = it.episodeNumber) }
            }

        availableEpisodes.firstOrNull { it !in watchedEpisodes }?.let { return it }
        availableEpisodes.lastOrNull()?.let { return it }

        val fallbackSeasonNumber = titleDetails.seasonCount?.takeIf { it > 0 }?.let { 1 } ?: 1
        return EpisodeTarget(seasonNumber = fallbackSeasonNumber, episodeNumber = 1)
    }
}
