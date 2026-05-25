package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.MetricSummary;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.RunLogReader;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.factualcritical.BernZurichConnectionEvaluation;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.SyntacticCluster;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.client.TokenUsage;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MetaAnalysisExporterTest {

    @Test
    void exportsRequestedCsvSummaryFields() {
        RunLogReader runLogReader = Mockito.mock(RunLogReader.class);
        RunLog runLog = runLog();
        when(runLogReader.read("main/run.json")).thenReturn(new NamedRunLog("main/run.json", runLog));
        MetaAnalysisExporter exporter = new MetaAnalysisExporter(runLogReader, objectMapper());

        List<MetaAnalysisRow> rows = exporter.exportRows(List.of(new NamedAnalysisResult(
                "main/analysis.json",
                analysisResult()
        )));

        assertThat(rows).hasSize(1);
        MetaAnalysisRow row = rows.getFirst();
        assertThat(row.seriesId()).isEqualTo("0007-openai-gpt-test-factual-baseline");
        assertThat(row.provider()).isEqualTo(InferenceProvider.OPENAI);
        assertThat(row.model()).isEqualTo("gpt-test");
        assertThat(row.modelVersion()).isEqualTo("gpt-test-version");
        assertThat(row.archetype()).isEqualTo(PromptEvaluation.FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION);
        assertThat(row.promptLanguage()).isEqualTo("DE");
        assertThat(row.setting()).isEqualTo("baseline");
        assertThat(row.temperature()).isEqualTo(0.0);
        assertThat(row.topP()).isEqualTo(1.0);
        assertThat(row.topK()).isEqualTo(1);
        assertThat(row.seed()).isEqualTo("1");
        assertThat(row.reasoning()).isEqualTo("off");
        assertThat(row.nRequested()).isEqualTo(3);
        assertThat(row.nSuccess()).isEqualTo(2);
        assertThat(row.nFailed()).isEqualTo(1);
        assertThat(row.semanticValidRate()).isEqualTo(0.5);
        assertThat(row.semanticOutlierRate()).isEqualTo(0.5);
        assertThat(row.literalUniqueCount()).isEqualTo(1);
        assertThat(row.literalTop1Share()).isEqualTo(1.0);
        assertThat(row.largestClusterShare()).isEqualTo(0.5);
        assertThat(row.medianRougeDistance()).isEqualTo(0.2);
        assertThat(row.p90RougeDistance()).isEqualTo(0.3);
        assertThat(row.medianBleuDistance()).isEqualTo(0.5);
        assertThat(row.p90BleuDistance()).isEqualTo(0.6);
        assertThat(row.inputTokensTotal()).isEqualTo(21);
        assertThat(row.inputTokensP10()).isEqualTo(10.0);
        assertThat(row.inputTokensMedian()).isEqualTo(10.5);
        assertThat(row.inputTokensP90()).isEqualTo(11.0);
        assertThat(row.outputTokensTotal()).isEqualTo(41);
        assertThat(row.outputTokensP10()).isEqualTo(20.0);
        assertThat(row.outputTokensMedian()).isEqualTo(20.5);
        assertThat(row.outputTokensP90()).isEqualTo(21.0);
        assertThat(row.reasoningTokensTotal()).isEqualTo(10);
        assertThat(row.reasoningTokensP10()).isEqualTo(0.0);
        assertThat(row.reasoningTokensMedian()).isEqualTo(3.0);
        assertThat(row.reasoningTokensP90()).isEqualTo(7.0);
        assertThat(row.durationSecondsTotal()).isEqualTo(9.0);
        assertThat(row.durationSecondsP10()).isEqualTo(2.0);
        assertThat(row.durationSecondsMedian()).isEqualTo(3.0);
        assertThat(row.durationSecondsP90()).isEqualTo(4.0);
    }

    private static AnalysisResult analysisResult() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        SyntacticAnalysis syntactic = new SyntacticAnalysis(List.of(new SyntacticCluster(
                0,
                1,
                new MetricSummary(1, 0.1, 0.2, 0.3, 0.4, 0.25),
                new MetricSummary(1, 0.4, 0.5, 0.6, 0.7, 0.55)
        )));
        return new AnalysisResult(
                "main/run.json",
                now,
                new AnalysisConfig(
                        PromptEvaluation.FACTUAL_CRITICAL_BERN_ZURICH_CONNECTION,
                        null,
                        new BernZurichConnectionConfig("08:02", "09:15", 0),
                        null,
                        null,
                        new BleuConfig(4, 0.1),
                        new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
                ),
                new AnalysisRunInfo(
                        "0007-openai-gpt-test-factual-baseline",
                        InferenceProvider.OPENAI,
                        "gpt-test",
                        "gpt-test-version",
                        3,
                        0.0,
                        1.0,
                        1,
                        1L,
                        Reasoning.OFF
                ),
                null,
                new BernZurichConnectionEvaluation(
                        2,
                        1,
                        1,
                        0.5,
                        List.of(2),
                        1,
                        1,
                        1,
                        Map.of(),
                        List.of(),
                        syntactic
                ),
                null,
                null,
                new LiteralAnalysis(false, 2, 1, 1.0)
        );
    }

    private static RunLog runLog() {
        OffsetDateTime started = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        List<RunLogEntry> repetitions = List.of(
                new RunLogEntry(
                        1,
                        started,
                        started.plusSeconds(2),
                        1L,
                        "https://example.test",
                        Map.of(),
                        "{}",
                        200,
                        Map.of(),
                        "{\"usage\":{\"output_tokens_details\":{\"reasoning_tokens\":3}}}",
                        "same",
                        new TokenUsage(10L, 20L, 30L)
                ),
                new RunLogEntry(
                        2,
                        started.plusSeconds(2),
                        started.plusSeconds(5),
                        1L,
                        "https://example.test",
                        Map.of(),
                        "{}",
                        200,
                        Map.of(),
                        "{}",
                        "same",
                        new TokenUsage(11L, 21L, 32L)
                ),
                RunLogEntry.servingError(
                        3,
                        started.plusSeconds(5),
                        started.plusSeconds(9),
                        1L,
                        "https://example.test",
                        Map.of(),
                        "{}",
                        500,
                        Map.of(),
                        "{\"usage\":{\"reasoning_output_tokens\":7}}",
                        500,
                        "server error",
                        "{}"
                )
        );
        return new RunLog(
                "0007-openai-gpt-test-factual-baseline",
                started,
                started.plusSeconds(9),
                InferenceProvider.OPENAI,
                "gpt-test",
                "gpt-test-version",
                null,
                null,
                3,
                new RunConfigLog(0.0, 1.0, 1, 1L, Reasoning.OFF),
                "prompt",
                repetitions
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
