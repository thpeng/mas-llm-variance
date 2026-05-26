package ch.thp.mas.llm.variance.metanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.analyze.PromptEvaluation;
import ch.thp.mas.llm.variance.client.InferenceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class BaselineScatterExporterTest {

    @Test
    void exportsOnlyBaselineRowsAndCollapsesGpt4oFamily() {
        BaselineScatterExporter exporter = new BaselineScatterExporter();

        List<BaselineScatterRow> rows = exporter.exportRows(List.of(
                row("0000-openai-gpt4o-20240513-roundtrip-de-baseline", "gpt-4o-2024-05-13", "baseline", 7, 100),
                row("0001-openai-gpt4o-20240513-roundtrip-de-mittel", "gpt-4o-2024-05-13", "mittel", 8, 100),
                row("0200-google-gemini35flash-roundtrip-de-baseline", "gemini-3.5-flash", "baseline", 3, 90)
        ));

        assertThat(rows).extracting(BaselineScatterRow::seriesId)
                .containsExactly(
                        "0000-openai-gpt4o-20240513-roundtrip-de-baseline",
                        "0200-google-gemini35flash-roundtrip-de-baseline"
                );
        assertThat(rows.getFirst().modelFamily()).isEqualTo("gpt-4o");
        assertThat(rows.getFirst().modelFamilyId()).isEqualTo(1);
        assertThat(rows.getFirst().archetypeId()).isEqualTo(1);
        assertThat(rows.getFirst().literalUniqueShare()).isEqualTo(0.07);
        assertThat(rows.getFirst().plotLiteralUniqueCount()).isEqualTo(6.165);
        assertThat(rows.getFirst().plotSemanticValidRate()).isEqualTo(0.9245);
        assertThat(rows.get(1).modelFamily()).isEqualTo("gemini-flash");
        assertThat(rows.get(1).modelFamilyId()).isEqualTo(4);
    }

    private static MetaAnalysisRow row(String seriesId, String model, String setting, int literalUniqueCount, int nSuccess) {
        return new MetaAnalysisRow(
                seriesId,
                InferenceProvider.OPENAI,
                model,
                null,
                PromptEvaluation.ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP,
                "DE",
                setting,
                0.0,
                1.0,
                1,
                null,
                "off",
                100,
                nSuccess,
                100 - nSuccess,
                0.95,
                0.05,
                literalUniqueCount,
                0.5,
                0.3,
                0.1,
                0.2,
                0.3,
                0.4,
                100,
                1.0,
                1.0,
                1.0,
                200,
                2.0,
                2.0,
                2.0,
                0,
                0.0,
                0.0,
                0.0,
                10.0,
                0.1,
                0.1,
                0.1
        );
    }
}
