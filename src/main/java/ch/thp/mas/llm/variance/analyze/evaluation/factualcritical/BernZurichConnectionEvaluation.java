package ch.thp.mas.llm.variance.analyze.evaluation.factualcritical;

import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.util.List;
import java.util.Map;

public record BernZurichConnectionEvaluation(
        int responseCount,
        int successCount,
        int outlierCount,
        double successShare,
        List<Integer> outliers,
        int departureFoundCount,
        int arrivalFoundCount,
        int changesFoundCount,
        Map<String, Integer> extraTimeCounts,
        List<BernZurichConnectionExtraction> extractions,
        SyntacticAnalysis syntactic
) {
}
