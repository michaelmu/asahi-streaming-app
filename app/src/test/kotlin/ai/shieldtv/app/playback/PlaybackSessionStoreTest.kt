package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class PlaybackSessionStoreTest {
    @Test
    fun save_and_load_round_trips_json_format() {
        val tempDir = createTempDir(prefix = "playback-store-test")
        val file = File(tempDir, "session.txt")
        val store = PlaybackSessionStoreBase(file)

        store.saveActiveResume(
            playbackItem(),
            PlaybackState(
                isBuffering = false,
                isPlaying = false,
                positionMs = 1200,
                durationMs = 2400
            ),
            seasonNumber = 1,
            episodeNumber = 2
        )

        val loaded = store.loadActiveResume()
        assertNotNull(loaded)
        assertEquals("The Matrix", loaded?.mediaTitle)
        assertEquals(50, loaded?.progressPercent)
        assertEquals(1, loaded?.seasonNumber)
        assertEquals(2, loaded?.episodeNumber)
    }

    @Test
    fun load_supports_legacy_line_format() {
        val tempDir = createTempDir(prefix = "playback-store-legacy")
        val file = File(tempDir, "session.txt")
        file.writeText(
            listOf(
                "The Matrix",
                "S01E02",
                "https://example.com/poster.jpg",
                "The Matrix",
                "1000",
                "2000",
                "50",
                "https://example.com/video.mkv",
                "1",
                "2",
                "123456789"
            ).joinToString("\n")
        )

        val loaded = PlaybackSessionStoreBase(file).loadActiveResume()
        assertNotNull(loaded)
        assertEquals("The Matrix", loaded?.mediaTitle)
        assertEquals(50, loaded?.progressPercent)
    }

    @Test
    fun load_returns_null_for_invalid_payload() {
        val tempDir = createTempDir(prefix = "playback-store-invalid")
        val file = File(tempDir, "session.txt")
        file.writeText("{bad json")

        val loaded = PlaybackSessionStoreBase(file).loadActiveResume()
        assertNull(loaded)
    }

    private fun playbackItem(): PlaybackItem {
        val mediaRef = MediaRef(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
            title = "The Matrix",
            year = 1999
        )
        val source = SourceResult(
            id = "test-source",
            mediaRef = mediaRef,
            providerId = "test",
            providerDisplayName = "Test",
            providerKind = ProviderKind.SCRAPER,
            debridService = DebridService.NONE,
            sourceSite = "Test",
            url = "https://example.com/video.mkv",
            displayName = "The Matrix",
            quality = ai.shieldtv.app.core.model.source.Quality.FHD_1080P,
            cacheStatus = CacheStatus.DIRECT
        )
        return PlaybackItem(
            mediaRef = mediaRef,
            title = mediaRef.title,
            subtitle = "S01E02",
            artworkUrl = "https://example.com/poster.jpg",
            stream = ResolvedStream(
                url = source.url,
                source = source
            )
        )
    }
}
