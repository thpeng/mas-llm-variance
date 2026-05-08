package ch.thp.mas.llm.variance.analyze;


import ch.thp.mas.llm.variance.analyze.semantic.SemanticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        SemanticAnalysis semantic,
        SyntacticAnalysis syntactic
) {
}
