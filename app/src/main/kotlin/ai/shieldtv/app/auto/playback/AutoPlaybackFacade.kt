package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.auto.model.AutoPlaybackResult
import ai.shieldtv.app.core.model.media.MediaRef

interface AutoPlaybackFacade {
    suspend fun playMovie(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun resume(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playShowDefault(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playEpisode(mediaRef: MediaRef, seasonNumber: Int, episodeNumber: Int): AutoPlaybackResult
}
