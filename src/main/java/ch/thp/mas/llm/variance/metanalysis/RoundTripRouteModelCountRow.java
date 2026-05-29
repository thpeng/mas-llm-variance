package ch.thp.mas.llm.variance.metanalysis;

public record RoundTripRouteModelCountRow(
        String model,
        String routeKey,
        String routeStations,
        int mentionCount,
        int modelRouteCount,
        double shareOfModelRoutes,
        int contributingSeriesCount
) {
}
