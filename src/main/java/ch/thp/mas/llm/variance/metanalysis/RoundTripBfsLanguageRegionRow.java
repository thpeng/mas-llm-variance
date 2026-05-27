package ch.thp.mas.llm.variance.metanalysis;

public record RoundTripBfsLanguageRegionRow(
        String dataset,
        String provider,
        String model,
        String modelVersion,
        String promptLanguage,
        String planName,
        BfsLanguageRegion bfsLanguageRegion,
        String bfsLanguageRegionLabel,
        String destinationsInModelRegion,
        int seriesStationMentionCount,
        double expectedCount,
        int observedCount,
        double expectedProbability,
        double observedProbability,
        double deltaCount,
        double deltaProbability,
        Double observedExpectedRatio,
        String direction
) {
}
