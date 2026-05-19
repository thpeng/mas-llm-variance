package ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative;

import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.util.List;

public record LucerneMarketingTextEvaluation(
        int responseCount,
        int successCount,
        int outlierCount,
        double successShare,
        List<Integer> outliers,
        int expectedSentenceCount,
        String requiredTerm,
        int sentenceCountMismatchCount,
        int requiredTermMissingCount,
        List<LucerneMarketingTextExtraction> extractions,
        SyntacticAnalysis syntactic
) {
}
