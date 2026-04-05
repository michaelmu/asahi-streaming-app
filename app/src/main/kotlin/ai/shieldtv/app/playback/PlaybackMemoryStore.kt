package ai.shieldtv.app.playback

import android.content.Context
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.SourceResult
import java.io.File

data class PlaybackMemoryRecord(
    val mediaType: String,
    val title: String,
    val tmdbId: String? = null,
    val imdbId: String? = null,
    val tvdbId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val sourceId: String? = null,
    val sourceUrl: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progressPercent: Int = 0,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun contentKey(): String = listOf(
        mediaType,
        tmdbId.orEmpty(),
        imdbId.orEmpty(),
        tvdbId.orEmpty(),
        title.lowercase(),
        seasonNumber?.toString().orEmpty(),
        episodeNumber?.toString().orEmpty()
    ).joinToString("|")
}

class PlaybackMemoryStore(
    context: Context
) : PlaybackMemoryStoreBase(File(context.filesDir, "playback/memory.tsv"))

open class PlaybackMemoryStoreBase(
    private val file: File
) {
    fun load(): List<PlaybackMemoryRecord> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull(::decodeLine).sortedByDescending { it.updatedAtEpochMs }
    }

    fun find(mediaRef: MediaRef, seasonNumber: Int?, episodeNumber: Int?): PlaybackMemoryRecord? {
        val key = keyFor(mediaRef, seasonNumber, episodeNumber)
        return load().firstOrNull { it.contentKey() == key }
    }

    fun record(mediaRef: MediaRef, seasonNumber: Int?, episodeNumber: Int?, source: SourceResult, positionMs: Long, durationMs: Long, progressPercent: Int) {
        val item = PlaybackMemoryRecord(
            mediaType = mediaRef.mediaType.name,
            title = mediaRef.title,
            tmdbId = mediaRef.ids.tmdbId,
            imdbId = mediaRef.ids.imdbId,
            tvdbId = mediaRef.ids.tvdbId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            sourceId = source.id,
            sourceUrl = source.url,
            positionMs = positionMs,
            durationMs = durationMs,
            progressPercent = progressPercent.coerceIn(0, 100),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        val all = load().associateBy { it.contentKey() }.toMutableMap()
        all[item.contentKey()] = item
        save(all.values.toList())
    }

    private fun save(items: List<PlaybackMemoryRecord>) {
        file.parentFile?.mkdirs()
        file.writeText(items.sortedByDescending { it.updatedAtEpochMs }.take(200).joinToString("\n", transform = ::encodeLine))
    }

    private fun keyFor(mediaRef: MediaRef, seasonNumber: Int?, episodeNumber: Int?): String {
        return PlaybackMemoryRecord(
            mediaType = mediaRef.mediaType.name,
            title = mediaRef.title,
            tmdbId = mediaRef.ids.tmdbId,
            imdbId = mediaRef.ids.imdbId,
            tvdbId = mediaRef.ids.tvdbId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        ).contentKey()
    }

    private fun encodeLine(item: PlaybackMemoryRecord): String {
        return listOf(
            item.mediaType,
            item.title,
            item.tmdbId.orEmpty(),
            item.imdbId.orEmpty(),
            item.tvdbId.orEmpty(),
            item.seasonNumber?.toString().orEmpty(),
            item.episodeNumber?.toString().orEmpty(),
            item.sourceId.orEmpty(),
            item.sourceUrl.orEmpty(),
            item.positionMs.toString(),
            item.durationMs.toString(),
            item.progressPercent.toString(),
            item.updatedAtEpochMs.toString()
        ).joinToString("\t") { it.replace("\n", " ").replace("\t", " ") }
    }

    private fun decodeLine(line: String): PlaybackMemoryRecord? {
        val parts = line.split('\t')
        if (parts.size < 13) return null
        return PlaybackMemoryRecord(
            mediaType = parts[0],
            title = parts[1],
            tmdbId = parts[2].ifBlank { null },
            imdbId = parts[3].ifBlank { null },
            tvdbId = parts[4].ifBlank { null },
            seasonNumber = parts[5].toIntOrNull(),
            episodeNumber = parts[6].toIntOrNull(),
            sourceId = parts[7].ifBlank { null },
            sourceUrl = parts[8].ifBlank { null },
            positionMs = parts[9].toLongOrNull() ?: 0L,
            durationMs = parts[10].toLongOrNull() ?: 0L,
            progressPercent = parts[11].toIntOrNull() ?: 0,
            updatedAtEpochMs = parts[12].toLongOrNull() ?: 0L
        )
    }
}
