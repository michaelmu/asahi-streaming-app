package ai.shieldtv.app.integration.scrapers.provider.comet

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class CometQueryBuilder(
    private val rdTokenProvider: () -> String?
) : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        val imdbId = request.mediaRef.ids.imdbId ?: return ProviderRequest(query = request.mediaRef.title)
        val token = rdTokenProvider().orEmpty()
        if (token.isBlank()) return ProviderRequest(query = request.mediaRef.title)

        val paramsJson = JSONObject().apply {
            put("indexers", JSONArray())
            put("maxResults", 0)
            put("maxSize", 0)
            put("resultFormat", JSONArray().put("All"))
            put("resolutions", JSONArray().put("All"))
            put("languages", JSONArray().put("All"))
            put("debridService", "realdebrid")
            put("debridApiKey", token)
            put("debridStreamProxyPassword", "")
        }
        val encoded = Base64.getEncoder().encodeToString(paramsJson.toString().toByteArray(Charsets.UTF_8))
        val path = when (request.mediaRef.mediaType) {
            MediaType.MOVIE -> "/$encoded/stream/movie/$imdbId.json"
            MediaType.SHOW, MediaType.EPISODE, MediaType.SEASON -> {
                val season = request.seasonNumber ?: 1
                val episode = request.episodeNumber ?: 1
                "/$encoded/stream/series/$imdbId:$season:$episode.json"
            }
        }
        return ProviderRequest(
            query = request.mediaRef.title,
            params = mapOf("path" to path)
        )
    }
}
