package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;

public record DeBaselineArchetypeVarianceRow(
        PromptEvaluation archetype,
        String archetypeLabel,
        String promptLanguage,
        String setting,
        int seriesCount,
        double meanLiteralUniqueCount,
        double medianLiteralUniqueCount,
        int maxLiteralUniqueCount
) {
}
