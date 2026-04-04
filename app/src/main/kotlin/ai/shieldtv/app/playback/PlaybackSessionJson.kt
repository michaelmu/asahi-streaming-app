package ai.shieldtv.app.playback

import org.json.JSONObject

internal object PlaybackSessionJson {
    private const val VERSION = 1

    fun encode(record: PlaybackSessionRecord): String {
        return JSONObject()
            .put("version", VERSION)
            .put("mediaTitle", record.mediaTitle)
            .put("subtitle", record.subtitle)
            .put("artworkUrl", record.artworkUrl)
            .put("queryHint", record.queryHint)
            .put("positionMs", record.positionMs)
            .put("durationMs", record.durationMs)
            .put("progressPercent", record.progressPercent)
            .put("playbackUrl", record.playbackUrl)
            .put("seasonNumber", record.seasonNumber)
            .put("episodeNumber", record.episodeNumber)
            .put("updatedAtEpochMs", record.updatedAtEpochMs)
            .toString()
    }

    fun decode(raw: String): PlaybackSessionRecord? {
        return runCatching {
            val json = JSONObject(raw)
            PlaybackSessionRecord(
                mediaTitle = json.optString("mediaTitle"),
                subtitle = json.optString("subtitle"),
                artworkUrl = json.optString("artworkUrl").ifBlank { null },
                queryHint = json.optString("queryHint"),
                positionMs = json.optLong("positionMs"),
                durationMs = json.optLong("durationMs"),
                progressPercent = json.optInt("progressPercent"),
                playbackUrl = json.optString("playbackUrl"),
                seasonNumber = json.optInt("seasonNumber").takeIf { json.has("seasonNumber") },
                episodeNumber = json.optInt("episodeNumber").takeIf { json.has("episodeNumber") },
                updatedAtEpochMs = json.optLong("updatedAtEpochMs")
            ).takeIf {
                it.mediaTitle.isNotBlank() && it.playbackUrl.isNotBlank()
            }
        }.getOrNull()
    }
}
