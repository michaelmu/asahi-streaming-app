package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.TitleDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoShowProgressResolverTest {
    private val resolver = DefaultAutoShowProgressResolver()

    @Test
    fun resume_target_exists_chosen() {
        val details = showDetails(
            1 to listOf(1, 2, 3),
            2 to listOf(1, 2)
        )

        val resolved = resolver.resolveDefaultEpisode(
            titleDetails = details,
            resumeTarget = EpisodeTarget(seasonNumber = 2, episodeNumber = 1),
            watchedEpisodes = setOf(EpisodeTarget(1, 1), EpisodeTarget(1, 2), EpisodeTarget(1, 3))
        )

        assertEquals(EpisodeTarget(2, 1), resolved)
    }

    @Test
    fun no_resume_next_unwatched_exists_chosen() {
        val details = showDetails(
            1 to listOf(1, 2, 3),
            2 to listOf(1, 2)
        )

        val resolved = resolver.resolveDefaultEpisode(
            titleDetails = details,
            watchedEpisodes = setOf(EpisodeTarget(1, 1), EpisodeTarget(1, 2), EpisodeTarget(2, 1))
        )

        assertEquals(EpisodeTarget(1, 3), resolved)
    }

    @Test
    fun no_progress_latest_episode_exists_chosen() {
        val details = showDetails(
            1 to listOf(1, 2),
            2 to listOf(1, 2, 3)
        )
        val watched = setOf(
            EpisodeTarget(1, 1),
            EpisodeTarget(1, 2),
            EpisodeTarget(2, 1),
            EpisodeTarget(2, 2),
            EpisodeTarget(2, 3)
        )

        val resolved = resolver.resolveDefaultEpisode(
            titleDetails = details,
            watchedEpisodes = watched
        )

        assertEquals(EpisodeTarget(2, 3), resolved)
    }

    @Test
    fun only_minimal_metadata_exists_returns_s01e01() {
        val details = TitleDetails(
            mediaRef = showMediaRef(),
            seasonCount = 3,
            episodeCount = 24
        )

        val resolved = resolver.resolveDefaultEpisode(titleDetails = details)

        assertEquals(EpisodeTarget(1, 1), resolved)
    }

    @Test
    fun sparse_seasons_episodes_still_produce_deterministic_output() {
        val details = showDetails(
            3 to listOf(2, 5),
            1 to listOf(4),
            2 to listOf(3)
        )

        val resolved = resolver.resolveDefaultEpisode(
            titleDetails = details,
            watchedEpisodes = setOf(EpisodeTarget(1, 4), EpisodeTarget(2, 3), EpisodeTarget(3, 2))
        )

        assertEquals(EpisodeTarget(3, 5), resolved)
    }

    private fun showDetails(vararg seasons: Pair<Int, List<Int>>): TitleDetails {
        return TitleDetails(
            mediaRef = showMediaRef(),
            seasonCount = seasons.size,
            episodeCount = seasons.sumOf { it.second.size },
            episodesBySeason = seasons.associate { (seasonNumber, episodes) ->
                seasonNumber to episodes.map { episodeNumber ->
                    EpisodeSummary(
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        title = "S${seasonNumber}E${episodeNumber}"
                    )
                }
            }
        )
    }

    private fun showMediaRef(): MediaRef = MediaRef(
        mediaType = MediaType.SHOW,
        ids = MediaIds(tmdbId = "95396", imdbId = "tt11280740", tvdbId = null),
        title = "Severance",
        year = 2022
    )
}
