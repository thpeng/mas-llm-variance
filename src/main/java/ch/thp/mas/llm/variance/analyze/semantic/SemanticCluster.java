package ch.thp.mas.llm.variance.analyze.semantic;


import ch.thp.mas.llm.variance.analyze.MetricSummary;
import java.util.List;

public record SemanticCluster(
        int clusterId,
        int size,
        List<Integer> repetitionIndices,
        Integer medoidRepetitionIndex,
        MetricSummary pairwiseCosineDistance
) {
}
