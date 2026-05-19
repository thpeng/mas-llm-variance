package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.List;

public record SwissRoundTripCluster(
        int clusterId,
        String roundTripKey,
        List<Destination> swissRoundTrip,
        int size,
        List<Integer> repetitionIndices,
        double shareOfSuccessfulExtractions
) {
}
