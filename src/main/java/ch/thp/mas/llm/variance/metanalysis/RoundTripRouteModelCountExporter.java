package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtraction;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtractionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoundTripRouteModelCountExporter {

    public List<RoundTripRouteModelCountRow> exportRows(List<NamedAnalysisResult> analyses) {
        List<AnalysisResult> roundTrips = analyses.stream()
                .map(NamedAnalysisResult::analysisResult)
                .filter(this::isIncludedRoundTrip)
                .toList();

        Map<String, Map<String, Integer>> routeCountsByModel = new TreeMap<>();
        Map<String, String> routeStationsByKey = new TreeMap<>();
        Map<String, Integer> routeCountByModel = new TreeMap<>();
        Map<String, Integer> seriesCountByModel = new TreeMap<>();

        for (AnalysisResult analysis : roundTrips) {
            String model = analysis.run().model();
            Map<String, Integer> routeCounts = routeCountsByModel.computeIfAbsent(model, ignored -> new TreeMap<>());
            seriesCountByModel.merge(model, 1, Integer::sum);
            for (SwissRoundTripExtraction extraction : analysis.swissRoundTrip().extractions()) {
                if (extraction.extractionStatus() != SwissRoundTripExtractionStatus.SUCCESS) {
                    continue;
                }
                String routeKey = routeKey(extraction.normalizedRoundTrip());
                routeCounts.merge(routeKey, 1, Integer::sum);
                routeCountByModel.merge(model, 1, Integer::sum);
                routeStationsByKey.putIfAbsent(routeKey, routeKey);
            }
        }

        List<RoundTripRouteModelCountRow> rows = new ArrayList<>();
        for (String model : routeCountsByModel.keySet()) {
            int modelRouteCount = routeCountByModel.getOrDefault(model, 0);
            routeCountsByModel.get(model).entrySet().stream()
                    .sorted(Comparator
                            .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                            .reversed()
                            .thenComparing(Map.Entry::getKey))
                    .forEach(entry -> rows.add(new RoundTripRouteModelCountRow(
                            model,
                            entry.getKey(),
                            routeStationsByKey.get(entry.getKey()),
                            entry.getValue(),
                            modelRouteCount,
                            share(entry.getValue(), modelRouteCount),
                            seriesCountByModel.getOrDefault(model, 0)
                    )));
        }
        return rows;
    }

    private boolean isIncludedRoundTrip(AnalysisResult analysis) {
        if (analysis.config() == null
                || analysis.config().promptEvaluation() != PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                || analysis.swissRoundTrip() == null
                || analysis.run() == null
                || analysis.run().model() == null) {
            return false;
        }
        return !analysis.run().model().toLowerCase(Locale.ROOT).startsWith("gpt-4o");
    }

    private String routeKey(List<Destination> route) {
        return route.stream()
                .map(Destination::name)
                .collect(Collectors.joining("|"));
    }

    private double share(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }
}
