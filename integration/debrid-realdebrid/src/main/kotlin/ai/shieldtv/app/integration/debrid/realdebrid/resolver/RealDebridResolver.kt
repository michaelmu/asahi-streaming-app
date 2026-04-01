package ai.shieldtv.app.integration.debrid.realdebrid.resolver

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentFile
import kotlinx.coroutines.delay
import kotlin.math.max

class RealDebridResolver(
    private val api: RealDebridApi
) {
    suspend fun resolveMagnet(magnet: String, mediaRef: MediaRef): ResolvedMagnetStream {
        val added = api.addMagnet(magnet)
            ?: throw IllegalStateException("Real-Debrid rejected magnet add request")

        val initialInfo = api.getTorrentInfo(added.id)
            ?: throw IllegalStateException("Real-Debrid did not return torrent info")

        val selectedFileIds = selectCandidateFiles(initialInfo.files, mediaRef)
            .ifEmpty { throw IllegalStateException("No playable files matched this torrent") }

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

    private fun selectCandidateFiles(files: List<RealDebridTorrentFile>, mediaRef: MediaRef): List<Int> {
        val playable = files.filter { isPlayableVideoFile(it.path) }
            .filterNot { looksLikeSample(it.path) }
            .filterNot { looksLikeJunk(it.path) }
        if (playable.isEmpty()) return emptyList()

        val ranked = playable
            .map { file -> file to scoreFile(file, mediaRef) }
            .sortedWith(
                compareByDescending<Pair<RealDebridTorrentFile, Int>> { it.second }
                    .thenByDescending { it.first.bytes }
            )

        return ranked.take(1).map { it.first.id }
    }

    private fun scoreFile(file: RealDebridTorrentFile, mediaRef: MediaRef): Int {
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

        if (mediaRef.mediaType == MediaType.MOVIE) {
            if (looksLikeMovieFile(path)) score += 10
            if (looksLikeEpisodeMarker(path)) score -= 40
        }

        if (looksLikeSample(path)) score -= 100
        if (looksLikeJunk(path)) score -= 100
        if (isSubtitleOrNfo(path)) score -= 200

        val sizeScore = when {
            file.bytes >= 40L * 1024 * 1024 * 1024 -> 18
            file.bytes >= 15L * 1024 * 1024 * 1024 -> 14
            file.bytes >= 4L * 1024 * 1024 * 1024 -> 10
            file.bytes >= 700L * 1024 * 1024 -> 6
            else -> 0
        }
        score += sizeScore

        return max(score, -999)
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
}

data class ResolvedMagnetStream(
    val streamUrl: String,
    val mimeType: String? = null
)
