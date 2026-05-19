package ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical;

import java.util.List;
import java.util.Map;

public record TravelerGuidanceFormatEvaluation(
        int responseCount,
        int exactMatchCount,
        int normalizedExactMatchCount,
        int noMatchCount,
        double exactMatchShare,
        double normalizedAcceptedShare,
        List<Integer> outliers,
        Map<String, Integer> forbiddenTemplateTermCounts,
        int additionalSentenceCandidateCount,
        List<TravelerGuidanceFormatExtraction> extractions
) {
}
