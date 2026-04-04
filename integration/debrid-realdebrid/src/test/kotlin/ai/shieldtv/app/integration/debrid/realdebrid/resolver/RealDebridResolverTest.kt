package ai.shieldtv.app.integration.debrid.realdebrid.resolver

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.integration.debrid.realdebrid.api.DeviceFlowResponse
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentAddResponse
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentFile
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentInfo
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridUnrestrictedLink
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RealDebridResolverTest {
    @Test
    fun resolveMagnet_prefers_episode_match_over_sample_and_extras() = runBlocking {
        val api = FakeResolverApi(
            files = listOf(
                RealDebridTorrentFile(id = 1, path = "/Show.Name.S01E02.sample.mkv", bytes = 80L * MB, selected = 0),
                RealDebridTorrentFile(id = 2, path = "/Show.Name.S01E02.1080p.WEB-DL.mkv", bytes = 2L * GB, selected = 0),
                RealDebridTorrentFile(id = 3, path = "/extras/featurette.mkv", bytes = 500L * MB, selected = 0)
            )
        )

        val resolver = RealDebridResolver(api)
        resolver.resolveMagnet(
            magnet = "magnet:?xt=urn:btih:test",
            mediaRef = episodeRef(),
            seasonNumber = 1,
            episodeNumber = 2,
            source = null
        )

        assertEquals("2", api.selectedIds)
    }

    @Test
    fun resolveMagnet_prefers_movie_title_year_match() = runBlocking {
        val api = FakeResolverApi(
            files = listOf(
                RealDebridTorrentFile(id = 1, path = "/The.Matrix.1999.1080p.BluRay.mkv", bytes = 4L * GB, selected = 0),
                RealDebridTorrentFile(id = 2, path = "/The.Matrix.Resurrections.2021.1080p.mkv", bytes = 3L * GB, selected = 0)
            )
        )

        val resolver = RealDebridResolver(api)
        resolver.resolveMagnet(
            magnet = "magnet:?xt=urn:btih:test",
            mediaRef = movieRef(),
            source = null
        )

        assertEquals("1", api.selectedIds)
    }

    @Test
    fun resolveMagnet_prefers_reasonable_size_for_quality() = runBlocking {
        val api = FakeResolverApi(
            files = listOf(
                RealDebridTorrentFile(id = 1, path = "/Movie.Title.2024.1080p.mkv", bytes = 120L * MB, selected = 0),
                RealDebridTorrentFile(id = 2, path = "/Movie.Title.2024.1080p.WEB-DL.mkv", bytes = 5L * GB, selected = 0)
            )
        )

        val resolver = RealDebridResolver(api)
        resolver.resolveMagnet(
            magnet = "magnet:?xt=urn:btih:test",
            mediaRef = MediaRef(
                mediaType = MediaType.MOVIE,
                ids = MediaIds(tmdbId = "999", imdbId = "tt9999999", tvdbId = null),
                title = "Movie Title",
                year = 2024
            ),
            source = SourceResult(
                id = "test",
                mediaRef = movieRef(),
                providerId = "test",
                providerDisplayName = "Test",
                providerKind = ProviderKind.SCRAPER,
                debridService = DebridService.REAL_DEBRID,
                sourceSite = "Test",
                url = "magnet:?xt=urn:btih:test",
                displayName = "Movie Title",
                quality = Quality.FHD_1080P,
                cacheStatus = CacheStatus.CACHED
            )
        )

        assertEquals("2", api.selectedIds)
    }

    private fun movieRef(): MediaRef = MediaRef(
        mediaType = MediaType.MOVIE,
        ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
        title = "The Matrix",
        year = 1999
    )

    private fun episodeRef(): MediaRef = MediaRef(
        mediaType = MediaType.SHOW,
        ids = MediaIds(tmdbId = "1", imdbId = "tt1000001", tvdbId = null),
        title = "Show Name",
        year = 2024
    )

    private class FakeResolverApi(
        private val files: List<RealDebridTorrentFile>
    ) : RealDebridApi {
        var selectedIds: String? = null

        override suspend fun startDeviceFlow(): DeviceFlowResponse = throw UnsupportedOperationException()
        override suspend fun getDeviceCredentials(deviceCode: String): RealDebridCredentialResponse? = throw UnsupportedOperationException()
        override suspend fun exchangeDeviceCredentialsForToken(
            deviceCode: String,
            clientId: String,
            clientSecret: String
        ): RealDebridTokenResponse? = throw UnsupportedOperationException()
        override suspend fun instantAvailability(infoHashes: List<String>): String = "{}"
        override suspend fun addMagnet(magnet: String): RealDebridTorrentAddResponse? = RealDebridTorrentAddResponse("torrent-1", "uri")
        override suspend fun getTorrentInfo(torrentId: String): RealDebridTorrentInfo? = RealDebridTorrentInfo(
            id = torrentId,
            filename = "test",
            status = "downloaded",
            links = files.map { "https://example.com/${it.id}" },
            files = files
        )
        override suspend fun selectTorrentFiles(torrentId: String, fileIdsCsv: String): Boolean {
            selectedIds = fileIdsCsv
            return true
        }
        override suspend fun unrestrictLink(link: String): RealDebridUnrestrictedLink? = RealDebridUnrestrictedLink(
            download = link,
            filename = "video.mkv",
            mimeType = "video/x-matroska"
        )
    }

    companion object {
        private const val MB = 1024L * 1024L
        private const val GB = 1024L * 1024L * 1024L
    }
}
