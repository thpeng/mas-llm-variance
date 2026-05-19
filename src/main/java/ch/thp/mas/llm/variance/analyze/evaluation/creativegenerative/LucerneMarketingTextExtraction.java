package ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative;

import java.util.List;

public record LucerneMarketingTextExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        int sentenceCount,
        int expectedSentenceCount,
        boolean containsRequiredTerm,
        String requiredTerm,
        LucerneMarketingTextStatus status,
        List<String> failureReasons
) {
}
