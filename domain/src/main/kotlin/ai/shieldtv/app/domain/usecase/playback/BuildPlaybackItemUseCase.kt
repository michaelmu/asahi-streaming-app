package ai.shieldtv.app.domain.usecase.playback

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.source.ResolvedStream

class BuildPlaybackItemUseCase {
    operator fun invoke(resolvedStream: ResolvedStream): PlaybackItem {
        return PlaybackItem(
            mediaRef = resolvedStream.source.mediaRef,
            title = resolvedStream.source.displayName,
            subtitle = resolvedStream.source.providerDisplayName,
            artworkUrl = null,
            stream = resolvedStream
        )
    }
}
