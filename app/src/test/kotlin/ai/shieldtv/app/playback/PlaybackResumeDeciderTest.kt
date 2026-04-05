package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumeDeciderTest {
    @Test
    fun returns_zero_when_record_missing() {
        assertEquals(0L, PlaybackResumeDecider.resumePositionFor(null, null, movieSource(), null, null))
    }

    @Test
    fun returns_zero_when_progress_too_low() {
        val record = record(progressPercent = 2, positionMs = 12000)
        assertEquals(0L, PlaybackResumeDecider.resumePositionFor(record, null, movieSource(), null, null))
    }

    @Test
    fun returns_zero_when_progress_too_high() {
        val record = record(progressPercent = 95, positionMs = 12000)
        assertEquals(0L, PlaybackResumeDecider.resumePositionFor(record, null, movieSource(), null, null))
    }

    @Test
    fun returns_zero_when_title_does_not_match() {
        val record = record(mediaTitle = "Something Else", progressPercent = 40, positionMs = 12000)
        assertEquals(0L, PlaybackResumeDecider.resumePositionFor(record, null, movieSource(), null, null))
    }

    @Test
    fun returns_position_when_movie_matches_and_progress_valid() {
        val record = record(progressPercent = 40, positionMs = 12000)
        assertEquals(12000L, PlaybackResumeDecider.resumePositionFor(record, null, movieSource(), null, null))
    }

    @Test
    fun returns_zero_when_show_episode_does_not_match() {
        val record = record(mediaTitle = "Severance", progressPercent = 40, positionMs = 33000, seasonNumber = 1, episodeNumber = 2)
        assertEquals(0L, PlaybackResumeDecider.resumePositionFor(record, null, showSource(season = 1, episode = 3), 1, 3))
    }

    @Test
    fun returns_position_when_show_episode_matches() {
        val record = record(mediaTitle = "Severance", progressPercent = 40, positionMs = 33000, seasonNumber = 1, episodeNumber = 2)
        assertEquals(33000L, PlaybackResumeDecider.resumePositionFor(record, null, showSource(season = 1, episode = 2), 1, 2))
    }

    private fun movieSource(): SourceResult = SourceResult(
        id = "movie-source",
        mediaRef = MediaRef(MediaType.MOVIE, MediaIds(null, null, null), "Dune: Part Two", year = 2024),
        providerId = "test-provider",
        providerDisplayName = "Test",
        providerKind = ProviderKind.SCRAPER,
        debridService = DebridService.REAL_DEBRID,
        sourceSite = "TestSite",
        url = "https://example.com/movie",
        displayName = "Dune Source",
        quality = Quality.FHD_1080P,
        cacheStatus = CacheStatus.CACHED
    )

    private fun showSource(season: Int, episode: Int): SourceResult = SourceResult(
        id = "show-source",
        mediaRef = MediaRef(MediaType.SHOW, MediaIds(null, null, null), "Severance", year = 2022),
        providerId = "test-provider",
        providerDisplayName = "Test",
        providerKind = ProviderKind.SCRAPER,
        debridService = DebridService.REAL_DEBRID,
        sourceSite = "TestSite",
        url = "https://example.com/show",
        displayName = "Severance Source",
        quality = Quality.FHD_1080P,
        cacheStatus = CacheStatus.CACHED,
        seasonNumber = season,
        episodeNumber = episode
    )

    private fun record(
        mediaTitle: String = "Dune: Part Two",
        progressPercent: Int,
        positionMs: Long,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) = PlaybackSessionRecord(
        mediaTitle = mediaTitle,
        subtitle = "Test",
        queryHint = mediaTitle,
        positionMs = positionMs,
        durationMs = 100000,
        progressPercent = progressPercent,
        playbackUrl = "https://example.com/video.mp4",
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber
    )
}
