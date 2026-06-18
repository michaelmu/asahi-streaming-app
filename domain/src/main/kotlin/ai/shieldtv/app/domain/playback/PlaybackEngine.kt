package ai.codexa.app.domain.playback

import ai.codexa.app.core.model.playback.PlaybackItem

interface PlaybackEngine {
    suspend fun prepare(item: PlaybackItem)
    fun play()
    fun pause()
    fun stop()
}
