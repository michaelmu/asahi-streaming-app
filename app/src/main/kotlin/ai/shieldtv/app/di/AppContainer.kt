package ai.shieldtv.app.di

import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.integration.metadata.tmdb.repository.TmdbMetadataRepository

object AppContainer {
    private val metadataRepository by lazy { TmdbMetadataRepository() }

    val searchTitlesUseCase by lazy {
        SearchTitlesUseCase(metadataRepository)
    }
}
