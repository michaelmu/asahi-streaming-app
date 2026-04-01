package ai.shieldtv.app.integration.playback.media3.engine

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.domain.playback.PlaybackEngine
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
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
    private var player: ExoPlayer? = null

    fun attach(context: Context): ExoPlayer {
        val existing = player
        if (existing != null) return existing

        return ExoPlayer.Builder(context.applicationContext).build().also { exoPlayer ->
            player = exoPlayer
            currentItem?.let { item ->
                exoPlayer.setMediaItem(MediaItem.fromUri(item.stream.url))
                exoPlayer.prepare()
            }
        }
    }

    override suspend fun prepare(item: PlaybackItem) {
        currentItem = item
        player?.apply {
            setMediaItem(MediaItem.fromUri(item.stream.url))
            prepare()
        }
        playbackState.value = PlaybackState(
            isBuffering = false,
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            errorMessage = null
        )
    }

    override fun play() {
        player?.play()
        playbackState.value = playbackState.value.copy(
            isPlaying = true,
            errorMessage = null
        )
    }

    override fun pause() {
        player?.pause()
        playbackState.value = playbackState.value.copy(isPlaying = false)
    }

    override fun stop() {
        player?.stop()
        playbackState.value = playbackState.value.copy(
            isPlaying = false,
            positionMs = 0,
            durationMs = 0
        )
    }

    override fun release() {
        player?.release()
        player = null
    }

    override fun getCurrentItem(): PlaybackItem? = currentItem

    override fun getCurrentUrl(): String? = currentItem?.stream?.url

    override fun observeState(): Flow<PlaybackState> = playbackState
}
