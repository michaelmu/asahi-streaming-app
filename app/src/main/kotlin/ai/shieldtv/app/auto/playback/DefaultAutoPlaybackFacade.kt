package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.auto.model.AutoPlaybackResult
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.usecase.auth.GetRealDebridAuthStateUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.history.episodeWatchKey
import ai.shieldtv.app.playback.PlaybackMemoryStoreBase
import ai.shieldtv.app.settings.SourcePreferencesStore

class DefaultAutoPlaybackFacade(
    private val getRealDebridAuthStateUseCase: GetRealDebridAuthStateUseCase,
    private val getTitleDetailsUseCase: GetTitleDetailsUseCase,
    private val findSourcesUseCase: FindSourcesUseCase,
    private val sourceSelector: AutoSourceSelector,
    private val showProgressResolver: AutoShowProgressResolver,
    private val playbackMemoryStore: PlaybackMemoryStoreBase,
    private val watchedEpisodeKeys: (MediaIds) -> Set<String>,
    private val sourcePreferencesStore: SourcePreferencesStore,
    private val availableProviderIds: () -> Set<String>
) : AutoPlaybackFacade {

    override suspend fun playMovie(mediaRef: MediaRef): AutoPlaybackResult {
        if (mediaRef.mediaType != MediaType.MOVIE) {
            return AutoPlaybackResult.Failed("Movie playback request was invalid.")
        }
        return selectPlayableSource(
            mediaRef = mediaRef,
            seasonNumber = null,
            episodeNumber = null
        )
    }

    override suspend fun resume(mediaRef: MediaRef): AutoPlaybackResult {
        return when (mediaRef.mediaType) {
            MediaType.MOVIE -> playMovie(mediaRef)
            MediaType.SHOW -> playShowDefault(mediaRef)
            else -> AutoPlaybackResult.Failed("Resume is not supported for this item.")
        }
    }

    override suspend fun playShowDefault(mediaRef: MediaRef): AutoPlaybackResult {
        if (mediaRef.mediaType != MediaType.SHOW) {
            return AutoPlaybackResult.Failed("Show playback request was invalid.")
        }

        val titleDetails = runCatching { getTitleDetailsUseCase(mediaRef) }
            .getOrElse { return AutoPlaybackResult.Failed("Show details are unavailable right now.") }

        val resumeTarget = playbackMemoryStore.find(mediaRef, seasonNumber = null, episodeNumber = null)
            ?.let { record ->
                if (record.seasonNumber != null && record.episodeNumber != null && record.progressPercent in 3..92) {
                    EpisodeTarget(record.seasonNumber, record.episodeNumber)
                } else {
                    null
                }
            }
            ?: playbackMemoryStore.load()
                .firstOrNull { record ->
                    record.mediaType == MediaType.SHOW.name &&
                        sameIds(mediaRef.ids, record.tmdbId, record.imdbId, record.tvdbId) &&
                        record.seasonNumber != null &&
                        record.episodeNumber != null &&
                        record.progressPercent in 3..92
                }
                ?.let { EpisodeTarget(it.seasonNumber!!, it.episodeNumber!!) }

        val watchedEpisodes = watchedEpisodeKeys(mediaRef.ids)
            .mapNotNull { parseEpisodeTarget(mediaRef.ids, mediaRef.title, it) }
            .toSet()

        val target = showProgressResolver.resolveDefaultEpisode(
            titleDetails = titleDetails,
            resumeTarget = resumeTarget,
            watchedEpisodes = watchedEpisodes
        ) ?: return AutoPlaybackResult.Failed("No playable episode was found.")

        return playEpisode(mediaRef, target.seasonNumber, target.episodeNumber)
    }

    override suspend fun playEpisode(mediaRef: MediaRef, seasonNumber: Int, episodeNumber: Int): AutoPlaybackResult {
        if (mediaRef.mediaType != MediaType.SHOW) {
            return AutoPlaybackResult.Failed("Episode playback request was invalid.")
        }
        return selectPlayableSource(
            mediaRef = mediaRef,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    private suspend fun selectPlayableSource(
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): AutoPlaybackResult {
        val authState = runCatching { getRealDebridAuthStateUseCase() }
            .getOrElse { return AutoPlaybackResult.Failed("Playback setup is unavailable right now.") }

        val preferences = sourcePreferencesStore.load()
        val enabledProviders = preferences.providerSelection.effectiveEnabledProviders(availableProviderIds())

        val request = SourceSearchRequest(
            mediaRef = mediaRef,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            filters = SourceFilters(
                movieMaxSizeGb = preferences.movieMaxSizeGb,
                episodeMaxSizeGb = preferences.episodeMaxSizeGb
            )
        )

        val sources = runCatching {
            findSourcesUseCase(
                request = request,
                enabledProviderIds = enabledProviders
            )
        }.getOrElse {
            return AutoPlaybackResult.Failed("Sources could not be loaded.")
        }

        val bestSource = sourceSelector.selectBestForAuto(sources)
            ?: return if (!authState.isLinked && sources.any { it.debridService.name == "REAL_DEBRID" }) {
                AutoPlaybackResult.Blocked("Link Real-Debrid to play this item in Auto.")
            } else {
                AutoPlaybackResult.Failed("No Auto-safe source was available.")
            }

        return AutoPlaybackResult.Ready(bestSource)
    }

    private fun parseEpisodeTarget(showIds: MediaIds, showTitle: String, watchedKey: String): EpisodeTarget? {
        val prefix = episodeWatchKey(showIds, showTitle, seasonNumber = 1, episodeNumber = 1)
            .substringBeforeLast("|season:1|episode:1")
        if (!watchedKey.startsWith(prefix)) return null
        val parts = watchedKey.split('|')
        val seasonNumber = parts.firstOrNull { it.startsWith("season:") }?.substringAfter(':')?.toIntOrNull()
        val episodeNumber = parts.firstOrNull { it.startsWith("episode:") }?.substringAfter(':')?.toIntOrNull()
        return if (seasonNumber != null && episodeNumber != null) {
            EpisodeTarget(seasonNumber, episodeNumber)
        } else {
            null
        }
    }

    private fun sameIds(ids: MediaIds, tmdbId: String?, imdbId: String?, tvdbId: String?): Boolean {
        return ids.tmdbId == tmdbId && ids.imdbId == imdbId && ids.tvdbId == tvdbId
    }
}
