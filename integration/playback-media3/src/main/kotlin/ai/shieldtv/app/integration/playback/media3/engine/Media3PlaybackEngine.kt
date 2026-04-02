package ai.shieldtv.app.integration.playback.media3.engine

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.domain.playback.PlaybackEngine
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class Media3PlaybackEngine : PlaybackEngine {
    enum class RenderMode {
        SURFACE_VIEW,
        TEXTURE_VIEW
    }

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
    private var renderMode: RenderMode = RenderMode.SURFACE_VIEW

    fun attach(context: Context): ExoPlayer {
        val existing = player
        if (existing != null) return existing

        return ExoPlayer.Builder(context.applicationContext).build().also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackStateValue: Int) {
                    updateState(
                        playerStateLabel = when (playbackStateValue) {
                            Player.STATE_IDLE -> "idle"
                            Player.STATE_BUFFERING -> "buffering"
                            Player.STATE_READY -> "ready"
                            Player.STATE_ENDED -> "ended"
                            else -> "unknown"
                        },
                        isBuffering = playbackStateValue == Player.STATE_BUFFERING,
                        isPlaying = exoPlayer.isPlaying,
                        errorMessage = null
                    )
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState(isPlaying = isPlaying)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    updateState(errorMessage = error.message ?: error.errorCodeName)
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val sizeLabel = if (videoSize.width > 0 && videoSize.height > 0) {
                        "${videoSize.width}x${videoSize.height}"
                    } else {
                        null
                    }
                    val format = exoPlayer.videoFormat?.sampleMimeType
                    updateState(videoSizeLabel = sizeLabel, videoFormat = format)
                }
            })
            player = exoPlayer
            currentItem?.let { item ->
                exoPlayer.setMediaItem(MediaItem.fromUri(item.stream.url))
                exoPlayer.prepare()
            }
        }
    }

    fun setRenderMode(mode: RenderMode) {
        renderMode = mode
    }

    fun getRenderMode(): RenderMode = renderMode

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
            durationMs = C.TIME_UNSET,
            playerStateLabel = "preparing",
            videoFormat = null,
            videoSizeLabel = null,
            errorMessage = null
        )
    }

    override fun play() {
        player?.play()
        updateState(
            isPlaying = true,
            playerStateLabel = playbackState.value.playerStateLabel,
            errorMessage = null
        )
    }

    override fun pause() {
        player?.pause()
        updateState(isPlaying = false)
    }

    override fun stop() {
        player?.stop()
        updateState(
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            playerStateLabel = "stopped"
        )
    }

    override fun release() {
        player?.release()
        player = null
    }

    override fun getCurrentItem(): PlaybackItem? = currentItem

    override fun getCurrentUrl(): String? = currentItem?.stream?.url

    override fun observeState(): Flow<PlaybackState> = playbackState

    private fun updateState(
        isBuffering: Boolean = playbackState.value.isBuffering,
        isPlaying: Boolean = playbackState.value.isPlaying,
        positionMs: Long = player?.currentPosition ?: playbackState.value.positionMs,
        durationMs: Long = player?.duration?.takeIf { it >= 0 } ?: playbackState.value.durationMs,
        playerStateLabel: String = playbackState.value.playerStateLabel,
        videoFormat: String? = playbackState.value.videoFormat,
        videoSizeLabel: String? = playbackState.value.videoSizeLabel,
        errorMessage: String? = playbackState.value.errorMessage
    ) {
        playbackState.value = PlaybackState(
            isBuffering = isBuffering,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            playerStateLabel = playerStateLabel,
            videoFormat = videoFormat,
            videoSizeLabel = videoSizeLabel,
            errorMessage = errorMessage
        )
    }
}
