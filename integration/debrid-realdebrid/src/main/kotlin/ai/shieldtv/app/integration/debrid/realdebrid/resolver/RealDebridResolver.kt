package ai.shieldtv.app.integration.debrid.realdebrid.resolver

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentFile
import kotlinx.coroutines.delay
import kotlin.math.max

class RealDebridResolver(
    private val api: RealDebridApi
) {
    suspend fun resolveMagnet(
        magnet: String,
        mediaRef: MediaRef,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        source: SourceResult? = null
    ): ResolvedMagnetStream {
        val added = api.addMagnet(magnet)
            ?: throw IllegalStateException("Real-Debrid rejected magnet add request")

        val initialInfo = api.getTorrentInfo(added.id)
            ?: throw IllegalStateException("Real-Debrid did not return torrent info")

        val selectedFileIds = selectCandidateFiles(
            files = initialInfo.files,
            mediaRef = mediaRef,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            quality = source?.quality ?: Quality.UNKNOWN
        ).ifEmpty { throw IllegalStateException("No playable files matched this torrent") }

        val selectOk = api.selectTorrentFiles(added.id, selectedFileIds.joinToString(","))
        if (!selectOk) {
            throw IllegalStateException("Real-Debrid file selection failed")
        }

        val finalInfo = pollForLinks(added.id)
            ?: throw IllegalStateException("Real-Debrid did not produce playable links")

        val selectedLink = chooseMatchingLink(finalInfo.files, finalInfo.links, selectedFileIds)
            ?: finalInfo.links.firstOrNull()
            ?: throw IllegalStateException("Real-Debrid returned no unrestricted candidate links")

        val unrestricted = api.unrestrictLink(selectedLink)
            ?: throw IllegalStateException("Real-Debrid unrestrict failed")

        return ResolvedMagnetStream(
            streamUrl = unrestricted.download,
            mimeType = unrestricted.mimeType
        )
    }

    private suspend fun pollForLinks(torrentId: String): ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentInfo? {
        repeat(6) { attempt ->
            val info = api.getTorrentInfo(torrentId)
            if (info != null && info.links.isNotEmpty()) {
                return info
            }
            if (attempt < 5) delay(1500)
        }
        return api.getTorrentInfo(torrentId)
    }

    private fun selectCandidateFiles(
        files: List<RealDebridTorrentFile>,
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?,
        quality: Quality
    ): List<Int> {
        val playable = files.filter { isPlayableVideoFile(it.path) }
            .filterNot { looksLikeSample(it.path) }
            .filterNot { looksLikeJunk(it.path) }
        if (playable.isEmpty()) return emptyList()

        val ranked = playable
            .map { file ->
                file to scoreFile(
                    file = file,
                    mediaRef = mediaRef,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    quality = quality
                )
            }
            .sortedWith(
                compareByDescending<Pair<RealDebridTorrentFile, Int>> { it.second }
                    .thenByDescending { it.first.bytes }
            )

        return ranked.take(1).map { it.first.id }
    }

    private fun scoreFile(
        file: RealDebridTorrentFile,
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?,
        quality: Quality
    ): Int {
        val path = file.path.substringAfterLast('/').ifBlank { file.path }
        val normalizedPath = normalize(path)
        val normalizedTitle = normalize(mediaRef.title)
        val normalizedOriginalTitle = mediaRef.originalTitle?.let(::normalize)
        val titleTokens = normalizedTitle.split(' ').filter { it.length > 1 }
        val originalTokens = normalizedOriginalTitle?.split(' ')?.filter { it.length > 1 }.orEmpty()

        var score = 0

        if (normalizedTitle.isNotBlank() && normalizedPath.contains(normalizedTitle)) {
            score += 120
        }
        if (!normalizedOriginalTitle.isNullOrBlank() && normalizedPath.contains(normalizedOriginalTitle)) {
            score += 100
        }

        score += titleTokens.fold(0) { acc, token -> acc + if (normalizedPath.contains(token)) 12 else 0 }
        score += originalTokens.fold(0) { acc, token -> acc + if (normalizedPath.contains(token)) 10 else 0 }

        mediaRef.year?.let { year ->
            if (normalizedPath.contains(year.toString())) score += 20
        }

        val hasEpisodeContext = seasonNumber != null && episodeNumber != null
        if (hasEpisodeContext) {
            val episodeMarkers = buildEpisodeMarkers(seasonNumber!!, episodeNumber!!)
            val seasonMarkers = buildSeasonMarkers(seasonNumber)

            if (episodeMarkers.any { marker -> marker in normalizedPath }) {
                score += 220
            }
            if (seasonMarkers.any { marker -> marker in normalizedPath }) {
                score += 40
            }
            if (looksLikeEpisodeMarker(path)) {
                score += 25
            }
        }

        when (mediaRef.mediaType) {
            MediaType.MOVIE -> {
                if (looksLikeMovieFile(path)) score += 10
                if (looksLikeEpisodeMarker(path)) score -= 40
            }
            MediaType.SHOW, MediaType.SEASON, MediaType.EPISODE -> {
                if (looksLikeEpisodeMarker(path)) score += 20
            }
        }

        if (looksLikeSample(path)) score -= 100
        if (looksLikeJunk(path)) score -= 100
        if (isSubtitleOrNfo(path)) score -= 200

        score += sizeHeuristicScore(
            bytes = file.bytes,
            mediaType = mediaRef.mediaType,
            hasEpisodeContext = hasEpisodeContext,
            quality = quality
        )

        return max(score, -999)
    }

    private fun sizeHeuristicScore(
        bytes: Long,
        mediaType: MediaType,
        hasEpisodeContext: Boolean,
        quality: Quality
    ): Int {
        if (bytes <= 0L) return 0

        val profile = expectedSizeProfile(
            mediaType = mediaType,
            hasEpisodeContext = hasEpisodeContext,
            quality = quality
        ) ?: return genericSizeScore(bytes)

        return when {
            bytes < profile.hardMin -> -85
            bytes < profile.softMin -> -35
            bytes in profile.idealMin..profile.idealMax -> 18
            bytes <= profile.softMax -> 6
            bytes <= profile.hardMax -> -20
            else -> -55
        }
    }

    private fun expectedSizeProfile(
        mediaType: MediaType,
        hasEpisodeContext: Boolean,
        quality: Quality
    ): SizeProfile? {
        val episodeLike = hasEpisodeContext || mediaType == MediaType.EPISODE
        return if (episodeLike) {
            when (quality) {
                Quality.UHD_4K -> SizeProfile(
                    hardMin = gb(1),
                    softMin = gb(2),
                    idealMin = gb(3),
                    idealMax = gb(12),
                    softMax = gb(16),
                    hardMax = gb(25)
                )
                Quality.FHD_1080P -> SizeProfile(
                    hardMin = mb(250),
                    softMin = mb(400),
                    idealMin = mb(700),
                    idealMax = gb(4),
                    softMax = gb(6),
                    hardMax = gb(10)
                )
                Quality.HD_720P -> SizeProfile(
                    hardMin = mb(150),
                    softMin = mb(250),
                    idealMin = mb(450),
                    idealMax = gb(2),
                    softMax = gb(3),
                    hardMax = gb(6)
                )
                Quality.SD, Quality.SCR, Quality.CAM, Quality.TELE, Quality.UNKNOWN -> SizeProfile(
                    hardMin = mb(100),
                    softMin = mb(180),
                    idealMin = mb(250),
                    idealMax = gb(2),
                    softMax = gb(3),
                    hardMax = gb(5)
                )
            }
        } else {
            when (quality) {
                Quality.UHD_4K -> SizeProfile(
                    hardMin = gb(4),
                    softMin = gb(8),
                    idealMin = gb(12),
                    idealMax = gb(40),
                    softMax = gb(60),
                    hardMax = gb(100)
                )
                Quality.FHD_1080P -> SizeProfile(
                    hardMin = mb(900),
                    softMin = gb(1) + mb(500),
                    idealMin = gb(3),
                    idealMax = gb(10),
                    softMax = gb(18),
                    hardMax = gb(30)
                )
                Quality.HD_720P -> SizeProfile(
                    hardMin = mb(450),
                    softMin = mb(700),
                    idealMin = gb(1),
                    idealMax = gb(4),
                    softMax = gb(8),
                    hardMax = gb(15)
                )
                Quality.SD, Quality.SCR, Quality.CAM, Quality.TELE, Quality.UNKNOWN -> SizeProfile(
                    hardMin = mb(300),
                    softMin = mb(500),
                    idealMin = mb(700),
                    idealMax = gb(3),
                    softMax = gb(6),
                    hardMax = gb(12)
                )
            }
        }
    }

    private fun genericSizeScore(bytes: Long): Int {
        return when {
            bytes >= 40L * 1024 * 1024 * 1024 -> 8
            bytes >= 15L * 1024 * 1024 * 1024 -> 12
            bytes >= 4L * 1024 * 1024 * 1024 -> 10
            bytes >= 700L * 1024 * 1024 -> 6
            else -> 0
        }
    }

    private fun buildEpisodeMarkers(seasonNumber: Int, episodeNumber: Int): List<String> {
        val s = seasonNumber.toString().padStart(2, '0')
        val e = episodeNumber.toString().padStart(2, '0')
        return listOf(
            "s${s}e${e}",
            "${seasonNumber}x${episodeNumber}",
            "season ${seasonNumber} episode ${episodeNumber}",
            "season${seasonNumber}episode${episodeNumber}",
            "ep ${episodeNumber}",
            "episode ${episodeNumber}"
        )
    }

    private fun buildSeasonMarkers(seasonNumber: Int): List<String> {
        val s = seasonNumber.toString().padStart(2, '0')
        return listOf(
            "s$s",
            "season ${seasonNumber}",
            "season${seasonNumber}"
        )
    }

    private fun chooseMatchingLink(
        files: List<RealDebridTorrentFile>,
        links: List<String>,
        selectedFileIds: List<Int>
    ): String? {
        if (links.isEmpty()) return null
        if (files.isEmpty()) return links.firstOrNull()

        val selectedIndices = files.mapIndexedNotNull { index, file ->
            if (file.id in selectedFileIds) index else null
        }
        return selectedIndices.firstNotNullOfOrNull { index -> links.getOrNull(index) }
    }

    private fun isPlayableVideoFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".mkv") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".avi") ||
            lower.endsWith(".mov") ||
            lower.endsWith(".m4v") ||
            lower.endsWith(".ts")
    }

    private fun isSubtitleOrNfo(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".srt") ||
            lower.endsWith(".sub") ||
            lower.endsWith(".idx") ||
            lower.endsWith(".nfo") ||
            lower.endsWith(".txt")
    }

    private fun looksLikeSample(path: String): Boolean {
        val lower = path.lowercase()
        return "sample" in lower || "/extras/" in lower || "/featurettes/" in lower
    }

    private fun looksLikeJunk(path: String): Boolean {
        val lower = path.lowercase()
        return "trailer" in lower ||
            "featurette" in lower ||
            "behind the scenes" in lower ||
            "interview" in lower ||
            "deleted scenes" in lower
    }

    private fun looksLikeMovieFile(path: String): Boolean {
        val lower = path.lowercase()
        return !("s01" in lower || "e01" in lower || "season 1" in lower)
    }

    private fun looksLikeEpisodeMarker(path: String): Boolean {
        val lower = path.lowercase()
        return Regex("s\\d{1,2}e\\d{1,2}").containsMatchIn(lower) ||
            Regex("\\b\\d{1,2}x\\d{1,2}\\b").containsMatchIn(lower)
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun mb(value: Long): Long = value * 1024L * 1024L
    private fun gb(value: Long): Long = value * 1024L * 1024L * 1024L
}

data class ResolvedMagnetStream(
    val streamUrl: String,
    val mimeType: String? = null
)

data class SizeProfile(
    val hardMin: Long,
    val softMin: Long,
    val idealMin: Long,
    val idealMax: Long,
    val softMax: Long,
    val hardMax: Long
)
