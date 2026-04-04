package ai.shieldtv.app.favorites

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult

class FavoritesCoordinator(
    private val favoritesStore: FavoritesStore
) {
    fun list(): List<FavoriteItem> = favoritesStore.load()

    fun favoriteKeys(): Set<String> = favoritesStore.load().mapTo(linkedSetOf()) { it.stableKey() }

    fun isFavorited(result: SearchResult): Boolean {
        return favoriteKeys().contains(result.toFavoriteItem().stableKey())
    }

    fun toggle(result: SearchResult): Boolean {
        val favorite = result.toFavoriteItem()
        return if (favoritesStore.isFavorited(favorite)) {
            favoritesStore.remove(favorite)
            false
        } else {
            favoritesStore.add(favorite)
            true
        }
    }

    fun listByType(mediaType: MediaType): List<SearchResult> {
        return favoritesStore.load()
            .filter { it.mediaType == mediaType }
            .sortedByDescending { it.addedAtEpochMs }
            .map { it.toSearchResult() }
    }
}

fun SearchResult.toFavoriteItem(): FavoriteItem {
    return FavoriteItem(
        mediaType = mediaRef.mediaType,
        ids = mediaRef.ids,
        title = mediaRef.title,
        year = mediaRef.year,
        subtitle = subtitle,
        artworkUrl = posterUrl ?: backdropUrl
    )
}

fun SearchResult.toFavoriteStableKey(): String = toFavoriteItem().stableKey()

fun FavoriteItem.toSearchResult(): SearchResult {
    return SearchResult(
        mediaRef = MediaRef(
            mediaType = mediaType,
            ids = ids,
            title = title,
            year = year
        ),
        subtitle = subtitle,
        posterUrl = artworkUrl,
        backdropUrl = artworkUrl,
        badges = listOf("Favorite")
    )
}
