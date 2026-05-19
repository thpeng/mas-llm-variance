package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

public record StationFrequency(
        Destination destination,
        int count,
        double shareOfSuccessfulExtractions
) {
}
