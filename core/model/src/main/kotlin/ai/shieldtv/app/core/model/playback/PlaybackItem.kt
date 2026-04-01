package ai.shieldtv.app.core.model.playback

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.ResolvedStream

data class PlaybackItem(
    val mediaRef: MediaRef,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val stream: ResolvedStream
)
