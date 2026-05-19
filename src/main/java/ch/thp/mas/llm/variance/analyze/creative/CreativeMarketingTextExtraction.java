package ch.thp.mas.llm.variance.analyze.creative;

import java.util.List;

public record CreativeMarketingTextExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        int sentenceCount,
        int expectedSentenceCount,
        boolean containsRequiredTerm,
        String requiredTerm,
        CreativeMarketingTextStatus status,
        List<String> failureReasons
) {
}
