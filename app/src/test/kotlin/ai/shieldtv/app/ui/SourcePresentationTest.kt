package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SourcePresentationTest {
    @Test
    fun cached_4k_source_is_labeled_top_pick() {
        assertEquals("TOP PICK", SourcePresentation.sourceLabel(source(CacheStatus.CACHED, Quality.UHD_4K)))
    }

    @Test
    fun direct_source_is_grouped_as_direct_links() {
        assertEquals("Direct Links", SourcePresentation.groupTitle(source(CacheStatus.DIRECT, Quality.FHD_1080P)))
    }

    @Test
    fun unchecked_source_is_grouped_as_other_options() {
        assertEquals("Other Options", SourcePresentation.groupTitle(source(CacheStatus.UNCHECKED, Quality.HD_720P)))
    }

    @Test
    fun quality_labels_are_human_readable() {
        assertEquals("1080p", SourcePresentation.qualityLabel(Quality.FHD_1080P))
        assertEquals("4K", SourcePresentation.qualityLabel(Quality.UHD_4K))
    }

    private fun source(cacheStatus: CacheStatus, quality: Quality) = SourceResult(
        id = "source",
        mediaRef = MediaRef(MediaType.MOVIE, MediaIds(null, null, null), "Dune", year = 2024),
        providerId = "provider",
        providerDisplayName = "Provider",
        providerKind = ProviderKind.SCRAPER,
        debridService = DebridService.REAL_DEBRID,
        sourceSite = "Site",
        url = "https://example.com/source",
        displayName = "Source",
        quality = quality,
        cacheStatus = cacheStatus
    )
}
