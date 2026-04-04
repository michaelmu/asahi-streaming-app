package ai.shieldtv.app.domain.source.ranking

data class SourceRankingContribution(
    val rule: String,
    val value: Long,
    val reason: String
)

data class SourceRankingExplanation(
    val totalScore: Long,
    val contributions: List<SourceRankingContribution>
)
