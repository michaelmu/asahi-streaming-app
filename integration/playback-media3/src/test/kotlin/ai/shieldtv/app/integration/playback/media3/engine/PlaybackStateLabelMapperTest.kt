package ai.shieldtv.app.integration.playback.media3.engine

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateLabelMapperTest {
    @Test
    fun maps_ready_and_playing_to_playing() {
        assertEquals("playing", PlaybackStateLabelMapper.fromPlaybackState(Player.STATE_READY, true))
    }

    @Test
    fun maps_ready_and_not_playing_to_paused() {
        assertEquals("paused", PlaybackStateLabelMapper.fromPlaybackState(Player.STATE_READY, false))
    }

    @Test
    fun maps_buffering_to_buffering() {
        assertEquals("buffering", PlaybackStateLabelMapper.fromPlaybackState(Player.STATE_BUFFERING, false))
    }

    @Test
    fun maps_ended_to_ended() {
        assertEquals("ended", PlaybackStateLabelMapper.fromPlaybackState(Player.STATE_ENDED, false))
    }

    @Test
    fun keeps_buffering_priority_on_is_playing_change() {
        assertEquals(
            "buffering",
            PlaybackStateLabelMapper.fromIsPlayingChange(false, true, Player.STATE_READY, "paused")
        )
    }

    @Test
    fun maps_ready_not_playing_to_paused_on_is_playing_change() {
        assertEquals(
            "paused",
            PlaybackStateLabelMapper.fromIsPlayingChange(false, false, Player.STATE_READY, "playing")
        )
    }
}
