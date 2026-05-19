package ch.thp.mas.llm.variance.analyze.creative;

import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.util.List;

public record CreativeMarketingTextAnalysis(
        int responseCount,
        int successCount,
        int outlierCount,
        double successShare,
        List<Integer> outliers,
        int expectedSentenceCount,
        String requiredTerm,
        int sentenceCountMismatchCount,
        int requiredTermMissingCount,
        List<CreativeMarketingTextExtraction> extractions,
        SyntacticAnalysis syntactic
) {
}
