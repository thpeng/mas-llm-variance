package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;

public record BaselineScatterRow(
        String seriesId,
        InferenceProvider provider,
        String model,
        String modelFamily,
        int modelFamilyId,
        PromptEvaluation archetype,
        int archetypeId,
        String promptLanguage,
        String setting,
        int nRequested,
        int nSuccess,
        int literalUniqueCount,
        Double literalUniqueShare,
        Double semanticValidRate,
        Double plotLiteralUniqueCount,
        Double plotSemanticValidRate
) {
}
