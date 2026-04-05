package ai.shieldtv.app.playback

import ai.shieldtv.app.ContinueWatchingItem

object ContinueWatchingHydrator {
    fun fromActiveResume(record: ActivePlaybackResumeRecord?): ContinueWatchingItem? {
        if (record == null) return null
        if (record.progressPercent !in 3..92) return null
        return ContinueWatchingItem(
            mediaTitle = record.mediaTitle,
            subtitle = record.subtitle.ifBlank {
                if (record.seasonNumber != null && record.episodeNumber != null) {
                    "S${record.seasonNumber.toString().padStart(2, '0')}E${record.episodeNumber.toString().padStart(2, '0')}"
                } else {
                    "In progress"
                }
            },
            artworkUrl = record.artworkUrl,
            queryHint = record.queryHint,
            progressPercent = record.progressPercent,
            mediaRef = null
        )
    }

    fun fromPersistedSession(record: PlaybackSessionRecord?): ContinueWatchingItem? = fromActiveResume(record)
}
