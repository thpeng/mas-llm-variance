package ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative;

public record LucerneMarketingTextConfig(
        int expectedSentenceCount,
        String requiredTerm
) {

    public LucerneMarketingTextConfig {
        if (expectedSentenceCount < 1) {
            throw new IllegalArgumentException("expectedSentenceCount must be at least 1");
        }
        if (requiredTerm == null || requiredTerm.isBlank()) {
            throw new IllegalArgumentException("requiredTerm must not be blank");
        }
    }
}
