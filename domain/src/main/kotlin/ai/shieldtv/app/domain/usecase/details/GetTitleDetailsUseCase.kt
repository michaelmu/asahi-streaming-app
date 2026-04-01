package ai.shieldtv.app.domain.usecase.details

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.domain.repository.MetadataRepository

class GetTitleDetailsUseCase(
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(mediaRef: MediaRef): TitleDetails {
        return metadataRepository.getTitleDetails(mediaRef)
    }
}
