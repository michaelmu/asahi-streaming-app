package ai.shieldtv.app.continuewatching

import android.content.Context
import ai.shieldtv.app.ContinueWatchingItem
import java.io.File

data class PersistedContinueWatchingItem(
    val mediaTitle: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val queryHint: String,
    val progressPercent: Int = 0,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun stableKey(): String = listOf(mediaTitle, subtitle, queryHint).joinToString("|")

    fun toUiItem(): ContinueWatchingItem = ContinueWatchingItem(
        mediaTitle = mediaTitle,
        subtitle = subtitle,
        artworkUrl = artworkUrl,
        queryHint = queryHint,
        progressPercent = progressPercent
    )
}

class ContinueWatchingStore(
    context: Context
) : ContinueWatchingStoreBase(File(context.filesDir, "continue-watching/items.json"))

open class ContinueWatchingStoreBase(
    private val file: File
) {
    fun load(): List<PersistedContinueWatchingItem> {
        if (!file.exists()) return emptyList()
        return decode(file.readText()).sortedByDescending { it.updatedAtEpochMs }
    }

    fun save(items: List<PersistedContinueWatchingItem>) {
        file.parentFile?.mkdirs()
        file.writeText(encode(items.sortedByDescending { it.updatedAtEpochMs }.take(6)))
    }

    fun record(item: PersistedContinueWatchingItem) {
        val existing = load().associateBy { it.stableKey() }.toMutableMap()
        existing[item.stableKey()] = item.copy(updatedAtEpochMs = System.currentTimeMillis())
        save(existing.values.toList())
    }

    private fun encode(items: List<PersistedContinueWatchingItem>): String {
        return items.joinToString(separator = "\n") { item ->
            listOf(
                item.mediaTitle,
                item.subtitle,
                item.artworkUrl.orEmpty(),
                item.queryHint,
                item.progressPercent.toString(),
                item.updatedAtEpochMs.toString()
            ).joinToString("\t") { field -> field.replace("\n", " ").replace("\t", " ") }
        }
    }

    private fun decode(raw: String): List<PersistedContinueWatchingItem> {
        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 6) return@mapNotNull null
                PersistedContinueWatchingItem(
                    mediaTitle = parts[0],
                    subtitle = parts[1],
                    artworkUrl = parts[2].ifBlank { null },
                    queryHint = parts[3],
                    progressPercent = parts[4].toIntOrNull() ?: 0,
                    updatedAtEpochMs = parts[5].toLongOrNull() ?: 0L
                )
            }
            .toList()
    }
}
