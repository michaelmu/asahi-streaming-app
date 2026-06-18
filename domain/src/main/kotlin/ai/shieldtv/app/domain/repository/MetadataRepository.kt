package ai.codexa.app.domain.repository

import ai.codexa.app.core.model.media.MediaRef

interface MetadataRepository {
    suspend fun search(query: String): List<MediaRef>
    suspend fun getTitleDetails(mediaRef: MediaRef): MediaRef
}
