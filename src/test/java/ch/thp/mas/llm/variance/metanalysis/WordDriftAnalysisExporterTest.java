package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.MetricSummary;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.Destination;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.JaccardSummary;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripCluster;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripEvaluation;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WordDriftAnalysisExporterTest {

    @Test
    void exportsDistinctLowercaseWordsAndPerResponseWordCounts() {
        RunLogReader runLogReader = Mockito.mock(RunLogReader.class);
        for (String planName : planNames()) {
            String sourceRun = "runs/" + planName + ".json";
            when(runLogReader.read(sourceRun)).thenReturn(new NamedRunLog(sourceRun, runLog(planName)));
        }
        WordDriftAnalysisExporter exporter = new WordDriftAnalysisExporter(runLogReader);

        List<WordDriftRow> rows = exporter.exportRows(analyses());

        assertThat(rows).hasSize(4);
        WordDriftRow row = rows.getFirst();
        assertThat(row.seriesId()).isEqualTo("0000-openai-gpt4o-20240513-roundtrip-de-baseline");
        assertThat(row.nSuccess()).isEqualTo(2);
        assertThat(row.literalDistinctResponseCount()).isEqualTo(2);
        assertThat(row.meanClusterSize()).isEqualTo(2.0);
        assertThat(row.meanRougeDistance()).isEqualTo(0.438);
        assertThat(row.meanBleuDistance()).isEqualTo(0.638);
        assertThat(row.distinctWordCount()).isEqualTo(5);
        assertThat(row.meanWordsPerResponse()).isEqualTo(3.5);
        assertThat(row.p10WordsPerResponse()).isEqualTo(3);
        assertThat(row.p90WordsPerResponse()).isEqualTo(4);
        assertThat(row.distinctWords()).isEqualTo("bern d eau luzern zurich");
    }

    @Test
    void failsWhenFiltersMatchNoRun() {
        RunLogReader runLogReader = Mockito.mock(RunLogReader.class);
        WordDriftAnalysisExporter exporter = new WordDriftAnalysisExporter(runLogReader);

        assertThatThrownBy(() -> exporter.exportRows(List.of()))
                .isInstanceOf(MetaAnalysisException.class)
                .hasMessageContaining("Missing hard-coded GPT-4o drift analyses");
    }

    private static List<NamedAnalysisResult> analyses() {
        return planNames().stream()
                .map(planName -> new NamedAnalysisResult("analysis/" + planName + ".json", analysisResult(planName)))
                .toList();
    }

    private static List<String> planNames() {
        return List.of(
                "0000-openai-gpt4o-20240513-roundtrip-de-baseline",
                "0001-openai-gpt4o-20240513-roundtrip-de-mittel",
                "0002-openai-gpt4o-20240806-roundtrip-de-baseline",
                "0003-openai-gpt4o-20241120-roundtrip-de-baseline"
        );
    }

    private static AnalysisResult analysisResult(String planName) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new AnalysisResult(
                "runs/" + planName + ".json",
                now,
                AnalysisConfig.defaults(),
                new AnalysisRunInfo(
                        planName,
                        InferenceProvider.OPENAI,
                        "gpt-4o-test",
                        "gpt-4o-test",
                        3,
                        0.0,
                        1.0,
                        null,
                        null,
                        Reasoning.OFF
                ),
                swissRoundTripEvaluation(),
                null,
                null,
                null,
                new LiteralAnalysis(false, 2, 2, 0.0)
        );
    }

    private static SwissRoundTripEvaluation swissRoundTripEvaluation() {
        List<SwissRoundTripCluster> clusters = List.of(
                new SwissRoundTripCluster(0, "A", List.of(Destination.ZURICH), 1, List.of(1), 0.25),
                new SwissRoundTripCluster(1, "B", List.of(Destination.LUCERNE), 3, List.of(2, 3, 4), 0.75)
        );
        SyntacticAnalysis syntactic = new SyntacticAnalysis(List.of(
                new SyntacticCluster(
                        0,
                        2,
                        new MetricSummary(2, 0.1, 0.2, 0.3, 0.4, 0.25),
                        new MetricSummary(2, 0.3, 0.4, 0.5, 0.6, 0.45)
                ),
                new SyntacticCluster(
                        1,
                        6,
                        new MetricSummary(6, 0.2, 0.4, 0.6, 0.8, 0.5),
                        new MetricSummary(6, 0.4, 0.6, 0.8, 1.0, 0.7)
                )
        ));
        return new SwissRoundTripEvaluation(
                4,
                4,
                0,
                0,
                0,
                2,
                0.75,
                List.of(),
                clusters,
                List.of(),
                List.of(),
                List.of(),
                new JaccardSummary(0, null, null, null, null, null, null),
                syntactic
        );
    }

    private static RunLog runLog(String planName) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new RunLog(
                planName,
                now,
                now.plusSeconds(3),
                InferenceProvider.OPENAI,
                "gpt-4o-test",
                "gpt-4o-test",
                null,
                null,
                3,
                new RunConfigLog(0.0, 1.0, null, null, Reasoning.OFF),
                "prompt",
                List.of(
                        new RunLogEntry(1, now, now.plusSeconds(1), "Zurich, Luzern! Zurich.", null),
                        new RunLogEntry(2, now.plusSeconds(1), now.plusSeconds(2), "Bern-Luzern d'Eau", null),
                        RunLogEntry.servingError(
                                3,
                                now.plusSeconds(2),
                                now.plusSeconds(3),
                                null,
                                "https://example.test",
                                Map.of(),
                                "{}",
                                500,
                                Map.of(),
                                "{}",
                                500,
                                "server error",
                                "{}"
                        )
                )
        );
    }
}
