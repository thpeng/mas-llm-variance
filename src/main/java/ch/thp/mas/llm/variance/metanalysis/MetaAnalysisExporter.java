package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.client.TokenUsage;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.RunLogEntryStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetaAnalysisExporter {

    private final RunLogReader runLogReader;
    private final ObjectMapper objectMapper;

    @Autowired
    public MetaAnalysisExporter(RunLogReader runLogReader) {
        this(runLogReader, new ObjectMapper().findAndRegisterModules());
    }

    MetaAnalysisExporter(RunLogReader runLogReader, ObjectMapper objectMapper) {
        this.runLogReader = runLogReader;
        this.objectMapper = objectMapper;
    }

    public List<MetaAnalysisRow> exportRows(List<NamedAnalysisResult> analyses) {
        return analyses.stream()
                .map(this::exportRow)
                .toList();
    }

    private MetaAnalysisRow exportRow(NamedAnalysisResult namedAnalysis) {
        AnalysisResult analysis = namedAnalysis.analysisResult();
        RunLog runLog = runLogReader.read(analysis.sourceRun()).runLog();
        int nSuccess = (int) runLog.repetitions().stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SUCCESS)
                .count();
        int nFailed = runLog.repetitions().size() - nSuccess;
        SemanticSummary semantic = semanticSummary(analysis);
        SyntacticSummary syntactic = syntacticSummary(analysis);
        TokenTotals tokens = tokenTotals(runLog.repetitions());
        DurationSummary durations = durationSummary(runLog.repetitions());

        return new MetaAnalysisRow(
                analysis.run().planName(),
                analysis.run().inferenceProvider(),
                analysis.run().model(),
                analysis.run().modelVersion(),
                analysis.config().promptEvaluation(),
                promptLanguage(analysis),
                setting(analysis.run().planName()),
                analysis.run().temperature(),
                analysis.run().topP(),
                analysis.run().topK(),
                seed(analysis),
                reasoning(analysis),
                analysis.run().iterations(),
                nSuccess,
                nFailed,
                semantic.validRate(),
                semantic.outlierRate(),
                analysis.literal() == null ? 0 : analysis.literal().distinctResponseCount(),
                literalTop1Share(runLog.repetitions()),
                semantic.largestClusterShare(),
                syntactic.medianRougeDistance(),
                syntactic.p90RougeDistance(),
                syntactic.medianBleuDistance(),
                syntactic.p90BleuDistance(),
                tokens.inputTokens(),
                tokens.inputTokensP10(),
                tokens.inputTokensMedian(),
                tokens.inputTokensP90(),
                tokens.outputTokens(),
                tokens.outputTokensP10(),
                tokens.outputTokensMedian(),
                tokens.outputTokensP90(),
                tokens.reasoningTokens(),
                tokens.reasoningTokensP10(),
                tokens.reasoningTokensMedian(),
                tokens.reasoningTokensP90(),
                durations.totalSeconds(),
                durations.p10Seconds(),
                durations.medianSeconds(),
                durations.p90Seconds()
        );
    }

    private SemanticSummary semanticSummary(AnalysisResult analysis) {
        if (analysis.swissRoundTrip() != null) {
            int count = analysis.swissRoundTrip().responseCount();
            return new SemanticSummary(
                    share(analysis.swissRoundTrip().successfulExtractionCount(), count),
                    share(analysis.swissRoundTrip().outliers().size(), count),
                    analysis.swissRoundTrip().topRoundTripShare()
            );
        }
        if (analysis.bernZurichConnection() != null) {
            int count = analysis.bernZurichConnection().responseCount();
            return new SemanticSummary(
                    analysis.bernZurichConnection().successShare(),
                    share(analysis.bernZurichConnection().outlierCount(), count),
                    largestShare(count, analysis.bernZurichConnection().successCount(),
                            analysis.bernZurichConnection().outlierCount())
            );
        }
        if (analysis.travelerGuidanceFormat() != null) {
            int count = analysis.travelerGuidanceFormat().responseCount();
            return new SemanticSummary(
                    analysis.travelerGuidanceFormat().normalizedAcceptedShare(),
                    share(analysis.travelerGuidanceFormat().noMatchCount(), count),
                    largestShare(count,
                            analysis.travelerGuidanceFormat().exactMatchCount(),
                            analysis.travelerGuidanceFormat().normalizedExactMatchCount(),
                            analysis.travelerGuidanceFormat().noMatchCount())
            );
        }
        if (analysis.lucerneMarketingText() != null) {
            int count = analysis.lucerneMarketingText().responseCount();
            return new SemanticSummary(
                    analysis.lucerneMarketingText().successShare(),
                    share(analysis.lucerneMarketingText().outlierCount(), count),
                    largestShare(count, analysis.lucerneMarketingText().successCount(),
                            analysis.lucerneMarketingText().outlierCount())
            );
        }
        return new SemanticSummary(null, null, null);
    }

    private SyntacticSummary syntacticSummary(AnalysisResult analysis) {
        SyntacticAnalysis syntactic = syntacticAnalysis(analysis);
        if (syntactic == null || syntactic.clusters() == null || syntactic.clusters().isEmpty()) {
            return new SyntacticSummary(null, null, null, null);
        }
        SyntacticCluster cluster = syntactic.clusters().stream()
                .max(Comparator.comparingInt(SyntacticCluster::pairCount))
                .orElse(null);
        if (cluster == null || cluster.rougeLDistance() == null || cluster.bleuDistance() == null) {
            return new SyntacticSummary(null, null, null, null);
        }
        return new SyntacticSummary(
                cluster.rougeLDistance().median(),
                cluster.rougeLDistance().p90(),
                cluster.bleuDistance().median(),
                cluster.bleuDistance().p90()
        );
    }

    private SyntacticAnalysis syntacticAnalysis(AnalysisResult analysis) {
        if (analysis.swissRoundTrip() != null) {
            return analysis.swissRoundTrip().syntactic();
        }
        if (analysis.bernZurichConnection() != null) {
            return analysis.bernZurichConnection().syntactic();
        }
        if (analysis.lucerneMarketingText() != null) {
            return analysis.lucerneMarketingText().syntactic();
        }
        return null;
    }

    private TokenTotals tokenTotals(List<RunLogEntry> entries) {
        long input = 0;
        long output = 0;
        long reasoning = 0;
        List<Double> inputValues = new ArrayList<>();
        List<Double> outputValues = new ArrayList<>();
        List<Double> reasoningValues = new ArrayList<>();
        for (RunLogEntry entry : entries) {
            TokenUsage tokenUsage = entry.tokenUsage();
            if (tokenUsage != null) {
                input += value(tokenUsage.inputTokens());
                output += value(tokenUsage.outputTokens());
                if (tokenUsage.inputTokens() != null) {
                    inputValues.add(tokenUsage.inputTokens().doubleValue());
                }
                if (tokenUsage.outputTokens() != null) {
                    outputValues.add(tokenUsage.outputTokens().doubleValue());
                }
            }
            long entryReasoningTokens = reasoningTokens(entry.responseBody());
            reasoning += entryReasoningTokens;
            reasoningValues.add((double) entryReasoningTokens);
        }
        return new TokenTotals(
                input,
                percentile(inputValues, 0.1),
                median(inputValues),
                percentile(inputValues, 0.9),
                output,
                percentile(outputValues, 0.1),
                median(outputValues),
                percentile(outputValues, 0.9),
                reasoning,
                percentile(reasoningValues, 0.1),
                median(reasoningValues),
                percentile(reasoningValues, 0.9)
        );
    }

    private long reasoningTokens(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return 0;
        }
        try {
            return reasoningTokens(objectMapper.readTree(responseBody));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long reasoningTokens(JsonNode node) {
        if (node == null) {
            return 0;
        }
        long total = 0;
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (isReasoningTokenField(field.getKey()) && field.getValue().isNumber()) {
                    total += field.getValue().asLong();
                } else {
                    total += reasoningTokens(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                total += reasoningTokens(child);
            }
        }
        return total;
    }

    private boolean isReasoningTokenField(String name) {
        return "reasoning_tokens".equals(name) || "reasoning_output_tokens".equals(name);
    }

    private DurationSummary durationSummary(List<RunLogEntry> entries) {
        long millis = 0;
        List<Double> values = new ArrayList<>();
        for (RunLogEntry entry : entries) {
            if (entry.startedAt() != null && entry.endedAt() != null) {
                long entryMillis = Duration.between(entry.startedAt(), entry.endedAt()).toMillis();
                millis += entryMillis;
                values.add(entryMillis / 1000.0);
            }
        }
        return new DurationSummary(
                millis / 1000.0,
                percentile(values, 0.1),
                median(values),
                percentile(values, 0.9)
        );
    }

    private Double literalTop1Share(List<RunLogEntry> entries) {
        List<String> responses = entries.stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SUCCESS)
                .map(RunLogEntry::response)
                .filter(Objects::nonNull)
                .toList();
        if (responses.isEmpty()) {
            return 0.0;
        }
        int topCount = responses.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values()
                .stream()
                .mapToInt(Long::intValue)
                .max()
                .orElse(0);
        return share(topCount, responses.size());
    }

    private String promptLanguage(AnalysisResult analysis) {
        if (analysis.config().swissRoundTrip() != null && analysis.config().swissRoundTrip().language() != null) {
            return analysis.config().swissRoundTrip().language().name();
        }
        return "DE";
    }

    private String setting(String planName) {
        String lower = planName.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("-mittel")) {
            return "mittel";
        }
        if (lower.contains("-hoch")) {
            return "hoch";
        }
        if (lower.contains("-baseline")) {
            return "baseline";
        }
        if (lower.contains("-nosampling")) {
            return "nosampling";
        }
        if (lower.contains("-reasoning-low")) {
            return "reasoning_low";
        }
        if (lower.contains("-reasoning-medium")) {
            return "reasoning_medium";
        }
        if (lower.contains("-reasoning-high")) {
            return "reasoning_high";
        }
        if (lower.contains("-reasoning-on")) {
            return "reasoning_on";
        }
        if (lower.contains("-seed-random")) {
            return "seed_random";
        }
        if (lower.contains("-seed-")) {
            return "seed";
        }
        return "unknown";
    }

    private String seed(AnalysisResult analysis) {
        if (analysis.run().seedSetting() != null && !analysis.run().seedSetting().isBlank()) {
            return analysis.run().seedSetting();
        }
        return analysis.run().seed() == null ? null : analysis.run().seed().toString();
    }

    private String reasoning(AnalysisResult analysis) {
        if (analysis.run().reasoningProviderValue() != null && !analysis.run().reasoningProviderValue().isBlank()) {
            return analysis.run().reasoningProviderValue();
        }
        return analysis.run().reasoning() == null ? null : analysis.run().reasoning().name().toLowerCase(java.util.Locale.ROOT);
    }

    private Double largestShare(int denominator, int... counts) {
        if (denominator == 0 || counts.length == 0) {
            return 0.0;
        }
        int max = 0;
        for (int count : counts) {
            max = Math.max(max, count);
        }
        return share(max, denominator);
    }

    private Double share(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private long value(Long value) {
        return value == null ? 0 : value;
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

    private record SemanticSummary(Double validRate, Double outlierRate, Double largestClusterShare) {
    }

    private record SyntacticSummary(
            Double medianRougeDistance,
            Double p90RougeDistance,
            Double medianBleuDistance,
            Double p90BleuDistance
    ) {
    }

    private record TokenTotals(
            long inputTokens,
            Double inputTokensP10,
            Double inputTokensMedian,
            Double inputTokensP90,
            long outputTokens,
            Double outputTokensP10,
            Double outputTokensMedian,
            Double outputTokensP90,
            long reasoningTokens,
            Double reasoningTokensP10,
            Double reasoningTokensMedian,
            Double reasoningTokensP90
    ) {
    }

    private record DurationSummary(Double totalSeconds, Double p10Seconds, Double medianSeconds, Double p90Seconds) {
    }
}
