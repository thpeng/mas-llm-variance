package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PositionDistribution;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoundTripMiddlePositionSummaryExporter {

    private static final List<String> INCLUDED_SERIES = List.of(
            "0301-lmstudio-apertus-roundtrip-de-mittel",
            "0401-lmstudio-qwen35-9b-roundtrip-de-mittel",
            "0201-google-gemini35flash-roundtrip-de-mittel",
            "0101-anthropic-sonnet46-roundtrip-de-mittel",
            "0005-openai-gpt54mini-roundtrip-de-mittel",
            "0501-lmstudio-gptoss20b-roundtrip-de-reasoning-low-mittel"
    );
    private static final Set<String> INCLUDED_SERIES_SET = Set.copyOf(INCLUDED_SERIES);

    public List<RoundTripMiddlePositionSummaryRow> exportRows(List<NamedAnalysisResult> analyses) {
        return analyses.stream()
                .map(NamedAnalysisResult::analysisResult)
                .filter(this::isIncluded)
                .map(this::toRow)
                .sorted(Comparator.comparingInt(row -> INCLUDED_SERIES.indexOf(row.seriesId())))
                .toList();
    }

    private boolean isIncluded(AnalysisResult analysis) {
        return analysis.config() != null
                && analysis.config().promptEvaluation() == PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                && analysis.run() != null
                && analysis.swissRoundTrip() != null
                && INCLUDED_SERIES_SET.contains(analysis.run().planName());
    }

    private RoundTripMiddlePositionSummaryRow toRow(AnalysisResult analysis) {
        Map<Integer, Integer> positionDistinctCounts = analysis.swissRoundTrip().positionDistributions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PositionDistribution::position,
                        position -> position.frequencies().size()
                ));
        return new RoundTripMiddlePositionSummaryRow(
                analysis.run().planName(),
                analysis.run().model(),
                analysis.swissRoundTrip().successfulExtractionCount(),
                analysis.swissRoundTrip().uniqueRoundTripCount(),
                analysis.swissRoundTrip().topRoundTripShare(),
                analysis.literal() == null ? 0 : analysis.literal().distinctResponseCount(),
                positionDistinctCounts.getOrDefault(1, 0),
                positionDistinctCounts.getOrDefault(2, 0),
                positionDistinctCounts.getOrDefault(3, 0),
                positionDistinctCounts.getOrDefault(4, 0),
                positionDistinctCounts.getOrDefault(5, 0)
        );
    }
}
