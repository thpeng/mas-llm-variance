package ch.thp.mas.llm.variance.analyze.literal;

import java.util.List;

public record LiteralAnalysis(
        boolean allResponsesIdentical,
        int responseCount,
        int distinctResponseCount,
        double exactMatchRate,
        List<LiteralClusterAnalysis> clusters
) {
}
