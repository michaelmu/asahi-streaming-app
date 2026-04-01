package ai.shieldtv.app.core.model.source

data class SourceFilters(
    val allowedQualities: Set<Quality> = emptySet(),
    val requireCachedOnly: Boolean = false
)
