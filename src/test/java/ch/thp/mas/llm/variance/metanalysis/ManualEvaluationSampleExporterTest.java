package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.NamedRunLog;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.PromptLanguage;
import ch.thp.mas.llm.variance.analyze.evaluation.advisoryrecommendation.SwissRoundTripConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManualEvaluationSampleExporterTest {

    @Test
    void exportsBlindManualEvaluationSample() {
        Map<String, RunLog> runLogs = Map.of(
                "runs/gemini.json", run(
                        "0201-google-gemini35flash-roundtrip-de-mittel",
                        "gemini-3.5-flash",
                        List.of("Zuerich\nLuzern", "Bern ist schoen")
                ),
                "runs/qwen.json", run(
                        "0401-lmstudio-qwen35-9b-roundtrip-de-mittel",
                        "qwen/qwen3.5-9b",
                        List.of("Eine sehr lange Antwort mit vielen Worten, die umgebrochen werden soll, weil sie sonst "
                                + "im JSON nur schwer lesbar waere.", "zweite Antwort")
                ),
                "runs/creative.json", run(
                        "0023-openai-gpt54mini-creative-mittel",
                        "gpt-5.4-mini-2026-03-17",
                        List.of("Luzern ist schoen. Die Stadt ist touristisch spannend. Apertus")
                ),
                "runs/ignored.json", run(
                        "0101-anthropic-sonnet46-roundtrip-de-mittel",
                        "claude-sonnet-4-6",
                        List.of("ignored")
                )
        );
        ManualEvaluationSampleExporter exporter = new ManualEvaluationSampleExporter(
                sourceRun -> new NamedRunLog(sourceRun, runLogs.get(sourceRun))
        );

        ManualEvaluationSampleExport export = exporter.exportSample(List.of(
                named("runs/gemini.json", "0201-google-gemini35flash-roundtrip-de-mittel", false),
                named("runs/qwen.json", "0401-lmstudio-qwen35-9b-roundtrip-de-mittel", false),
                named("runs/creative.json", "0023-openai-gpt54mini-creative-mittel", true),
                named("runs/ignored.json", "0101-anthropic-sonnet46-roundtrip-de-mittel", false)
        ));

        assertThat(export.roundTrip().itemCount()).isEqualTo(4);
        assertThat(export.creative().itemCount()).isEqualTo(1);
        assertThat(export.roundTrip().items()).extracting(ManualEvaluationSampleItem::sampleNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(export.roundTrip().items()).extracting(ManualEvaluationSampleItem::id)
                .allMatch(id -> id.startsWith("bms-"))
                .doesNotHaveDuplicates();
        assertThat(export.roundTrip().items()).flatExtracting(ManualEvaluationSampleItem::responseLines)
                .contains("Zuerich", "Luzern", "Bern ist schoen", "zweite Antwort");
        assertThat(export.roundTrip().items()).allSatisfy(item -> {
            assertThat(item.evaluation()).isInstanceOf(ManualRoundTripEvaluationFields.class);
            ManualRoundTripEvaluationFields evaluation = (ManualRoundTripEvaluationFields) item.evaluation();
            assertThat(evaluation.halluzination()).isNull();
        });
        assertThat(export.creative().items()).allSatisfy(item -> {
            assertThat(item.evaluation()).isInstanceOf(ManualCreativeEvaluationFields.class);
            ManualCreativeEvaluationFields evaluation = (ManualCreativeEvaluationFields) item.evaluation();
            assertThat(evaluation.tourismusbezug()).isNull();
            assertThat(evaluation.luzernbezug()).isNull();
            assertThat(evaluation.halluzination()).isNull();
        });
        assertThat(export.creative().items()).flatExtracting(ManualEvaluationSampleItem::responseLines)
                .contains("Luzern ist schoen. Die Stadt ist touristisch spannend. [REDACTED]");
    }

    private static NamedAnalysisResult named(String sourceRun, String planName, boolean creative) {
        return new NamedAnalysisResult(planName + ".json", analysis(sourceRun, planName, creative));
    }

    private static AnalysisResult analysis(String sourceRun, String planName, boolean creative) {
        AnalysisConfig config = creative
                ? new AnalysisConfig(
                        PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING,
                        null,
                        null,
                        null,
                        new LucerneMarketingTextConfig(3, "Luzern"),
                        new BleuConfig(4, 0.1),
                        new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
                )
                : new AnalysisConfig(
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
                100,
                0.7,
                0.4,
                20,
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
        List<RunLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            entries.add(new RunLogEntry(
                    i + 1,
                    OffsetDateTime.parse("2026-05-23T16:00:00+02:00"),
                    OffsetDateTime.parse("2026-05-23T16:00:01+02:00"),
                    responses.get(i),
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
                new RunConfigLog(0.7, 0.4, 20, null, Reasoning.OFF),
                "prompt",
                entries
        );
    }
}
