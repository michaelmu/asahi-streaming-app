package ai.shieldtv.app.integration.debrid.realdebrid.api

data class RealDebridTorrentAddResponse(
    val id: String,
    val uri: String? = null
)

data class RealDebridTorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int
)

data class RealDebridTorrentInfo(
    val id: String,
    val filename: String,
    val status: String,
    val links: List<String>,
    val files: List<RealDebridTorrentFile>
)

data class RealDebridUnrestrictedLink(
    val download: String,
    val filename: String? = null,
    val mimeType: String? = null
)
