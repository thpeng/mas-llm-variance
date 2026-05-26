package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.client.InferenceProvider;

public record WordDriftRow(
        String seriesId,
        InferenceProvider provider,
        String model,
        String modelVersion,
        int nSuccess,
        int literalDistinctResponseCount,
        Double meanClusterSize,
        Double meanRougeDistance,
        Double meanBleuDistance,
        int distinctWordCount,
        Double meanWordsPerResponse,
        Integer p10WordsPerResponse,
        Integer p90WordsPerResponse,
        String distinctWords
) {
}
