package ch.thp.mas.llm.variance.analyze.creative;

public record CreativeMarketingTextConfig(
        int expectedSentenceCount,
        String requiredTerm
) {

    public CreativeMarketingTextConfig {
        if (expectedSentenceCount < 1) {
            throw new IllegalArgumentException("expectedSentenceCount must be at least 1");
        }
        if (requiredTerm == null || requiredTerm.isBlank()) {
            throw new IllegalArgumentException("requiredTerm must not be blank");
        }
    }
}
