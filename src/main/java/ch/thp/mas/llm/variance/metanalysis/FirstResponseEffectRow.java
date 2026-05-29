package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;

public record FirstResponseEffectRow(
        String seriesId,
        InferenceProvider provider,
        String model,
        String modelFamily,
        PromptEvaluation archetype,
        String promptLanguage,
        String setting,
        String reasoning,
        int nRequested,
        int nEntries,
        int nSuccess,
        int nFailed,
        int literalUniqueCount,
        FirstResponseEffectClassification classification,
        int firstResponseCount,
        int dominantResponseCount,
        int restUniqueCount,
        Double firstTtftSeconds,
        Double restTtftSecondsP10,
        Double restTtftSecondsMedian,
        Double restTtftSecondsP90,
        String firstResponseSha256,
        String dominantResponseSha256,
        String variantSummary
) {
}
