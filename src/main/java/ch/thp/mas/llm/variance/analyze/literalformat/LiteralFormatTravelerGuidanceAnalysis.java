package ch.thp.mas.llm.variance.analyze.literalformat;

import java.util.List;
import java.util.Map;

public record LiteralFormatTravelerGuidanceAnalysis(
        int responseCount,
        int exactMatchCount,
        int normalizedExactMatchCount,
        int noMatchCount,
        double exactMatchShare,
        double normalizedAcceptedShare,
        List<Integer> outliers,
        Map<String, Integer> forbiddenTemplateTermCounts,
        int additionalSentenceCandidateCount,
        List<LiteralFormatTravelerGuidanceExtraction> extractions
) {
}
