package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceRankingContribution
import ai.shieldtv.app.domain.source.ranking.SourceRankingExplanation

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

    fun explain(source: SourceResult): SourceRankingExplanation {
        val score = score(source)
        return SourceRankingExplanation(
            totalScore = score.total,
            contributions = score.contributions.map {
                SourceRankingContribution(
                    rule = it.rule,
                    value = it.value,
                    reason = it.reason
                )
            }
        )
    }
}
