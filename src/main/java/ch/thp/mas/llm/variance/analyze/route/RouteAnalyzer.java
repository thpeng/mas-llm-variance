package ch.thp.mas.llm.variance.analyze.route;

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
public class RouteAnalyzer {

    private final RouteStationExtractor extractor;

    public RouteAnalyzer(RouteStationExtractor extractor) {
        this.extractor = extractor;
    }

    public RouteAnalysis analyze(List<String> responses, RouteConfig config, RouteSyntacticAnalysisFactory syntacticFactory) {
        List<RouteExtraction> extractions = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            extractions.add(extract(i + 1, responses.get(i), config.expectedStationCount()));
        }

        List<RouteExtraction> successful = extractions.stream()
                .filter(extraction -> extraction.extractionStatus() == RouteExtractionStatus.SUCCESS)
                .toList();
        Map<String, List<RouteExtraction>> byRoute = new LinkedHashMap<>();
        for (RouteExtraction extraction : successful) {
            byRoute.computeIfAbsent(routeKey(extraction.normalizedRoute()), ignored -> new ArrayList<>()).add(extraction);
        }

        List<RouteCluster> clusters = clusters(byRoute, successful.size());
        List<Integer> outliers = extractions.stream()
                .filter(extraction -> extraction.extractionStatus() != RouteExtractionStatus.SUCCESS)
                .map(RouteExtraction::responseIndex)
                .toList();

        return new RouteAnalysis(
                responses.size(),
                successful.size(),
                count(extractions, RouteExtractionStatus.PARTIAL),
                count(extractions, RouteExtractionStatus.FAILED),
                extractions.stream().mapToInt(extraction -> extraction.unknownNames().size()).sum(),
                byRoute.size(),
                clusters.stream().map(RouteCluster::shareOfSuccessfulExtractions).max(Double::compareTo).orElse(null),
                extractions,
                clusters,
                outliers,
                stationFrequencies(successful),
                positionDistributions(successful, config.expectedStationCount()),
                jaccard(successful),
                syntacticFactory.create(clusters)
        );
    }

    private RouteExtraction extract(int responseIndex, String response, int expectedStationCount) {
        List<String> rawNames = extractor.extract(response);
        List<Destination> normalized = new ArrayList<>();
        List<String> unknownNames = new ArrayList<>();
        for (String rawName : rawNames) {
            Destination.fromRawName(rawName).ifPresentOrElse(normalized::add, () -> unknownNames.add(rawName));
        }
        RouteExtractionStatus status;
        if (rawNames.isEmpty()) {
            status = RouteExtractionStatus.FAILED;
        } else if (rawNames.size() == expectedStationCount && unknownNames.isEmpty()) {
            status = RouteExtractionStatus.SUCCESS;
        } else {
            status = RouteExtractionStatus.PARTIAL;
        }
        return new RouteExtraction(responseIndex, response, rawNames, normalized, status, unknownNames);
    }

    private List<RouteCluster> clusters(Map<String, List<RouteExtraction>> byRoute, int successfulCount) {
        List<RouteCluster> clusters = new ArrayList<>();
        int clusterId = 0;
        for (Map.Entry<String, List<RouteExtraction>> entry : byRoute.entrySet()) {
            List<RouteExtraction> extractions = entry.getValue();
            clusters.add(new RouteCluster(
                    clusterId++,
                    entry.getKey(),
                    extractions.getFirst().normalizedRoute(),
                    extractions.size(),
                    extractions.stream().map(RouteExtraction::responseIndex).toList(),
                    successfulCount == 0 ? 0.0 : (double) extractions.size() / successfulCount
            ));
        }
        return clusters;
    }

    private List<StationFrequency> stationFrequencies(List<RouteExtraction> successful) {
        Map<Destination, Integer> counts = new EnumMap<>(Destination.class);
        for (RouteExtraction extraction : successful) {
            Set<Destination> routeSet = new HashSet<>(extraction.normalizedRoute());
            for (Destination destination : routeSet) {
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

    private List<PositionDistribution> positionDistributions(List<RouteExtraction> successful, int expectedStationCount) {
        List<PositionDistribution> distributions = new ArrayList<>();
        for (int position = 0; position < expectedStationCount; position++) {
            Map<Destination, Integer> counts = new EnumMap<>(Destination.class);
            for (RouteExtraction extraction : successful) {
                if (extraction.normalizedRoute().size() > position) {
                    counts.merge(extraction.normalizedRoute().get(position), 1, Integer::sum);
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

    private JaccardSummary jaccard(List<RouteExtraction> successful) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < successful.size(); i++) {
            for (int j = i + 1; j < successful.size(); j++) {
                values.add(jaccard(successful.get(i).normalizedRoute(), successful.get(j).normalizedRoute()));
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

    private int count(List<RouteExtraction> extractions, RouteExtractionStatus status) {
        return (int) extractions.stream()
                .filter(extraction -> extraction.extractionStatus() == status)
                .count();
    }

    private String routeKey(List<Destination> route) {
        return String.join("|", route.stream().map(Destination::name).toList());
    }

    @FunctionalInterface
    public interface RouteSyntacticAnalysisFactory {
        ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis create(List<RouteCluster> clusters);
    }
}
