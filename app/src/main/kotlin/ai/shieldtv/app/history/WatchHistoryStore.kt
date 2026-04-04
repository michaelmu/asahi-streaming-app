package ai.shieldtv.app.history

import android.content.Context
import ai.shieldtv.app.core.model.media.MediaType
import java.io.File

class WatchHistoryStore(
    context: Context
) : WatchHistoryStoreBase(File(context.filesDir, "history/items.json"))

open class WatchHistoryStoreBase(
    private val file: File
) {
    fun load(): List<WatchHistoryItem> {
        if (!file.exists()) return emptyList()
        return WatchHistoryJson.decode(file.readText())
            .sortedByDescending { it.watchedAtEpochMs }
    }

    fun save(items: List<WatchHistoryItem>) {
        file.parentFile?.mkdirs()
        file.writeText(WatchHistoryJson.encode(items.sortedByDescending { it.watchedAtEpochMs }))
    }

    fun record(item: WatchHistoryItem) {
        val existing = load().associateBy { it.stableKey() }.toMutableMap()
        existing[item.stableKey()] = item.copy(watchedAtEpochMs = System.currentTimeMillis())
        save(existing.values.toList())
    }

    fun listByType(mediaType: MediaType): List<WatchHistoryItem> {
        return load().filter { item ->
            when (mediaType) {
                MediaType.MOVIE -> item.mediaType == MediaType.MOVIE
                MediaType.SHOW -> item.mediaType == MediaType.SHOW
                else -> false
            }
        }
    }

    fun hasWatched(item: WatchHistoryItem): Boolean {
        return load().any { it.stableKey() == item.stableKey() }
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
