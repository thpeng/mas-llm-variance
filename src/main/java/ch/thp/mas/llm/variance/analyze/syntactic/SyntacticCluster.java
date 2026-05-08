package ch.thp.mas.llm.variance.analyze.syntactic;


import ch.thp.mas.llm.variance.analyze.MetricSummary;
public record SyntacticCluster(
        int clusterId,
        int pairCount,
        MetricSummary rougeLDistance,
        MetricSummary bleuDistance
) {
}
