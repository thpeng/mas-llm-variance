package ch.thp.mas.llm.variance.analyze.route;

import java.util.List;

public record RouteCluster(
        int clusterId,
        String routeKey,
        List<Destination> route,
        int size,
        List<Integer> repetitionIndices,
        double shareOfSuccessfulExtractions
) {
}
