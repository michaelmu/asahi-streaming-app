package ai.shieldtv.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinueWatchingHydratorTest {
    @Test
    fun fromActiveResume_returns_item_for_resume_worthy_progress() {
        val item = ContinueWatchingHydrator.fromActiveResume(
            ActivePlaybackResumeRecord(
                mediaTitle = "The Matrix",
                subtitle = "",
                artworkUrl = "https://example.com/poster.jpg",
                queryHint = "The Matrix",
                positionMs = 1000,
                durationMs = 2000,
                progressPercent = 50,
                playbackUrl = "https://example.com/video.mkv",
                seasonNumber = 1,
                episodeNumber = 2
            )
        )

        requireNotNull(item)
        assertEquals("The Matrix", item.mediaTitle)
        assertEquals("S01E02", item.subtitle)
    }

    @Test
    fun fromActiveResume_returns_null_when_progress_not_resume_worthy() {
        val item = ContinueWatchingHydrator.fromActiveResume(
            ActivePlaybackResumeRecord(
                mediaTitle = "The Matrix",
                subtitle = "",
                artworkUrl = null,
                queryHint = "The Matrix",
                positionMs = 10,
                durationMs = 2000,
                progressPercent = 1,
                playbackUrl = "https://example.com/video.mkv"
            )
        )

        assertNull(item)
    }
}
