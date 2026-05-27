package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;

public record RoundTripLanguageDestinationRow(
        String dataset,
        String provider,
        String model,
        String modelVersion,
        String promptLanguage,
        String planName,
        Destination destination,
        int modelCount,
        int modelStationMentionCount,
        double expectedProbability,
        int seriesStationMentionCount,
        double expectedCount,
        int observedCount,
        double observedProbability,
        double deltaCount,
        double deltaProbability,
        Double observedExpectedRatio,
        String direction
) {
}
