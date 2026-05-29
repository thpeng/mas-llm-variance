package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.RunLogEntryStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirstResponseEffectExporterTest {

    @Test
    void classifiesSelectedLocalBaselineRuns() {
        Map<String, RunLog> runLogs = Map.of(
                "runs/apertus-first.json", run(
                        "0300-lmstudio-apertus-roundtrip-de-baseline",
                        "swiss-ai_apertus-8b-instruct-2509",
                        List.of("warm", "stable", "stable")
                ),
                "runs/qwen-same.json", run(
                        "0400-lmstudio-qwen35-9b-roundtrip-de-baseline",
                        "qwen/qwen3.5-9b",
                        List.of("same", "same", "same")
                ),
                "runs/gptoss-other.json", run(
                        "0500-lmstudio-gptoss20b-roundtrip-de-reasoning-low-baseline",
                        "openai/gpt-oss-20b",
                        List.of("a", "b", "a")
                ),
                "runs/gptoss-medium.json", run(
                        "0508-lmstudio-gptoss20b-roundtrip-de-reasoning-medium-baseline",
                        "openai/gpt-oss-20b",
                        List.of("ignored", "ignored")
                )
        );
        FirstResponseEffectExporter exporter = new FirstResponseEffectExporter(
                sourceRun -> new NamedRunLog(sourceRun, runLogs.get(sourceRun))
        );

        List<FirstResponseEffectRow> rows = exporter.exportRows(List.of(
                named("runs/apertus-first.json", "0300-lmstudio-apertus-roundtrip-de-baseline"),
                named("runs/qwen-same.json", "0400-lmstudio-qwen35-9b-roundtrip-de-baseline"),
                named("runs/gptoss-other.json", "0500-lmstudio-gptoss20b-roundtrip-de-reasoning-low-baseline"),
                named("runs/gptoss-medium.json", "0508-lmstudio-gptoss20b-roundtrip-de-reasoning-medium-baseline")
        ));

        assertThat(rows).extracting(FirstResponseEffectRow::seriesId)
                .containsExactly(
                        "0300-lmstudio-apertus-roundtrip-de-baseline",
                        "0500-lmstudio-gptoss20b-roundtrip-de-reasoning-low-baseline",
                        "0400-lmstudio-qwen35-9b-roundtrip-de-baseline"
                );
        assertThat(rows.get(0).classification()).isEqualTo(FirstResponseEffectClassification.FIRST_DIFF_REST_SAME);
        assertThat(rows.get(0).firstResponseCount()).isEqualTo(1);
        assertThat(rows.get(0).dominantResponseCount()).isEqualTo(2);
        assertThat(rows.get(0).restUniqueCount()).isEqualTo(1);
        assertThat(rows.get(0).firstTtftSeconds()).isEqualTo(0.1);
        assertThat(rows.get(0).restTtftSecondsP10()).isEqualTo(0.2);
        assertThat(rows.get(0).restTtftSecondsMedian()).isEqualTo(0.25);
        assertThat(rows.get(0).restTtftSecondsP90()).isEqualTo(0.3);
        assertThat(rows.get(1).classification()).isEqualTo(FirstResponseEffectClassification.OTHER);
        assertThat(rows.get(2).classification()).isEqualTo(FirstResponseEffectClassification.ALL_SAME);
        assertThat(rows.get(2).literalUniqueCount()).isEqualTo(1);
    }

    private static NamedAnalysisResult named(String sourceRun, String planName) {
        return new NamedAnalysisResult(planName + ".json", analysis(sourceRun, planName));
    }

    private static AnalysisResult analysis(String sourceRun, String planName) {
        AnalysisConfig config = new AnalysisConfig(
                PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP,
                new SwissRoundTripConfig(5, PromptLanguage.DE),
                null,
                null,
                null,
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
        );
        AnalysisRunInfo run = new AnalysisRunInfo(
                planName,
                InferenceProvider.LMSTUDIO,
                "model",
                null,
                3,
                0.0,
                1.0,
                1,
                null,
                Reasoning.OFF
        );
        return new AnalysisResult(
                sourceRun,
                OffsetDateTime.parse("2026-05-23T16:00:00+02:00"),
                config,
                run,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static RunLog run(String planName, String model, List<String> responses) {
        List<RunLogEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            entries.add(new RunLogEntry(
                    i + 1,
                    OffsetDateTime.parse("2026-05-23T16:00:00+02:00"),
                    OffsetDateTime.parse("2026-05-23T16:00:01+02:00"),
                    RunLogEntryStatus.SUCCESS,
                    null,
                    null,
                    null,
                    null,
                    200,
                    null,
                    "{\"stats\":{\"time_to_first_token_seconds\":" + ((i + 1) / 10.0) + "}}",
                    responses.get(i),
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return new RunLog(
                planName,
                OffsetDateTime.parse("2026-05-23T16:00:00+02:00"),
                OffsetDateTime.parse("2026-05-23T16:00:01+02:00"),
                InferenceProvider.LMSTUDIO,
                model,
                null,
                null,
                responses.size(),
                new RunConfigLog(0.0, 1.0, 1, null, Reasoning.OFF),
                "prompt",
                entries
        );
    }
}
