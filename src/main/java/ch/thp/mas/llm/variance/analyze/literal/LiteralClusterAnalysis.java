package ch.thp.mas.llm.variance.analyze.literal;

public record LiteralClusterAnalysis(
        int clusterId,
        int responseCount,
        int pairCount,
        int exactMatchPairCount,
        double exactMatchRate,
        int distinctResponseCount
) {
}
