package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.SourceResult

data class SourceScoreContribution(
    val rule: String,
    val value: Long,
    val reason: String
)

data class SourceScore(
    val total: Long,
    val contributions: List<SourceScoreContribution>
)

fun interface SourceScoreRule {
    fun evaluate(source: SourceResult): SourceScoreContribution?
}

internal class SourceScorer(
    private val rules: List<SourceScoreRule>
) {
    fun score(source: SourceResult): SourceScore {
        val contributions = rules.mapNotNull { it.evaluate(source) }
        return SourceScore(
            total = contributions.sumOf { it.value },
            contributions = contributions
        )
    }
}
