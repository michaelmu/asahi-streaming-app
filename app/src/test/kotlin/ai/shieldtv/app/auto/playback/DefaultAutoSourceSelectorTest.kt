package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAutoSourceSelectorTest {
    private val selector = DefaultAutoSourceSelector()

    @Test
    fun cached_beats_direct() {
        val direct = source(id = "direct", cacheStatus = CacheStatus.DIRECT)
        val cached = source(id = "cached", cacheStatus = CacheStatus.CACHED)

        assertEquals(cached, selector.selectBestForAuto(listOf(direct, cached)))
    }

    @Test
    fun direct_chosen_when_no_cached_exists() {
        val unchecked = source(id = "unchecked", cacheStatus = CacheStatus.UNCHECKED)
        val direct = source(id = "direct", cacheStatus = CacheStatus.DIRECT)

        assertEquals(direct, selector.selectBestForAuto(listOf(unchecked, direct)))
    }

    @Test
    fun uncached_only_returns_null() {
        val uncached = source(id = "uncached", cacheStatus = CacheStatus.UNCACHED)
        val unchecked = source(id = "unchecked", cacheStatus = CacheStatus.UNCHECKED)

        assertNull(selector.selectBestForAuto(listOf(uncached, unchecked)))
    }

    @Test
    fun empty_list_returns_null() {
        assertNull(selector.selectBestForAuto(emptyList()))
    }

    @Test
    fun mixed_list_with_cached_later_still_selects_first_cached_match() {
        val unchecked = source(id = "unchecked", cacheStatus = CacheStatus.UNCHECKED)
        val firstCached = source(id = "cached-1", cacheStatus = CacheStatus.CACHED)
        val secondCached = source(id = "cached-2", cacheStatus = CacheStatus.CACHED)
        val direct = source(id = "direct", cacheStatus = CacheStatus.DIRECT)

        assertEquals(firstCached, selector.selectBestForAuto(listOf(unchecked, direct, firstCached, secondCached)))
    }

    private fun source(id: String, cacheStatus: CacheStatus): SourceResult = SourceResult(
        id = id,
        mediaRef = MediaRef(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = "438631", imdbId = "tt15239678", tvdbId = null),
            title = "Dune: Part Two",
            year = 2024
        ),
        providerId = "test-provider",
        providerDisplayName = "Test Provider",
        providerKind = ProviderKind.SCRAPER,
        debridService = DebridService.REAL_DEBRID,
        sourceSite = "test-site",
        url = "https://example.com/$id",
        displayName = "Source $id",
        quality = Quality.FHD_1080P,
        cacheStatus = cacheStatus
    )
}
