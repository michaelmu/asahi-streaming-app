package ai.shieldtv.app.integration.debrid.realdebrid.resolver

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridTorrentFile
import kotlinx.coroutines.delay

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
        if (playable.isEmpty()) return emptyList()

        val preferred = when (mediaRef.mediaType) {
            MediaType.MOVIE -> playable
                .filterNot { looksLikeSample(it.path) }
                .sortedByDescending { it.bytes }
            else -> playable
                .filterNot { looksLikeSample(it.path) }
                .sortedByDescending { it.bytes }
        }

        return preferred.take(1).map { it.id }
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

    private fun looksLikeSample(path: String): Boolean {
        val lower = path.lowercase()
        return "sample" in lower || "/extras/" in lower || "/featurettes/" in lower
    }
}

data class ResolvedMagnetStream(
    val streamUrl: String,
    val mimeType: String? = null
)
