package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluation;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluation;
import java.time.OffsetDateTime;

public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        SwissRoundTripEvaluation swissRoundTrip,
        BernZurichConnectionEvaluation bernZurichConnection,
        TravelerGuidanceFormatEvaluation travelerGuidanceFormat,
        LucerneMarketingTextEvaluation lucerneMarketingText,
        LiteralAnalysis literal
) {
}
