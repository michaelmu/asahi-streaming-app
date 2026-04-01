package ai.shieldtv.app.domain.repository

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails

interface MetadataRepository {
    suspend fun search(query: String): List<SearchResult>
    suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails
}
