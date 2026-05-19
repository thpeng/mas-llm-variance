package ch.thp.mas.llm.variance.analyze.route;

import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.util.List;

public record RouteAnalysis(
        int responseCount,
        int successfulExtractionCount,
        int partialExtractionCount,
        int failedExtractionCount,
        int unknownNameCount,
        int uniqueRouteCount,
        Double topRouteShare,
        List<RouteExtraction> extractions,
        List<RouteCluster> clusters,
        List<Integer> outliers,
        List<StationFrequency> stationFrequencies,
        List<PositionDistribution> positionDistributions,
        JaccardSummary pairwiseJaccard,
        SyntacticAnalysis syntactic
) {
}
