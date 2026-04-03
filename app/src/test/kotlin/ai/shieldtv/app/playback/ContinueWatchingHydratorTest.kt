package ai.shieldtv.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinueWatchingHydratorTest {
    @Test
    fun returns_null_when_record_missing() {
        assertNull(ContinueWatchingHydrator.fromPersistedSession(null))
    }

    @Test
    fun returns_null_when_progress_too_low() {
        assertNull(ContinueWatchingHydrator.fromPersistedSession(record(progressPercent = 2)))
    }

    @Test
    fun returns_null_when_progress_too_high() {
        assertNull(ContinueWatchingHydrator.fromPersistedSession(record(progressPercent = 95)))
    }

    @Test
    fun creates_continue_watching_item_for_valid_progress() {
        val item = ContinueWatchingHydrator.fromPersistedSession(record(progressPercent = 42))
        assertEquals("Severance", item?.mediaTitle)
        assertEquals(42, item?.progressPercent)
        assertEquals("S01E02", item?.subtitle)
    }

    private fun record(progressPercent: Int) = PlaybackSessionRecord(
        mediaTitle = "Severance",
        subtitle = "S01E02",
        artworkUrl = "https://example.com/art.jpg",
        queryHint = "Severance",
        positionMs = 42000,
        durationMs = 100000,
        progressPercent = progressPercent,
        playbackUrl = "https://example.com/video.mp4",
        seasonNumber = 1,
        episodeNumber = 2
    )
}
