package ai.shieldtv.app.favorites

import android.content.Context
import java.io.File

class FavoritesStore(
    context: Context
) : FavoritesStoreBase(File(context.filesDir, "favorites/items.json"))

open class FavoritesStoreBase(
    private val file: File
) {
    fun load(): List<FavoriteItem> {
        if (!file.exists()) return emptyList()
        return FavoritesJson.decode(file.readText())
            .sortedByDescending { it.addedAtEpochMs }
    }

    fun save(items: List<FavoriteItem>) {
        file.parentFile?.mkdirs()
        file.writeText(FavoritesJson.encode(items.sortedByDescending { it.addedAtEpochMs }))
    }

    fun add(item: FavoriteItem) {
        val existing = load().associateBy { it.stableKey() }.toMutableMap()
        existing[item.stableKey()] = item.copy(addedAtEpochMs = System.currentTimeMillis())
        save(existing.values.toList())
    }

    fun remove(item: FavoriteItem) {
        val remaining = load().filterNot { it.stableKey() == item.stableKey() }
        save(remaining)
    }

    fun isFavorited(item: FavoriteItem): Boolean {
        return load().any { it.stableKey() == item.stableKey() }
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
