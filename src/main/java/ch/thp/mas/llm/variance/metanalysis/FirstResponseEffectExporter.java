package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.RunLogEntryStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FirstResponseEffectExporter {

    private final Function<String, NamedRunLog> runLogReader;
    private final ObjectMapper objectMapper;

    @Autowired
    public FirstResponseEffectExporter(RunLogReader runLogReader) {
        this(runLogReader::read, new ObjectMapper().findAndRegisterModules());
    }

    FirstResponseEffectExporter(Function<String, NamedRunLog> runLogReader) {
        this(runLogReader, new ObjectMapper().findAndRegisterModules());
    }

    FirstResponseEffectExporter(Function<String, NamedRunLog> runLogReader, ObjectMapper objectMapper) {
        this.runLogReader = runLogReader;
        this.objectMapper = objectMapper;
    }

    public List<FirstResponseEffectRow> exportRows(List<NamedAnalysisResult> analyses) {
        return analyses.stream()
                .map(this::withRunLog)
                .filter(this::isSelectedLocalBaseline)
                .map(this::exportRow)
                .sorted(Comparator.comparing(FirstResponseEffectRow::modelFamily)
                        .thenComparing(FirstResponseEffectRow::seriesId))
                .toList();
    }

    private NamedAnalysisRun withRunLog(NamedAnalysisResult namedAnalysis) {
        AnalysisResult analysis = namedAnalysis.analysisResult();
        return new NamedAnalysisRun(analysis, runLogReader.apply(analysis.sourceRun()).runLog());
    }

    private boolean isSelectedLocalBaseline(NamedAnalysisRun item) {
        String family = modelFamily(item.runLog().model());
        String planName = item.runLog().planName();
        return switch (family) {
            case "apertus", "qwen" -> normalized(planName).contains("-baseline");
            case "gpt-oss" -> normalized(planName).contains("reasoning-low-baseline");
            default -> false;
        };
    }

    private FirstResponseEffectRow exportRow(NamedAnalysisRun item) {
        AnalysisResult analysis = item.analysis();
        RunLog runLog = item.runLog();
        List<RunLogEntry> entries = runLog.repetitions().stream()
                .sorted(Comparator.comparingInt(RunLogEntry::index))
                .toList();
        List<String> responseKeys = entries.stream()
                .map(this::responseKey)
                .toList();
        Map<String, List<Integer>> positionsByResponse = positionsByResponse(responseKeys);
        String firstResponse = responseKeys.isEmpty() ? "" : responseKeys.getFirst();
        List<String> rest = responseKeys.size() <= 1 ? List.of() : responseKeys.subList(1, responseKeys.size());
        int restUniqueCount = (int) rest.stream().distinct().count();
        String dominantResponse = dominantResponse(positionsByResponse);
        TtftSummary ttft = ttftSummary(entries);

        return new FirstResponseEffectRow(
                runLog.planName(),
                runLog.inferenceProvider(),
                runLog.model(),
                modelFamily(runLog.model()),
                analysis.config().promptEvaluation(),
                promptLanguage(analysis),
                setting(runLog.planName()),
                reasoning(runLog),
                runLog.iterations(),
                entries.size(),
                successCount(entries),
                entries.size() - successCount(entries),
                positionsByResponse.size(),
                classification(responseKeys, restUniqueCount),
                positionsByResponse.getOrDefault(firstResponse, List.of()).size(),
                positionsByResponse.getOrDefault(dominantResponse, List.of()).size(),
                restUniqueCount,
                ttft.first(),
                ttft.restP10(),
                ttft.restMedian(),
                ttft.restP90(),
                sha256(firstResponse),
                sha256(dominantResponse),
                variantSummary(positionsByResponse)
        );
    }

    private Map<String, List<Integer>> positionsByResponse(List<String> responses) {
        Map<String, List<Integer>> positionsByResponse = new LinkedHashMap<>();
        for (int i = 0; i < responses.size(); i++) {
            positionsByResponse.computeIfAbsent(responses.get(i), ignored -> new ArrayList<>()).add(i + 1);
        }
        return positionsByResponse;
    }

    private FirstResponseEffectClassification classification(List<String> responses, int restUniqueCount) {
        long uniqueCount = responses.stream().distinct().count();
        if (uniqueCount <= 1) {
            return FirstResponseEffectClassification.ALL_SAME;
        }
        if (responses.size() > 1 && !responses.getFirst().equals(responses.get(1)) && restUniqueCount == 1) {
            return FirstResponseEffectClassification.FIRST_DIFF_REST_SAME;
        }
        return FirstResponseEffectClassification.OTHER;
    }

    private String responseKey(RunLogEntry entry) {
        if (entry.status() == RunLogEntryStatus.SUCCESS) {
            return entry.response() == null ? "" : entry.response();
        }
        String errorType = entry.errorType() == null ? entry.status().name() : entry.errorType();
        return "<ERROR:" + errorType + ":" + entry.errorStatusCode() + ">";
    }

    private int successCount(List<RunLogEntry> entries) {
        return (int) entries.stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SUCCESS)
                .count();
    }

    private TtftSummary ttftSummary(List<RunLogEntry> entries) {
        Double first = entries.isEmpty() ? null : timeToFirstTokenSeconds(entries.getFirst());
        List<Double> rest = entries.size() <= 1 ? List.of() : entries.subList(1, entries.size()).stream()
                .map(this::timeToFirstTokenSeconds)
                .filter(value -> value != null)
                .toList();
        return new TtftSummary(first, percentile(rest, 0.1), median(rest), percentile(rest, 0.9));
    }

    private Double timeToFirstTokenSeconds(RunLogEntry entry) {
        String responseBody = entry.responseBody();
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody).findValue("time_to_first_token_seconds");
            return node == null || !node.isNumber() ? null : node.asDouble();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(percentile * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }

    private Double median(List<Double> values) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private String dominantResponse(Map<String, List<Integer>> positionsByResponse) {
        return positionsByResponse.entrySet().stream()
                .max(Comparator.<Map.Entry<String, List<Integer>>>comparingInt(entry -> entry.getValue().size())
                        .thenComparingInt(entry -> -entry.getValue().getFirst()))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String variantSummary(Map<String, List<Integer>> positionsByResponse) {
        int[] counter = {1};
        return positionsByResponse.entrySet().stream()
                .map(entry -> "v" + counter[0]++ + "=" + entry.getValue().size() + "@"
                        + ranges(entry.getValue()) + "#" + shortSha256(entry.getKey()))
                .collect(Collectors.joining(";"));
    }

    private String ranges(List<Integer> positions) {
        if (positions.isEmpty()) {
            return "";
        }
        List<String> ranges = new ArrayList<>();
        int start = positions.getFirst();
        int previous = start;
        for (int i = 1; i < positions.size(); i++) {
            int current = positions.get(i);
            if (current == previous + 1) {
                previous = current;
            } else {
                ranges.add(range(start, previous));
                start = current;
                previous = current;
            }
        }
        ranges.add(range(start, previous));
        return String.join("|", ranges);
    }

    private String range(int start, int end) {
        return start == end ? Integer.toString(start) : start + "-" + end;
    }

    private String promptLanguage(AnalysisResult analysis) {
        if (analysis.config().promptEvaluation() == PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP
                && analysis.config().swissRoundTrip() != null
                && analysis.config().swissRoundTrip().language() != null) {
            return analysis.config().swissRoundTrip().language().name();
        }
        return PromptLanguage.DE.name();
    }

    private String modelFamily(String model) {
        String normalized = normalized(model);
        if (normalized.contains("apertus")) {
            return "apertus";
        }
        if (normalized.contains("qwen")) {
            return "qwen";
        }
        if (normalized.contains("gpt-oss")) {
            return "gpt-oss";
        }
        return model == null ? "" : model;
    }

    private String setting(String planName) {
        String normalized = normalized(planName);
        if (normalized.contains("-hoch")) {
            return "hoch";
        }
        if (normalized.contains("-mittel")) {
            return "mittel";
        }
        return "baseline";
    }

    private String reasoning(RunLog runLog) {
        if (runLog.config() == null || runLog.config().reasoning() == null) {
            return "";
        }
        return runLog.config().reasoning().value();
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String shortSha256(String value) {
        String hash = sha256(value);
        return hash.isEmpty() ? "" : hash.substring(0, 12);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new MetaAnalysisException("Could not hash response.", e);
        }
    }

    private record NamedAnalysisRun(AnalysisResult analysis, RunLog runLog) {
    }

    private record TtftSummary(Double first, Double restP10, Double restMedian, Double restP90) {
    }
}
