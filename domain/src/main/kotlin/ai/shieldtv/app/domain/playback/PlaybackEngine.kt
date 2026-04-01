package ai.shieldtv.app.domain.playback

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import kotlinx.coroutines.flow.Flow

interface PlaybackEngine {
    suspend fun prepare(item: PlaybackItem)
    fun play()
    fun pause()
    fun stop()
    fun getCurrentItem(): PlaybackItem?
    fun observeState(): Flow<PlaybackState>
}
