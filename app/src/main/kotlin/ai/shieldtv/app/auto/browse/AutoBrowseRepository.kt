package ai.shieldtv.app.auto.browse

import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.core.model.media.MediaType

interface AutoBrowseRepository {
    suspend fun root(): List<AutoBrowseNode>
    suspend fun favorites(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun recent(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun continueWatching(): List<AutoBrowseNode>
    suspend fun search(query: String, mediaType: MediaType?): List<AutoBrowseNode>
}
