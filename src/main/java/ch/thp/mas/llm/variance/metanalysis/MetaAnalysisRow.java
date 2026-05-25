package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;

public record MetaAnalysisRow(
        String seriesId,
        InferenceProvider provider,
        String model,
        String modelVersion,
        PromptEvaluation archetype,
        String promptLanguage,
        String setting,
        Double temperature,
        Double topP,
        Integer topK,
        String seed,
        String reasoning,
        int nRequested,
        int nSuccess,
        int nFailed,
        Double semanticValidRate,
        Double semanticOutlierRate,
        int literalUniqueCount,
        Double literalTop1Share,
        Double largestClusterShare,
        Double medianRougeDistance,
        Double p90RougeDistance,
        Double medianBleuDistance,
        Double p90BleuDistance,
        long inputTokensTotal,
        Double inputTokensP10,
        Double inputTokensMedian,
        Double inputTokensP90,
        long outputTokensTotal,
        Double outputTokensP10,
        Double outputTokensMedian,
        Double outputTokensP90,
        long reasoningTokensTotal,
        Double reasoningTokensP10,
        Double reasoningTokensMedian,
        Double reasoningTokensP90,
        Double durationSecondsTotal,
        Double durationSecondsP10,
        Double durationSecondsMedian,
        Double durationSecondsP90
) {
}
