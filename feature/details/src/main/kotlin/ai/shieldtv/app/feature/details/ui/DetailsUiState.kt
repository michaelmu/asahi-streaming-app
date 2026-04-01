package ai.shieldtv.app.feature.details.ui

import ai.shieldtv.app.core.model.media.TitleDetails

data class DetailsUiState(
    val loading: Boolean = false,
    val item: TitleDetails? = null,
    val error: String? = null
)
