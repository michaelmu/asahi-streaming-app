package ai.shieldtv.app.sources

import android.content.Context
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceOrigin
import ai.shieldtv.app.core.model.source.SourceResult
import java.io.File

data class CachedSourceListEntry(
    val cacheKey: String,
    val savedAtEpochMs: Long,
    val sources: List<SourceResult>
)

class SourceListCacheStore(
    context: Context
) : SourceListCacheStoreBase(File(context.filesDir, "sources/cache.tsv"))

open class SourceListCacheStoreBase(
    private val file: File
) {
    fun load(cacheKey: String, maxAgeMs: Long): List<SourceResult>? {
        if (!file.exists()) return null
        val now = System.currentTimeMillis()
        return file.readLines()
            .mapNotNull(::decodeLine)
            .firstOrNull { it.cacheKey == cacheKey && now - it.savedAtEpochMs <= maxAgeMs }
            ?.sources
    }

    fun save(cacheKey: String, sources: List<SourceResult>) {
        val existing = if (file.exists()) file.readLines().mapNotNull(::decodeLine) else emptyList()
        val filtered = existing.filterNot { it.cacheKey == cacheKey }
        val updated = listOf(CachedSourceListEntry(cacheKey, System.currentTimeMillis(), sources.take(40))) + filtered
        file.parentFile?.mkdirs()
        file.writeText(updated.take(50).joinToString("\n", transform = ::encodeLine))
    }

    fun keyFor(mediaRef: MediaRef, seasonNumber: Int?, episodeNumber: Int?, authLinked: Boolean): String {
        return listOf(
            mediaRef.mediaType.name,
            mediaRef.ids.tmdbId.orEmpty(),
            mediaRef.ids.imdbId.orEmpty(),
            mediaRef.ids.tvdbId.orEmpty(),
            mediaRef.title.lowercase(),
            mediaRef.year?.toString().orEmpty(),
            seasonNumber?.toString().orEmpty(),
            episodeNumber?.toString().orEmpty(),
            if (authLinked) "auth" else "anon"
        ).joinToString("|")
    }

    private fun encodeLine(entry: CachedSourceListEntry): String {
        val payload = entry.sources.joinToString("~~") { source ->
            listOf(
                source.id,
                source.mediaRef.mediaType.name,
                source.mediaRef.ids.tmdbId.orEmpty(),
                source.mediaRef.ids.imdbId.orEmpty(),
                source.mediaRef.ids.tvdbId.orEmpty(),
                source.mediaRef.title,
                source.mediaRef.year?.toString().orEmpty(),
                source.providerId,
                source.providerDisplayName,
                source.providerKind.name,
                source.debridService.name,
                source.sourceSite.orEmpty(),
                source.url,
                source.displayName,
                source.quality.name,
                source.cacheStatus.name,
                source.seasonNumber?.toString().orEmpty(),
                source.episodeNumber?.toString().orEmpty(),
                source.infoHash.orEmpty(),
                source.sizeBytes?.toString().orEmpty(),
                source.sizeLabel.orEmpty(),
                source.score?.toString().orEmpty()
            ).joinToString("§") { it.replace("\n", " ").replace("\t", " ").replace("§", " ").replace("~~", " ") }
        }
        return listOf(entry.cacheKey, entry.savedAtEpochMs.toString(), payload).joinToString("\t")
    }

    private fun decodeLine(line: String): CachedSourceListEntry? {
        val parts = line.split('\t', limit = 3)
        if (parts.size < 3) return null
        val sources = parts[2].split("~~").mapNotNull { raw ->
            val fields = raw.split('§')
            if (fields.size < 22) return@mapNotNull null
            SourceResult(
                id = fields[0],
                mediaRef = MediaRef(
                    mediaType = ai.shieldtv.app.core.model.media.MediaType.valueOf(fields[1]),
                    ids = ai.shieldtv.app.core.model.media.MediaIds(
                        tmdbId = fields[2].ifBlank { null },
                        imdbId = fields[3].ifBlank { null },
                        tvdbId = fields[4].ifBlank { null }
                    ),
                    title = fields[5],
                    year = fields[6].toIntOrNull()
                ),
                providerId = fields[7],
                providerDisplayName = fields[8],
                providerKind = ProviderKind.valueOf(fields[9]),
                debridService = DebridService.valueOf(fields[10]),
                sourceSite = fields[11].ifBlank { null },
                url = fields[12],
                displayName = fields[13],
                quality = Quality.valueOf(fields[14]),
                cacheStatus = CacheStatus.valueOf(fields[15]),
                seasonNumber = fields[16].toIntOrNull(),
                episodeNumber = fields[17].toIntOrNull(),
                infoHash = fields[18].ifBlank { null },
                sizeBytes = fields[19].toLongOrNull(),
                sizeLabel = fields[20].ifBlank { null },
                score = fields[21].toDoubleOrNull(),
                rawMetadata = emptyMap(),
                origins = listOf(
                    SourceOrigin(
                        providerId = fields[7],
                        providerDisplayName = fields[8],
                        displayName = fields[13],
                        cacheStatus = CacheStatus.valueOf(fields[15]),
                        sizeBytes = fields[19].toLongOrNull(),
                        quality = Quality.valueOf(fields[14])
                    )
                )
            )
        }
        return CachedSourceListEntry(parts[0], parts[1].toLongOrNull() ?: 0L, sources)
    }
}
