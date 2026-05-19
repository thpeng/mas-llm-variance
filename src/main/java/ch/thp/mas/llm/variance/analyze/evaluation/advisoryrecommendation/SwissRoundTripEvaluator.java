package ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SwissRoundTripEvaluator {

    private final SwissRoundTripStationExtractor extractor;

    public SwissRoundTripEvaluator(SwissRoundTripStationExtractor extractor) {
        this.extractor = extractor;
    }

    public SwissRoundTripEvaluation analyze(List<String> responses, SwissRoundTripConfig config, SyntacticAnalysisFactory syntacticFactory) {
        List<SwissRoundTripExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config.expectedStationCount()));
        }

        List<SwissRoundTripExtraction> successful = extractions.stream()
                .filter(extraction -> extraction.extractionStatus() == SwissRoundTripExtractionStatus.SUCCESS)
                .toList();
        Map<String, List<SwissRoundTripExtraction>> byRoundTrip = new LinkedHashMap<>();
        for (SwissRoundTripExtraction extraction : successful) {
            byRoundTrip.computeIfAbsent(roundTripKey(extraction.normalizedRoundTrip()), ignored -> new ArrayList<>()).add(extraction);
        }

        List<SwissRoundTripCluster> clusters = clusters(byRoundTrip, successful.size());
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.extractionStatus() != SwissRoundTripExtractionStatus.SUCCESS)
                .map(SwissRoundTripExtraction::responseIndex)
                .toList();

        return new SwissRoundTripEvaluation(
                responses.size(),
                successful.size(),
                count(extractions, SwissRoundTripExtractionStatus.PARTIAL),
                count(extractions, SwissRoundTripExtractionStatus.FAILED),
                extractions.stream().mapToInt(extraction -> extraction.unknownNames().size()).sum(),
                byRoundTrip.size(),
                clusters.stream().map(SwissRoundTripCluster::shareOfSuccessfulExtractions).max(Double::compareTo).orElse(null),
                extractions,
                clusters,
                outliers,
                stationFrequencies(successful),
                positionDistributions(successful, config.expectedStationCount()),
                jaccard(successful),
                syntacticFactory.create(clusters)
        );
    }

    private SwissRoundTripExtraction extract(int responseIndex, String response, int expectedStationCount) {
        List<String> rawNames = extractor.extract(response);
        List<Destination> normalized = new ArrayList<>();
        List<String> unknownNames = new ArrayList<>();
        for (String rawName : rawNames) {
            Destination.fromRawName(rawName).ifPresentOrElse(normalized::add, () -> unknownNames.add(rawName));
        }
        SwissRoundTripExtractionStatus status;
        if (rawNames.isEmpty()) {
            status = SwissRoundTripExtractionStatus.FAILED;
        } else if (rawNames.size() == expectedStationCount && unknownNames.isEmpty()) {
            status = SwissRoundTripExtractionStatus.SUCCESS;
        } else {
            status = SwissRoundTripExtractionStatus.PARTIAL;
        }
        return new SwissRoundTripExtraction(responseIndex, response, rawNames, normalized, status, unknownNames);
    }

    private List<SwissRoundTripCluster> clusters(Map<String, List<SwissRoundTripExtraction>> byRoundTrip, int successfulCount) {
        List<SwissRoundTripCluster> clusters = new ArrayList<>();
        int clusterId = 0;
        for (Map.Entry<String, List<SwissRoundTripExtraction>> entry : byRoundTrip.entrySet()) {
            List<SwissRoundTripExtraction> extractions = entry.getValue();
            clusters.add(new SwissRoundTripCluster(
                    clusterId++,
                    entry.getKey(),
                    extractions.getFirst().normalizedRoundTrip(),
                    extractions.size(),
                    extractions.stream().map(SwissRoundTripExtraction::responseIndex).toList(),
                    successfulCount == 0 ? 0.0 : (double) extractions.size() / successfulCount
            ));
        }
        return clusters;
    }

    private List<StationFrequency> stationFrequencies(List<SwissRoundTripExtraction> successful) {
        Map<Destination, Integer> counts = new EnumMap<>(Destination.class);
        for (SwissRoundTripExtraction extraction : successful) {
            Set<Destination> roundTripSet = new HashSet<>(extraction.normalizedRoundTrip());
            for (Destination destination : roundTripSet) {
                counts.merge(destination, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new StationFrequency(
                        entry.getKey(),
                        entry.getValue(),
                        successful.isEmpty() ? 0.0 : (double) entry.getValue() / successful.size()))
                .toList();
    }

    private List<PositionDistribution> positionDistributions(List<SwissRoundTripExtraction> successful, int expectedStationCount) {
        List<PositionDistribution> distributions = new ArrayList<>();
        for (int position = 0; position < expectedStationCount; position++) {
            Map<Destination, Integer> counts = new EnumMap<>(Destination.class);
            for (SwissRoundTripExtraction extraction : successful) {
                if (extraction.normalizedRoundTrip().size() > position) {
                    counts.merge(extraction.normalizedRoundTrip().get(position), 1, Integer::sum);
                }
            }
            List<PositionDestinationFrequency> frequencies = counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new PositionDestinationFrequency(entry.getKey(), entry.getValue()))
                    .toList();
            distributions.add(new PositionDistribution(position + 1, frequencies));
        }
        return distributions;
    }

    private JaccardSummary jaccard(List<SwissRoundTripExtraction> successful) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < successful.size(); i++) {
            for (int j = i + 1; j < successful.size(); j++) {
                values.add(jaccard(successful.get(i).normalizedRoundTrip(), successful.get(j).normalizedRoundTrip()));
            }
        }
        if (values.isEmpty()) {
            return new JaccardSummary(0, null, null, null, null, null, null);
        }
        values.sort(Comparator.naturalOrder());
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return new JaccardSummary(
                values.size(),
                values.getFirst(),
                nearestRank(values, 0.1),
                median(values),
                nearestRank(values, 0.9),
                values.getLast(),
                sum / values.size()
        );
    }

    private double jaccard(List<Destination> left, List<Destination> right) {
        Set<Destination> leftSet = new HashSet<>(left);
        Set<Destination> rightSet = new HashSet<>(right);
        Set<Destination> intersection = new HashSet<>(leftSet);
        intersection.retainAll(rightSet);
        Set<Destination> union = new HashSet<>(leftSet);
        union.addAll(rightSet);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private Double median(List<Double> sorted) {
        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private Double nearestRank(List<Double> sorted, double percentile) {
        int rank = (int) Math.ceil(percentile * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }

    private int count(List<SwissRoundTripExtraction> extractions, SwissRoundTripExtractionStatus status) {
        return (int) extractions.stream()
                .filter(extraction -> extraction.extractionStatus() == status)
                .count();
    }

    private String roundTripKey(List<Destination> swissRoundTrip) {
        return String.join("|", swissRoundTrip.stream().map(Destination::name).toList());
    }

    @FunctionalInterface
    public interface SyntacticAnalysisFactory {
        ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis create(List<SwissRoundTripCluster> clusters);
    }
}
