package ai.shieldtv.app.integration.playback.media3.engine

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.domain.playback.PlaybackEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class Media3PlaybackEngine : PlaybackEngine {
    private val playbackState = MutableStateFlow(
        PlaybackState(
            isBuffering = false,
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            errorMessage = null
        )
    )

    override suspend fun prepare(item: PlaybackItem) {
        // TODO: wire Media3 player preparation.
    }

    override fun play() {
        // TODO: wire Media3 play.
    }

    override fun pause() {
        // TODO: wire Media3 pause.
    }

    override fun stop() {
        // TODO: wire Media3 stop.
    }

    override fun observeState(): Flow<PlaybackState> = playbackState
}
