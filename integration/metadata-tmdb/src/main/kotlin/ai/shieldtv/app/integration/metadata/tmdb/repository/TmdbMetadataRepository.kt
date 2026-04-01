package ai.shieldtv.app.integration.metadata.tmdb.repository

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.domain.repository.MetadataRepository

class TmdbMetadataRepository : MetadataRepository {
    override suspend fun search(query: String): List<SearchResult> {
        return emptyList()
    }

    override suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails {
        return TitleDetails(mediaRef = mediaRef)
    }
}
