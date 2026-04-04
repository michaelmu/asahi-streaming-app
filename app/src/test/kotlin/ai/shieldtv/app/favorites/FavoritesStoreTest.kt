package ai.shieldtv.app.favorites

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FavoritesStoreTest {
    @Test
    fun add_persists_and_sorts_most_recent_first() {
        val tempDir = createTempDir(prefix = "favorites-store")
        val store = FavoritesStoreBase(File(tempDir, "favorites.json"))

        val first = favorite(title = "First", tmdbId = "1", addedAt = 1000)
        val second = favorite(title = "Second", tmdbId = "2", addedAt = 2000)

        store.save(listOf(first))
        store.add(second)

        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals("Second", loaded.first().title)
    }

    @Test
    fun add_same_item_refreshes_recency_instead_of_duplicating() {
        val tempDir = createTempDir(prefix = "favorites-dedupe")
        val store = FavoritesStoreBase(File(tempDir, "favorites.json"))

        val item = favorite(title = "Movie", tmdbId = "1", addedAt = 1000)
        store.save(listOf(item))
        store.add(item.copy(addedAtEpochMs = 500))

        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals("Movie", loaded.single().title)
    }

    @Test
    fun remove_deletes_item() {
        val tempDir = createTempDir(prefix = "favorites-remove")
        val store = FavoritesStoreBase(File(tempDir, "favorites.json"))

        val item = favorite(title = "Movie", tmdbId = "1", addedAt = 1000)
        store.save(listOf(item))
        store.remove(item)

        assertTrue(store.load().isEmpty())
    }

    @Test
    fun isFavorited_matches_by_stable_key() {
        val tempDir = createTempDir(prefix = "favorites-check")
        val store = FavoritesStoreBase(File(tempDir, "favorites.json"))

        val item = favorite(title = "Movie", tmdbId = "1", addedAt = 1000)
        store.save(listOf(item))

        assertTrue(store.isFavorited(item.copy(addedAtEpochMs = 9999)))
        assertFalse(store.isFavorited(favorite(title = "Other", tmdbId = "2", addedAt = 1000)))
    }

    private fun favorite(title: String, tmdbId: String, addedAt: Long): FavoriteItem {
        return FavoriteItem(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = tmdbId, imdbId = null, tvdbId = null),
            title = title,
            year = 2024,
            artworkUrl = "https://example.com/$tmdbId.jpg",
            addedAtEpochMs = addedAt
        )
    }
}
