package ai.shieldtv.app.playback

import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import android.content.Context
import java.io.File

data class PlaybackSessionRecord(
    val mediaTitle: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val queryHint: String,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Int,
    val playbackUrl: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

class PlaybackSessionStore(
    context: Context
) {
    private val file = File(context.filesDir, "playback/session.txt")

    fun save(item: PlaybackItem, state: PlaybackState, seasonNumber: Int?, episodeNumber: Int?) {
        file.parentFile?.mkdirs()
        val progressPercent = if (state.durationMs > 0) {
            ((state.positionMs * 100) / state.durationMs).toInt().coerceIn(0, 100)
        } else {
            0
        }
        file.writeText(
            listOf(
                item.mediaRef.title,
                item.subtitle.orEmpty(),
                item.artworkUrl.orEmpty(),
                item.mediaRef.title,
                state.positionMs.toString(),
                state.durationMs.toString(),
                progressPercent.toString(),
                item.stream.url,
                seasonNumber?.toString().orEmpty(),
                episodeNumber?.toString().orEmpty(),
                System.currentTimeMillis().toString()
            ).joinToString("\n")
        )
    }

    fun load(): PlaybackSessionRecord? {
        if (!file.exists()) return null
        val lines = file.readLines()
        if (lines.size < 8) return null
        return PlaybackSessionRecord(
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

    fun clear() {
        if (file.exists()) file.delete()
    }
}
