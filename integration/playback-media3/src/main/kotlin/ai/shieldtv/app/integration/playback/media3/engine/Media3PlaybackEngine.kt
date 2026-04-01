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

    private var currentItem: PlaybackItem? = null

    override suspend fun prepare(item: PlaybackItem) {
        currentItem = item
        playbackState.value = PlaybackState(
            isBuffering = false,
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            errorMessage = null
        )
    }

    override fun play() {
        playbackState.value = playbackState.value.copy(
            isPlaying = true,
            errorMessage = null
        )
    }

    override fun pause() {
        playbackState.value = playbackState.value.copy(isPlaying = false)
    }

    override fun stop() {
        playbackState.value = playbackState.value.copy(
            isPlaying = false,
            positionMs = 0,
            durationMs = 0
        )
    }

    override fun getCurrentItem(): PlaybackItem? = currentItem

    override fun observeState(): Flow<PlaybackState> = playbackState
}
