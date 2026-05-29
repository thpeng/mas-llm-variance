package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtraction;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtractionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class RoundTripStationModelCountExporter {

    public List<RoundTripStationModelCountRow> exportRows(List<NamedAnalysisResult> analyses) {
        List<AnalysisResult> roundTrips = analyses.stream()
                .map(NamedAnalysisResult::analysisResult)
                .filter(this::isIncludedRoundTrip)
                .toList();

        Map<String, EnumMap<Destination, Integer>> countsByModel = new TreeMap<>();
        Map<String, Integer> mentionCountByModel = new TreeMap<>();
        Map<String, Integer> seriesCountByModel = new TreeMap<>();
        java.util.Set<Destination> observedDestinations = new java.util.TreeSet<>(Comparator.comparing(Destination::name));

        for (AnalysisResult analysis : roundTrips) {
            String model = analysis.run().model();
            EnumMap<Destination, Integer> counts = countsByModel.computeIfAbsent(model, ignored -> new EnumMap<>(Destination.class));
            seriesCountByModel.merge(model, 1, Integer::sum);
            for (SwissRoundTripExtraction extraction : analysis.swissRoundTrip().extractions()) {
                if (extraction.extractionStatus() != SwissRoundTripExtractionStatus.SUCCESS) {
                    continue;
                }
                for (Destination destination : extraction.normalizedRoundTrip()) {
                    counts.merge(destination, 1, Integer::sum);
                    mentionCountByModel.merge(model, 1, Integer::sum);
                    observedDestinations.add(destination);
                }
            }
        }

        List<RoundTripStationModelCountRow> rows = new ArrayList<>();
        for (String model : countsByModel.keySet()) {
            int modelMentionCount = mentionCountByModel.getOrDefault(model, 0);
            for (Destination destination : observedDestinations) {
                int count = countsByModel.get(model).getOrDefault(destination, 0);
                rows.add(new RoundTripStationModelCountRow(
                        model,
                        destination,
                        count,
                        modelMentionCount,
                        share(count, modelMentionCount),
                        seriesCountByModel.getOrDefault(model, 0)
                ));
            }
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

    private double share(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }
}
