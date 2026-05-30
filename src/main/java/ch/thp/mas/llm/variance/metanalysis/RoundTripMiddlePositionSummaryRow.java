package ch.thp.mas.llm.variance.metanalysis;

public record RoundTripMiddlePositionSummaryRow(
        String seriesId,
        String model,
        int successfulExtractionCount,
        int uniqueRouteCount,
        Double topRouteShare,
        int literalUniqueCount,
        int position1DistinctCount,
        int position2DistinctCount,
        int position3DistinctCount,
        int position4DistinctCount,
        int position5DistinctCount
) {
}
