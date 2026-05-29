package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtraction;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripExtractionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoundTripLanguageDestinationExporter {

    private static final List<PromptLanguage> LANGUAGES = List.of(
            PromptLanguage.DE,
            PromptLanguage.FR,
            PromptLanguage.IT,
            PromptLanguage.EN
    );

    public RoundTripLanguageDestinationExport exportRows(List<NamedAnalysisResult> analyses) {
        List<Series> series = analyses.stream()
                .map(NamedAnalysisResult::analysisResult)
                .filter(this::isIncludedRoundTripBaseline)
                .map(this::series)
                .sorted(Comparator.comparing(Series::model).thenComparing(Series::promptLanguage))
                .toList();

        Map<String, Map<Destination, Integer>> modelDestinationCounts = modelDestinationCounts(series);
        Map<String, Integer> modelMentionCounts = modelMentionCounts(series);

        List<RoundTripLanguageDestinationRow> destinationRows = destinationRows(
                series,
                modelDestinationCounts,
                modelMentionCounts
        );
        List<RoundTripBfsLanguageRegionRow> bfsRows = bfsRows(series, destinationRows);

        return new RoundTripLanguageDestinationExport(
                destinationRows,
                bfsRows
        );
    }

    private boolean isIncludedRoundTripBaseline(AnalysisResult analysis) {
        if (analysis.config() == null
                || analysis.config().promptEvaluation() != PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                || analysis.config().swissRoundTrip() == null
                || analysis.swissRoundTrip() == null) {
            return false;
        }
        PromptLanguage language = analysis.config().swissRoundTrip().language();
        if (!LANGUAGES.contains(language)) {
            return false;
        }

        String planName = analysis.run().planName();
        String lower = planName.toLowerCase(Locale.ROOT);
        return lower.contains("roundtrip-" + language.name().toLowerCase(Locale.ROOT))
                && lower.contains("baseline")
                && !lower.contains("mittel")
                && !lower.contains("seed")
                && !lower.contains("reasoning-medium")
                && !lower.contains("reasoning-high");
    }

    private Series series(AnalysisResult analysis) {
        EnumMap<Destination, Integer> counts = new EnumMap<>(Destination.class);
        int mentionCount = 0;
        for (SwissRoundTripExtraction extraction : analysis.swissRoundTrip().extractions()) {
            if (extraction.extractionStatus() != SwissRoundTripExtractionStatus.SUCCESS) {
                continue;
            }
            for (Destination destination : extraction.normalizedRoundTrip()) {
                counts.merge(destination, 1, Integer::sum);
                mentionCount++;
            }
        }
        return new Series(
                dataset(analysis.sourceRun()),
                analysis.run().inferenceProvider().name(),
                analysis.run().model(),
                analysis.run().modelVersion(),
                analysis.config().swissRoundTrip().language().name(),
                analysis.run().planName(),
                counts,
                mentionCount
        );
    }

    private Map<String, Map<Destination, Integer>> modelDestinationCounts(List<Series> series) {
        Map<String, Map<Destination, Integer>> result = new java.util.TreeMap<>();
        for (Series item : series) {
            Map<Destination, Integer> counts = result.computeIfAbsent(item.model(), ignored -> new EnumMap<>(Destination.class));
            item.destinationCounts().forEach((destination, count) -> counts.merge(destination, count, Integer::sum));
        }
        return result;
    }

    private Map<String, Integer> modelMentionCounts(List<Series> series) {
        Map<String, Integer> result = new java.util.TreeMap<>();
        for (Series item : series) {
            result.merge(item.model(), item.stationMentionCount(), Integer::sum);
        }
        return result;
    }

    private List<RoundTripLanguageDestinationRow> destinationRows(
            List<Series> series,
            Map<String, Map<Destination, Integer>> modelDestinationCounts,
            Map<String, Integer> modelMentionCounts
    ) {
        List<RoundTripLanguageDestinationRow> rows = new ArrayList<>();
        for (Series item : series) {
            Map<Destination, Integer> modelCounts = modelDestinationCounts.get(item.model());
            int modelMentionCount = modelMentionCounts.get(item.model());
            List<Destination> destinations = modelCounts.keySet().stream()
                    .sorted(Comparator.comparing(Destination::name))
                    .toList();
            for (Destination destination : destinations) {
                int observedCount = item.destinationCounts().getOrDefault(destination, 0);
                int modelCount = modelCounts.get(destination);
                double expectedProbability = share(modelCount, modelMentionCount);
                double expectedCount = expectedProbability * item.stationMentionCount();
                double observedProbability = share(observedCount, item.stationMentionCount());
                double deltaCount = observedCount - expectedCount;
                double deltaProbability = observedProbability - expectedProbability;
                rows.add(new RoundTripLanguageDestinationRow(
                        item.dataset(),
                        item.provider(),
                        item.model(),
                        item.modelVersion(),
                        item.promptLanguage(),
                        item.planName(),
                        destination,
                        modelCount,
                        modelMentionCount,
                        expectedProbability,
                        item.stationMentionCount(),
                        expectedCount,
                        observedCount,
                        observedProbability,
                        deltaCount,
                        deltaProbability,
                        expectedCount == 0 ? null : observedCount / expectedCount,
                        direction(deltaCount)
                ));
            }
        }
        return rows;
    }

    private List<RoundTripBfsLanguageRegionRow> bfsRows(
            List<Series> series,
            List<RoundTripLanguageDestinationRow> destinationRows
    ) {
        List<RoundTripBfsLanguageRegionRow> rows = new ArrayList<>();
        for (Series item : series) {
            List<RoundTripLanguageDestinationRow> itemRows = destinationRows.stream()
                    .filter(row -> row.model().equals(item.model())
                            && row.promptLanguage().equals(item.promptLanguage())
                            && row.planName().equals(item.planName()))
                    .toList();
            for (BfsLanguageRegion region : BfsLanguageRegion.values()) {
                List<RoundTripLanguageDestinationRow> regionRows = itemRows.stream()
                        .filter(row -> RoundTripBfsDestinationLanguageRegion.region(row.destination()) == region)
                        .toList();
                double expectedCount = regionRows.stream().mapToDouble(RoundTripLanguageDestinationRow::expectedCount).sum();
                int observedCount = regionRows.stream().mapToInt(RoundTripLanguageDestinationRow::observedCount).sum();
                double expectedProbability = share(expectedCount, item.stationMentionCount());
                double observedProbability = share(observedCount, item.stationMentionCount());
                double deltaCount = observedCount - expectedCount;
                double deltaProbability = observedProbability - expectedProbability;
                rows.add(new RoundTripBfsLanguageRegionRow(
                        item.dataset(),
                        item.provider(),
                        item.model(),
                        item.modelVersion(),
                        item.promptLanguage(),
                        item.planName(),
                        region,
                        region.label(),
                        destinations(regionRows),
                        item.stationMentionCount(),
                        expectedCount,
                        observedCount,
                        expectedProbability,
                        observedProbability,
                        deltaCount,
                        deltaProbability,
                        expectedCount == 0 ? null : observedCount / expectedCount,
                        direction(deltaCount)
                ));
            }
        }
        return rows;
    }

    private String destinations(List<RoundTripLanguageDestinationRow> rows) {
        return rows.stream()
                .map(row -> row.destination().name())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String direction(double deltaCount) {
        if (deltaCount > 0.000001) {
            return "OVER";
        }
        if (deltaCount < -0.000001) {
            return "UNDER";
        }
        return "AS_EXPECTED";
    }

    private double share(double numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    private String dataset(String sourceRun) {
        if (sourceRun == null || sourceRun.isBlank()) {
            return "";
        }
        int separator = sourceRun.indexOf('/');
        return separator < 0 ? sourceRun : sourceRun.substring(0, separator);
    }

    private record Series(
            String dataset,
            String provider,
            String model,
            String modelVersion,
            String promptLanguage,
            String planName,
            Map<Destination, Integer> destinationCounts,
            int stationMentionCount
    ) {
    }
}
