package ch.thp.mas.llm.variance.metanalysis;

import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.MetricSummary;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripCluster;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.RunLogEntryStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class WordDriftAnalysisExporter {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");
    private static final List<String> GPT_4O_DRIFT_PLAN_NAMES = List.of(
            "0000-openai-gpt4o-20240513-roundtrip-de-baseline",
            "0001-openai-gpt4o-20240513-roundtrip-de-mittel",
            "0002-openai-gpt4o-20240806-roundtrip-de-baseline",
            "0003-openai-gpt4o-20241120-roundtrip-de-baseline"
    );

    private final RunLogReader runLogReader;

    public WordDriftAnalysisExporter(RunLogReader runLogReader) {
        this.runLogReader = runLogReader;
    }

    public List<WordDriftRow> exportRows(List<NamedAnalysisResult> analyses) {
        Map<String, AnalysisResult> byPlanName = analyses.stream()
                .map(NamedAnalysisResult::analysisResult)
                .collect(java.util.stream.Collectors.toMap(
                        analysis -> analysis.run().planName(),
                        analysis -> analysis
                ));
        List<WordDriftRow> rows = GPT_4O_DRIFT_PLAN_NAMES.stream()
                .filter(byPlanName::containsKey)
                .map(byPlanName::get)
                .map(this::exportRow)
                .toList();
        if (rows.size() != GPT_4O_DRIFT_PLAN_NAMES.size()) {
            List<String> missing = GPT_4O_DRIFT_PLAN_NAMES.stream()
                    .filter(planName -> !byPlanName.containsKey(planName))
                    .toList();
            throw new MetaAnalysisException("Missing hard-coded GPT-4o drift analyses: " + missing);
        }
        return rows;
    }

    private WordDriftRow exportRow(AnalysisResult analysis) {
        RunLog runLog = runLogReader.read(analysis.sourceRun()).runLog();
        List<RunLogEntry> successful = runLog.repetitions().stream()
                .filter(entry -> entry.status() == RunLogEntryStatus.SUCCESS)
                .filter(entry -> entry.response() != null)
                .toList();
        List<Integer> wordCounts = new ArrayList<>();
        Set<String> distinctWords = new TreeSet<>();
        for (RunLogEntry entry : successful) {
            List<String> words = words(entry.response());
            wordCounts.add(words.size());
            distinctWords.addAll(words);
        }
        return new WordDriftRow(
                runLog.planName(),
                runLog.inferenceProvider(),
                runLog.model(),
                runLog.modelVersion(),
                successful.size(),
                literalDistinctResponseCount(analysis),
                meanClusterSize(analysis),
                weightedMeanSyntacticDistance(analysis, DistanceMetric.ROUGE),
                weightedMeanSyntacticDistance(analysis, DistanceMetric.BLEU),
                distinctWords.size(),
                mean(wordCounts),
                nearestRank(wordCounts, 0.1),
                nearestRank(wordCounts, 0.9),
                String.join(" ", distinctWords)
        );
    }

    private List<String> words(String response) {
        List<String> words = new ArrayList<>();
        var matcher = WORD_PATTERN.matcher(response.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    private int literalDistinctResponseCount(AnalysisResult analysis) {
        return analysis.literal() == null ? 0 : analysis.literal().distinctResponseCount();
    }

    private Double meanClusterSize(AnalysisResult analysis) {
        if (analysis.swissRoundTrip() == null
                || analysis.swissRoundTrip().clusters() == null
                || analysis.swissRoundTrip().clusters().isEmpty()) {
            return null;
        }
        List<Integer> sizes = analysis.swissRoundTrip().clusters().stream()
                .map(SwissRoundTripCluster::size)
                .toList();
        return mean(sizes);
    }

    private Double weightedMeanSyntacticDistance(AnalysisResult analysis, DistanceMetric metric) {
        if (analysis.swissRoundTrip() == null) {
            return null;
        }
        SyntacticAnalysis syntactic = analysis.swissRoundTrip().syntactic();
        if (syntactic == null || syntactic.clusters() == null || syntactic.clusters().isEmpty()) {
            return null;
        }
        double weightedSum = 0.0;
        int pairCount = 0;
        for (SyntacticCluster cluster : syntactic.clusters()) {
            MetricSummary summary = metric == DistanceMetric.ROUGE ? cluster.rougeLDistance() : cluster.bleuDistance();
            if (summary != null && summary.mean() != null && cluster.pairCount() > 0) {
                weightedSum += summary.mean() * cluster.pairCount();
                pairCount += cluster.pairCount();
            }
        }
        if (pairCount == 0) {
            return null;
        }
        return round3(weightedSum / pairCount);
    }

    private Double mean(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        double sum = 0.0;
        for (int value : values) {
            sum += value;
        }
        return round3(sum / values.size());
    }

    private Integer nearestRank(List<Integer> values, double percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(percentile * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }

    private Double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private enum DistanceMetric {
        ROUGE,
        BLEU
    }
}
