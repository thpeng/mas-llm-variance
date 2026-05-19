package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.util.List;

public record SwissRoundTripEvaluation(
        int responseCount,
        int successfulExtractionCount,
        int partialExtractionCount,
        int failedExtractionCount,
        int unknownNameCount,
        int uniqueRoundTripCount,
        Double topRoundTripShare,
        List<SwissRoundTripExtraction> extractions,
        List<SwissRoundTripCluster> clusters,
        List<Integer> outliers,
        List<StationFrequency> stationFrequencies,
        List<PositionDistribution> positionDistributions,
        JaccardSummary pairwiseJaccard,
        SyntacticAnalysis syntactic
) {
}
