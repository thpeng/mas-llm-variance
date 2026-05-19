package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.factual.FactualTravelInfoAnalysis;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.route.RouteAnalysis;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        RouteAnalysis route,
        FactualTravelInfoAnalysis factualTravelInfo,
        LiteralAnalysis literal
) {
}
