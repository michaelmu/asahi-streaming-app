package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WatchHistoryStoreTest {
    @Test
    fun record_persists_and_sorts_most_recent_first() {
        val tempDir = createTempDir(prefix = "history-store")
        val store = WatchHistoryStoreBase(File(tempDir, "history.json"))

        val first = movie(title = "First", tmdbId = "1", watchedAt = 1000)
        val second = movie(title = "Second", tmdbId = "2", watchedAt = 2000)

        store.save(listOf(first))
        store.record(second)

        val loaded = store.load()
        assertEquals(2, loaded.size)
        assertEquals("Second", loaded.first().title)
    }

    @Test
    fun record_same_movie_refreshes_recency_instead_of_duplicating() {
        val tempDir = createTempDir(prefix = "history-dedupe-movie")
        val store = WatchHistoryStoreBase(File(tempDir, "history.json"))

        val item = movie(title = "Movie", tmdbId = "1", watchedAt = 1000)
        store.save(listOf(item))
        store.record(item.copy(watchedAtEpochMs = 500))

        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals("Movie", loaded.single().title)
    }

    @Test
    fun record_same_show_different_episode_keeps_distinct_entries() {
        val tempDir = createTempDir(prefix = "history-episodes")
        val store = WatchHistoryStoreBase(File(tempDir, "history.json"))

        val episodeOne = episode(showTitle = "Severance", tmdbId = "show1", season = 1, episode = 1, watchedAt = 1000)
        val episodeTwo = episode(showTitle = "Severance", tmdbId = "show1", season = 1, episode = 2, watchedAt = 2000)

        store.record(episodeOne)
        store.record(episodeTwo)

        val loaded = store.listByType(MediaType.SHOW)
        assertEquals(2, loaded.size)
        assertEquals(2, loaded.first().episodeNumber)
    }

    @Test
    fun hasWatched_matches_by_stable_key() {
        val tempDir = createTempDir(prefix = "history-check")
        val store = WatchHistoryStoreBase(File(tempDir, "history.json"))

        val item = episode(showTitle = "Andor", tmdbId = "show2", season = 1, episode = 3, watchedAt = 1000)
        store.save(listOf(item))

        assertTrue(store.hasWatched(item.copy(watchedAtEpochMs = 9999)))
        assertFalse(store.hasWatched(episode(showTitle = "Andor", tmdbId = "show2", season = 1, episode = 4, watchedAt = 1000)))
    }

    private fun movie(title: String, tmdbId: String, watchedAt: Long): WatchHistoryItem {
        return WatchHistoryItem(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = tmdbId, imdbId = null, tvdbId = null),
            title = title,
            year = 2024,
            artworkUrl = "https://example.com/$tmdbId.jpg",
            watchedAtEpochMs = watchedAt
        )
    }

    private fun episode(showTitle: String, tmdbId: String, season: Int, episode: Int, watchedAt: Long): WatchHistoryItem {
        return WatchHistoryItem(
            mediaType = MediaType.SHOW,
            ids = MediaIds(tmdbId = tmdbId, imdbId = null, tvdbId = null),
            title = showTitle,
            year = 2024,
            artworkUrl = "https://example.com/$tmdbId.jpg",
            subtitle = "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}",
            seasonNumber = season,
            episodeNumber = episode,
            episodeTitle = "Episode $episode",
            watchedAtEpochMs = watchedAt
        )
    }
}
