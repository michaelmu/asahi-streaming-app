package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceResult

object PlaybackResumeDecider {
    fun resumePositionFor(
        record: ActivePlaybackResumeRecord?,
        remembered: PlaybackMemoryRecord?,
        source: SourceResult,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): Long {
        val rememberedPosition = remembered
            ?.takeIf { it.progressPercent in 3..92 }
            ?.positionMs
            ?: 0L
        if (rememberedPosition > 0L) return rememberedPosition

        if (record == null) return 0L
        val titleMatches = record.mediaTitle.equals(source.mediaRef.title, ignoreCase = true)
        val episodeMatches = if (source.mediaRef.mediaType == MediaType.SHOW) {
            record.seasonNumber == (seasonNumber ?: source.seasonNumber) &&
                record.episodeNumber == (episodeNumber ?: source.episodeNumber)
        } else {
            true
        }
        val progressOk = record.progressPercent in 3..92
        return if (titleMatches && episodeMatches && progressOk) record.positionMs else 0L
    }
}
