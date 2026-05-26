package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;

public record CreativeControlQuantileRow(
        String seriesId,
        InferenceProvider provider,
        String model,
        String modelVersion,
        String modelFamily,
        PromptEvaluation archetype,
        String promptLanguage,
        String setting,
        String settingLabel,
        int plotOrder,
        Double temperature,
        Double topP,
        Integer topK,
        String reasoning,
        int nRequested,
        int nSuccess,
        int literalUniqueCount,
        double literalTop1Share,
        double semanticValidRate,
        int pairCount,
        Double p10RougeDistance,
        Double medianRougeDistance,
        Double p90RougeDistance,
        Double p10BleuDistance,
        Double medianBleuDistance,
        Double p90BleuDistance
) {
}
