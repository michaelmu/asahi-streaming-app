package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import android.content.Context
import java.io.File

typealias PlaybackSessionRecord = ActivePlaybackResumeRecord

class PlaybackSessionStore(
    context: Context
) : PlaybackSessionStoreBase(File(context.filesDir, "playback/session.txt"))

open class PlaybackSessionStoreBase(
    private val file: File
) {

    fun saveActiveResume(item: PlaybackItem, state: PlaybackState, seasonNumber: Int?, episodeNumber: Int?) {
        file.parentFile?.mkdirs()
        val progressPercent = if (state.durationMs > 0) {
            ((state.positionMs * 100) / state.durationMs).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val record = ActivePlaybackResumeRecord(
            mediaTitle = item.mediaRef.title,
            subtitle = item.subtitle.orEmpty(),
            artworkUrl = item.artworkUrl,
            queryHint = item.mediaRef.title,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            progressPercent = progressPercent,
            playbackUrl = item.stream.url,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        file.writeText(PlaybackSessionJson.encode(record))
    }

    fun loadActiveResume(): ActivePlaybackResumeRecord? {
        if (!file.exists()) return null
        val raw = file.readText()
        return PlaybackSessionJson.decode(raw) ?: loadLegacy(raw)
    }

    private fun loadLegacy(raw: String): ActivePlaybackResumeRecord? {
        val lines = raw.lines()
        if (lines.size < 8) return null
        return ActivePlaybackResumeRecord(
            mediaTitle = lines[0],
            subtitle = lines[1],
            artworkUrl = lines[2].ifBlank { null },
            queryHint = lines[3],
            positionMs = lines[4].toLongOrNull() ?: 0L,
            durationMs = lines[5].toLongOrNull() ?: 0L,
            progressPercent = lines[6].toIntOrNull() ?: 0,
            playbackUrl = lines[7],
            seasonNumber = lines.getOrNull(8)?.toIntOrNull(),
            episodeNumber = lines.getOrNull(9)?.toIntOrNull(),
            updatedAtEpochMs = lines.getOrNull(10)?.toLongOrNull() ?: 0L
        )
    }

    fun clearActiveResume() {
        if (file.exists()) file.delete()
    }

    // Backward-compatible names for existing callers.
    fun save(item: PlaybackItem, state: PlaybackState, seasonNumber: Int?, episodeNumber: Int?) =
        saveActiveResume(item, state, seasonNumber, episodeNumber)

    fun load(): ActivePlaybackResumeRecord? = loadActiveResume()

    fun clear() = clearActiveResume()
}
