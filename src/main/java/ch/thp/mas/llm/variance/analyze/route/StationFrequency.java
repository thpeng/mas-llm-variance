package ch.thp.mas.llm.variance.analyze.route;

public record StationFrequency(
        Destination destination,
        int count,
        double shareOfSuccessfulExtractions
) {
}
