package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSessionStoreTest {
    @Test
    fun load_returns_null_when_file_missing() {
        val store = PlaybackSessionStoreBase(tempFile())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun save_and_load_round_trip_works() {
        val store = PlaybackSessionStoreBase(tempFile())
        store.clear()

        store.save(
            item = playbackItem(),
            state = PlaybackState(
                isBuffering = false,
                isPlaying = true,
                positionMs = 12000,
                durationMs = 100000,
                playerStateLabel = "playing",
                errorMessage = null
            ),
            seasonNumber = 1,
            episodeNumber = 2
        )

        val loaded = store.load()
        assertNotNull(loaded)
        assertEquals("Severance", loaded?.mediaTitle)
        assertEquals(12000L, loaded?.positionMs)
        assertEquals(12, loaded?.progressPercent)
        assertEquals(1, loaded?.seasonNumber)
        assertEquals(2, loaded?.episodeNumber)
    }

    private fun tempFile(): File {
        val dir = createTempDir(prefix = "playback-store-test")
        return File(dir, "session.txt")
    }

    private fun playbackItem(): PlaybackItem = PlaybackItem(
        mediaRef = MediaRef(MediaType.SHOW, MediaIds(null, null, null), "Severance", year = 2022),
        title = "Severance",
        subtitle = "S01E02",
        artworkUrl = "https://example.com/art.jpg",
        stream = ResolvedStream(
            url = "https://example.com/video.mp4",
            headers = emptyMap(),
            mimeType = "video/mp4",
            source = SourceResult(
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
                seasonNumber = 1,
                episodeNumber = 2
            )
        )
    )
}
