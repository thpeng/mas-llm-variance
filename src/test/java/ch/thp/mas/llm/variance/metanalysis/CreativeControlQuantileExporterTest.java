package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.AnalysisConfig;
import ch.thp.mas.llm.variance.analyze.AnalysisResult;
import ch.thp.mas.llm.variance.analyze.AnalysisRunInfo;
import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.analyze.TextTokenizer;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextConfig;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextEvaluation;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextExtraction;
import ch.thp.mas.llm.variance.analyze.evaluation.creativegenerative.LucerneMarketingTextStatus;
import ch.thp.mas.llm.variance.analyze.literal.LiteralAnalysis;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.BleuMetric;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeConfig;
import ch.thp.mas.llm.variance.analyze.syntactic.RougeLMetric;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import ch.thp.mas.llm.variance.client.Reasoning;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreativeControlQuantileExporterTest {

    @Test
    void exportsCreativeGptAndApertusControlRowsWithP10MedianAndP90() {
        CreativeControlQuantileExporter exporter = new CreativeControlQuantileExporter(
                new RougeLMetric(new TextTokenizer()),
                new BleuMetric(new TextTokenizer())
        );

        List<CreativeControlQuantileRow> rows = exporter.exportRows(List.of(
                named("0020-openai-gpt54mini-creative-baseline", "gpt-5.4-mini-2026-03-17"),
                named("0310-lmstudio-apertus-creative-mittel", "swiss-ai_apertus-8b-instruct-2509"),
                named("0107-anthropic-sonnet46-creative-baseline", "claude-sonnet-4-6")
        ));

        assertThat(rows).extracting(CreativeControlQuantileRow::seriesId)
                .containsExactly(
                        "0020-openai-gpt54mini-creative-baseline",
                        "0310-lmstudio-apertus-creative-mittel"
                );
        assertThat(rows.getFirst().modelFamily()).isEqualTo("gpt-5.4-mini");
        assertThat(rows.getFirst().settingLabel()).isEqualTo("Basis");
        assertThat(rows.getFirst().plotOrder()).isEqualTo(6);
        assertThat(rows.getFirst().pairCount()).isEqualTo(3);
        assertThat(rows.getFirst().p10RougeDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().medianRougeDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().p90RougeDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().p10BleuDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().medianBleuDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().p90BleuDistance()).isEqualTo(0.0);
        assertThat(rows.getFirst().literalTop1Share()).isEqualTo(1.0);
    }

    private static NamedAnalysisResult named(String planName, String model) {
        return new NamedAnalysisResult(planName + ".json", analysis(planName, model));
    }

    private static AnalysisResult analysis(String planName, String model) {
        String response = "Luzern ist schön. Luzern liegt am See. Luzern lädt zum Reisen ein.";
        List<LucerneMarketingTextExtraction> extractions = List.of(
                extraction(1, response),
                extraction(2, response),
                extraction(3, response)
        );
        LucerneMarketingTextEvaluation evaluation = new LucerneMarketingTextEvaluation(
                3,
                3,
                0,
                1.0,
                List.of(),
                3,
                "Luzern",
                0,
                0,
                extractions,
                null
        );
        AnalysisConfig config = new AnalysisConfig(
                PromptEvaluation.CREATIVE_GENERATIVE_LUCERNE_MARKETING,
                null,
                null,
                null,
                new LucerneMarketingTextConfig(3, "Luzern"),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1)
        );
        AnalysisRunInfo run = new AnalysisRunInfo(
                planName,
                model.contains("apertus") ? InferenceProvider.LMSTUDIO : InferenceProvider.OPENAI,
                model,
                model,
                3,
                0.0,
                1.0,
                model.contains("apertus") ? 1 : null,
                null,
                Reasoning.OFF
        );
        return new AnalysisResult(
                "main_100_iterations/" + planName + ".json",
                OffsetDateTime.parse("2026-05-23T16:00:00+02:00"),
                config,
                run,
                null,
                null,
                null,
                evaluation,
                new LiteralAnalysis(true, 3, 1, 1.0)
        );
    }

    private static LucerneMarketingTextExtraction extraction(int responseIndex, String response) {
        return new LucerneMarketingTextExtraction(
                responseIndex,
                response,
                response,
                3,
                3,
                true,
                "Luzern",
                LucerneMarketingTextStatus.SUCCESS,
                List.of()
        );
    }
}
