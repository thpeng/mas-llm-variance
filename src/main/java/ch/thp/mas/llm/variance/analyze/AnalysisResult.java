package ch.thp.mas.llm.variance.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluation;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.evaluation.literalformatcritical.TravelerGuidanceFormatEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluation;
import ch.thp.mas.llm.variance.run.ExecutionEnvironmentLog;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        ExecutionEnvironmentLog environment,
        AnalysisConfig config,
        AnalysisRunInfo run,
        SwissRoundTripEvaluation swissRoundTrip,
        BernZurichConnectionEvaluation bernZurichConnection,
        TravelerGuidanceFormatEvaluation travelerGuidanceFormat,
        LucerneMarketingTextEvaluation lucerneMarketingText,
        LiteralAnalysis literal
) {

    public AnalysisResult(
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
        this(sourceRun, analyzedAt, null, config, run, swissRoundTrip, bernZurichConnection, travelerGuidanceFormat,
                lucerneMarketingText, literal);
    }
}
